"""
Combine CNN-based predictions with OCR keyword matches to infer the most likely museum.
"""

from __future__ import annotations

import json
import logging
from functools import lru_cache
from pathlib import Path
from typing import Dict, List, Optional, Tuple

LOGGER = logging.getLogger(__name__)

DEFAULT_KEYWORD_PATH = (
    Path(__file__).resolve().parent.parent / "data" / "museum_keywords.json"
)


class KeywordNotFoundError(RuntimeError):
    """Raised when the keyword mapping file does not exist."""


@lru_cache(maxsize=1)
def load_keyword_map(path: Optional[Path] = None) -> Dict[str, List[str]]:
    """
    Load museum keyword mappings from JSON.
    """
    keyword_path = Path(path or DEFAULT_KEYWORD_PATH)
    if not keyword_path.exists():
        raise KeywordNotFoundError(f"키워드 파일을 찾을 수 없습니다: {keyword_path}")
    data = json.loads(keyword_path.read_text(encoding="utf-8"))
    normalized = {
        name: [keyword.lower() for keyword in keywords] for name, keywords in data.items()
    }
    LOGGER.info("키워드 맵 로드 완료 (entries=%d)", len(normalized))
    return normalized


def match_keywords(
    texts: List[str],
    keyword_map: Dict[str, List[str]],
    min_match_ratio: float = 0.34,
) -> Optional[Tuple[str, float, List[str]]]:
    """
    Return the best matching museum based on OCR text.

    Returns tuple of (label, score, matched_keywords) if the best score exceeds threshold.
    Score is matched_keyword_count / total_keywords.
    """
    if not texts:
        return None

    joined_text = " ".join(text.lower() for text in texts)
    best_label: Optional[str] = None
    best_score: float = 0.0
    best_keywords: List[str] = []

    for label, keywords in keyword_map.items():
        matched = [kw for kw in keywords if kw in joined_text]
        if not matched:
            continue
        score = len(matched) / len(keywords)
        if score > best_score:
            best_score = score
            best_label = label
            best_keywords = matched

    if best_label and best_score >= min_match_ratio:
        return best_label, best_score, best_keywords
    return None


def combine_results(
    classifier_results: List[Dict[str, float]],
    ocr_texts: List[str],
    keyword_map: Dict[str, List[str]],
    min_match_ratio: float = 0.3,
    ocr_weight: float = 0.9,
) -> Dict[str, object]:
    """
    Combine classifier predictions with OCR keyword matches.

    Returns a dictionary describing the final decision and supporting evidence.
    """
    top_classifier = classifier_results[0] if classifier_results else None

    keyword_match = match_keywords(ocr_texts, keyword_map, min_match_ratio=min_match_ratio)
    if keyword_match:
        label, score, matched_keywords = keyword_match
        confidence = min(1.0, ocr_weight * (0.6 + 0.4 * score))
        LOGGER.info(
            "OCR 기반 결론: %s (score=%.2f, keywords=%s)",
            label,
            score,
            matched_keywords,
        )
        return {
            "source": "ocr",
            "label": label,
            "keywords": matched_keywords,
            "classifier_top1": top_classifier,
        }

    if top_classifier:
        LOGGER.info(
            "분류기 기반 결론: %s (confidence=%.2f)",
            top_classifier["label"],
            top_classifier["confidence"],
        )
        return {
            "source": "classifier",
            "label": top_classifier["label"],
            "keywords": [],
            "classifier_top3": classifier_results[:3],
        }

    return {
        "source": "unknown",
        "label": None,
        "keywords": [],
    }