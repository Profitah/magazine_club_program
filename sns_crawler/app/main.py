from fastapi import FastAPI, HTTPException, Query, File, UploadFile, Form
from fastapi.middleware.cors import CORSMiddleware
from typing import List, Optional
from datetime import datetime
import uvicorn
import logging
from dotenv import load_dotenv

# .env 파일 로드
load_dotenv()

from app.utils.helpers import (
    upload_bytes_to_s3
)

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="SNS Crawler Service", 
    description="매거진 클럽을 위한 SNS 크롤링 서비스",
    version="1.0.0"
)

# CORS 설정 (Java Spring Boot와 통신을 위해)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 프로덕션에서는 특정 도메인만 허용
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
async def root():
    """서비스 상태 확인"""
    return {
        "message": "SNS Crawler Service is running",
        "service": "매거진 클럽 SNS 크롤러",
        "version": "1.0.0",
        "timestamp": datetime.now().isoformat()
    }

@app.get("/health")
async def health_check():
    """헬스 체크"""
    return {
        "status": "healthy", 
        "timestamp": datetime.now().isoformat(),
        "service": "sns_crawler"
    }

@app.post("/images/upload")
async def upload_image_to_s3(
    file: UploadFile = File(..., description="업로드할 이미지 파일"),
    username: str = Form("user")
):
    """사용자가 업로드한 이미지를 S3에 저장하고 퍼블릭 URL 반환"""
    try:
        content = await file.read()
        if not content:
            raise HTTPException(status_code=400, detail="빈 파일입니다.")
        url = upload_bytes_to_s3(content, file.filename, username=username)
        return {
            "username": username,
            "uploaded": url,
            "success": True
        }
    except Exception as e:
        logger.error(f"파일 업로드 실패: {e}")
        raise HTTPException(status_code=500, detail=f"S3 업로드 실패: {str(e)}")

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000, reload=True)