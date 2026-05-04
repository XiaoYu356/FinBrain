import pytest
from unittest.mock import Mock, patch, AsyncMock
from agents.graph import build_graph, AgentState


class TestAgentGraph:
    
    def test_agent_state_initialization(self):
        state = AgentState(
            messages=[],
            user_id="test-user",
            session_id="test-session"
        )
        assert state["messages"] == []
        assert state["user_id"] == "test-user"
        assert state["session_id"] == "test-session"
    
    @patch("agents.graph.build_graph")
    def test_build_graph_returns_compiled_graph(self, mock_build):
        mock_graph = Mock()
        mock_build.return_value = mock_graph
        
        graph = build_graph()
        
        assert graph is not None
        mock_build.assert_called_once()


class TestAgentTools:
    
    def test_product_search_tool_definition(self):
        from tools.product_tools import search_products
        
        assert hasattr(search_products, "name")
        assert hasattr(search_products, "description")
    
    def test_risk_assessment_tool_definition(self):
        from tools.risk_tools import assess_risk
        
        assert hasattr(assess_risk, "name")
        assert hasattr(assess_risk, "description")
