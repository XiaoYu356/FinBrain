from typing import List, Dict, Any, Optional, Callable
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
import json
import logging
import asyncio

logger = logging.getLogger("finbrain-ai.memory")


class StorageStatus(Enum):
    PENDING = "pending"
    SYNCED = "synced"
    FAILED = "failed"


@dataclass
class Message:
    role: str
    content: str
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())
    metadata: Dict[str, Any] = field(default_factory=dict)
    _redis_synced: bool = False
    _mysql_synced: bool = False
    
    def to_dict(self) -> dict:
        return {
            "role": self.role,
            "content": self.content,
            "timestamp": self.timestamp,
            "metadata": self.metadata
        }
    
    @classmethod
    def from_dict(cls, data: dict) -> "Message":
        return cls(
            role=data.get("role", ""),
            content=data.get("content", ""),
            timestamp=data.get("timestamp", datetime.now().isoformat()),
            metadata=data.get("metadata", {})
        )


@dataclass
class ToolCall:
    action: str
    action_input: Dict[str, Any]
    observation: Any
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())
    
    def to_dict(self) -> dict:
        return {
            "action": self.action,
            "action_input": self.action_input,
            "observation": self.observation,
            "timestamp": self.timestamp
        }
    
    @classmethod
    def from_dict(cls, data: dict) -> "ToolCall":
        return cls(
            action=data.get("action", ""),
            action_input=data.get("action_input", {}),
            observation=data.get("observation"),
            timestamp=data.get("timestamp", datetime.now().isoformat())
        )


