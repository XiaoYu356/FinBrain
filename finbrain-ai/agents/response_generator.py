from typing import Dict, Any, List
from agents.state import AgentState
from rag.retriever import Retriever
from llm.qwen_client import QwenLLM


async def rag_search(state: AgentState) -> Dict[str, Any]:
    retriever = Retriever()
    query = state.get("current_message", "")
    
    context = retriever.get_context(query)
    
    return {"rag_context": context}


def build_conversation_context(messages: List[dict], max_turns: int = 5) -> str:
    if not messages:
        return ""
    
    recent_messages = messages[-max_turns * 2:] if len(messages) > max_turns * 2 else messages
    
    context_parts = []
    for msg in recent_messages:
        role = msg.get("role", "")
        content = msg.get("content", "")
        if role == "user":
            context_parts.append(f"用户：{content}")
        elif role == "assistant":
            context_parts.append(f"助手：{content}")
    
    return "\n".join(context_parts)


async def generate_response(state: AgentState) -> Dict[str, Any]:
    llm = QwenLLM()
    
    message = state.get("current_message", "")
    intent = state.get("intent", "chat")
    tool_result = state.get("tool_result")
    rag_context = state.get("rag_context", "")
    messages = state.get("messages", [])
    
    system_prompt = """你是FinBrain金融智能顾问，专注于为用户提供专业的理财建议和服务。
请用专业、友好、简洁的语言回答用户问题。
如果用户询问理财产品，请根据搜索结果推荐合适的产品。
如果用户询问收益计算，请给出详细的计算结果。
如果用户询问资产情况，请准确展示用户的资产信息。
如果用户提到之前的对话内容，请结合上下文回答。"""
    
    conversation_context = build_conversation_context(messages)
    
    user_prompt = ""
    
    if conversation_context:
        user_prompt += f"【历史对话】\n{conversation_context}\n\n"
    
    user_prompt += f"【当前问题】\n用户问题：{message}\n\n"
    
    if intent == "risk_assessment":
        user_prompt += "用户想要进行风险测评。请引导用户前往风险测评页面完成测评，说明测评的重要性和好处。\n\n"
    
    if tool_result:
        user_prompt += f"【工具调用结果】\n{tool_result}\n\n"
    
    if rag_context:
        user_prompt += f"【参考文档】\n{rag_context}\n\n"
    
    user_prompt += "请根据以上信息回答用户问题："
    
    full_prompt = f"{system_prompt}\n\n{user_prompt}"
    
    try:
        response = llm._call(full_prompt)
    except Exception as e:
        response = f"抱歉，服务暂时不可用。错误信息：{str(e)}"
    
    return {"response": response}
