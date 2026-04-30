from typing import List, Optional
from rag.milvus_store import MilvusStore
from llm.embeddings import QwenEmbeddings


class Retriever:
    def __init__(self, store: Optional[MilvusStore] = None):
        self.store = store or MilvusStore()
        self.embeddings = QwenEmbeddings()
    
    def retrieve(self, query: str, top_k: int = 5) -> List[dict]:
        results = self.store.search(query, top_k)
        return self._rerank(query, results)
    
    def _rerank(self, query: str, documents: List[dict]) -> List[dict]:
        return sorted(documents, key=lambda x: x.get("score", 0), reverse=True)
    
    def get_context(self, query: str, top_k: int = 3) -> str:
        documents = self.retrieve(query, top_k)
        
        if not documents:
            return ""
        
        context_parts = []
        for i, doc in enumerate(documents, 1):
            context_parts.append(f"[文档{i}]\n{doc['content']}\n")
        
        return "\n".join(context_parts)
