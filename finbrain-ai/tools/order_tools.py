from typing import Dict, Any
from java_client import java_client


async def create_order(user_id: int, product_id: int, amount: float) -> Dict[str, Any]:
    result = await java_client.create_order(
        user_id=user_id,
        product_id=product_id,
        amount=amount
    )
    
    if result.get("code") == 200:
        data = result.get("data", {})
        if data.get("success"):
            return {"success": True, "order": data}
        return {"success": False, "message": data.get("message", "下单失败")}
    return {"success": False, "message": result.get("message", "下单失败")}


async def query_orders(user_id: int) -> Dict[str, Any]:
    result = await java_client.get_orders(user_id)
    
    if result.get("code") == 200:
        return {"success": True, "orders": result.get("data", [])}
    return {"success": False, "message": result.get("message", "查询订单失败")}
