import pytest
from unittest.mock import Mock, patch
from tools.product_tools import search_products, get_product_detail
from tools.risk_tools import assess_risk
from tools.order_tools import get_user_orders, calculate_income


class TestProductTools:
    
    @pytest.mark.asyncio
    async def test_search_products_empty_result(self):
        with patch("tools.product_tools.requests.get") as mock_get:
            mock_get.return_value = Mock(
                status_code=200,
                json=lambda: {"data": {"records": [], "total": 0}}
            )
            
            result = await search_products("测试产品")
            
            assert result is not None
    
    @pytest.mark.asyncio
    async def test_search_products_with_results(self):
        with patch("tools.product_tools.requests.get") as mock_get:
            mock_get.return_value = Mock(
                status_code=200,
                json=lambda: {
                    "data": {
                        "records": [
                            {"id": 1, "productName": "测试产品", "annualReturnRate": 5.0}
                        ],
                        "total": 1
                    }
                }
            )
            
            result = await search_products("测试")
            
            assert result is not None
    
    @pytest.mark.asyncio
    async def test_get_product_detail_not_found(self):
        with patch("tools.product_tools.requests.get") as mock_get:
            mock_get.return_value = Mock(
                status_code=404,
                json=lambda: {"message": "产品不存在"}
            )
            
            result = await get_product_detail(999)
            
            assert result is not None


class TestRiskTools:
    
    @pytest.mark.asyncio
    async def test_assess_risk_conservative(self):
        answers = {
            "age": "50",
            "income": "stable",
            "experience": "none",
            "risk_tolerance": "low",
            "investment_goal": "preserve"
        }
        
        with patch("tools.risk_tools.requests.post") as mock_post:
            mock_post.return_value = Mock(
                status_code=200,
                json=lambda: {
                    "data": {
                        "riskLevel": "R1",
                        "riskType": "保守型"
                    }
                }
            )
            
            result = await assess_risk(answers)
            
            assert result is not None
    
    @pytest.mark.asyncio
    async def test_assess_risk_aggressive(self):
        answers = {
            "age": "25",
            "income": "high",
            "experience": "extensive",
            "risk_tolerance": "high",
            "investment_goal": "growth"
        }
        
        with patch("tools.risk_tools.requests.post") as mock_post:
            mock_post.return_value = Mock(
                status_code=200,
                json=lambda: {
                    "data": {
                        "riskLevel": "R5",
                        "riskType": "进取型"
                    }
                }
            )
            
            result = await assess_risk(answers)
            
            assert result is not None


class TestOrderTools:
    
    @pytest.mark.asyncio
    async def test_get_user_orders_empty(self):
        with patch("tools.order_tools.requests.get") as mock_get:
            mock_get.return_value = Mock(
                status_code=200,
                json=lambda: {"data": {"records": [], "total": 0}}
            )
            
            result = await get_user_orders("test-user")
            
            assert result is not None
    
    @pytest.mark.asyncio
    async def test_calculate_income(self):
        with patch("tools.order_tools.requests.post") as mock_post:
            mock_post.return_value = Mock(
                status_code=200,
                json=lambda: {
                    "data": {
                        "expectedIncome": 123.45,
                        "annualizedRate": 5.0
                    }
                }
            )
            
            result = await calculate_income(
                amount=10000,
                annual_rate=5.0,
                days=90
            )
            
            assert result is not None
