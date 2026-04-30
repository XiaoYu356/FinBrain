from typing import Dict, Any
from agents.state import AgentState
from tools import (
    search_products, calculate_income, get_user_asset,
    create_order, query_orders, submit_risk_assessment
)
import json


async def execute_tool(state: AgentState) -> Dict[str, Any]:
    intent = state.get("intent")
    user_id = state.get("user_id")
    message = state.get("current_message", "")
    
    tool_result = None
    tool_name = intent
    
    try:
        if intent == "search_product":
            product_type = extract_product_type(message)
            tool_result = await search_products(product_type=product_type)
        
        elif intent == "calculate_income":
            params = extract_income_params(message)
            tool_result = await calculate_income(**params)
        
        elif intent == "get_asset":
            tool_result = await get_user_asset(user_id)
        
        elif intent == "create_order":
            params = extract_order_params(message)
            if params:
                tool_result = await create_order(
                    user_id=user_id,
                    product_id=params.get("product_id", 1),
                    amount=params.get("amount", 10000)
                )
            else:
                tool_result = {"success": False, "message": "请提供产品ID和购买金额"}
        
        elif intent == "query_orders":
            tool_result = await query_orders(user_id)
        
        elif intent == "risk_assessment":
            tool_result = {"success": True, "message": "请前往风险测评页面完成测评"}
    
    except Exception as e:
        tool_result = {"success": False, "message": str(e)}
    
    return {
        "tool_name": tool_name,
        "tool_result": tool_result
    }


def extract_product_type(message: str) -> str:
    if "稳健" in message or "低风险" in message:
        return "R2"
    elif "进取" in message or "高风险" in message:
        return "R4"
    elif "保守" in message:
        return "R1"
    elif "激进" in message:
        return "R5"
    return None


def extract_income_params(message: str) -> dict:
    import re
    
    amount_match = re.search(r"(\d+(?:\.\d+)?)\s*[万元]", message)
    rate_match = re.search(r"(\d+(?:\.\d+)?)\s*%", message)
    days_match = re.search(r"(\d+)\s*天", message)
    
    amount = float(amount_match.group(1)) * 10000 if amount_match else 10000
    rate = float(rate_match.group(1)) if rate_match else 5.0
    days = int(days_match.group(1)) if days_match else 365
    
    return {
        "amount": amount,
        "annual_rate": rate,
        "term_days": days
    }


def extract_order_params(message: str) -> dict:
    import re
    
    product_id = 1
    amount = 10000
    
    id_match = re.search(r"产品[编号ID]*(\d+)", message)
    if id_match:
        product_id = int(id_match.group(1))
    
    amount_match = re.search(r"(\d+(?:\.\d+)?)\s*[万元]", message)
    if amount_match:
        amount = float(amount_match.group(1))
        if "万" in message:
            amount *= 10000
    
    return {"product_id": product_id, "amount": amount}
