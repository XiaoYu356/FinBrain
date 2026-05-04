from typing import Dict, Any, List
from java_client import java_client


async def get_user_risk_level(user_id: int) -> Dict[str, Any]:
    result = await java_client.get_user_risk_level(user_id)
    
    if result.get("code") == 200:
        data = result.get("data", {})
        if data.get("hasAssessment"):
            return {
                "has_assessment": True,
                "score": data.get("score"),
                "risk_level": data.get("riskLevel"),
                "risk_level_desc": data.get("riskLevelDesc"),
                "assessment_time": data.get("assessmentTime")
            }
        return {
            "has_assessment": False,
            "message": data.get("message", "用户尚未完成风险测评")
        }
    return {"has_assessment": False, "message": result.get("message", "查询失败")}


async def submit_risk_assessment(user_id: int, answers: List[Dict[str, str]]) -> Dict[str, Any]:
    result = await java_client.submit_risk_assessment(user_id, answers)
    
    if result.get("code") == 200:
        data = result.get("data", {})
        if data.get("success"):
            return {
                "success": True,
                "score": data.get("score"),
                "risk_level": data.get("riskLevel"),
                "risk_level_desc": data.get("riskLevelDesc")
            }
        return {"success": False, "message": data.get("message", "提交失败")}
    return {"success": False, "message": result.get("message", "提交失败")}
