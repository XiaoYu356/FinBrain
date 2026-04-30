from typing import TypedDict, List, Optional, Any


class AgentState(TypedDict):
    messages: List[dict]
    user_id: int
    session_id: str
    current_message: str
    intent: Optional[str]
    tool_name: Optional[str]
    tool_args: Optional[dict]
    tool_result: Optional[Any]
    rag_context: Optional[str]
    response: Optional[str]
