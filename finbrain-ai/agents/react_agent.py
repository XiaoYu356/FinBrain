import json
import re
from typing import Dict, Any, Literal, Optional
from agents.state import AgentState
from agents.memory import memory_manager
from tools.tool_definitions import get_tool_descriptions, get_tool_names
from tools import (
    search_products, calculate_income, get_user_asset,
    create_order, query_orders, submit_risk_assessment, get_user_risk_level
)
from rag.retriever import Retriever
from llm.qwen_client import QwenLLM
import logging

logger = logging.getLogger("finbrain-ai.react")

REACT_PROMPT = """你是一个专业的金融智能顾问，可以使用工具帮助用户完成各种金融相关任务。

## 当前用户信息

- 用户ID: {user_id}
- 会话ID: {session_id}

## 可用工具

{tool_descriptions}

## 响应格式

请严格按照以下格式思考和回复：

Thought: 分析用户需求，思考下一步应该做什么
Action: 选择要使用的工具名称（必须是上述工具之一，或 FINISH 表示任务完成）
Action Input: 工具需要的参数（JSON格式）

如果已经获得足够信息可以直接回答用户，使用：
Thought: 我已经获得足够信息，可以直接回答用户
Action: FINISH
Final Answer: 给用户的完整回复

## 重要规则

1. 每次只能选择一个工具
2. Action 必须是工具名称之一或 FINISH
3. Action Input 必须是有效的 JSON 格式
4. 如果用户问题涉及多个步骤，请逐步完成
5. 使用 FINISH 时，Final Answer 应该是对用户问题的完整、专业的回答
6. 涉及用户个人数据（资产、订单等）的工具，系统会自动识别当前用户，无需传递用户ID参数

## 当前对话

{conversation_context}

## 用户问题

{user_message}

## 历史操作

{tool_history}

请开始你的思考和行动："""


def build_conversation_context(messages: list, max_turns: int = 3) -> str:
    if not messages:
        return "（无历史对话）"
    
    recent = messages[-max_turns * 2:] if len(messages) > max_turns * 2 else messages
    parts = []
    for msg in recent:
        role = "用户" if msg.get("role") == "user" else "助手"
        content = msg.get("content", "")
        parts.append(f"{role}：{content}")
    
    return "\n".join(parts)


def build_tool_history(tool_history: list) -> str:
    if not tool_history:
        return "（暂无历史操作）"
    
    parts = []
    for i, record in enumerate(tool_history, 1):
        action = record.get("action", "unknown")
        action_input = record.get("action_input", {})
        observation = record.get("observation", "")
        
        observation_str = str(observation)
        if len(observation_str) > 500:
            observation_str = observation_str[:500] + "..."
        
        parts.append(f"步骤{i}：调用 {action}，参数：{json.dumps(action_input, ensure_ascii=False)}")
        parts.append(f"结果：{observation_str}")
    
    return "\n".join(parts)


def parse_react_response(response: str) -> Dict[str, Any]:
    thought_match = re.search(r"Thought:\s*(.+?)(?=\nAction:|$)", response, re.DOTALL)
    action_match = re.search(r"Action:\s*(\w+)", response)
    action_input_match = re.search(r"Action Input:\s*(\{.+?\})", response, re.DOTALL)
    final_answer_match = re.search(r"Final Answer:\s*(.+?)$", response, re.DOTALL)
    
    result = {
        "thought": thought_match.group(1).strip() if thought_match else "",
        "action": action_match.group(1).strip() if action_match else None,
        "action_input": {},
        "final_answer": final_answer_match.group(1).strip() if final_answer_match else None
    }
    
    if action_input_match:
        try:
            result["action_input"] = json.loads(action_input_match.group(1))
        except json.JSONDecodeError:
            logger.warning(f"Failed to parse action input: {action_input_match.group(1)}")
            result["action_input"] = {}
    
    return result


async def think(state: AgentState) -> Dict[str, Any]:
    logger.info("=== ReAct Think ===")
    
    user_id = state.get("user_id")
    session_id = state.get("session_id", "default")
    memory = await memory_manager.get_memory(user_id, session_id)
    
    memory.extract_preferences_from_message(state.get("current_message", ""))
    
    llm = QwenLLM(temperature=0.1)
    
    conversation_context = memory.get_formatted_context(max_turns=5)
    tool_history_context = memory.get_tool_history_context(max_calls=3)
    
    current_tool_history = build_tool_history(state.get("tool_history", []))
    if current_tool_history != "（暂无历史操作）":
        tool_history_context = f"{tool_history_context}\n\n【当前会话操作】\n{current_tool_history}"
    
    prompt = REACT_PROMPT.format(
        user_id=user_id,
        session_id=session_id,
        tool_descriptions=get_tool_descriptions(),
        conversation_context=conversation_context,
        user_message=state.get("current_message", ""),
        tool_history=tool_history_context
    )
    
    try:
        response = llm._call(prompt)
        logger.info(f"LLM Response:\n{response}")
        
        parsed = parse_react_response(response)
        
        logger.info(f"Parsed - Thought: {parsed['thought']}")
        logger.info(f"Parsed - Action: {parsed['action']}")
        logger.info(f"Parsed - Action Input: {parsed['action_input']}")
        
        update = {
            "thought": parsed["thought"],
            "action": parsed["action"],
            "action_input": parsed["action_input"],
            "step_count": state.get("step_count", 0) + 1
        }
        
        if parsed["final_answer"]:
            update["final_answer"] = parsed["final_answer"]
        
        return update
        
    except Exception as e:
        logger.error(f"Think error: {e}")
        return {
            "error": str(e),
            "final_answer": "抱歉，我在思考过程中遇到了问题，请稍后重试。"
        }


