from pydantic_settings import BaseSettings
from functools import lru_cache
import os
from pathlib import Path


def find_env_file() -> str:
    current_dir = Path(__file__).resolve().parent
    env_file = current_dir / ".env"
    if env_file.exists():
        return str(env_file)
    
    parent_env = current_dir.parent / ".env"
    if parent_env.exists():
        return str(parent_env)
    
    return ".env"


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
    
    MINIO_ENDPOINT: str = "localhost"
    MINIO_PORT: int = 9000
    MINIO_ACCESS_KEY: str = "minioadmin"
    MINIO_SECRET_KEY: str = "minioadmin"
    MINIO_BUCKET: str = "finbrain-docs"
    
    LANGSMITH_API_KEY: str = ""
    LANGSMITH_PROJECT: str = "finbrain"
    LANGCHAIN_TRACING_V2: bool = True
    
    class Config:
        env_file = find_env_file()
        env_file_encoding = "utf-8"
        extra = "ignore"


@lru_cache()
def get_settings() -> Settings:
    settings = Settings()
    
    if settings.LANGSMITH_API_KEY:
        os.environ["LANGSMITH_API_KEY"] = settings.LANGSMITH_API_KEY
        os.environ["LANGSMITH_PROJECT"] = settings.LANGSMITH_PROJECT
        os.environ["LANGCHAIN_TRACING_V2"] = "true"
    
    return settings
