import pytest
from httpx import AsyncClient
from main import app


@pytest.fixture
def client():
    return AsyncClient(app=app, base_url="http://test")


class TestHealthAPI:
    
    @pytest.mark.asyncio
    async def test_health_check(self, client):
        response = await client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"


class TestChatAPI:
    
    @pytest.mark.asyncio
    async def test_chat_without_auth(self, client):
        response = await client.post(
            "/api/chat",
            json={"message": "你好", "session_id": "test-session"}
        )
        assert response.status_code in [200, 401]
    
    @pytest.mark.asyncio
    async def test_chat_with_invalid_input(self, client):
        response = await client.post(
            "/api/chat",
            json={"message": "", "session_id": "test-session"}
        )
        assert response.status_code in [400, 401, 422]


class TestToolsAPI:
    
    @pytest.mark.asyncio
    async def test_list_tools(self, client):
        response = await client.get("/api/tools")
        assert response.status_code in [200, 401]
