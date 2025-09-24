from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from typing import List, Optional
from datetime import datetime
import uvicorn
import logging

from app.models.instagram import InstagramThumbnail, InstagramResponse
from app.services.instagram_crawler import InstagramCrawler
from app.utils.helpers import (
    convert_urls_to_base64, 
    validate_instagram_username,
    format_thumbnail_data
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

# 크롤러 인스턴스
crawler = InstagramCrawler()

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

@app.get("/instagram/thumbnails/{username}")
async def get_thumbnails(
    username: str, 
    use_selenium: bool = Query(False, description="Selenium 사용 여부"),
    as_base64: bool = Query(False, description="Base64 인코딩 여부")
):
    """인스타그램 썸네일 이미지 URL 조회"""
    try:
        # 사용자명 유효성 검사
        if not validate_instagram_username(username):
            raise HTTPException(status_code=400, detail="유효하지 않은 인스타그램 사용자명입니다.")
        
        logger.info(f"인스타그램 썸네일 조회 시작: {username}")
        
        # 썸네일 URL 크롤링
        thumbnail_urls = crawler.get_thumbnails(username, use_selenium)
        
        if not thumbnail_urls:
            return InstagramResponse(
                username=username,
                thumbnails=[],
                total_count=0,
                crawled_at=datetime.now(),
                success=False,
                message="썸네일을 찾을 수 없습니다."
            )
        
        # Base64 변환 요청 시
        if as_base64:
            from app.utils.helpers import convert_urls_to_base64
            base64_images = convert_urls_to_base64(thumbnail_urls)
            return {
                "username": username,
                "base64_images": base64_images,
                "count": len(base64_images),
                "crawled_at": datetime.now().isoformat(),
                "success": True
            }
        
        # 일반 URL 반환
        thumbnails = [
            InstagramThumbnail(
                thumbnail_url=url,
                post_id=f"{username}_{i+1}",
                author=username,
                post_type="image"
            )
            for i, url in enumerate(thumbnail_urls)
        ]
        
        response = InstagramResponse(
            username=username,
            thumbnails=thumbnails,
            total_count=len(thumbnails),
            crawled_at=datetime.now(),
            success=True,
            message=f"{len(thumbnails)}개의 썸네일을 성공적으로 조회했습니다."
        )
        
        logger.info(f"인스타그램 썸네일 조회 완료: {username} - {len(thumbnails)}개")
        return response
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"인스타그램 썸네일 조회 실패: {e}")
        raise HTTPException(status_code=500, detail=f"크롤링 실패: {str(e)}")

@app.get("/instagram/thumbnails/urls/{username}")
async def get_thumbnail_urls(
    username: str, 
    use_selenium: bool = Query(False, description="Selenium 사용 여부")
):
    """썸네일 URL만 간단하게 반환"""
    try:
        if not validate_instagram_username(username):
            raise HTTPException(status_code=400, detail="유효하지 않은 인스타그램 사용자명입니다.")
        
        thumbnail_urls = crawler.get_thumbnails(username, use_selenium)
        
        return {
            "username": username,
            "thumbnail_urls": thumbnail_urls,
            "count": len(thumbnail_urls),
            "crawled_at": datetime.now().isoformat(),
            "success": len(thumbnail_urls) > 0
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"썸네일 URL 조회 실패: {e}")
        raise HTTPException(status_code=500, detail=f"크롤링 실패: {str(e)}")


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000, reload=True)