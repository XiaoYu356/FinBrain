import os
from typing import List
from langchain_text_splitters import RecursiveCharacterTextSplitter


class DocumentLoader:
    def __init__(self, chunk_size: int = 500, chunk_overlap: int = 50):
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separators=["\n\n", "\n", "。", "！", "？", "；", "，", " ", ""]
        )
    
    def load_text_file(self, file_path: str) -> List[dict]:
        with open(file_path, "r", encoding="utf-8") as f:
            content = f.read()
        
        return self._split_text(content, {"source": file_path})
    
    def load_directory(self, directory: str) -> List[dict]:
        documents = []
        
        for root, _, files in os.walk(directory):
            for file in files:
                if file.endswith((".txt", ".md")):
                    file_path = os.path.join(root, file)
                    documents.extend(self.load_text_file(file_path))
        
        return documents
    
    def load_from_string(self, content: str, metadata: dict = None) -> List[dict]:
        return self._split_text(content, metadata or {})
    
    def _split_text(self, content: str, metadata: dict) -> List[dict]:
        chunks = self.text_splitter.split_text(content)
        
        return [
            {
                "content": chunk,
                "metadata": metadata
            }
            for chunk in chunks
        ]
