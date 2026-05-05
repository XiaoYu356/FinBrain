import logging
import sys
import os
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List
import asyncio
import time

from config import get_settings
settings = get_settings()
os.environ['HF_ENDPOINT'] = settings.HF_ENDPOINT

from agents import agent_graph, create_initial_state
from agents.memory import memory_manager
from rag.milvus_store import MilvusStore
from rag.document_loader import DocumentLoader
from rag.reranker import get_reranker
from java_client import java_client
from utils.redis_client import redis_memory_store, close_redis

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)

logger = logging.getLogger("finbrain-ai")

logger.info(f"使用 HuggingFace 镜像: {settings.HF_ENDPOINT}")
logger.info(f"重排序模型: {settings.RERANKER_MODEL}")

app = FastAPI(
    title="FinBrain AI Service",
    description="金融智能顾问AI服务 - 基于ReAct架构，支持Redis持久化",
    version="2.1.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

logger.info("=" * 50)
logger.info("FinBrain AI Service 初始化中...")
logger.info("=" * 50)


class ChatRequest(BaseModel):
    message: str
    user_id: int
    session_id: Optional[str] = None


class ChatResponse(BaseModel):
    response: str
    tool_calls: Optional[List[dict]] = None
    steps: Optional[int] = None


class AddDocumentRequest(BaseModel):
    content: str
    metadata: Optional[dict] = None


class SearchRequest(BaseModel):
    query: str
    top_k: Optional[int] = 5


@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "finbrain-ai", "version": "2.1.0"}


