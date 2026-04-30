from typing import Dict, Any
from agents.state import AgentState
from rag.retriever import Retriever
from llm.qwen_client import QwenLLM


async def rag_search(state: AgentState) -> Dict[str, Any]:
    retriever = Retriever()
    query = state.get("current_message", "")
    
    context = retriever.get_context(query)
    
    return {"rag_context": context}


async def generate_response(state: AgentState) -> Dict[str, Any]:
    llm = QwenLLM()
    
    message = state.get("current_message", "")
    intent = state.get("intent", "chat")
    tool_result = state.get("tool_result")
    rag_context = state.get("rag_context", "")
    
    system_prompt = """你是FinBrain金融智能顾问，专注于为用户提供专业的理财建议和服务。
请用专业、友好、简洁的语言回答用户问题。
如果用户询问理财产品，请根据搜索结果推荐合适的产品。
如果用户询问收益计算，请给出详细的计算结果。
如果用户询问资产情况，请准确展示用户的资产信息。"""
    
    user_prompt = f"用户问题：{message}\n\n"
    
    if tool_result:
        user_prompt += f"工具调用结果：{tool_result}\n\n"
    
    if rag_context:
        user_prompt += f"参考文档：\n{rag_context}\n\n"
    
    user_prompt += "请根据以上信息回答用户问题："
    
    full_prompt = f"{system_prompt}\n\n{user_prompt}"
    
    try:
        response = llm._call(full_prompt)
    except Exception as e:
        response = f"抱歉，服务暂时不可用。错误信息：{str(e)}"
    
    return {"response": response}
