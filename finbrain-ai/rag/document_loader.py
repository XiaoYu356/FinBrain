import logging
import os
from typing import List, Optional

from langchain_text_splitters import RecursiveCharacterTextSplitter

from rag.image_loader import ImageLoader
from rag.pdf_loader import PDFLoader
from rag.doc_loader import DocLoader
from rag.ppt_loader import PPTLoader
from rag.csv_loader import CSVLoader

logger = logging.getLogger(__name__)


class DocumentLoader:
    def __init__(self, chunk_size: int = 1000, chunk_overlap: int = 100):
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separators=["\n\n", "\n", "。", "！", "？", "；", "，", " ", ""]
        )

    @classmethod
    def get_extension(cls, filename: str) -> str:
        return "." + filename.rsplit(".", 1)[-1].lower() if "." in filename else ""

    @classmethod
    def load_from_bytes(
        cls,
        file_data: bytes,
        filename: str,
        csv_columns: Optional[List[str]] = None,
    ) -> str:
        ext = cls.get_extension(filename)
        logger.info(f"Loading document: {filename}, extension: {ext}, size: {len(file_data)} bytes")

        try:
            if ImageLoader.is_supported(filename):
                logger.info(f"Using ImageLoader for {filename}")
                result = ImageLoader.load_from_bytes(file_data)
                logger.info(f"ImageLoader extracted {len(result)} characters")
                return result

            if PDFLoader.is_supported(filename):
                logger.info(f"Using PDFLoader for {filename}")
                result = PDFLoader.load_from_bytes(file_data)
                logger.info(f"PDFLoader extracted {len(result)} characters")
                return result

            if DocLoader.is_supported(filename):
                logger.info(f"Using DocLoader for {filename}")
                result = DocLoader.load_from_bytes(file_data)
                logger.info(f"DocLoader extracted {len(result)} characters")
                return result

            if PPTLoader.is_supported(filename):
                logger.info(f"Using PPTLoader for {filename}")
                result = PPTLoader.load_from_bytes(file_data)
                logger.info(f"PPTLoader extracted {len(result)} characters")
                return result

            if CSVLoader.is_supported(filename):
                logger.info(f"Using CSVLoader for {filename}")
                result = CSVLoader.load_from_bytes(file_data, columns_to_read=csv_columns)
                logger.info(f"CSVLoader extracted {len(result)} characters")
                return result

            if ext in {".txt", ".md"}:
                logger.info(f"Using text decoder for {filename}")
                result = file_data.decode("utf-8", errors="ignore")
                logger.info(f"Text decoder extracted {len(result)} characters")
                return result

            logger.warning(f"Unknown file type: {ext}, trying UTF-8 decode")
            return file_data.decode("utf-8", errors="ignore")

        except Exception as e:
            logger.error(f"Failed to load document {filename}: {e}", exc_info=True)
            raise

    def load_text_file(self, file_path: str) -> List[dict]:
        filename = os.path.basename(file_path)
        with open(file_path, "rb") as f:
            content = self.load_from_bytes(f.read(), filename)

        return self._split_text(content, {"source": file_path})

    def load_directory(self, directory: str) -> List[dict]:
        documents = []

        for root, _, files in os.walk(directory):
            for file in files:
                file_path = os.path.join(root, file)
                try:
                    documents.extend(self.load_text_file(file_path))
                except Exception as e:
                    logger.warning(f"Failed to load {file_path}: {e}")

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

    @classmethod
    def get_supported_extensions(cls) -> set:
        return (
            ImageLoader.SUPPORTED_EXTENSIONS
            | PDFLoader.SUPPORTED_EXTENSIONS
            | DocLoader.SUPPORTED_EXTENSIONS
            | PPTLoader.SUPPORTED_EXTENSIONS
            | CSVLoader.SUPPORTED_EXTENSIONS
            | {".txt", ".md"}
        )
