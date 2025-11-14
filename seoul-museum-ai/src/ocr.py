"""
OCR utilities for extracting text from museum entrance photos.
"""

from __future__ import annotations

import io
import logging
from functools import lru_cache
from typing import List, Tuple

import easyocr
import numpy as np
from PIL import Image

LOGGER = logging.getLogger(__name__)


@lru_cache(maxsize=1)
def _get_reader(langs: Tuple[str, ...] = ("ko", "en")) -> easyocr.Reader:
    """
    Cached EasyOCR Reader instance. Default languages: Korean + English.
    """
    LOGGER.info("EasyOCR 리더 초기화 (langs=%s)", langs)
    return easyocr.Reader(list(langs), gpu=False)


def extract_text(image_bytes: bytes, min_confidence: float = 0.3) -> List[str]:
    """
    Extract text snippets from image bytes using EasyOCR.

    Args:
        image_bytes: Raw image bytes (JPEG/PNG/etc)
        min_confidence: Minimum confidence threshold (0.0-1.0)

    Returns a list of text strings filtered by confidence.
    """
    try:
        # Convert bytes to PIL Image, then to numpy array
        img = Image.open(io.BytesIO(image_bytes))
        # Convert RGBA to RGB if needed
        if img.mode == "RGBA":
            img = img.convert("RGB")
        img_array = np.array(img)
        
        reader = _get_reader()
        # EasyOCR expects numpy array
        results = reader.readtext(img_array, detail=1)  # returns (bbox, text, confidence)
        filtered = [text.strip() for _, text, conf in results if conf >= min_confidence and text.strip()]
        LOGGER.info("OCR 추출 결과: %s (신뢰도 임계값: %.2f)", filtered, min_confidence)
        return filtered
    except Exception as exc:
        LOGGER.warning("OCR 추출 실패: %s", exc, exc_info=True)
        return []

