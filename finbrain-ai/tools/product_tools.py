from typing import Optional, Dict, Any
from java_client import java_client


async def search_products(
    product_type: Optional[str] = None,
    min_rate: Optional[float] = None,
    max_rate: Optional[float] = None,
    min_term: Optional[int] = None,
    max_term: Optional[int] = None
) -> Dict[str, Any]:
    result = await java_client.search_products(
        product_type=product_type,
        min_rate=min_rate,
        max_rate=max_rate,
        min_term=min_term,
        max_term=max_term
    )
    
    if result.get("code") == 200:
        return {"success": True, "products": result.get("data", [])}
    return {"success": False, "message": result.get("message", "搜索失败")}


async def calculate_income(
    amount: float,
    annual_rate: float,
    term_days: int,
    invest_type: str = "one_time",
    invest_period: int = 1
) -> Dict[str, Any]:
    result = await java_client.calculate_income(
        amount=amount,
        annual_rate=annual_rate,
        term_days=term_days,
        invest_type=invest_type,
        invest_period=invest_period
    )
    
    if result.get("code") == 200:
        return {"success": True, "data": result.get("data", {})}
    return {"success": False, "message": result.get("message", "计算失败")}
