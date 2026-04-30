from typing import Dict, Any
from java_client import java_client


async def get_user_asset(user_id: int) -> Dict[str, Any]:
    result = await java_client.get_user_asset(user_id)
    
    if result.get("code") == 200:
        return {"success": True, "asset": result.get("data", {})}
    return {"success": False, "message": result.get("message", "获取资产失败")}
