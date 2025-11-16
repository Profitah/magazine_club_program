import base64
import requests
from typing import List, Optional
import logging
import os
import boto3
from botocore.config import Config
from datetime import datetime

logger = logging.getLogger(__name__)

def download_image_as_base64(image_url: str) -> Optional[str]:
    """이미지를 다운로드하여 Base64로 인코딩"""
    try:
        response = requests.get(image_url, timeout=10)
        response.raise_for_status()
        
        # 이미지 데이터를 Base64로 인코딩
        image_base64 = base64.b64encode(response.content).decode('utf-8')
        
        # MIME 타입 결정
        content_type = response.headers.get('content-type', 'image/jpeg')
        
        return f"data:{content_type};base64,{image_base64}"
    except Exception as e:
        logger.error(f"이미지 다운로드 실패 {image_url}: {e}")
        return None

def convert_urls_to_base64(image_urls: List[str]) -> List[str]:
    """URL 리스트를 Base64 이미지 리스트로 변환"""
    base64_images = []
    
    for url in image_urls:
        base64_image = download_image_as_base64(url)
        if base64_image:
            base64_images.append(base64_image)
    
    return base64_images

def validate_instagram_username(username: str) -> bool:
    """인스타그램 사용자명 유효성 검사"""
    if not username:
        return False
    
    # 인스타그램 사용자명 규칙: 1-30자, 영문자, 숫자, 점, 언더스코어만 허용
    import re
    pattern = r'^[a-zA-Z0-9._]{1,30}$'
    return bool(re.match(pattern, username))

def validate_hashtag(hashtag: str) -> bool:
    """해시태그 유효성 검사"""
    if not hashtag:
        return False
    
    # 해시태그 규칙: 1-100자, 영문자, 숫자, 언더스코어만 허용
    import re
    pattern = r'^[a-zA-Z0-9_]{1,100}$'
    return bool(re.match(pattern, hashtag))

def clean_hashtag(hashtag: str) -> str:
    """해시태그 정리 (# 제거, 소문자 변환)"""
    if hashtag.startswith('#'):
        hashtag = hashtag[1:]
    return hashtag.lower()

def format_thumbnail_data(thumbnail_urls: List[str], username: str) -> List[dict]:
    """썸네일 데이터 포맷팅"""
    formatted_data = []
    
    for i, url in enumerate(thumbnail_urls):
        formatted_data.append({
            "post_id": f"{username}_{i+1}",
            "thumbnail_url": url,
            "author": username,
            "post_type": "image"
        })
    
    return formatted_data

def upload_images_to_s3(image_urls: List[str], username: str) -> List[str]:
    """주어진 이미지 URL들을 S3로 업로드하고 S3 URL 리스트를 반환"""
    bucket = os.getenv('AWS_S3_BUCKET_NAME', '')
    region = os.getenv('AWS_S3_REGION', 'ap-southeast-2')
    if not bucket:
        logger.error("AWS_S3_BUCKET_NAME이 설정되지 않았습니다.")
        return []
    
    s3 = boto3.client(
        's3',
        region_name=region,
        config=Config(retries={'max_attempts': 3, 'mode': 'standard'})
    )
    
    uploaded_urls: List[str] = []
    ts = datetime.utcnow().strftime('%Y%m%dT%H%M%SZ')
    
    for idx, url in enumerate(image_urls):
        try:
            resp = requests.get(url, timeout=15)
            resp.raise_for_status()
            content_type = resp.headers.get('Content-Type', 'image/jpeg')
            ext = 'jpg'
            if 'png' in content_type:
                ext = 'png'
            elif 'webp' in content_type:
                ext = 'webp'
            key = f"instagram/{username}/{ts}_{idx+1}.{ext}"
            s3.put_object(
                Bucket=bucket,
                Key=key,
                Body=resp.content,
                ContentType=content_type,
                ACL='public-read'
            )
            public_url = f"https://{bucket}.s3.{region}.amazonaws.com/{key}"
            uploaded_urls.append(public_url)
            logger.info(f"S3 업로드 성공: {public_url}")
        except Exception as e:
            logger.error(f"S3 업로드 실패 ({url}): {e}")
            continue
    
    return uploaded_urls

def upload_bytes_to_s3(file_bytes: bytes, filename: str, username: str = "user") -> str:
    """바이트 데이터를 S3로 업로드하고 퍼블릭 URL 반환"""
    bucket = os.getenv('AWS_S3_BUCKET_NAME', '')
    region = os.getenv('AWS_S3_REGION', 'ap-southeast-2')
    if not bucket:
        raise ValueError("AWS_S3_BUCKET_NAME이 설정되지 않았습니다.")
    
    s3 = boto3.client(
        's3',
        region_name=region,
        config=Config(retries={'max_attempts': 3, 'mode': 'standard'})
    )
    
    # 확장자/컨텐츠타입 추정
    ext = (filename.rsplit('.', 1)[-1].lower() if '.' in filename else 'jpg')
    content_type = {
        'jpg': 'image/jpeg',
        'jpeg': 'image/jpeg',
        'png': 'image/png',
        'webp': 'image/webp'
    }.get(ext, 'application/octet-stream')
    
    ts = datetime.utcnow().strftime('%Y%m%dT%H%M%SZ')
    safe_name = filename.replace('/', '_')
    key = f"uploads/{username}/{ts}_{safe_name}"
    
    s3.put_object(
        Bucket=bucket,
        Key=key,
        Body=file_bytes,
        ContentType=content_type,
        ACL='public-read'
    )
    return f"https://{bucket}.s3.{region}.amazonaws.com/{key}"