@app.delete("/session")
async def delete_session(user_id: int, session_id: str):
    logger.info(f"收到删除会话记忆请求 - 用户ID: {user_id}, 会话ID: {session_id}")
    
    try:
        memory_manager.clear_memory(user_id, session_id)
        logger.info(f"内存缓存已清除 - 用户ID: {user_id}, 会话ID: {session_id}")
        
        if redis_memory_store:
            result = await redis_memory_store.clear_session(user_id, session_id)
            if result:
                logger.info(f"Redis 数据已清除 - 用户ID: {user_id}, 会话ID: {session_id}")
            else:
                logger.warning(f"Redis 数据清除可能失败 - 用户ID: {user_id}, 会话ID: {session_id}")
        
        return {"success": True, "message": "会话记忆删除成功"}
    
    except Exception as e:
        logger.error(f"删除会话记忆失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.on_event("startup")
async def startup_event():
    memory_manager.set_redis_store(redis_memory_store)
    memory_manager.set_mysql_client(java_client)
    
    if settings.RERANKER_PRELOAD:
        logger.info("=" * 50)
        logger.info("开始预加载模型...")
        logger.info("=" * 50)
        
        try:
            logger.info("正在预加载重排序模型 (CrossEncoder)...")
            reranker = get_reranker(use_llm=False)
            logger.info(f"重排序器实例创建成功: {type(reranker).__name__}")
            
            if hasattr(reranker, 'preload'):
                logger.info("开始调用 preload 方法...")
                reranker.preload()
                logger.info("✅ 重排序模型预加载成功")
            else:
                logger.info("✅ 重排序模型已就绪")
        except Exception as e:
            logger.error(f"❌ 重排序模型预加载失败: {e}", exc_info=True)
            logger.warning("将在首次使用时加载模型")
        
        logger.info("=" * 50)
    else:
        logger.info("重排序模型预加载已禁用，将在首次使用时加载")
    
    logger.info("🚀 FinBrain AI Service 启动成功!")
    logger.info("📡 服务地址: http://0.0.0.0:8001")
    logger.info("📖 API文档: http://0.0.0.0:8001/docs")
    logger.info("💾 Redis持久化已启用")
    logger.info("🗄️ MySQL持久化已启用")
    logger.info("=" * 50)


@app.on_event("shutdown")
async def shutdown_event():
    logger.info("正在保存所有会话记忆...")
    results = await memory_manager.save_all_memories()
    logger.info(f"保存结果 - Redis: {results['redis_success']}成功/{results['redis_failed']}失败, "
                f"MySQL: {results['mysql_success']}成功/{results['mysql_failed']}失败")
    await close_redis()
    logger.info("👋 FinBrain AI Service 已关闭")


@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    start_time = time.time()
    logger.info(f"收到聊天请求 - 用户ID: {request.user_id}, 会话ID: {request.session_id}")
    logger.info(f"用户消息: {request.message[:100]}{'...' if len(request.message) > 100 else ''}")
    
    try:
        session_id = request.session_id or "default"
        memory = await memory_manager.get_memory(request.user_id, session_id)
        
        try:
            history_result = await java_client.get_chat_history(
                request.user_id, 
                session_id
            )
            if history_result.get("code") == 200:
                history_data = history_result.get("data", [])
                if history_data and not memory.messages:
                    memory = await memory_manager.load_memory_from_external(
                        request.user_id,
                        session_id,
                        history_data
                    )
                    logger.info(f"从外部加载历史消息: {len(memory.messages)} 条")
        except Exception as e:
            logger.warning(f"获取历史记录失败: {e}")
        
        memory.add_message("user", request.message)
        
        initial_state = create_initial_state(
            user_id=request.user_id,
            current_message=request.message,
            session_id=session_id,
            messages=[m.to_dict() for m in memory.messages]
        )
        
        logger.info("开始执行 ReAct Agent Graph...")
        result = await agent_graph.ainvoke(initial_state)
        
        response_text = result.get("response", "抱歉，我无法处理您的请求。")
        memory.add_message("assistant", response_text)
        
        tool_history = result.get("tool_history", [])
        step_count = result.get("step_count", 0)
        
        tool_calls = []
        for record in tool_history:
            tool_calls.append({
                "tool": record.get("action"),
                "input": record.get("action_input"),
                "success": record.get("observation", {}).get("success", False) if isinstance(record.get("observation"), dict) else True
            })
        
        if tool_calls:
            logger.info(f"工具调用: {[tc['tool'] for tc in tool_calls]}")
        
        user_prefs = memory.user_preferences
        if user_prefs:
            logger.info(f"用户偏好: {user_prefs}")
        
        save_results = await memory_manager.save_memory(request.user_id, session_id)
        if not save_results["redis"]:
            logger.warning("Redis 保存失败，数据仅保存在内存中")
        if not save_results["mysql"]:
            logger.warning("MySQL 保存失败，消息可能丢失")
        
        elapsed_time = time.time() - start_time
        logger.info(f"请求处理完成 - 步数: {step_count}, 耗时: {elapsed_time:.2f}秒")
        
        return ChatResponse(
            response=response_text,
            tool_calls=tool_calls if tool_calls else None,
            steps=step_count
        )
    
    except Exception as e:
        logger.error(f"处理请求时发生错误: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/rag/status")
async def get_rag_status():
    try:
        store = MilvusStore()
        count = store.get_document_count()
        return {
            "success": True,
            "collection_name": "finbrain_docs",
            "document_count": count,
            "status": "ready" if count > 0 else "empty"
        }
    except Exception as e:
        logger.error(f"获取RAG状态失败: {str(e)}", exc_info=True)
        return {
            "success": False,
            "error": str(e),
            "status": "error"
        }


@app.post("/rag/load")
async def load_rag_documents():
    logger.info("开始加载RAG文档...")
    
    try:
        store = MilvusStore()
        
        logger.info("创建Milvus集合...")
        store.create_collection(dimension=1536)
        
        count = store.get_document_count()
        logger.info(f"当前文档数量: {count}")
        
        if count > 0:
            logger.info(f"Milvus 已有 {count} 条文档，跳过加载")
            return {"success": True, "message": "文档已存在", "count": count}
        
        sample_docs = [
            {
                "content": "稳健型理财产品适合风险承受能力较低的投资者，主要投资于国债、央行票据等低风险资产，预期年化收益率通常在3%-5%之间。",
                "metadata": {"type": "product_info", "category": "stable"}
            },
            {
                "content": "进取型理财产品适合有一定投资经验和风险承受能力的投资者，可能投资股票、基金等高风险资产，预期年化收益率可达8%-15%，但也存在较大亏损风险。",
                "metadata": {"type": "product_info", "category": "aggressive"}
            },
            {
                "content": "定投是一种分散投资风险的方式，通过定期定额投资，可以平滑市场波动，降低投资成本，适合长期投资规划。",
                "metadata": {"type": "investment_knowledge", "category": "strategy"}
            },
            {
                "content": "风险测评是评估投资者风险承受能力的重要工具，通常包括年龄、收入、投资经验、风险偏好等多个维度，测评结果分为C1-C5五个等级。",
                "metadata": {"type": "risk_knowledge", "category": "assessment"}
            }
        ]
        
        logger.info(f"插入 {len(sample_docs)} 条文档...")
        store.insert_documents(sample_docs)
        
        logger.info(f"✅ RAG文档加载成功，共 {len(sample_docs)} 条")
        return {"success": True, "message": "文档加载成功", "count": len(sample_docs)}
    
    except Exception as e:
        logger.error(f"加载RAG文档失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/rag/documents")
async def get_documents(page: int = 1, page_size: int = 10):
    try:
        store = MilvusStore()
        result = store.get_all_documents(page=page, page_size=page_size)
        return {"success": True, "data": result}
    except Exception as e:
        logger.error(f"获取文档列表失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/rag/documents")
async def add_document(request: AddDocumentRequest):
    try:
        store = MilvusStore()
        store.create_collection(dimension=1536)
        
        doc = {
            "content": request.content,
            "metadata": request.metadata or {}
        }
        store.insert_documents([doc])
        
        logger.info(f"添加文档成功: {request.content[:50]}...")
        return {"success": True, "message": "文档添加成功"}
    except Exception as e:
        logger.error(f"添加文档失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/rag/documents/{doc_id}")
async def delete_document(doc_id: int):
    try:
        store = MilvusStore()
        success = store.delete_document(doc_id)
        
        if success:
            try:
                from rag.bm25_retriever import remove_document_from_bm25
                remove_document_from_bm25(doc_id)
                logger.info("BM25 索引已更新")
            except Exception as e:
                logger.warning(f"更新 BM25 索引失败: {e}")
            
            logger.info(f"删除文档成功: {doc_id}")
            return {"success": True, "message": "文档删除成功"}
        else:
            return {"success": False, "message": "文档不存在或删除失败"}
    except Exception as e:
        logger.error(f"删除文档失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/rag/search")
async def search_documents(request: SearchRequest):
    try:
        store = MilvusStore()
        results = store.search(request.query, top_k=request.top_k)
        
        return {"success": True, "data": results}
    except Exception as e:
        logger.error(f"检索文档失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


class ProcessDocumentRequest(BaseModel):
    document_id: int
    object_name: str
    bucket_name: str
    file_type: str


@app.post("/rag/process-document")
async def process_document(request: ProcessDocumentRequest):
    from rag.minio_client import get_document_content
    from rag.document_loader import DocumentLoader
    
    logger.info(f"开始处理文档: {request.document_id}, 类型: {request.file_type}")
    
    try:
        content = get_document_content(
            request.bucket_name,
            request.object_name,
            request.file_type
        )
        
        if not content or not content.strip():
            return {"success": False, "message": "文档内容为空"}
        
        loader = DocumentLoader()
        chunks = loader.load_from_string(content, {"document_id": request.document_id})
        
        store = MilvusStore()
        store.create_collection(dimension=1536)
        
        documents = []
        for i, chunk in enumerate(chunks):
            chunk["metadata"]["chunk_index"] = i
            chunk["metadata"]["file_type"] = request.file_type
            documents.append(chunk)
        
        store.insert_documents(documents)
        
        try:
            from rag.bm25_retriever import rebuild_bm25_index
            rebuild_bm25_index()
            logger.info("BM25 索引已重建")
        except Exception as e:
            logger.warning(f"更新 BM25 索引失败: {e}")
        
        logger.info(f"文档处理完成: {request.document_id}, 切块数: {len(chunks)}")
        return {"success": True, "chunk_count": len(chunks)}
    
    except Exception as e:
        logger.error(f"处理文档失败: {str(e)}", exc_info=True)
        return {"success": False, "message": str(e)}


class DeleteDocumentRequest(BaseModel):
    document_id: int


class DeleteDocumentsRequest(BaseModel):
    document_ids: List[int]


@app.post("/rag/delete-document")
async def delete_document_vectors(request: DeleteDocumentRequest):
    try:
        store = MilvusStore()
        success = store.delete_document(request.document_id)
        
        if success:
            try:
                from rag.bm25_retriever import remove_document_from_bm25
                remove_document_from_bm25(request.document_id)
                logger.info("BM25 索引已更新")
            except Exception as e:
                logger.warning(f"更新 BM25 索引失败: {e}")
            
            logger.info(f"向量删除成功: {request.document_id}")
            return {"success": True, "message": "向量删除成功"}
        else:
            return {"success": False, "message": "删除失败"}
    except Exception as e:
        logger.error(f"删除向量失败: {str(e)}", exc_info=True)
        return {"success": False, "message": str(e)}


@app.post("/rag/delete-documents")
async def delete_documents_vectors(request: DeleteDocumentsRequest):
    try:
        store = MilvusStore()
        result = store.delete_documents(request.document_ids)
        
        if result["success"]:
            try:
                from rag.bm25_retriever import remove_document_from_bm25
                for doc_id in request.document_ids:
                    remove_document_from_bm25(doc_id)
                logger.info("BM25 索引已更新")
            except Exception as e:
                logger.warning(f"更新 BM25 索引失败: {e}")
            
            logger.info(f"批量删除向量成功: {len(request.document_ids)} 个文档")
            return {"success": True, "message": f"成功删除 {result['deleted_count']} 个向量", "deleted_count": result['deleted_count']}
        else:
            return {"success": False, "message": "删除失败"}
    except Exception as e:
        logger.error(f"批量删除向量失败: {str(e)}", exc_info=True)
        return {"success": False, "message": str(e)}


@app.get("/rag/evaluation/test-doc")
async def get_test_document():
    from rag.evaluator import get_knowledge_doc
    
    return {
        "success": True,
        "data": {
            "filename": "金融理财产品知识手册.md",
            "content": get_knowledge_doc()
        }
    }


@app.get("/rag/evaluation/test-docs")
async def get_test_doc_list():
    from rag.evaluator import get_test_doc_list
    
    return {
        "success": True,
        "data": {
            "docs": get_test_doc_list(),
            "local_path": "d:\\code\\FinBrain\\测试文档\\",
            "total": len(get_test_doc_list())
        }
    }


@app.get("/rag/evaluation/test-cases")
async def get_test_cases():
    from rag.evaluator import get_test_cases
    
    cases = get_test_cases()
    return {
        "success": True,
        "data": {
            "total": len(cases),
            "cases": [
                {
                    "question": c.question,
                    "ground_truth": c.ground_truth,
                    "relevant_docs": c.relevant_docs
                }
                for c in cases
            ]
        }
    }


class EvaluateRequest(BaseModel):
    top_k: int = 5
    save_record: bool = True
    config: dict = {}
    retrieval_type: str = "vector_only"
    vector_top_k: int = 20
    keyword_top_k: int = 20
    fusion_weights: Optional[dict] = None
    rerank_enabled: bool = False
    rerank_top_k: int = 5


@app.post("/rag/evaluation/run")
async def run_evaluation(request: EvaluateRequest):
    from rag.evaluator import (
        get_test_cases, EvaluationRecord, 
        save_evaluation_record, TestCase
    )
    from rag.retriever import Retriever
    from datetime import datetime
    
    try:
        test_cases = get_test_cases()
        retriever = Retriever()
        
        import re
        def normalize(text):
            return re.sub(r'[^\w\u4e00-\u9fff]', '', text).lower()
        
        def is_relevant(rel_doc, ret_content):
            norm_rel = normalize(rel_doc)
            norm_ret = normalize(ret_content)
            
            if norm_rel in norm_ret:
                return True
            
            rel_chars = list(norm_rel)
            matched = sum(1 for c in rel_chars if c in norm_ret)
            return matched >= len(rel_chars) * 0.7
        
        results = []
        total_precision = 0
        total_recall = 0
        
        for case in test_cases:
            retrieved_docs = retriever.retrieve(
                case.question,
                top_k=request.top_k,
                retrieval_type=request.retrieval_type,
                vector_top_k=request.vector_top_k,
                keyword_top_k=request.keyword_top_k,
                fusion_weights=request.fusion_weights,
                rerank_enabled=request.rerank_enabled,
                rerank_top_k=request.rerank_top_k
            )
            retrieved_contents = [doc.get("content", "") for doc in retrieved_docs]
            
            relevant_docs_found = 0
            for rel_doc in case.relevant_docs:
                for ret_content in retrieved_contents:
                    if is_relevant(rel_doc, ret_content):
                        relevant_docs_found += 1
                        break
            
            relevant_retrieved_count = 0
            for ret_content in retrieved_contents:
                for rel_doc in case.relevant_docs:
                    if is_relevant(rel_doc, ret_content):
                        relevant_retrieved_count += 1
                        break
            
            precision = relevant_retrieved_count / len(retrieved_contents) if retrieved_contents else 0
            recall = relevant_docs_found / len(case.relevant_docs) if case.relevant_docs else 0
            
            total_precision += precision
            total_recall += recall
            
            results.append({
                "question": case.question,
                "precision": precision,
                "recall": recall,
                "retrieved_count": len(retrieved_contents),
                "relevant_found": relevant_docs_found
            })
        
        n = len(test_cases)
        avg_precision = total_precision / n if n > 0 else 0
        avg_recall = total_recall / n if n > 0 else 0
        f1 = 2 * avg_precision * avg_recall / (avg_precision + avg_recall) if (avg_precision + avg_recall) > 0 else 0
        
        evaluation_data = {
            "timestamp": datetime.now().isoformat(),
            "precision": round(avg_precision, 4),
            "recall": round(avg_recall, 4),
            "f1_score": round(f1, 4),
            "faithfulness": 0,
            "answer_relevancy": 0,
            "context_recall": round(avg_recall, 4),
            "context_precision": round(avg_precision, 4),
            "config": {
                **request.config,
                "retrieval_type": request.retrieval_type,
                "vector_top_k": request.vector_top_k,
                "keyword_top_k": request.keyword_top_k,
                "fusion_weights": request.fusion_weights,
                "rerank_enabled": request.rerank_enabled,
                "rerank_top_k": request.rerank_top_k
            },
            "details": results
        }
        
        if request.save_record:
            record = EvaluationRecord(**evaluation_data)
            save_evaluation_record(record)
        
        return {
            "success": True,
            "data": evaluation_data
        }
    except Exception as e:
        logger.error(f"评估失败: {str(e)}", exc_info=True)
        return {"success": False, "message": str(e)}


@app.get("/rag/evaluation/history")
async def get_evaluation_history():
    from rag.evaluator import load_evaluation_history
    
    history = load_evaluation_history()
    return {
        "success": True,
        "data": history
    }


@app.post("/rag/evaluation/compare")
async def compare_evaluations(baseline_id: int, current_id: int):
    from rag.evaluator import load_evaluation_history, compare_evaluations
    
    history = load_evaluation_history()
    
    if baseline_id >= len(history) or current_id >= len(history):
        return {"success": False, "message": "无效的评估记录ID"}
    
    result = compare_evaluations(history[baseline_id], history[current_id])
    return {
        "success": True,
        "data": result
    }


@app.post("/rag/bm25/rebuild")
async def rebuild_bm25():
    try:
        from rag.bm25_retriever import rebuild_bm25_index
        bm25 = rebuild_bm25_index()
        return {
            "success": True,
            "message": "BM25 索引重建成功",
            "doc_count": len(bm25.doc_ids),
            "term_count": len(bm25.df)
        }
    except Exception as e:
        logger.error(f"重建 BM25 索引失败: {e}", exc_info=True)
        return {"success": False, "message": str(e)}


@app.get("/rag/bm25/status")
async def get_bm25_status():
    try:
        from rag.bm25_retriever import get_bm25
        bm25 = get_bm25()
        return {
            "success": True,
            "data": {
                "initialized": bm25._initialized,
                "doc_count": len(bm25.doc_ids),
                "term_count": len(bm25.df),
                "avgdl": bm25.avgdl
            }
        }
    except Exception as e:
        return {"success": False, "message": str(e)}


@app.delete("/rag/bm25/clear")
async def clear_bm25():
    try:
        from rag.bm25_retriever import get_bm25_redis_storage
        storage = get_bm25_redis_storage()
        storage.clear_index()
        return {"success": True, "message": "BM25 索引已清除"}
    except Exception as e:
        return {"success": False, "message": str(e)}


@app.delete("/rag/vectors/clear")
async def clear_all_vectors():
    try:
        from rag.milvus_store import MilvusStore
        from rag.bm25_retriever import get_bm25_redis_storage
        
        store = MilvusStore()
        store.clear_all()
        
        storage = get_bm25_redis_storage()
        storage.clear_index()
        
        return {"success": True, "message": "所有向量数据和BM25索引已清除"}
    except Exception as e:
        logger.error(f"清除向量数据失败: {e}", exc_info=True)
        return {"success": False, "message": str(e)}


@app.get("/rag/vectors/stats")
async def get_vector_stats():
    try:
        from rag.milvus_store import MilvusStore
        store = MilvusStore()
        stats = store.get_stats()
        return {"success": True, "data": stats}
    except Exception as e:
        return {"success": False, "message": str(e)}


if __name__ == "__main__":
    import uvicorn
    settings = get_settings()
    logger.info("启动 Uvicorn 服务器...")
    uvicorn.run(
        app, 
        host="0.0.0.0", 
        port=8001,
        log_level="info"
    )
