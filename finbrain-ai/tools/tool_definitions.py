from typing import List, Dict, Any
from pydantic import BaseModel, Field


class ToolParameter(BaseModel):
    name: str = Field(description="参数名称")
    type: str = Field(description="参数类型: string, number, integer, boolean, array, object")
    description: str = Field(description="参数描述")
    required: bool = Field(default=True, description="是否必填")
    enum: List[str] = Field(default=None, description="枚举值列表")


class ToolDefinition(BaseModel):
    name: str = Field(description="工具名称")
    description: str = Field(description="工具功能描述")
    parameters: List[ToolParameter] = Field(default_factory=list, description="参数列表")
    when_to_use: str = Field(description="何时使用此工具")
    examples: List[str] = Field(default_factory=list, description="使用示例")
    requires_user_context: bool = Field(default=False, description="是否需要用户上下文（自动注入user_id）")


TOOL_DEFINITIONS: List[ToolDefinition] = [
    ToolDefinition(
        name="rag_search",
        description="""搜索金融知识库，获取理财相关知识、产品说明、投资建议等。

知识库包含以下类型的信息：
1. 理财产品说明（如：什么是稳健型产品、产品风险等级含义）
2. 投资知识科普（如：定投的原理、复利计算、风险分散策略）
3. 政策法规解读（如：理财新规、投资者适当性管理）
4. 风险提示和注意事项

返回结果为相关的知识文档片段。""",
        parameters=[
            ToolParameter(
                name="query",
                type="string",
                description="搜索问题或关键词，如'什么是稳健型理财'、'定投的优势'、'如何分散投资风险'",
                required=True
            )
        ],
        when_to_use="当用户询问理财知识、产品说明、投资建议、风险提示等需要背景信息的问题时使用",
        examples=[
            "用户问'什么是稳健型理财？' → 调用 rag_search(query='稳健型理财产品说明')",
            "用户问'定投有什么好处？' → 调用 rag_search(query='定投的优势')",
            "用户问'如何降低投资风险？' → 调用 rag_search(query='投资风险分散策略')"
        ]
    ),
    
    ToolDefinition(
        name="search_products",
        description="""搜索理财产品数据库，根据条件筛选产品。

返回产品列表，包含产品名称、风险等级、预期收益率、投资期限等信息。""",
        parameters=[
            ToolParameter(
                name="product_type",
                type="string",
                description="产品风险等级：R1(保守型)、R2(稳健型)、R3(平衡型)、R4(进取型)、R5(激进型)",
                required=False,
                enum=["R1", "R2", "R3", "R4", "R5"]
            ),
            ToolParameter(
                name="min_rate",
                type="number",
                description="最低预期年化收益率（百分比），如 5.0 表示 5%",
                required=False
            ),
            ToolParameter(
                name="max_rate",
                type="number",
                description="最高预期年化收益率（百分比）",
                required=False
            ),
            ToolParameter(
                name="min_term",
                type="integer",
                description="最短投资期限（天）",
                required=False
            ),
            ToolParameter(
                name="max_term",
                type="integer",
                description="最长投资期限（天）",
                required=False
            )
        ],
        when_to_use="当用户想要推荐、查找、搜索具体理财产品时使用",
        examples=[
            "用户问'推荐一些稳健的产品' → 调用 search_products(product_type='R2')",
            "用户问'有没有收益5%以上的产品？' → 调用 search_products(min_rate=5.0)",
            "用户问'短期理财产品有哪些？' → 调用 search_products(max_term=90)"
        ]
    ),
    
    ToolDefinition(
        name="calculate_income",
        description="""计算投资收益，支持一次性投资和定期定额投资（定投）。

返回预期收益金额、年化收益率、总投资金额等信息。""",
        parameters=[
            ToolParameter(
                name="amount",
                type="number",
                description="投资金额（元），如 10000 表示 1万元",
                required=True
            ),
            ToolParameter(
                name="annual_rate",
                type="number",
                description="年化收益率（百分比），如 5.0 表示 5%",
                required=True
            ),
            ToolParameter(
                name="term_days",
                type="integer",
                description="投资期限（天）",
                required=True
            ),
            ToolParameter(
                name="invest_type",
                type="string",
                description="投资方式：one_time(一次性投资) 或 regular(定期定额)",
                required=False,
                enum=["one_time", "regular"]
            ),
            ToolParameter(
                name="invest_period",
                type="integer",
                description="定投周期（天），仅当 invest_type=regular 时有效",
                required=False
            )
        ],
        when_to_use="当用户想要计算投资收益、利息、回报时使用",
        examples=[
            "用户问'10万元存365天，5%收益能赚多少？' → 调用 calculate_income(amount=100000, annual_rate=5.0, term_days=365)",
            "用户问'每月定投1000元，年化6%，投1年能赚多少？' → 调用 calculate_income(amount=1000, annual_rate=6.0, term_days=365, invest_type='regular', invest_period=30)"
        ]
    ),
    
    ToolDefinition(
        name="get_user_asset",
        description="""查询当前用户的资产情况，包括总资产、可用余额、持仓明细、累计收益等。

系统会自动识别当前登录用户，无需传递用户ID。""",
        parameters=[],
        when_to_use="当用户想要查看自己的资产、余额、持仓情况时使用",
        examples=[
            "用户问'我的资产有多少？' → 调用 get_user_asset()",
            "用户问'查看我的账户余额' → 调用 get_user_asset()"
        ],
        requires_user_context=True
    ),
    
    ToolDefinition(
        name="create_order",
        description="""创建理财订单，购买理财产品。

需要产品ID和购买金额。创建前请确认用户已了解产品信息。
系统会自动识别当前登录用户，无需传递用户ID。""",
        parameters=[
            ToolParameter(
                name="product_id",
                type="integer",
                description="产品ID",
                required=True
            ),
            ToolParameter(
                name="amount",
                type="number",
                description="购买金额（元）",
                required=True
            )
        ],
        when_to_use="当用户明确要购买某个理财产品时使用",
        examples=[
            "用户说'我要购买产品ID为1的产品，买1万元' → 调用 create_order(product_id=1, amount=10000)"
        ],
        requires_user_context=True
    ),
    
    ToolDefinition(
        name="query_orders",
        description="""查询当前用户的订单记录，包括历史订单、持仓订单等。

返回订单列表，包含订单状态、产品信息、金额等。
系统会自动识别当前登录用户，无需传递用户ID。""",
        parameters=[],
        when_to_use="当用户想要查看自己的订单、购买记录、持仓情况时使用",
        examples=[
            "用户问'我的订单有哪些？' → 调用 query_orders()",
            "用户问'我买过什么产品？' → 调用 query_orders()"
        ],
        requires_user_context=True
    ),
    
    ToolDefinition(
        name="get_user_risk_level",
        description="""查询当前用户的风险测评结果。

返回用户是否已完成风险测评，以及当前的风险等级（C1-C5）。
系统会自动识别当前登录用户，无需传递用户ID。

风险等级说明：
- C1: 保守型（适合R1产品）
- C2: 谨慎型（适合R1-R2产品）
- C3: 稳健型（适合R1-R3产品）
- C4: 进取型（适合R1-R4产品）
- C5: 激进型（适合所有产品）""",
        parameters=[],
        when_to_use="当需要了解用户的风险承受能力时使用，特别是在推荐理财产品之前，应先查询用户的风险等级",
        examples=[
            "用户问'推荐适合我的产品' → 先调用 get_user_risk_level() 查询风险等级",
            "用户问'我能买什么产品？' → 先调用 get_user_risk_level() 查询风险等级"
        ],
        requires_user_context=True
    ),
    
    ToolDefinition(
        name="submit_risk_assessment",
        description="""提交风险测评问卷，评估用户风险承受能力。

需要用户提供测评答案，返回风险等级（C1-C5）。
系统会自动识别当前登录用户，无需传递用户ID。""",
        parameters=[
            ToolParameter(
                name="answers",
                type="array",
                description="测评答案列表，每个答案包含 question_id 和 answer",
                required=True
            )
        ],
        when_to_use="当用户完成风险测评问卷，需要提交答案时使用",
        examples=[
            "用户完成测评后 → 调用 submit_risk_assessment(answers=[...])"
        ],
        requires_user_context=True
    )
]


def get_tool_descriptions() -> str:
    """生成用于 LLM 的工具描述文本"""
    descriptions = []
    for tool in TOOL_DEFINITIONS:
        desc = f"【{tool.name}】\n"
        desc += f"功能：{tool.description}\n"
        if tool.parameters:
            desc += "参数：\n"
            for param in tool.parameters:
                required = "必填" if param.required else "可选"
                enum_info = f"，可选值：{', '.join(param.enum)}" if param.enum else ""
                desc += f"  - {param.name}({param.type}, {required})：{param.description}{enum_info}\n"
        else:
            desc += "参数：无（系统自动识别当前用户）\n"
        desc += f"使用场景：{tool.when_to_use}\n"
        descriptions.append(desc)
    return "\n".join(descriptions)


def get_tool_by_name(name: str) -> ToolDefinition:
    """根据名称获取工具定义"""
    for tool in TOOL_DEFINITIONS:
        if tool.name == name:
            return tool
    return None


def get_tool_names() -> List[str]:
    """获取所有工具名称"""
    return [tool.name for tool in TOOL_DEFINITIONS]
