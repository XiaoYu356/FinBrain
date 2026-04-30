from langgraph.graph import StateGraph, END
from agents.state import AgentState
from agents.nodes import detect_intent, route_intent
from agents.tool_executor import execute_tool
from agents.response_generator import rag_search, generate_response


def create_agent_graph():
    workflow = StateGraph(AgentState)
    
    workflow.add_node("detect_intent", detect_intent)
    workflow.add_node("execute_tool", execute_tool)
    workflow.add_node("rag_search", rag_search)
    workflow.add_node("generate_response", generate_response)
    
    workflow.set_entry_point("detect_intent")
    
    workflow.add_conditional_edges(
        "detect_intent",
        route_intent,
        {
            "tool": "execute_tool",
            "rag": "rag_search",
            "chat": "generate_response"
        }
    )
    
    workflow.add_edge("execute_tool", "generate_response")
    workflow.add_edge("rag_search", "generate_response")
    workflow.add_edge("generate_response", END)
    
    return workflow.compile()


agent_graph = create_agent_graph()
