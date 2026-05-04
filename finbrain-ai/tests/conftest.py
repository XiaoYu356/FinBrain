import pytest
import os


@pytest.fixture(scope="session")
def test_config():
    return {
        "java_api_base": os.getenv("JAVA_API_BASE", "http://localhost:8080"),
        "test_user_id": os.getenv("TEST_USER_ID", "1"),
        "test_session_id": os.getenv("TEST_SESSION_ID", "test-session-001")
    }


@pytest.fixture(autouse=True)
def reset_mocks():
    yield
