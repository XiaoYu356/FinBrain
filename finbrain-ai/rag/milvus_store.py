from typing import List, Optional
from pymilvus import connections, Collection, FieldSchema, CollectionSchema, DataType, utility
from config import get_settings
from llm.embeddings import QwenEmbeddings
import json


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
        
        metadata_list = []
        for doc in documents:
            meta = doc.get("metadata", {})
            if isinstance(meta, dict):
                metadata_list.append(json.dumps(meta, ensure_ascii=False))
            else:
                metadata_list.append(str(meta))
        
        data = [
            [doc["content"] for doc in documents],
            embeddings,
            metadata_list
        ]
        
        self.collection.insert(data)
        self.collection.flush()
    
    def search(self, query: str, top_k: int = 5) -> List[dict]:
        if not self.collection:
            if utility.has_collection(self.collection_name):
                self.collection = Collection(self.collection_name)
            else:
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
    
    def get_document_count(self) -> int:
        if not self.collection:
            if utility.has_collection(self.collection_name):
                self.collection = Collection(self.collection_name)
            else:
                return 0
        self.collection.load()
        return self.collection.num_entities
    
    def get_all_documents(self, page: int = 1, page_size: int = 10) -> dict:
        if not self.collection:
            if utility.has_collection(self.collection_name):
                self.collection = Collection(self.collection_name)
            else:
                return {"total": 0, "documents": []}
        
        self.collection.load()
        total = self.collection.num_entities
        
        results = self.collection.query(
            expr="id >= 0",
            output_fields=["id", "content", "metadata"],
            limit=page_size,
            offset=(page - 1) * page_size
        )
        
        documents = []
        for item in results:
            content = item.get("content", "")
            documents.append({
                "id": item.get("id"),
                "content": content,
                "content_preview": content[:200] + "..." if len(content) > 200 else content,
                "metadata": item.get("metadata", "{}")
            })
        
        return {"total": total, "documents": documents}
    
    def delete_document(self, doc_id: int) -> bool:
        if not self.collection:
            if utility.has_collection(self.collection_name):
                self.collection = Collection(self.collection_name)
            else:
                return False
        
        self.collection.delete(f"id in [{doc_id}]")
        self.collection.flush()
        return True