async def act(state: AgentState) -> Dict[str, Any]:
    action = state.get("action")
    action_input = state.get("action_input", {})
    user_id = state.get("user_id")
    session_id = state.get("session_id", "default")
    
    logger.info(f"=== ReAct Act: {action} ===")
    logger.info(f"Action Input: {action_input}")
    
    observation = None
    tool_record = {
        "action": action,
        "action_input": action_input,
        "observation": None
    }
    
    try:
        if action == "rag_search":
            query = action_input.get("query", state.get("current_message", ""))
            retriever = Retriever()
            context = retriever.get_context(query)
            observation = {"success": True, "context": context}
            
        elif action == "search_products":
            observation = await search_products(
                product_type=action_input.get("product_type"),
                min_rate=action_input.get("min_rate"),
                max_rate=action_input.get("max_rate"),
                min_term=action_input.get("min_term"),
                max_term=action_input.get("max_term")
            )
            
        elif action == "calculate_income":
            observation = await calculate_income(
                amount=action_input.get("amount", 10000),
                annual_rate=action_input.get("annual_rate", 5.0),
                term_days=action_input.get("term_days", 365),
                invest_type=action_input.get("invest_type", "one_time"),
                invest_period=action_input.get("invest_period", 1)
            )
            
        elif action == "get_user_asset":
            observation = await get_user_asset(user_id=user_id)
            
        elif action == "create_order":
            observation = await create_order(
                user_id=user_id,
                product_id=action_input.get("product_id", 1),
                amount=action_input.get("amount", 10000)
            )
            
        elif action == "query_orders":
            observation = await query_orders(user_id=user_id)
            
        elif action == "get_user_risk_level":
            observation = await get_user_risk_level(user_id=user_id)
            
        elif action == "submit_risk_assessment":
            observation = await submit_risk_assessment(
                user_id=user_id,
                answers=action_input.get("answers", [])
            )
            
        else:
            observation = {"success": False, "error": f"Unknown action: {action}"}
            
    except Exception as e:
        logger.error(f"Act error: {e}")
        observation = {"success": False, "error": str(e)}
    
    tool_record["observation"] = observation
    logger.info(f"Observation: {observation}")
    
    memory = await memory_manager.get_memory(user_id, session_id)
    memory.add_tool_call(action, action_input, observation)
    
    return {
        "observation": observation,
        "tool_history": [tool_record]
    }


def should_continue(state: AgentState) -> Literal["continue", "end"]:
    if state.get("final_answer"):
        logger.info("Decision: END (has final answer)")
        return "end"
    
    if state.get("error"):
        logger.info("Decision: END (has error)")
        return "end"
    
    action = state.get("action")
    if action == "FINISH":
        logger.info("Decision: END (action is FINISH)")
        return "end"
    
    step_count = state.get("step_count", 0)
    max_steps = state.get("max_steps", 5)
    if step_count >= max_steps:
        logger.info(f"Decision: END (max steps reached: {step_count}/{max_steps})")
        return "end"
    
    logger.info(f"Decision: CONTINUE (step {step_count}/{max_steps})")
    return "continue"


async def generate_final_response(state: AgentState) -> Dict[str, Any]:
    final_answer = state.get("final_answer")
    
    if final_answer:
        return {"response": final_answer}
    
    llm = QwenLLM()
    
    tool_history = state.get("tool_history", [])
    context_parts = []
    
    for record in tool_history:
        action = record.get("action")
        observation = record.get("observation")
        if observation:
            context_parts.append(f"【{action}结果】\n{observation}")
    
    context = "\n\n".join(context_parts) if context_parts else ""
    
    prompt = f"""你是一个专业的金融智能顾问。请根据以下信息回答用户问题。

用户问题：{state.get("current_message", "")}

{context}

请给出专业、友好、简洁的回答："""

    try:
        response = llm._call(prompt)
        return {"response": response}
    except Exception as e:
        logger.error(f"Generate response error: {e}")
        return {"response": "抱歉，服务暂时不可用，请稍后重试。"}