class ConversationMemory:
    def __init__(
        self,
        user_id: int = None,
        session_id: str = None,
        max_messages: int = 20,
        max_tool_calls: int = 10,
        summary_threshold: int = 10
    ):
        self.user_id = user_id
        self.session_id = session_id
        self.max_messages = max_messages
        self.max_tool_calls = max_tool_calls
        self.summary_threshold = summary_threshold
        
        self.messages: List[Message] = []
        self.tool_calls: List[ToolCall] = []
        self.summary: Optional[str] = None
        self.user_preferences: Dict[str, Any] = {}
        
        self._dirty: bool = False
        self._redis_synced: bool = True
        self._mysql_synced: bool = True
        self._pending_mysql_messages: List[Message] = []
    
    def add_message(self, role: str, content: str, metadata: dict = None):
        message = Message(
            role=role,
            content=content,
            metadata=metadata or {}
        )
        self.messages.append(message)
        self._dirty = True
        self._redis_synced = False
        self._mysql_synced = False
        self._pending_mysql_messages.append(message)
        
        if len(self.messages) > self.max_messages:
            self._trim_messages()
    
    def add_tool_call(self, action: str, action_input: dict, observation: Any):
        tool_call = ToolCall(
            action=action,
            action_input=action_input,
            observation=observation
        )
        self.tool_calls.append(tool_call)
        self._dirty = True
        self._redis_synced = False
        
        if len(self.tool_calls) > self.max_tool_calls:
            self.tool_calls = self.tool_calls[-self.max_tool_calls:]
    
    def _trim_messages(self):
        if len(self.messages) > self.max_messages:
            old_messages = self.messages[:-self.max_messages]
            self.messages = self.messages[-self.max_messages:]
            
            if self.summary:
                self._update_summary(old_messages)
    
    def _update_summary(self, old_messages: List[Message]):
        pass
    
    def get_context_window(self, max_turns: int = 5) -> List[Message]:
        if len(self.messages) <= max_turns * 2:
            return self.messages
        return self.messages[-max_turns * 2:]
    
    def get_formatted_context(self, max_turns: int = 5) -> str:
        recent_messages = self.get_context_window(max_turns)
        
        if not recent_messages:
            return "（无历史对话）"
        
        parts = []
        
        if self.summary:
            parts.append(f"【对话摘要】\n{self.summary}\n")
        
        parts.append("【近期对话】")
        for msg in recent_messages:
            role = "用户" if msg.role == "user" else "助手"
            content = msg.content
            if len(content) > 200:
                content = content[:200] + "..."
            parts.append(f"{role}：{content}")
        
        return "\n".join(parts)
    
    def get_tool_history_context(self, max_calls: int = 5) -> str:
        recent_calls = self.tool_calls[-max_calls:] if self.tool_calls else []
        
        if not recent_calls:
            return "（暂无历史工具调用）"
        
        parts = ["【历史工具调用】"]
        for i, call in enumerate(recent_calls, 1):
            observation_str = str(call.observation)
            if len(observation_str) > 300:
                observation_str = observation_str[:300] + "..."
            
            parts.append(
                f"{i}. {call.action}({json.dumps(call.action_input, ensure_ascii=False)}) "
                f"→ {observation_str}"
            )
        
        return "\n".join(parts)
    
    def set_user_preference(self, key: str, value: Any):
        self.user_preferences[key] = value
        self._dirty = True
        self._redis_synced = False
    
    def get_user_preference(self, key: str, default: Any = None) -> Any:
        return self.user_preferences.get(key, default)
    
    def extract_preferences_from_message(self, message: str):
        if "稳健" in message or "低风险" in message:
            self.set_user_preference("risk_preference", "conservative")
        elif "进取" in message or "高收益" in message:
            self.set_user_preference("risk_preference", "aggressive")
        
        import re
        amount_match = re.search(r"(\d+(?:\.\d+)?)\s*[万元]", message)
        if amount_match:
            amount = float(amount_match.group(1))
            if "万" in message:
                amount *= 10000
            self.set_user_preference("last_mentioned_amount", amount)
    
    def to_dict(self) -> dict:
        return {
            "messages": [m.to_dict() for m in self.messages],
            "tool_calls": [t.to_dict() for t in self.tool_calls],
            "summary": self.summary,
            "user_preferences": self.user_preferences
        }
    
    @classmethod
    def from_dict(cls, data: dict, user_id: int = None, session_id: str = None) -> "ConversationMemory":
        memory = cls(user_id=user_id, session_id=session_id)
        memory.messages = [Message.from_dict(m) for m in data.get("messages", [])]
        memory.tool_calls = [ToolCall.from_dict(t) for t in data.get("tool_calls", [])]
        memory.summary = data.get("summary")
        memory.user_preferences = data.get("user_preferences", {})
        memory._dirty = False
        memory._redis_synced = True
        memory._mysql_synced = True
        return memory
    
    def get_pending_mysql_messages(self) -> List[Message]:
        return self._pending_mysql_messages.copy()
    
    def clear_pending_mysql_messages(self):
        self._pending_mysql_messages.clear()
    
    def mark_redis_synced(self):
        self._redis_synced = True
        if self._mysql_synced:
            self._dirty = False
    
    def mark_mysql_synced(self):
        self._mysql_synced = True
        if self._redis_synced:
            self._dirty = False
    
    def get_sync_status(self) -> dict:
        return {
            "dirty": self._dirty,
            "redis_synced": self._redis_synced,
            "mysql_synced": self._mysql_synced,
            "pending_mysql_count": len(self._pending_mysql_messages)
        }


