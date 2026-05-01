import re
from typing import Dict, Any
from agents.state import AgentState


INTENT_PATTERNS = {
    "search_product": [
        r"推荐.*产品",
        r"搜索.*理财",
        r"查找.*产品",
        r"有什么.*产品",
        r"产品.*列表",
        r"稳健.*产品",
        r"进取.*产品",
        r"低风险.*产品",
        r"高收益.*产品",
        r"有哪些.*产品",
        r"产品.*推荐"
    ],
    "calculate_income": [
        r"计算.*收益",
        r"收益.*多少",
        r"能赚.*多少",
        r"利息.*多少",
        r"投资.*回报",
        r"收益.*计算"
    ],
    "get_asset": [
        r"我的.*资产",
        r"查看.*余额",
        r"账户.*情况",
        r"资产.*总览",
        r"有多少钱",
        r"查看.*资产",
        r"资产.*情况",
        r"查询.*资产",
        r"余额.*多少",
        r"我的.*余额",
        r"帮我.*资产",
        r"帮我.*余额"
    ],
    "create_order": [
        r"购买.*产品",
        r"申购.*理财",
        r"下单",
        r"买入",
        r"购买.*理财"
    ],
    "query_orders": [
        r"我的.*订单",
        r"订单.*记录",
        r"查看.*订单",
        r"申购.*记录",
        r"查询.*订单"
    ],
    "risk_assessment": [
        r"风险.*测评",
        r"测评.*风险",
        r"评估.*风险",
        r"风险.*等级"
    ],
    "rag_query": [
        r"产品.*说明",
        r"理财.*知识",
        r"投资.*建议",
        r"什么是.*理财",
        r"如何.*投资"
    ]
}


def detect_intent(state: AgentState) -> Dict[str, Any]:
    message = state.get("current_message", "").lower()
    
    for intent, patterns in INTENT_PATTERNS.items():
        for pattern in patterns:
            if re.search(pattern, message):
                return {"intent": intent}
    
    return {"intent": "chat"}


def route_intent(state: AgentState) -> str:
    intent = state.get("intent", "chat")
    
    if intent in ["search_product", "calculate_income", "get_asset", "create_order", "query_orders", "risk_assessment"]:
        return "tool"
    elif intent == "rag_query":
        return "rag"
    else:
        return "chat"
