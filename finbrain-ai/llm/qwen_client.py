from typing import Any, List, Optional
from langchain_core.language_models.llms import LLM
from langchain_core.callbacks import CallbackManagerForLLMRun
import dashscope
from dashscope import Generation
from config import get_settings


class QwenLLM(LLM):
    model: str = "qwen-max"
    temperature: float = 0.7
    max_tokens: int = 2000
    
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        settings = get_settings()
        dashscope.api_key = settings.DASHSCOPE_API_KEY
        if kwargs.get("model"):
            self.model = kwargs["model"]
    
    @property
    def _llm_type(self) -> str:
        return "qwen"
    
    def _call(
        self,
        prompt: str,
        stop: Optional[List[str]] = None,
        run_manager: Optional[CallbackManagerForLLMRun] = None,
        **kwargs: Any,
    ) -> str:
        response = Generation.call(
            model=self.model,
            prompt=prompt,
            temperature=self.temperature,
            max_tokens=self.max_tokens,
            stop=stop,
        )
        
        if response.status_code == 200:
            return response.output.text
        else:
            raise Exception(f"Qwen API error: {response.code} - {response.message}")
    
    @property
    def _identifying_params(self) -> dict:
        return {
            "model": self.model,
            "temperature": self.temperature,
            "max_tokens": self.max_tokens,
        }
