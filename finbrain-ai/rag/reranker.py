from typing import List, Dict, Optional, Tuple
import numpy as np
import logging

logger = logging.getLogger(__name__)


class CrossEncoderReranker:
    def __init__(self, model_name: str = None):
        if model_name is None:
            from config import get_settings
            settings = get_settings()
            model_name = settings.RERANKER_MODEL
        
        self.model_name = model_name
        self._model = None
        logger.info(f"初始化 CrossEncoder 重排序器 (模型: {model_name})")
    
    def _load_model(self):
        if self._model is None:
            logger.info(f"正在加载 CrossEncoder 模型: {self.model_name}")
            try:
                from sentence_transformers import CrossEncoder
                logger.info("导入 CrossEncoder 成功，开始初始化模型...")
                self._model = CrossEncoder(self.model_name)
                logger.info(f"✅ CrossEncoder 模型加载成功")
            except ImportError as e:
                logger.error(f"❌ 未安装 sentence-transformers: {e}")
                raise ImportError("请安装 sentence-transformers: pip install sentence-transformers")
            except Exception as e:
                logger.error(f"❌ CrossEncoder 模型加载失败: {e}", exc_info=True)
                raise
        else:
            logger.info("CrossEncoder 模型已加载，跳过重复加载")
        return self._model
    
    def preload(self):
        logger.info("预加载 CrossEncoder 模型...")
        return self._load_model()
    
    def rerank(
        self,
        query: str,
        documents: List[Dict],
        top_k: int = 3
    ) -> List[Dict]:
        if not documents:
            return []
        
        logger.info(f"CrossEncoder 重排序开始 - 文档数: {len(documents)}, 目标数: {top_k}")
        
        model = self._load_model()
        
        pairs = [[query, doc.get("content", "")] for doc in documents]
        
        logger.info(f"正在计算 {len(pairs)} 个文档对的相关性分数...")
        scores = model.predict(pairs)
        
        if isinstance(scores, np.ndarray):
            scores = scores.tolist()
        
        logger.info(f"相关性分数计算完成")
        
        scored_docs = list(zip(documents, scores))
        scored_docs.sort(key=lambda x: x[1], reverse=True)
        
        results = []
        for doc, score in scored_docs[:top_k]:
            result = dict(doc)
            result["rerank_score"] = float(score)
            results.append(result)
        
        logger.info(f"CrossEncoder 重排序完成，返回 top {top_k} 个结果")
        
        return results


class LLMReranker:
    def __init__(self):
        pass
    
    def rerank(
        self,
        query: str,
        documents: List[Dict],
        top_k: int = 3
    ) -> List[Dict]:
        if not documents:
            return []
        
        logger.info(f"LLM 重排序开始 - 文档数: {len(documents)}, 目标数: {top_k}")
        
        try:
            from llm.llm import get_llm
            
            llm = get_llm()
            
            doc_texts = []
            for i, doc in enumerate(documents):
                content = doc.get("content", "")[:500]
                doc_texts.append(f"[{i}] {content}")
            
            prompt = f"""请根据问题对以下文档进行相关性打分（0-10分），只返回分数最高的{top_k}个文档编号，用逗号分隔。

问题：{query}

文档：
{chr(10).join(doc_texts)}

请只返回文档编号，例如：0,2,5"""
            
            logger.info(f"正在调用 LLM 进行重排序...")
            response = llm.invoke(prompt)
            content = response.content if hasattr(response, 'content') else str(response)
            logger.info(f"LLM 响应: {content}")
            
            import re
            numbers = re.findall(r'\d+', content)
            selected_indices = [int(n) for n in numbers[:top_k] if int(n) < len(documents)]
            
            logger.info(f"解析出的文档索引: {selected_indices}")
            
            if not selected_indices:
                logger.warning("未能解析出有效的文档索引，返回前 top_k 个文档")
                return documents[:top_k]
            
            results = []
            for idx in selected_indices:
                result = dict(documents[idx])
                result["rerank_score"] = 1.0 - (len(results) * 0.1)
                results.append(result)
            
            logger.info(f"LLM 重排序完成，返回 {len(results)} 个结果")
            
            return results
            
        except Exception as e:
            logger.error(f"LLM 重排序失败: {str(e)}")
            return documents[:top_k]


_reranker_instance: Optional[CrossEncoderReranker] = None


def get_reranker(use_llm: bool = False) -> CrossEncoderReranker | LLMReranker:
    if use_llm:
        return LLMReranker()
    
    global _reranker_instance
    if _reranker_instance is None:
        _reranker_instance = CrossEncoderReranker()
    return _reranker_instance
