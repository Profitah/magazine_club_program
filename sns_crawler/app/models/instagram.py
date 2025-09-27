from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

class InstagramThumbnail(BaseModel):
    """인스타그램 썸네일 이미지 모델"""
    thumbnail_url: str
    post_id: Optional[str] = None
    caption: Optional[str] = None
    author: Optional[str] = None
    post_date: Optional[datetime] = None
    like_count: Optional[int] = None
    comment_count: Optional[int] = None
    post_type: Optional[str] = "image"  # image, video, carousel

class InstagramResponse(BaseModel):
    """인스타그램 크롤링 응답 모델"""
    username: str
    thumbnails: List[InstagramThumbnail]
    total_count: int
    crawled_at: datetime
    success: bool = True
    message: Optional[str] = None
