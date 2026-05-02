import redis.asyncio as redis
from typing import Optional, Any
import json
import logging
from config import get_settings

logger = logging.getLogger("finbrain-ai.redis")

_settings = None
_redis_pool: Optional[redis.ConnectionPool] = None


async def get_redis_client() -> Optional[redis.Redis]:
    global _redis_pool, _settings
    
    if _redis_pool is None:
        _settings = get_settings()
        try:
            _redis_pool = redis.ConnectionPool(
                host=_settings.REDIS_HOST,
                port=_settings.REDIS_PORT,
                password=_settings.REDIS_PASSWORD if _settings.REDIS_PASSWORD else None,
                decode_responses=True,
                socket_connect_timeout=5,
                socket_timeout=5,
                retry_on_timeout=True,
                health_check_interval=30
            )
            client = redis.Redis(connection_pool=_redis_pool)
            await client.ping()
            logger.info(f"Redis 连接成功: {_settings.REDIS_HOST}:{_settings.REDIS_PORT}")
            return client
        except Exception as e:
            logger.warning(f"Redis 连接失败: {e}，将使用内存存储")
            _redis_pool = None
            return None
    
    return redis.Redis(connection_pool=_redis_pool)


async def close_redis():
    global _redis_pool
    if _redis_pool:
        await _redis_pool.disconnect()
        _redis_pool = None
        logger.info("Redis 连接已关闭")


class RedisMemoryStore:
    KEY_PREFIX = "finbrain:memory"
    KEY_TTL = 86400 * 7
    
    def __init__(self):
        pass
    
    async def _get_client(self) -> Optional[redis.Redis]:
        return await get_redis_client()
    
    def _make_key(self, user_id: int, session_id: str, data_type: str) -> str:
        return f"{self.KEY_PREFIX}:{user_id}:{session_id}:{data_type}"
    
    async def save_messages(self, user_id: int, session_id: str, messages: list) -> bool:
        client = await self._get_client()
        if not client:
            return False
        
        key = self._make_key(user_id, session_id, "messages")
        try:
            await client.setex(
                key,
                self.KEY_TTL,
                json.dumps(messages, ensure_ascii=False)
            )
            return True
        except Exception as e:
            logger.error(f"保存消息到 Redis 失败: {e}")
            return False
    
    async def load_messages(self, user_id: int, session_id: str) -> list:
        client = await self._get_client()
        if not client:
            return []
        
        key = self._make_key(user_id, session_id, "messages")
        try:
            data = await client.get(key)
            if data:
                return json.loads(data)
            return []
        except Exception as e:
            logger.error(f"从 Redis 加载消息失败: {e}")
            return []
    
    async def save_tool_calls(self, user_id: int, session_id: str, tool_calls: list) -> bool:
        client = await self._get_client()
        if not client:
            return False
        
        key = self._make_key(user_id, session_id, "tool_calls")
        try:
            await client.setex(
                key,
                self.KEY_TTL,
                json.dumps(tool_calls, ensure_ascii=False, default=str)
            )
            return True
        except Exception as e:
            logger.error(f"保存工具调用到 Redis 失败: {e}")
            return False
    
    async def load_tool_calls(self, user_id: int, session_id: str) -> list:
        client = await self._get_client()
        if not client:
            return []
        
        key = self._make_key(user_id, session_id, "tool_calls")
        try:
            data = await client.get(key)
            if data:
                return json.loads(data)
            return []
        except Exception as e:
            logger.error(f"从 Redis 加载工具调用失败: {e}")
            return []
    
    async def save_preferences(self, user_id: int, session_id: str, preferences: dict) -> bool:
        client = await self._get_client()
        if not client:
            return False
        
        key = self._make_key(user_id, session_id, "preferences")
        try:
            await client.setex(
                key,
                self.KEY_TTL,
                json.dumps(preferences, ensure_ascii=False)
            )
            return True
        except Exception as e:
            logger.error(f"保存用户偏好到 Redis 失败: {e}")
            return False
    
    async def load_preferences(self, user_id: int, session_id: str) -> dict:
        client = await self._get_client()
        if not client:
            return {}
        
        key = self._make_key(user_id, session_id, "preferences")
        try:
            data = await client.get(key)
            if data:
                return json.loads(data)
            return {}
        except Exception as e:
            logger.error(f"从 Redis 加载用户偏好失败: {e}")
            return {}
    
    async def save_summary(self, user_id: int, session_id: str, summary: str) -> bool:
        client = await self._get_client()
        if not client:
            return False
        
        key = self._make_key(user_id, session_id, "summary")
        try:
            await client.setex(key, self.KEY_TTL, summary)
            return True
        except Exception as e:
            logger.error(f"保存摘要到 Redis 失败: {e}")
            return False
    
    async def load_summary(self, user_id: int, session_id: str) -> Optional[str]:
        client = await self._get_client()
        if not client:
            return None
        
        key = self._make_key(user_id, session_id, "summary")
        try:
            return await client.get(key)
        except Exception as e:
            logger.error(f"从 Redis 加载摘要失败: {e}")
            return None
    
    async def clear_session(self, user_id: int, session_id: str) -> bool:
        client = await self._get_client()
        if not client:
            logger.warning(f"Redis 客户端不可用，无法清除会话数据")
            return False
        
        keys = [
            self._make_key(user_id, session_id, "messages"),
            self._make_key(user_id, session_id, "tool_calls"),
            self._make_key(user_id, session_id, "preferences"),
            self._make_key(user_id, session_id, "summary")
        ]
        try:
            logger.info(f"准备删除 Redis keys: {keys}")
            deleted = await client.delete(*keys)
            logger.info(f"已删除 {deleted} 个 Redis keys")
            return True
        except Exception as e:
            logger.error(f"清除会话数据失败: {e}", exc_info=True)
            return False
    
    async def save_full_memory(self, user_id: int, session_id: str, memory_data: dict) -> bool:
        client = await self._get_client()
        if not client:
            return False
        
        try:
            await self.save_messages(user_id, session_id, memory_data.get("messages", []))
            await self.save_tool_calls(user_id, session_id, memory_data.get("tool_calls", []))
            await self.save_preferences(user_id, session_id, memory_data.get("user_preferences", {}))
            if memory_data.get("summary"):
                await self.save_summary(user_id, session_id, memory_data["summary"])
            return True
        except Exception as e:
            logger.error(f"保存完整记忆到 Redis 失败: {e}")
            return False
    
    async def load_full_memory(self, user_id: int, session_id: str) -> dict:
        return {
            "messages": await self.load_messages(user_id, session_id),
            "tool_calls": await self.load_tool_calls(user_id, session_id),
            "user_preferences": await self.load_preferences(user_id, session_id),
            "summary": await self.load_summary(user_id, session_id)
        }


redis_memory_store = RedisMemoryStore()
