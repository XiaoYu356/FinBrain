from typing import List, Optional, Dict
import logging
from rag.milvus_store import MilvusStore
from llm.embeddings import QwenEmbeddings

logger = logging.getLogger(__name__)


class Retriever:
    def __init__(self, store: Optional[MilvusStore] = None):
        self.store = store or MilvusStore()
        self.embeddings = QwenEmbeddings()
    
    def retrieve(
        self,
        query: str,
        top_k: int = 5,
        retrieval_type: str = "vector_only",
        vector_top_k: int = 10,
        keyword_top_k: int = 10,
        fusion_weights: Optional[Dict[str, float]] = None,
        rerank_enabled: bool = False,
        rerank_top_k: int = 3,
        use_llm_rerank: bool = False
    ) -> List[dict]:
        logger.info(f"开始检索 - 类型: {retrieval_type}, 重排序: {rerank_enabled}")
        
        if retrieval_type == "vector_only":
            results = self._vector_search(query, top_k if not rerank_enabled else vector_top_k)
        elif retrieval_type == "keyword_only":
            results = self._keyword_search(query, top_k if not rerank_enabled else keyword_top_k)
        elif retrieval_type == "hybrid":
            results = self._hybrid_search(
                query,
                vector_top_k=vector_top_k,
                keyword_top_k=keyword_top_k,
                fusion_weights=fusion_weights,
                top_k=top_k if not rerank_enabled else max(vector_top_k, keyword_top_k)
            )
        else:
            results = self._vector_search(query, top_k)
        
        logger.info(f"检索完成，获得 {len(results)} 个文档")
        
        if rerank_enabled:
            logger.info(f"开始重排序，目标返回 top {rerank_top_k} 个结果")
            results = self._rerank(query, results, rerank_top_k, use_llm_rerank)
            logger.info(f"重排序完成，返回 {len(results)} 个文档")
        
        return results
    
    def _vector_search(self, query: str, top_k: int) -> List[dict]:
        results = self.store.search(query, top_k)
        return self._normalize_scores(results)
    
    def _keyword_search(self, query: str, top_k: int) -> List[dict]:
        from rag.bm25_retriever import get_bm25, init_bm25_from_milvus
        
        bm25 = get_bm25()
        if not bm25.documents:
            init_bm25_from_milvus()
        
        results = bm25.search(query, top_k)
        
        formatted_results = []
        for r in results:
            formatted_results.append({
                "content": r.get("content", ""),
                "score": r.get("score", 0),
                "metadata": "{}"
            })
        
        return self._normalize_scores(formatted_results)
    
    def _hybrid_search(
        self,
        query: str,
        vector_top_k: int = 10,
        keyword_top_k: int = 10,
        fusion_weights: Optional[Dict[str, float]] = None,
        top_k: int = 10
    ) -> List[dict]:
        if fusion_weights is None:
            fusion_weights = {"vector": 0.5, "keyword": 0.5}
        
        logger.info(f"开始混合检索 - 向量top_k={vector_top_k}, 关键词top_k={keyword_top_k}, 融合权重={fusion_weights}")
        
        logger.info("执行向量检索...")
        vector_results = self._vector_search(query, vector_top_k)
        logger.info(f"向量检索完成，获得 {len(vector_results)} 个结果")
        
        logger.info("执行关键词检索...")
        keyword_results = self._keyword_search(query, keyword_top_k)
        logger.info(f"关键词检索完成，获得 {len(keyword_results)} 个结果")
        
        merged = {}
        
        for doc in vector_results:
            content = doc.get("content", "")
            key = content[:100]
            merged[key] = {
                "content": content,
                "vector_score": doc.get("score", 0),
                "keyword_score": 0,
                "metadata": doc.get("metadata", "{}")
            }
        
        for doc in keyword_results:
            content = doc.get("content", "")
            key = content[:100]
            if key in merged:
                merged[key]["keyword_score"] = doc.get("score", 0)
            else:
                merged[key] = {
                    "content": content,
                    "vector_score": 0,
                    "keyword_score": doc.get("score", 0),
                    "metadata": doc.get("metadata", "{}")
                }
        
        logger.info(f"合并检索结果，去重后共 {len(merged)} 个文档")
        
        for key, doc in merged.items():
            doc["score"] = (
                doc["vector_score"] * fusion_weights.get("vector", 0.6) +
                doc["keyword_score"] * fusion_weights.get("keyword", 0.4)
            )
        
        results = list(merged.values())
        results.sort(key=lambda x: x["score"], reverse=True)
        
        logger.info(f"混合检索完成，返回 top {top_k} 个结果")
        for i, doc in enumerate(results[:top_k], 1):
            logger.info(f"  [{i}] 融合分数={doc['score']:.4f} (向量={doc['vector_score']:.4f}, 关键词={doc['keyword_score']:.4f})")
        
        return results[:top_k]
    
    def _normalize_scores(self, results: List[dict]) -> List[dict]:
        if not results:
            return results
        
        scores = [r.get("score", 0) for r in results]
        max_score = max(scores) if scores else 1
        min_score = min(scores) if scores else 0
        range_score = max_score - min_score if max_score != min_score else 1
        
        for r in results:
            original_score = r.get("score", 0)
            r["score"] = (original_score - min_score) / range_score
        
        return results
    
    def _rerank(
        self,
        query: str,
        documents: List[dict],
        top_k: int = 3,
        use_llm: bool = False
    ) -> List[dict]:
        if not documents:
            return []
        
        logger.info(f"使用 {'LLM' if use_llm else 'CrossEncoder'} 重排序器")
        
        from rag.reranker import get_reranker
        
        reranker = get_reranker(use_llm=use_llm)
        results = reranker.rerank(query, documents, top_k)
        
        logger.info(f"重排序结果:")
        for i, doc in enumerate(results, 1):
            rerank_score = doc.get('rerank_score', 0)
            logger.info(f"  [{i}] 重排序分数={rerank_score:.4f}")
        
        return results
    
    def get_context(self, query: str, top_k: int = 3) -> str:
        documents = self.retrieve(query, top_k=top_k)
        
        if not documents:
            return ""
        
        context_parts = []
        for i, doc in enumerate(documents, 1):
            context_parts.append(f"[文档{i}]\n{doc['content']}\n")
        
        return "\n".join(context_parts)
