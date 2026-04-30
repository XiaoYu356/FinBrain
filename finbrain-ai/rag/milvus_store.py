from typing import List, Optional
from pymilvus import connections, Collection, FieldSchema, CollectionSchema, DataType, utility
from config import get_settings
from llm.embeddings import QwenEmbeddings


class MilvusStore:
    def __init__(self, collection_name: str = "finbrain_docs"):
        self.settings = get_settings()
        self.collection_name = collection_name
        self.embeddings = QwenEmbeddings()
        self.collection: Optional[Collection] = None
        self._connect()
    
    def _connect(self):
        connections.connect(
            alias="default",
            host=self.settings.MILVUS_HOST,
            port=self.settings.MILVUS_PORT
        )
    
    def create_collection(self, dimension: int = 1536):
        if utility.has_collection(self.collection_name):
            self.collection = Collection(self.collection_name)
            return
        
        fields = [
            FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
            FieldSchema(name="content", dtype=DataType.VARCHAR, max_length=65535),
            FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=dimension),
            FieldSchema(name="metadata", dtype=DataType.VARCHAR, max_length=2048)
        ]
        
        schema = CollectionSchema(fields=fields, description="Financial documents")
        self.collection = Collection(name=self.collection_name, schema=schema)
        
        index_params = {
            "metric_type": "COSINE",
            "index_type": "IVF_FLAT",
            "params": {"nlist": 128}
        }
        self.collection.create_index(field_name="embedding", index_params=index_params)
    
    def insert_documents(self, documents: List[dict]):
        if not self.collection:
            self.create_collection()
        
        contents = [doc["content"] for doc in documents]
        embeddings = self.embeddings.embed_documents(contents)
        
        data = [
            [doc["content"] for doc in documents],
            embeddings,
            [doc.get("metadata", "{}") for doc in documents]
        ]
        
        self.collection.insert(data)
        self.collection.flush()
    
    def search(self, query: str, top_k: int = 5) -> List[dict]:
        if not self.collection:
            return []
        
        self.collection.load()
        
        query_embedding = self.embeddings.embed_query(query)
        
        search_params = {"metric_type": "COSINE", "params": {"nprobe": 16}}
        results = self.collection.search(
            data=[query_embedding],
            anns_field="embedding",
            param=search_params,
            limit=top_k,
            output_fields=["content", "metadata"]
        )
        
        documents = []
        for hits in results:
            for hit in hits:
                documents.append({
                    "content": hit.entity.get("content"),
                    "metadata": hit.entity.get("metadata"),
                    "score": hit.score
                })
        
        return documents
    
    def delete_collection(self):
        if utility.has_collection(self.collection_name):
            utility.drop_collection(self.collection_name)
