from typing import List
import dashscope
from dashscope import TextEmbedding
from config import get_settings


class QwenEmbeddings:
    def __init__(self):
        settings = get_settings()
        dashscope.api_key = settings.DASHSCOPE_API_KEY
        self.model = settings.EMBEDDING_MODEL
    
    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        result = []
        batch_size = 25
        
        for i in range(0, len(texts), batch_size):
            batch = texts[i:i + batch_size]
            response = TextEmbedding.call(
                model=self.model,
                input=batch
            )
            
            if response.status_code == 200:
                for item in response.output["embeddings"]:
                    result.append(item["embedding"])
            else:
                raise Exception(f"Embedding API error: {response.code}")
        
        return result
    
    def embed_query(self, text: str) -> List[float]:
        response = TextEmbedding.call(
            model=self.model,
            input=[text]
        )
        
        if response.status_code == 200:
            return response.output["embeddings"][0]["embedding"]
        else:
            raise Exception(f"Embedding API error: {response.code}")
