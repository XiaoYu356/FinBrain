import logging
import sys
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional
import asyncio
import time

from agents import agent_graph
from rag.milvus_store import MilvusStore
from rag.document_loader import DocumentLoader
from config import get_settings

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)

logger = logging.getLogger("finbrain-ai")

app = FastAPI(
    title="FinBrain AI Service",
    description="金融智能顾问AI服务",
    version="1.0.0"
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
    intent: Optional[str] = None
    tool_used: Optional[str] = None


@app.get("/health")
async def health_check():
    logger.debug("健康检查请求")
    return {"status": "healthy", "service": "finbrain-ai"}


@app.on_event("startup")
async def startup_event():
    logger.info("🚀 FinBrain AI Service 启动成功!")
    logger.info("📡 服务地址: http://0.0.0.0:8001")
    logger.info("📖 API文档: http://0.0.0.0:8001/docs")
    logger.info("💡 健康检查: http://0.0.0.0:8001/health")


@app.on_event("shutdown")
async def shutdown_event():
    logger.info("👋 FinBrain AI Service 正在关闭...")


@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    start_time = time.time()
    logger.info(f"收到聊天请求 - 用户ID: {request.user_id}, 会话ID: {request.session_id}")
    logger.info(f"用户消息: {request.message[:100]}{'...' if len(request.message) > 100 else ''}")
    
    try:
        initial_state = {
            "messages": [],
            "user_id": request.user_id,
            "session_id": request.session_id or "default",
            "current_message": request.message,
            "intent": None,
            "tool_name": None,
            "tool_args": None,
            "tool_result": None,
            "rag_context": None,
            "response": None
        }
        
        logger.debug("开始执行 Agent Graph...")
        result = await agent_graph.ainvoke(initial_state)
        
        intent = result.get("intent")
        tool_used = result.get("tool_name")
        
        if intent:
            logger.info(f"识别意图: {intent}")
        if tool_used:
            logger.info(f"调用工具: {tool_used}")
        
        elapsed_time = time.time() - start_time
        logger.info(f"请求处理完成 - 耗时: {elapsed_time:.2f}秒")
        
        return ChatResponse(
            response=result.get("response", "抱歉，我无法处理您的请求。"),
            intent=intent,
            tool_used=tool_used
        )
    
    except Exception as e:
        logger.error(f"处理请求时发生错误: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/rag/load")
async def load_rag_documents():
    logger.info("开始加载RAG文档...")
    
    try:
        store = MilvusStore()
        loader = DocumentLoader()
        
        logger.info("创建Milvus集合...")
        store.create_collection(dimension=1536)
        
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
