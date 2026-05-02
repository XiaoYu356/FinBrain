import httpx
from typing import Any, Dict, Optional
from config import get_settings


class FinBrainClient:
    def __init__(self):
        self.settings = get_settings()
        self.base_url = self.settings.JAVA_API_BASE
        self.timeout = 30.0
    
    async def _request(
        self, 
        method: str, 
        endpoint: str, 
        params: Optional[Dict] = None,
        json_data: Optional[Dict] = None
    ) -> Dict[str, Any]:
        url = f"{self.base_url}{endpoint}"
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            if method == "GET":
                response = await client.get(url, params=params)
            elif method == "POST":
                response = await client.post(url, params=params, json=json_data)
            else:
                raise ValueError(f"Unsupported method: {method}")
            
            response.raise_for_status()
            return response.json()
    
    async def search_products(
        self,
        product_type: Optional[str] = None,
        min_rate: Optional[float] = None,
        max_rate: Optional[float] = None,
        min_term: Optional[int] = None,
        max_term: Optional[int] = None
    ) -> Dict[str, Any]:
        params = {}
        if product_type:
            params["productType"] = product_type
        if min_rate is not None:
            params["minRate"] = min_rate
        if max_rate is not None:
            params["maxRate"] = max_rate
        if min_term is not None:
            params["minTerm"] = min_term
        if max_term is not None:
            params["maxTerm"] = max_term
        
        return await self._request("POST", "/ai-tools/search-products", params=params)
    
    async def calculate_income(
        self,
        amount: float,
        annual_rate: float,
        term_days: int,
        invest_type: str = "one_time",
        invest_period: int = 1
    ) -> Dict[str, Any]:
        json_data = {
            "amount": amount,
            "annualRate": annual_rate,
            "termDays": term_days,
            "investType": invest_type,
            "investPeriod": invest_period
        }
        return await self._request("POST", "/ai-tools/calculate-income", json_data=json_data)
    
    async def get_user_asset(self, user_id: int) -> Dict[str, Any]:
        return await self._request("GET", f"/ai-tools/user-asset/{user_id}")
    
    async def create_order(
        self, 
        user_id: int, 
        product_id: int, 
        amount: float
    ) -> Dict[str, Any]:
        params = {
            "userId": user_id,
            "productId": product_id,
            "amount": amount
        }
        return await self._request("POST", "/ai-tools/create-order", params=params)
    
    async def get_orders(self, user_id: int) -> Dict[str, Any]:
        return await self._request("GET", f"/ai-tools/orders/{user_id}")
    
    async def submit_risk_assessment(
        self, 
        user_id: int, 
        answers: list
    ) -> Dict[str, Any]:
        return await self._request(
            "POST", 
            "/ai-tools/risk-assessment",
            params={"userId": user_id},
            json_data={"answers": answers}
        )
    
    async def get_chat_history(self, user_id: int, session_id: str) -> Dict[str, Any]:
        return await self._request(
            "GET",
            f"/ai-tools/chat-history/{session_id}",
            params={"userId": user_id}
        )
    
    async def save_chat_message(
        self,
        user_id: int,
        session_id: str,
        role: str,
        content: str
    ) -> Dict[str, Any]:
        json_data = {
            "userId": user_id,
            "sessionId": session_id,
            "role": role,
            "content": content
        }
        return await self._request(
            "POST",
            "/ai-tools/chat-message",
            json_data=json_data
        )
    
    async def save_chat_messages(
        self,
        user_id: int,
        session_id: str,
        messages: list
    ) -> Dict[str, Any]:
        json_data = {
            "userId": user_id,
            "sessionId": session_id,
            "messages": messages
        }
        return await self._request(
            "POST",
            "/ai-tools/chat-messages",
            json_data=json_data
        )
