from minio import Minio
from config import get_settings

_settings = get_settings()

minio_client = Minio(
    f"{_settings.MINIO_ENDPOINT}:{_settings.MINIO_PORT}",
    access_key=_settings.MINIO_ACCESS_KEY,
    secret_key=_settings.MINIO_SECRET_KEY,
    secure=False
)


def get_document_content(bucket_name: str, object_name: str, file_type: str) -> str:
    from rag.document_loader import DocumentLoader

    response = minio_client.get_object(bucket_name, object_name)
    content = response.read()

    filename = object_name.split("/")[-1] if "/" in object_name else object_name

    return DocumentLoader.load_from_bytes(content, filename)
