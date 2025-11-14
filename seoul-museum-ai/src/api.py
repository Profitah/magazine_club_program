"""
FastAPI application that exposes the museum entrance classification model.
"""

from __future__ import annotations

import asyncio
import logging
import os
from functools import partial
from pathlib import Path
from typing import Any, Dict, List, Optional

from fastapi import FastAPI, File, HTTPException, UploadFile, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from .ensemble import combine_results, load_keyword_map
from .predict import ModelNotReadyError, get_classifier, warmup
from .ocr import extract_text

LOGGER = logging.getLogger(__name__)
logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))

MODEL_PATH = os.getenv("MODEL_PATH")
CLASS_MAP_PATH = os.getenv("CLASS_MAP_PATH")
DEFAULT_TOPK = int(os.getenv("DEFAULT_TOPK", "3"))
KEYWORD_PATH = os.getenv("KEYWORD_PATH")
OCR_MIN_CONFIDENCE = float(os.getenv("OCR_MIN_CONFIDENCE", "0.45"))
OCR_MIN_MATCH_RATIO = float(os.getenv("OCR_MIN_MATCH_RATIO", "0.3"))
OCR_WEIGHT = float(os.getenv("OCR_WEIGHT", "0.9"))

app = FastAPI(
    title="Seoul Museum Entrance Classifier",
    description="정문 사진을 입력하면 미술관을 추정해 주는 추론 API",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=os.getenv("CORS_ALLOW_ORIGINS", "*").split(","),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
async def startup_event() -> None:
    """
    Preload the model at startup to reduce the latency of the first request.
    """
    try:
        classifier = get_classifier(MODEL_PATH, CLASS_MAP_PATH)
        loop = asyncio.get_running_loop()
        await loop.run_in_executor(None, warmup, classifier)
        LOGGER.info("모델 워밍업 완료")
        load_keyword_map(Path(KEYWORD_PATH) if KEYWORD_PATH else None)
        LOGGER.info("키워드 사전 로드 완료")
    except ModelNotReadyError as error:
        LOGGER.error("모델 로딩 실패: %s", error)
        raise
    except Exception as exc:  # pragma: no cover - defensive logging
        LOGGER.exception("워밍업 도중 예기치 못한 오류 발생", exc_info=exc)
        raise


@app.get("/health", summary="헬스 체크")
async def health() -> Dict[str, Any]:
    """
    Check application readiness.
    """
    try:
        classifier = get_classifier(MODEL_PATH, CLASS_MAP_PATH)
        return {"status": "ok", "classes": len(classifier.class_names)}
    except ModelNotReadyError as error:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(error)) from error


@app.post(
    "/predict",
    summary="정문 이미지로 미술관 분류",
    response_description="Top-K 예측 결과",
)
async def predict(
    top_k: int = DEFAULT_TOPK,
    include_text: bool = False,
    include_decision: bool = True,
    file: UploadFile = File(...),
) -> JSONResponse:
    """
    Run inference on the uploaded image.
    """
    content_type = file.content_type or ""
    if not content_type.startswith("image/"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"이미지 파일만 업로드할 수 있습니다. (받은 컨텐츠 타입: {content_type})",
        )

    image_bytes = await file.read()
    if len(image_bytes) == 0:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="빈 파일입니다.")

    try:
        classifier = get_classifier(MODEL_PATH, CLASS_MAP_PATH)
        loop = asyncio.get_running_loop()
        predictions: List[Dict[str, float]] = await loop.run_in_executor(
            None,
            classifier.predict,
            image_bytes,
            top_k,
        )

        need_ocr = include_text or include_decision
        ocr_text: List[str] = []
        if need_ocr:
            extract_callable = partial(extract_text, image_bytes, OCR_MIN_CONFIDENCE)
            ocr_text = await loop.run_in_executor(None, extract_callable)

        decision: Optional[Dict[str, Any]] = None
        if include_decision:
            keyword_map = load_keyword_map(Path(KEYWORD_PATH) if KEYWORD_PATH else None)
            decision = combine_results(
                predictions,
                ocr_text,
                keyword_map,
                min_match_ratio=OCR_MIN_MATCH_RATIO,
                ocr_weight=OCR_WEIGHT,
            )
    except ModelNotReadyError as error:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(error)) from error
    except Exception as exc:  # pragma: no cover - defensive logging
        LOGGER.exception("예측 중 오류 발생", exc_info=exc)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="예측 중 오류가 발생했습니다.")

    return JSONResponse(
        {
            "top_k": top_k,
            "results": [
                {"rank": idx + 1, "label": entry["label"], "confidence": entry["confidence"]}
                for idx, entry in enumerate(predictions)
            ],
            "ocr_text": ocr_text if include_text else None,
            "decision": decision,
        }
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "src.api:app",
        host=os.getenv("HOST", "0.0.0.0"),
        port=int(os.getenv("PORT", "8000")),
        reload=os.getenv("UVICORN_RELOAD", "false").lower() == "true",
    )

