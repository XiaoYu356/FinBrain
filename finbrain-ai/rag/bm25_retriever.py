import jieba
from typing import List, Dict, Optional
from collections import Counter
import math
import re
import json
import logging

logger = logging.getLogger(__name__)


class BM25:
    def __init__(self, k1: float = 1.5, b: float = 0.75):
        self.k1 = k1
        self.b = b
        self.documents: List[List[str]] = []
        self.doc_ids: List[int] = []
        self.doc_lengths: List[int] = []
        self.avgdl: float = 0
        self.df: Dict[str, int] = {}
        self.idf: Dict[str, float] = {}
        self.doc_contents: List[str] = []
        self._initialized = False
    
    def tokenize(self, text: str) -> List[str]:
        text = re.sub(r'[^\w\u4e00-\u9fff]', ' ', text)
        tokens = jieba.lcut(text.lower())
        return [t for t in tokens if len(t) > 1]
    
    def index_documents(self, documents: List[Dict]):
        self.documents = []
        self.doc_ids = []
        self.doc_lengths = []
        self.doc_contents = []
        self.df = {}
        
        for doc in documents:
            doc_id = doc.get("id", len(self.doc_ids))
            content = doc.get("content", "")
            
            tokens = self.tokenize(content)
            self.documents.append(tokens)
            self.doc_ids.append(doc_id)
            self.doc_lengths.append(len(tokens))
            self.doc_contents.append(content)
            
            term_freq = set(tokens)
            for term in term_freq:
                self.df[term] = self.df.get(term, 0) + 1
        
        self._update_idf()
        self._initialized = True
        logger.info(f"BM25 索引构建完成: {len(self.documents)} 文档, {len(self.df)} 词项")
    
    def add_document(self, doc_id: int, content: str):
        if doc_id in self.doc_ids:
            self._remove_document_internal(doc_id)
        
        tokens = self.tokenize(content)
        
        self.documents.append(tokens)
        self.doc_ids.append(doc_id)
        self.doc_lengths.append(len(tokens))
        self.doc_contents.append(content)
        
        term_freq = set(tokens)
        for term in term_freq:
            self.df[term] = self.df.get(term, 0) + 1
        
        self._update_idf()
        self._initialized = True
        logger.info(f"BM25 增量添加文档: {doc_id}")
    
    def remove_document(self, doc_id: int):
        if doc_id not in self.doc_ids:
            return
        
        self._remove_document_internal(doc_id)
        self._update_idf()
        logger.info(f"BM25 增量删除文档: {doc_id}")
    
    def _remove_document_internal(self, doc_id: int):
        idx = self.doc_ids.index(doc_id)
        
        old_tokens = self.documents[idx]
        term_freq = set(old_tokens)
        for term in term_freq:
            if term in self.df:
                self.df[term] -= 1
                if self.df[term] <= 0:
                    del self.df[term]
        
        self.documents.pop(idx)
        self.doc_ids.pop(idx)
        self.doc_lengths.pop(idx)
        self.doc_contents.pop(idx)
    
    def _update_idf(self):
        N = len(self.documents)
        self.avgdl = sum(self.doc_lengths) / N if N > 0 else 0
        
        for term in list(self.idf.keys()):
            if term not in self.df:
                del self.idf[term]
        
        for term, df in self.df.items():
            self.idf[term] = math.log((N - df + 0.5) / (df + 0.5) + 1)
    
    def search(self, query: str, top_k: int = 10) -> List[Dict]:
        if not self._initialized:
            return []
        
        query_tokens = self.tokenize(query)
        scores = []
        
        for i, doc_tokens in enumerate(self.documents):
            score = self._score_document(query_tokens, doc_tokens, i)
            scores.append({
                "doc_id": self.doc_ids[i],
                "score": score,
                "content": self.doc_contents[i]
            })
        
        scores.sort(key=lambda x: x["score"], reverse=True)
        return scores[:top_k]
    
    def _score_document(self, query_tokens: List[str], doc_tokens: List[str], doc_idx: int) -> float:
        score = 0
        doc_len = self.doc_lengths[doc_idx]
        doc_freq = Counter(doc_tokens)
        
        for term in query_tokens:
            if term not in self.idf:
                continue
            
            tf = doc_freq.get(term, 0)
            idf = self.idf[term]
            
            numerator = tf * (self.k1 + 1)
            denominator = tf + self.k1 * (1 - self.b + self.b * doc_len / self.avgdl)
            
            score += idf * numerator / denominator
        
        return score


