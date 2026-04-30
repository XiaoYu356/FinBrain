from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    DASHSCOPE_API_KEY: str = ""
    LLM_MODEL: str = "qwen-max"
    EMBEDDING_MODEL: str = "text-embedding-v1"
    
    MILVUS_HOST: str = "localhost"
    MILVUS_PORT: int = 19530
    
    JAVA_API_BASE: str = "http://localhost:8080"
    
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_PASSWORD: str = ""
    
    LANGSMITH_API_KEY: str = ""
    LANGSMITH_PROJECT: str = "finbrain"
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    return Settings()
