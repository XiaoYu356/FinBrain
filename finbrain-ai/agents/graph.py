from langgraph.graph import StateGraph, END
from agents.state import AgentState
from agents.react_agent import (
    think, act, should_continue, generate_final_response
)


def create_agent_graph():
    workflow = StateGraph(AgentState)
    
    workflow.add_node("think", think)
    workflow.add_node("act", act)
    workflow.add_node("generate_response", generate_final_response)
    
    workflow.set_entry_point("think")
    
    workflow.add_conditional_edges(
        "think",
        should_continue,
        {
            "continue": "act",
            "end": "generate_response"
        }
    )
    
    workflow.add_edge("act", "think")
    workflow.add_edge("generate_response", END)
    
    return workflow.compile()


agent_graph = create_agent_graph()