class BM25RedisStorage:
    KEY_PREFIX = "finbrain:bm25"
    
    def __init__(self):
        self._sync_client = None
    
    def _get_sync_client(self):
        if self._sync_client is None:
            try:
                import redis
                from config import get_settings
                settings = get_settings()
                self._sync_client = redis.Redis(
                    host=settings.REDIS_HOST,
                    port=settings.REDIS_PORT,
                    password=settings.REDIS_PASSWORD if settings.REDIS_PASSWORD else None,
                    decode_responses=True
                )
                self._sync_client.ping()
                logger.info("BM25 Redis 连接成功")
            except Exception as e:
                logger.warning(f"BM25 Redis 连接失败: {e}，将使用内存存储")
                self._sync_client = None
        return self._sync_client
    
    def _make_key(self, *parts) -> str:
        return ":".join([self.KEY_PREFIX] + [str(p) for p in parts])
    
    def save_documents(self, documents: List[Dict]) -> bool:
        client = self._get_sync_client()
        if not client:
            return False
        
        try:
            pipe = client.pipeline()
            
            pipe.delete(self._make_key("chunks"))
            pipe.delete(self._make_key("doc_chunks"))
            
            doc_chunk_map = {}
            for doc in documents:
                chunk_id = doc.get("id")
                content = doc.get("content", "")
                metadata = doc.get("metadata", "{}")
                
                if chunk_id and content:
                    pipe.hset(self._make_key("chunks"), str(chunk_id), content)
                    
                    try:
                        if isinstance(metadata, str):
                            meta = json.loads(metadata)
                        else:
                            meta = metadata
                        document_id = meta.get("document_id")
                        if document_id:
                            if str(document_id) not in doc_chunk_map:
                                doc_chunk_map[str(document_id)] = []
                            doc_chunk_map[str(document_id)].append(str(chunk_id))
                    except:
                        pass
            
            for doc_id, chunk_ids in doc_chunk_map.items():
                pipe.hset(self._make_key("doc_chunks"), doc_id, json.dumps(chunk_ids))
            
            pipe.execute()
            logger.info(f"BM25 文档已保存到 Redis: {len(documents)} 个分片")
            return True
            
        except Exception as e:
            logger.error(f"保存 BM25 文档到 Redis 失败: {e}")
            return False
    
    def load_documents(self) -> List[Dict]:
        client = self._get_sync_client()
        if not client:
            return []
        
        try:
            chunks = client.hgetall(self._make_key("chunks"))
            
            documents = []
            for chunk_id, content in chunks.items():
                documents.append({
                    "id": int(chunk_id),
                    "content": content
                })
            
            logger.info(f"BM25 文档已从 Redis 加载: {len(documents)} 个分片")
            return documents
            
        except Exception as e:
            logger.error(f"从 Redis 加载 BM25 文档失败: {e}")
            return []
    
    def add_chunk(self, chunk_id: int, content: str) -> bool:
        client = self._get_sync_client()
        if not client:
            return False
        
        try:
            client.hset(self._make_key("chunks"), str(chunk_id), content)
            logger.info(f"BM25 Redis 增量添加分片: {chunk_id}")
            return True
        except Exception as e:
            logger.error(f"BM25 Redis 增量添加失败: {e}")
            return False
    
    def remove_chunks_by_document_id(self, document_id: int) -> bool:
        client = self._get_sync_client()
        if not client:
            return False
        
        try:
            chunk_ids_json = client.hget(self._make_key("doc_chunks"), str(document_id))
            
            if not chunk_ids_json:
                logger.info(f"Redis 中没有文档 {document_id} 的分片映射")
                return True
            
            chunk_ids = json.loads(chunk_ids_json)
            
            if chunk_ids:
                client.hdel(self._make_key("chunks"), *chunk_ids)
                client.hdel(self._make_key("doc_chunks"), str(document_id))
                logger.info(f"BM25 Redis 删除文档 {document_id} 的 {len(chunk_ids)} 个分片")
            
            return True
            
        except Exception as e:
            logger.error(f"BM25 Redis 删除文档失败: {e}")
            return False
    
    def clear_all(self) -> bool:
        client = self._get_sync_client()
        if not client:
            return False
        
        try:
            client.delete(
                self._make_key("chunks"),
                self._make_key("doc_chunks")
            )
            logger.info("BM25 Redis 文档已清除")
            return True
        except Exception as e:
            logger.error(f"清除 BM25 Redis 文档失败: {e}")
            return False


_bm25_instance: Optional[BM25] = None
_bm25_redis_storage: Optional[BM25RedisStorage] = None


def get_bm25() -> BM25:
    global _bm25_instance
    if _bm25_instance is None:
        _bm25_instance = BM25()
    return _bm25_instance


def get_bm25_redis_storage() -> BM25RedisStorage:
    global _bm25_redis_storage
    if _bm25_redis_storage is None:
        _bm25_redis_storage = BM25RedisStorage()
    return _bm25_redis_storage


def init_bm25_from_milvus(force_rebuild: bool = False):
    bm25 = get_bm25()
    redis_storage = get_bm25_redis_storage()
    
    documents = redis_storage.load_documents()
    
    if documents and not force_rebuild:
        logger.info(f"从 Redis 加载 {len(documents)} 个文档分片，开始构建 BM25 索引...")
        bm25.index_documents(documents)
        return bm25
    
    from rag.milvus_store import MilvusStore
    
    store = MilvusStore()
    store.create_collection()
    
    if not store.collection:
        logger.warning("Milvus collection 未初始化")
        return bm25
    
    store.collection.load()
    
    try:
        results = store.collection.query(
            expr="id >= 0",
            output_fields=["id", "content", "metadata"],
            limit=10000
        )
    except Exception as e:
        logger.error(f"查询 Milvus 失败: {e}")
        results = []
    
    documents = []
    for item in results:
        chunk_id = item.get("id")
        content = item.get("content", "")
        metadata = item.get("metadata", "{}")
        if chunk_id and content:
            documents.append({
                "id": chunk_id,
                "content": content,
                "metadata": metadata
            })
    
    logger.info(f"从 Milvus 加载 {len(documents)} 个文档分片")
    
    redis_storage.save_documents(documents)
    
    bm25.index_documents(documents)
    
    return bm25


def rebuild_bm25_index():
    return init_bm25_from_milvus(force_rebuild=True)


def add_chunk_to_bm25(chunk_id: int, content: str):
    bm25 = get_bm25()
    redis_storage = get_bm25_redis_storage()
    
    redis_storage.add_chunk(chunk_id, content)
    
    bm25.add_document(chunk_id, content)


def remove_document_from_bm25(document_id: int):
    bm25 = get_bm25()
    redis_storage = get_bm25_redis_storage()
    
    redis_storage.remove_chunks_by_document_id(document_id)
    
    documents = redis_storage.load_documents()
    bm25.index_documents(documents)