class MemoryManager:
    _instance = None
    _memories: Dict[str, ConversationMemory] = {}
    _redis_store = None
    _mysql_client = None
    _sync_locks: Dict[str, asyncio.Lock] = {}
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def set_redis_store(self, redis_store):
        self._redis_store = redis_store
    
    def set_mysql_client(self, mysql_client):
        self._mysql_client = mysql_client
    
    def _make_key(self, user_id: int, session_id: str) -> str:
        return f"{user_id}_{session_id}"
    
    def _get_lock(self, key: str) -> asyncio.Lock:
        if key not in self._sync_locks:
            self._sync_locks[key] = asyncio.Lock()
        return self._sync_locks[key]
    
    async def get_memory(self, user_id: int, session_id: str) -> ConversationMemory:
        key = self._make_key(user_id, session_id)
        
        if key not in self._memories:
            memory = ConversationMemory(user_id=user_id, session_id=session_id)
            
            if self._redis_store:
                try:
                    redis_data = await self._redis_store.load_full_memory(user_id, session_id)
                    if redis_data.get("messages") or redis_data.get("user_preferences"):
                        memory = ConversationMemory.from_dict(
                            redis_data,
                            user_id=user_id,
                            session_id=session_id
                        )
                        logger.info(f"从 Redis 加载记忆: user={user_id}, session={session_id}")
                except Exception as e:
                    logger.warning(f"从 Redis 加载记忆失败: {e}")
            
            self._memories[key] = memory
        
        return self._memories[key]
    
    async def save_memory(
        self,
        user_id: int,
        session_id: str,
        save_to_mysql: bool = True
    ) -> Dict[str, bool]:
        key = self._make_key(user_id, session_id)
        memory = self._memories.get(key)
        
        if not memory:
            return {"redis": False, "mysql": False}
        
        lock = self._get_lock(key)
        async with lock:
            results = {"redis": True, "mysql": True}
            
            if memory._dirty or not memory._redis_synced:
                if self._redis_store:
                    try:
                        await self._redis_store.save_full_memory(
                            user_id,
                            session_id,
                            memory.to_dict()
                        )
                        memory.mark_redis_synced()
                        logger.debug(f"记忆已保存到 Redis: user={user_id}, session={session_id}")
                    except Exception as e:
                        logger.error(f"保存记忆到 Redis 失败: {e}")
                        results["redis"] = False
            
            if save_to_mysql and memory._pending_mysql_messages:
                if self._mysql_client:
                    try:
                        for msg in memory.get_pending_mysql_messages():
                            await self._mysql_client.save_chat_message(
                                user_id=user_id,
                                session_id=session_id,
                                role=msg.role,
                                content=msg.content
                            )
                        memory.clear_pending_mysql_messages()
                        memory.mark_mysql_synced()
                        logger.debug(f"消息已保存到 MySQL: user={user_id}, session={session_id}")
                    except Exception as e:
                        logger.error(f"保存消息到 MySQL 失败: {e}")
                        results["mysql"] = False
            
            return results
    
    async def save_all_memories(self) -> Dict[str, int]:
        results = {"redis_success": 0, "redis_failed": 0, "mysql_success": 0, "mysql_failed": 0}
        
        for key, memory in self._memories.items():
            if memory._dirty or memory._pending_mysql_messages:
                parts = key.split("_", 1)
                if len(parts) == 2:
                    user_id = int(parts[0])
                    session_id = parts[1]
                    save_results = await self.save_memory(user_id, session_id)
                    
                    if save_results["redis"]:
                        results["redis_success"] += 1
                    else:
                        results["redis_failed"] += 1
                    
                    if save_results["mysql"]:
                        results["mysql_success"] += 1
                    else:
                        results["mysql_failed"] += 1
        
        return results
    
    def clear_memory(self, user_id: int, session_id: str):
        key = self._make_key(user_id, session_id)
        if key in self._memories:
            del self._memories[key]
        if key in self._sync_locks:
            del self._sync_locks[key]
    
    async def load_memory_from_external(
        self,
        user_id: int,
        session_id: str,
        history_data: List[dict]
    ) -> ConversationMemory:
        memory = await self.get_memory(user_id, session_id)
        
        if not memory.messages:
            for item in history_data:
                role = item.get("role")
                content = item.get("content")
                if role and content:
                    msg = Message(role=role, content=content)
                    msg._mysql_synced = True
                    memory.messages.append(msg)
        
        return memory
    
    async def get_sync_status(self, user_id: int, session_id: str) -> dict:
        memory = await self.get_memory(user_id, session_id)
        return memory.get_sync_status()
    
    async def force_sync(self, user_id: int, session_id: str) -> Dict[str, bool]:
        return await self.save_memory(user_id, session_id, save_to_mysql=True)


memory_manager = MemoryManager()
