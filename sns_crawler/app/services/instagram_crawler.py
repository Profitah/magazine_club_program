import requests
from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from webdriver_manager.chrome import ChromeDriverManager
from typing import List, Optional
import time
import json
import re
from datetime import datetime
import logging

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class InstagramCrawler:
    """인스타그램 크롤링 서비스"""
    
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        })
    
    def get_thumbnails_simple(self, username: str) -> List[str]:
        """간단한 방법으로 썸네일 URL만 추출"""
        try:
            logger.info(f"간단한 방법으로 {username} 썸네일 크롤링 시작")
            url = f"https://www.instagram.com/{username}/"
            response = self.session.get(url, timeout=10)
            response.raise_for_status()
            
            soup = BeautifulSoup(response.content, 'html.parser')
            thumbnail_urls = []
            
            # 인스타그램의 JSON 데이터에서 이미지 URL 추출
            scripts = soup.find_all('script', type='application/ld+json')
            for script in scripts:
                try:
                    data = json.loads(script.string)
                    if 'image' in data:
                        thumbnail_urls.append(data['image'])
                except:
                    continue
            
            # img 태그에서 썸네일 URL 추출
            img_tags = soup.find_all('img', src=re.compile(r'scontent.*\.jpg'))
            for img in img_tags:
                src = img.get('src')
                if src and 'scontent' in src and 'profile' not in src:
                    thumbnail_urls.append(src)
            
            # 중복 제거
            unique_urls = list(set(thumbnail_urls))
            logger.info(f"썸네일 {len(unique_urls)}개 추출 완료")
            return unique_urls
            
        except Exception as e:
            logger.error(f"간단한 크롤링 실패: {e}")
            return []
    
    def get_thumbnails_selenium(self, username: str) -> List[str]:
        """Selenium을 사용한 고급 크롤링"""
        chrome_options = Options()
        chrome_options.add_argument('--headless')
        chrome_options.add_argument('--no-sandbox')
        chrome_options.add_argument('--disable-dev-shm-usage')
        chrome_options.add_argument('--disable-gpu')
        chrome_options.add_argument('--window-size=1920,1080')
        chrome_options.add_argument('--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36')
        
        driver = None
        try:
            logger.info(f"Selenium으로 {username} 썸네일 크롤링 시작")
            driver = webdriver.Chrome(
                service=webdriver.chrome.service.Service(ChromeDriverManager().install()),
                options=chrome_options
            )
            
            url = f"https://www.instagram.com/{username}/"
            driver.get(url)
            
            # 페이지 로딩 대기
            WebDriverWait(driver, 10).until(
                EC.presence_of_element_located((By.CSS_SELECTOR, "article"))
            )
            
            # 스크롤하여 더 많은 게시글 로드
            driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
            time.sleep(2)
            
            thumbnail_urls = []
            
            # 게시글 썸네일 이미지 찾기
            img_elements = driver.find_elements(By.CSS_SELECTOR, "article img[src*='scontent']")
            for img in img_elements:
                src = img.get_attribute('src')
                if src and 'scontent' in src and 'profile' not in src:
                    thumbnail_urls.append(src)
            
            # 중복 제거
            unique_urls = list(set(thumbnail_urls))
            logger.info(f"Selenium으로 썸네일 {len(unique_urls)}개 추출 완료")
            return unique_urls
            
        except Exception as e:
            logger.error(f"Selenium 크롤링 실패: {e}")
            return []
        finally:
            if driver:
                driver.quit()
    
    def get_thumbnails(self, username: str, use_selenium: bool = False) -> List[str]:
        """썸네일 URL 조회 (메인 메서드)"""
        if use_selenium:
            return self.get_thumbnails_selenium(username)
        else:
            return self.get_thumbnails_simple(username)
    