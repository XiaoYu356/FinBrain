from typing import TypedDict, List, Optional, Any, Annotated
from operator import add


class AgentState(TypedDict):
    messages: List[dict]
    user_id: int
    session_id: str
    current_message: str
    
    thought: Optional[str]
    action: Optional[str]
    action_input: Optional[dict]
    observation: Optional[str]
    
    step_count: int
    max_steps: int
    
    tool_history: Annotated[List[dict], add]
    
    final_answer: Optional[str]
    response: Optional[str]
    
    error: Optional[str]


def create_initial_state(
    user_id: int,
    current_message: str,
    session_id: str = "default",
    messages: List[dict] = None,
    max_steps: int = 5
) -> AgentState:
    return {
        "messages": messages or [],
        "user_id": user_id,
        "session_id": session_id,
        "current_message": current_message,
        "thought": None,
        "action": None,
        "action_input": None,
        "observation": None,
        "step_count": 0,
        "max_steps": max_steps,
        "tool_history": [],
        "final_answer": None,
        "response": None,
        "error": None
    }
