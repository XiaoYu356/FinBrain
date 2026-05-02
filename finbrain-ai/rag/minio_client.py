from minio import Minio
from config import get_settings
import io

_settings = get_settings()

minio_client = Minio(
    f"{_settings.MINIO_ENDPOINT}:{_settings.MINIO_PORT}",
    access_key=_settings.MINIO_ACCESS_KEY,
    secret_key=_settings.MINIO_SECRET_KEY,
    secure=False
)

def get_document_content(bucket_name: str, object_name: str, file_type: str) -> str:
    response = minio_client.get_object(bucket_name, object_name)
    content = response.read()
    
    if file_type in ['txt', 'md']:
        return content.decode('utf-8')
    
    elif file_type == 'pdf':
        from pypdf import PdfReader
        pdf_reader = PdfReader(io.BytesIO(content))
        text = ""
        for page in pdf_reader.pages:
            text += page.extract_text() + "\n"
        return text
    
    elif file_type == 'docx':
        from docx import Document
        doc = Document(io.BytesIO(content))
        text = ""
        for para in doc.paragraphs:
            text += para.text + "\n"
        return text
    
    else:
        return content.decode('utf-8', errors='ignore')
