import requests
from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from webdriver_manager.chrome import ChromeDriverManager
from typing import List, Optional, Tuple
import time
import json
import re
import os
import base64
from datetime import datetime
import logging

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('/tmp/instagram_crawler.log', encoding='utf-8')
    ]
)
logger = logging.getLogger(__name__)

class InstagramCrawler:
    """인스타그램 크롤링 서비스"""
    
    def __init__(self, access_token: Optional[str] = None):
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        })
        # Instagram Graph API 액세스 토큰 (환경 변수에서 가져오거나 직접 전달)
        self.access_token = access_token or os.getenv('INSTAGRAM_ACCESS_TOKEN')
        self.graph_api_base = 'https://graph.instagram.com'
        # 로그인 자격 증명
        self.ig_username = os.getenv('INSTAGRAM_USERNAME')
        self.ig_password = os.getenv('INSTAGRAM_PASSWORD')
        # 로그인 세션 쿠키가 있으면 requests 세션에 적용
        session_id = os.getenv('INSTAGRAM_SESSIONID')
        if session_id:
            try:
                self.session.cookies.set('sessionid', session_id, domain='.instagram.com')
                logger.info("requests 세션에 INSTAGRAM_SESSIONID 쿠키 적용 완료")
            except Exception as e:
                logger.warning(f"requests 세션 쿠키 적용 실패: {e}")
    
    def _extract_image_urls_from_html(self, html_content: str) -> List[str]:
        """HTML 문자열에서 div 스타일 및 img 속성 기반으로 이미지 URL 추출"""
        try:
            thumbnail_urls: List[str] = []
            seen_urls = set()
            soup = BeautifulSoup(html_content, 'html.parser')
            
            # 1) div의 background-image 스타일에서 URL 추출
            for div in soup.find_all('div'):
                style = div.get('style', '')
                if style:
                    match = re.search(r'background-image:\s*url\(\s*[\'"]?([^\'")]+)[\'"]?\s*\)', style, re.IGNORECASE)
                    if match:
                        url = match.group(1)
                        if url and ('scontent' in url or 'cdninstagram' in url):
                            if ('_19/' in url or '/t51.2885-19/' in url or
                                'static.cdninstagram.com' in url or 'profile' in url.lower()):
                                pass
                            else:
                                base_url = url.split('?')[0]
                                if base_url not in seen_urls:
                                    seen_urls.add(base_url)
                                    thumbnail_urls.append(url)
            
            # 2) 모든 img 태그에서 src, srcset 유사 속성 추출
            for img in soup.find_all('img'):
                for attr in ['src', 'srcset', 'data-src', 'data-lazy-src', 'data-original']:
                    src = img.get(attr, '')
                    if not src:
                        continue
                    if ',' in src:
                        src = src.split(',')[0].strip().split(' ')[0]
                    if ('scontent' in src or 'cdninstagram' in src):
                        if ('_19/' in src or '/t51.2885-19/' in src or
                            'static.cdninstagram.com' in src or 'profile' in src.lower()):
                            continue
                        base_url = src.split('?')[0]
                        if base_url not in seen_urls:
                            seen_urls.add(base_url)
                            thumbnail_urls.append(src)
                            break
            
            # 3) 정규식으로 소스 전체에서 패턴 매칭 (보강)
            patterns = [
                r'https?://[^"\'<>\s]*scontent[^"\'<>\s]+\.(jpg|jpeg|webp|png)[^"\'<>\s]*',
                r'https?://[^"\'<>\s]*cdninstagram[^"\'<>\s]+\.(jpg|jpeg|webp|png)[^"\'<>\s]*',
            ]
            for pattern in patterns:
                matches = re.findall(pattern, html_content, re.IGNORECASE)
                # 위 패턴은 캡쳐 그룹 포함되므로 후처리
                for m in re.finditer(pattern, html_content, re.IGNORECASE):
                    url = m.group(0)
                    if ('_19/' in url or '/t51.2885-19/' in url or
                        'static.cdninstagram.com' in url or 'profile' in url.lower()):
                        continue
                    base_url = url.split('?')[0]
                    if base_url not in seen_urls:
                        seen_urls.add(base_url)
                        thumbnail_urls.append(url)
            
            return thumbnail_urls
        except Exception as e:
            logger.warning(f"HTML 파싱 보조 추출 실패: {e}")
            return []
    
    def _perform_login(self, driver: webdriver.Chrome) -> bool:
        """Selenium으로 Instagram 로그인 수행 (성공 시 sessionid를 requests 세션에도 반영)"""
        try:
            if not self.ig_username or not self.ig_password:
                logger.warning("INSTAGRAM_USERNAME 또는 INSTAGRAM_PASSWORD 환경변수가 없어 로그인할 수 없습니다.")
                return False
            
            logger.info("Instagram 로그인 페이지로 이동...")
            driver.get("https://www.instagram.com/accounts/login/")
            time.sleep(5)
            
            # 페이지 로딩 보조 대기
            try:
                WebDriverWait(driver, 20).until(
                    lambda d: d.execute_script('return document.readyState') == 'complete'
                )
            except:
                pass
            
            # 입력 필드 찾기
            try:
                username_input = WebDriverWait(driver, 20).until(
                    EC.presence_of_element_located((By.CSS_SELECTOR, "input[name='username']"))
                )
                password_input = WebDriverWait(driver, 20).until(
                    EC.presence_of_element_located((By.CSS_SELECTOR, "input[name='password']"))
                )
            except Exception as e:
                logger.error(f"로그인 입력 필드를 찾지 못했습니다: {e}")
                return False
            
            # 값 입력
            username_input.clear()
            username_input.send_keys(self.ig_username)
            password_input.clear()
            password_input.send_keys(self.ig_password)
            
            # 제출 버튼 클릭
            try:
                submit_btn = driver.find_element(By.CSS_SELECTOR, "button[type='submit']")
                submit_btn.click()
            except:
                # 엔터로 제출 시도
                password_input.submit()
            
            # 로그인 결과 대기
            time.sleep(6)
            
            # 쿠키 확인
            cookies = driver.get_cookies()
            sessionid_cookie = next((c for c in cookies if c.get('name') == 'sessionid'), None)
            if sessionid_cookie and sessionid_cookie.get('value'):
                session_value = sessionid_cookie.get('value')
                try:
                    self.session.cookies.set('sessionid', session_value, domain='.instagram.com')
                    logger.info("로그인 성공: requests 세션에 sessionid 반영 완료")
                except Exception as e:
                    logger.warning(f"requests 세션 쿠키 설정 실패: {e}")
                return True
            
            logger.warning("로그인 후 sessionid 쿠키를 찾지 못했습니다.")
            return False
        
        except Exception as e:
            logger.error(f"Selenium 로그인 실패: {e}", exc_info=True)
            return False
    
    def get_thumbnails_simple(self, username: str) -> List[str]:
        """간단한 방법으로 썸네일 URL만 추출 (페이지 소스에서 직접 추출) - 개선된 버전"""
        try:
            logger.info(f"간단한 방법으로 {username} 썸네일 크롤링 시작")
            
            thumbnail_urls = []
            seen_urls = set()
            
            # 방법 1: JSON 엔드포인트 시도 (더 안정적)
            json_url = f"https://www.instagram.com/{username}/?__a=1&__d=dis"
            headers = {
                'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
                'Accept': 'application/json',
                'Accept-Language': 'ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7',
                'Referer': f'https://www.instagram.com/{username}/',
                'X-Requested-With': 'XMLHttpRequest',
            }
            
            try:
                response = self.session.get(json_url, headers=headers, timeout=15)
                if response.status_code == 200:
                    try:
                        data = response.json()
                        logger.info("JSON 엔드포인트에서 데이터 가져오기 성공")
                        
                        # 재귀적으로 모든 이미지 URL 찾기
                        def extract_all_urls(obj, urls_list):
                            if isinstance(obj, dict):
                                for key, value in obj.items():
                                    # URL 관련 키 확인 (더 포괄적으로)
                                    if isinstance(value, str):
                                        key_lower = key.lower()
                                        if any(keyword in key_lower for keyword in ['url', 'src', 'image', 'photo', 'media', 'display', 'thumbnail']):
                                            # scontent 또는 cdninstagram 포함 확인
                                            if ('scontent' in value or 'cdninstagram' in value) and '_19/' not in value and 'static.cdninstagram.com' not in value:
                                                # 프로필 이미지 제외
                                                if 'profile' not in value.lower() and '/t51.2885-19/' not in value:
                                                    base_url = value.split('?')[0]
                                                    if base_url not in seen_urls:
                                                        seen_urls.add(base_url)
                                                        urls_list.append(value)
                                                        logger.info(f"JSON에서 URL 발견: {value[:100]}...")
                                    else:
                                        extract_all_urls(value, urls_list)
                            elif isinstance(obj, list):
                                for item in obj:
                                    extract_all_urls(item, urls_list)
                        
                        extract_all_urls(data, thumbnail_urls)
                        if thumbnail_urls:
                            logger.info(f"JSON 엔드포인트에서 {len(thumbnail_urls)}개 URL 추출")
                            return thumbnail_urls[:4]
                    except json.JSONDecodeError:
                        logger.warning("JSON 파싱 실패, HTML 파싱으로 폴백")
            except Exception as e:
                logger.warning(f"JSON 엔드포인트 실패: {e}, HTML 파싱으로 폴백")
            
            # 방법 2: HTML 페이지 소스 파싱
            url = f"https://www.instagram.com/{username}/"
            headers = {
                'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
                'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
                'Accept-Language': 'ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7',
                'Accept-Encoding': 'gzip, deflate, br',
                'Connection': 'keep-alive',
                'Upgrade-Insecure-Requests': '1',
            }
            response = self.session.get(url, headers=headers, timeout=15)
            response.raise_for_status()
            
            html_content = response.text
            logger.info(f"페이지 소스 길이: {len(html_content)} 문자")
            
            # 방법 2-1: script 태그에서 JSON 데이터 찾기 (더 포괄적)
            soup = BeautifulSoup(html_content, 'html.parser')
            script_tags = soup.find_all('script', type='application/json')
            
            for script in script_tags:
                try:
                    data = json.loads(script.string)
                    def extract_from_json(obj, urls_list):
                        if isinstance(obj, dict):
                            for key, value in obj.items():
                                if isinstance(value, str):
                                    if any(kw in key.lower() for kw in ['url', 'src', 'image', 'photo', 'media', 'display', 'thumbnail']):
                                        if ('scontent' in value or 'cdninstagram' in value) and '_19/' not in value and 'static.cdninstagram.com' not in value:
                                            if 'profile' not in value.lower():
                                                base_url = value.split('?')[0]
                                                if base_url not in seen_urls:
                                                    seen_urls.add(base_url)
                                                    urls_list.append(value)
                                                    logger.info(f"Script JSON에서 URL 발견: {value[:100]}...")
                                else:
                                    extract_from_json(value, urls_list)
                        elif isinstance(obj, list):
                            for item in obj:
                                extract_from_json(item, urls_list)
                    extract_from_json(data, thumbnail_urls)
                except:
                    continue
            
            # 방법 2-2: 인라인 스크립트에서 JSON 데이터 찾기
            inline_scripts = soup.find_all('script', string=re.compile(r'window\._sharedData|__additionalDataLoaded|edge_owner_to_timeline_media'))
            for script in inline_scripts:
                script_text = script.string
                # window._sharedData 패턴
                shared_data_match = re.search(r'window\._sharedData\s*=\s*({.+?});', script_text, re.DOTALL)
                if shared_data_match:
                    try:
                        data = json.loads(shared_data_match.group(1))
                        def extract_from_shared_data(obj, urls_list):
                            if isinstance(obj, dict):
                                for key, value in obj.items():
                                    if isinstance(value, str) and any(kw in key.lower() for kw in ['url', 'src', 'image', 'display', 'thumbnail']):
                                        if ('scontent' in value or 'cdninstagram' in value) and '_19/' not in value:
                                            base_url = value.split('?')[0]
                                            if base_url not in seen_urls:
                                                seen_urls.add(base_url)
                                                urls_list.append(value)
                                                logger.info(f"_sharedData에서 URL 발견: {value[:100]}...")
                                    else:
                                        extract_from_shared_data(value, urls_list)
                            elif isinstance(obj, list):
                                for item in obj:
                                    extract_from_shared_data(item, urls_list)
                        extract_from_shared_data(data, thumbnail_urls)
                    except:
                        continue
            
            # 방법 3: 모든 이미지 URL 패턴 찾기 (더 포괄적)
            url_patterns = [
                r'https?://[^"\'<>\s]*scontent[^"\'<>\s]+\.(jpg|jpeg|webp|png)[^"\'<>\s]*',
                r'https?://[^"\'<>\s]*cdninstagram[^"\'<>\s]+\.(jpg|jpeg|webp|png)[^"\'<>\s]*',
                r'https?://[^"\'<>\s]*instagram[^"\'<>\s]+\.(jpg|jpeg|webp|png)[^"\'<>\s]*',
                r'"(https?://[^"]*scontent[^"]*\.(jpg|jpeg|webp|png)[^"]*)"',
                r"'(https?://[^']*scontent[^']*\.(jpg|jpeg|webp|png)[^']*)'",
            ]
            
            for pattern in url_patterns:
                matches = re.findall(pattern, html_content, re.IGNORECASE)
                for match in matches:
                    url = match[0] if isinstance(match, tuple) else match
                    # 프로필 이미지 제외
                    if '_19/' in url or '/t51.2885-19/' in url or 'profile' in url.lower():
                        continue
                    # static 제외
                    if 'static.cdninstagram.com' in url:
                        continue
                    base_url = url.split('?')[0]
                    if base_url not in seen_urls:
                        seen_urls.add(base_url)
                        thumbnail_urls.append(url)
                        logger.info(f"정규식으로 URL 발견: {url[:100]}...")
            
            # 방법 4: img 태그에서 추출
            img_tags = soup.find_all('img')
            for img in img_tags:
                for attr in ['src', 'srcset', 'data-src', 'data-lazy-src', 'data-original']:
                    src = img.get(attr, '')
                    if src:
                        # srcset에서 첫 번째 URL 추출
                        if ',' in src:
                            src = src.split(',')[0].strip().split(' ')[0]
                        if ('scontent' in src or 'cdninstagram' in src) and '_19/' not in src and 'static.cdninstagram.com' not in src:
                            if 'profile' not in src.lower():
                                base_url = src.split('?')[0]
                                if base_url not in seen_urls:
                                    seen_urls.add(base_url)
                                    thumbnail_urls.append(src)
                                    logger.info(f"img 태그에서 URL 발견: {src[:100]}...")
            
            # 방법 5: style 속성의 background-image
            style_pattern = r'background-image:\s*url\(["\']?([^"\'()]+)["\']?\)'
            style_matches = re.findall(style_pattern, html_content, re.IGNORECASE)
            for match in style_matches:
                if ('scontent' in match or 'cdninstagram' in match) and '_19/' not in match and 'static.cdninstagram.com' not in match:
                    if 'profile' not in match.lower():
                        base_url = match.split('?')[0]
                        if base_url not in seen_urls:
                            seen_urls.add(base_url)
                            thumbnail_urls.append(match)
                            logger.info(f"background-image에서 URL 발견: {match[:100]}...")
            
            # 처음 4개만 반환
            thumbnail_urls = thumbnail_urls[:4]
            logger.info(f"썸네일 {len(thumbnail_urls)}개 추출 완료")
            return thumbnail_urls
            
        except Exception as e:
            logger.error(f"간단한 크롤링 실패: {e}", exc_info=True)
            return []
    
    def get_thumbnails_selenium(self, username: str) -> List[str]:
        """Selenium을 사용한 고급 크롤링 - 게시물 썸네일만 추출"""
        chrome_options = Options()
        # headless 모드: 환경변수로 제어 (기본값: headless on)
        headless_env = os.getenv('INSTAGRAM_HEADLESS', 'true').lower()
        if headless_env not in ['false', '0', 'no', 'off']:
            chrome_options.add_argument('--headless=new')
        chrome_options.add_argument('--no-sandbox')
        chrome_options.add_argument('--disable-dev-shm-usage')
        chrome_options.add_argument('--disable-gpu')
        chrome_options.add_argument('--disable-blink-features=AutomationControlled')
        chrome_options.add_argument('--window-size=1920,1080')
        chrome_options.add_argument('--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36')
        chrome_options.add_experimental_option("excludeSwitches", ["enable-automation"])
        chrome_options.add_experimental_option('useAutomationExtension', False)
        
        # 사용자 지정 Chrome 프로필 사용 (로그인 세션 재사용)
        # INSTAGRAM_CHROME_PROFILE: Chrome 사용자 데이터 디렉토리 (예: ~/Library/Application Support/Google/Chrome)
        # INSTAGRAM_CHROME_PROFILE_NAME: 프로필 디렉토리 이름 (예: Default)
        import tempfile
        custom_user_data_dir = os.getenv('INSTAGRAM_CHROME_PROFILE')
        custom_profile_name = os.getenv('INSTAGRAM_CHROME_PROFILE_NAME', 'Default')
        used_custom_profile = False
        profile_dir = None
        try:
            if custom_user_data_dir and os.path.exists(custom_user_data_dir):
                chrome_options.add_argument(f'--user-data-dir={custom_user_data_dir}')
                # 프로필 이름이 존재하면 함께 지정 (macOS/Windows 프로필 구조 지원)
                candidate_profile_path = os.path.join(custom_user_data_dir, custom_profile_name)
                if os.path.exists(candidate_profile_path):
                    chrome_options.add_argument(f'--profile-directory={custom_profile_name}')
                used_custom_profile = True
                logger.info(f"사용자 지정 Chrome 프로필 사용: {custom_user_data_dir} (profile={custom_profile_name})")
            else:
                # 임시 프로필 디렉토리 사용 (기존 Chrome과 충돌 방지)
                profile_dir = tempfile.mkdtemp(prefix='chrome_profile_')
                chrome_options.add_argument(f'--user-data-dir={profile_dir}')
                logger.info(f"임시 Chrome 프로필 디렉토리 사용: {profile_dir}")
        except Exception as e:
            logger.warning(f"Chrome 프로필 설정 실패: {e}")
            # 실패 시 임시 프로필로 폴백
            profile_dir = tempfile.mkdtemp(prefix='chrome_profile_')
            chrome_options.add_argument(f'--user-data-dir={profile_dir}')
            logger.info(f"임시 Chrome 프로필 디렉토리 사용(폴백): {profile_dir}")
        
        # macOS에서 Chrome 경로 자동 감지
        import platform
        if platform.system() == 'Darwin':
            chrome_paths = [
                '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
                '/Applications/Chromium.app/Contents/MacOS/Chromium'
            ]
            for path in chrome_paths:
                if os.path.exists(path):
                    chrome_options.binary_location = path
                    break
        
        driver = None
        try:
            logger.info(f"Selenium으로 {username} 게시물 썸네일 크롤링 시작")
            driver = webdriver.Chrome(
                service=webdriver.chrome.service.Service(ChromeDriverManager().install()),
                options=chrome_options
            )
            
            # 자동화 감지 방지
            driver.execute_cdp_cmd('Page.addScriptToEvaluateOnNewDocument', {
                'source': '''
                    Object.defineProperty(navigator, 'webdriver', {
                        get: () => undefined
                    });
                    // User-Agent 추가 속성
                    Object.defineProperty(navigator, 'plugins', {
                        get: () => [1, 2, 3, 4, 5]
                    });
                    Object.defineProperty(navigator, 'languages', {
                        get: () => ['ko-KR', 'ko', 'en-US', 'en']
                    });
                '''
            })
            
            # 추가 헤더 설정
            driver.execute_cdp_cmd('Network.setUserAgentOverride', {
                "userAgent": 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
            })
            
            # 먼저 인스타그램 메인 페이지 접속하여 쿠키 확인
            logger.info("인스타그램 메인 페이지 접속 중...")
            driver.get("https://www.instagram.com/")
            time.sleep(5)
            
            # 환경변수에 세션 쿠키가 있으면 Selenium에도 적용
            try:
                env_session = os.getenv('INSTAGRAM_SESSIONID')
                if env_session:
                    logger.info("환경 변수 INSTAGRAM_SESSIONID 감지 - Selenium 쿠키 적용 시도")
                    # 도메인 일치 필요: instagram.com 로드 후 추가
                    driver.add_cookie({
                        'name': 'sessionid',
                        'value': env_session,
                        'domain': '.instagram.com',
                        'path': '/',
                        'httpOnly': True,
                        'secure': True,
                    })
                    driver.get("https://www.instagram.com/")  # 쿠키 적용 후 새로고침
                    time.sleep(3)
            except Exception as e:
                logger.warning(f"Selenium 쿠키 적용 실패: {e}")
            
            # 세션 쿠키가 없거나 로그인 페이지면 로그인 시도
            need_login = False
            try:
                cookies = driver.get_cookies()
                has_session = any(c.get('name') == 'sessionid' for c in cookies)
                current_title = driver.title or ''
                current_url = driver.current_url or ''
                redirected_to_login = ('login' in current_title.lower() or 
                                       'log in' in current_title.lower() or 
                                       '/accounts/login' in current_url)
                need_login = (not has_session) or redirected_to_login
            except:
                need_login = True
            
            if need_login:
                logger.info("로그인 필요 감지 - Selenium 로그인 절차 수행 시도")
                if not self._perform_login(driver):
                    logger.warning("로그인 실패 또는 크리덴셜 부재 - 로그인 없이 진행(제한적 결과 가능)")
                else:
                    # 로그인 성공 후 메인 페이지 리프레시
                    try:
                        driver.get("https://www.instagram.com/")
                        time.sleep(3)
                    except:
                        pass
            
            # 현재 쿠키 확인
            cookies = driver.get_cookies()
            logger.info(f"현재 쿠키 개수: {len(cookies)}")
            if cookies:
                logger.info("쿠키 발견 - 로그인 상태일 수 있습니다")
                # sessionid 쿠키 확인
                sessionid = [c for c in cookies if c.get('name') == 'sessionid']
                if sessionid:
                    logger.info("sessionid 쿠키 발견 - 로그인된 상태입니다!")
                else:
                    logger.warning("sessionid 쿠키 없음 - 로그인되지 않은 상태입니다")
            
            url = f"https://www.instagram.com/{username}/"
            logger.info(f"프로필 URL 접속: {url}")
            driver.get(url)
            
            # 페이지 로딩 대기 (더 긴 대기 시간)
            time.sleep(5)  # 초기 로딩 대기
            
            # 페이지가 완전히 로드될 때까지 대기
            try:
                WebDriverWait(driver, 20).until(
                    lambda d: d.execute_script('return document.readyState') == 'complete'
                )
                logger.info("페이지 로딩 완료")
            except:
                logger.warning("페이지 로딩 대기 시간 초과, 계속 진행")
            
            time.sleep(5)  # 추가 대기 (동적 콘텐츠 로딩)
            
            # 접속 후 다시 쿠키 확인
            cookies_after = driver.get_cookies()
            sessionid_after = [c for c in cookies_after if c.get('name') == 'sessionid']
            if sessionid_after:
                logger.info("프로필 페이지 접속 후 sessionid 쿠키 확인됨 - 로그인 상태 유지")
            else:
                logger.warning("프로필 페이지 접속 후 sessionid 쿠키 없음 - 로그인 필요")
            
            # 페이지 제목 확인 (로그인 요구 여부 체크)
            page_title = driver.title
            logger.info(f"페이지 제목: {page_title}")
            
            # 현재 URL 확인
            current_url = driver.current_url
            logger.info(f"현재 URL: {current_url}")
            
            # 로그인 페이지로 리다이렉트되었는지 확인
            is_login_page = 'login' in page_title.lower() or 'log in' in page_title.lower() or '/accounts/login' in current_url
            if is_login_page:
                logger.warning("로그인 페이지로 리다이렉트되었습니다. 인스타그램이 로그인을 요구하고 있습니다.")
                logger.warning("로그인 없이 크롤링을 시도하지만, 결과가 제한적일 수 있습니다.")
                # 로그인 페이지에서도 페이지 소스에 일부 데이터가 있을 수 있으므로 계속 진행
                # 하지만 실제 프로필 페이지로 이동 시도
                try:
                    logger.info("실제 프로필 URL로 직접 이동 시도...")
                    profile_url = f"https://www.instagram.com/{username}/"
                    driver.get(profile_url)
                    time.sleep(5)  # 페이지 로딩 대기
                    current_url = driver.current_url
                    page_title = driver.title
                    logger.info(f"재시도 후 현재 URL: {current_url}")
                    logger.info(f"재시도 후 페이지 제목: {page_title}")
                except Exception as e:
                    logger.warning(f"프로필 페이지 재접속 실패: {e}")
            
            # 페이지 소스 길이 확인 (디버깅)
            page_source_length = len(driver.page_source)
            logger.info(f"페이지 소스 길이: {page_source_length} 문자")
            
            # 다양한 선택자로 게시물 컨테이너 찾기 시도
            selectors_to_try = [
                "article a",  # article 태그 안의 링크
                "a[href*='/p/']",  # 게시물 링크
                "._aagu",  # 최신 Instagram 클래스
                "._aagv",  # 최신 Instagram 클래스
                "[role='main'] article",  # 메인 콘텐츠 영역의 article
            ]
            
            elements_found = False
            for selector in selectors_to_try:
                try:
                    elements = driver.find_elements(By.CSS_SELECTOR, selector)
                    if elements:
                        logger.info(f"선택자 '{selector}'로 {len(elements)}개 요소 발견")
                        elements_found = True
                        break
                except:
                    continue
            
            if not elements_found:
                logger.warning("게시물 컨테이너를 찾지 못했습니다. 계속 진행합니다.")
            
            # 스크롤하여 게시물 로드 (더 많은 스크롤)
            logger.info("페이지 스크롤 시작 (게시물 이미지 로드)...")
            scroll_count = 0
            last_height = driver.execute_script("return document.body.scrollHeight")
            
            for i in range(5):  # 최대 5번 스크롤
                # 스크롤 다운
                driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
                time.sleep(3)  # 스크롤 후 대기 (이미지 로딩 시간)
                
                # 새로운 콘텐츠가 로드되었는지 확인
                new_height = driver.execute_script("return document.body.scrollHeight")
                if new_height == last_height:
                    logger.info(f"더 이상 새로운 콘텐츠가 없음 (스크롤 {i+1}/5)")
                    break
                last_height = new_height
                scroll_count += 1
                logger.info(f"스크롤 {scroll_count} 완료, 페이지 높이: {new_height}")
            
            # 추가 대기 (마지막 이미지 로딩)
            time.sleep(5)
            
            logger.info("Selenium으로 이미지 요소 찾기 시작...")
            
            # 여러 방법으로 이미지 추출 시도 (안전한 JavaScript 실행)
            try:
                image_data = driver.execute_script("""
                var results = [];
                var seen = new Set();
                
                // 방법 1: _aagu 클래스 안의 _aagv 클래스 (최신 인스타그램 구조)
                var aaguContainers = document.querySelectorAll('._aagu');
                console.log('_aagu 컨테이너 개수:', aaguContainers.length);
                var containers1 = [];
                for (var i = 0; i < aaguContainers.length; i++) {
                    var aagv = aaguContainers[i].querySelector('._aagv');
                    if (aagv) {
                        containers1.push(aagv);
                    }
                }
                console.log('_aagu 안의 _aagv 개수:', containers1.length);
                
                // 방법 1-2: 직접 _aagv 클래스도 찾기
                var directAagv = document.querySelectorAll('._aagv');
                console.log('직접 _aagv 클래스 개수:', directAagv.length);
                // 중복 제거하면서 합치기
                for (var i = 0; i < directAagv.length; i++) {
                    if (containers1.indexOf(directAagv[i]) === -1) {
                        containers1.push(directAagv[i]);
                    }
                }
                console.log('최종 _aagv 컨테이너 개수:', containers1.length);
                
                // 방법 2: article 태그 안의 이미지 (게시물)
                var containers2 = document.querySelectorAll('article img');
                console.log('article img 개수:', containers2.length);
                
                // 방법 2-1: article 태그 안의 링크에서 이미지 찾기
                var articleLinks = document.querySelectorAll('article a[href*="/p/"]');
                console.log('article 내 게시물 링크 개수:', articleLinks.length);
                var articleLinkImages = [];
                for (var i = 0; i < articleLinks.length; i++) {
                    var link = articleLinks[i];
                    var img = link.querySelector('img');
                    if (img) {
                        articleLinkImages.push(img);
                    }
                }
                console.log('article 링크 내 이미지 개수:', articleLinkImages.length);
                
                // 방법 3: 모든 img 태그 중 scontent 포함
                var allImages = document.querySelectorAll('img');
                console.log('전체 img 태그 개수:', allImages.length);
                
                // 방법 3-1: 모든 img 태그에서 실제 로드된 이미지 URL 확인
                var loadedImages = [];
                for (var i = 0; i < allImages.length; i++) {
                    var img = allImages[i];
                    var src = img.src || img.getAttribute('srcset') || img.getAttribute('data-src') || '';
                    if (src && (src.includes('scontent') || src.includes('cdninstagram'))) {
                        loadedImages.push(img);
                    }
                }
                console.log('scontent/cdninstagram 포함 이미지 개수:', loadedImages.length);
                
                // 방법 4: 페이지 소스에서 직접 찾기 (정규식 개선)
                var pageSource = document.documentElement.innerHTML;
                // 여러 패턴 시도 - 더 포괄적인 패턴 사용
                var scontentMatches = null;
                try {
                    // 패턴 1: .jpg로 끝나는 URL
                    var pattern1 = new RegExp('https?://[^"\'<>\s]*scontent[^"\'<>\s]+\.jpg[^"\'<>\s]*', 'gi');
                    scontentMatches = pageSource.match(pattern1);
                    // 패턴 2: .jpeg로 끝나는 URL
                    if (!scontentMatches || scontentMatches.length === 0) {
                        var pattern2 = new RegExp('https?://[^"\'<>\s]*scontent[^"\'<>\s]+\.jpeg[^"\'<>\s]*', 'gi');
                        scontentMatches = pageSource.match(pattern2);
                    }
                    // 패턴 3: .webp로 끝나는 URL
                    if (!scontentMatches || scontentMatches.length === 0) {
                        var pattern3 = new RegExp('https?://[^"\'<>\s]*scontent[^"\'<>\s]+\.webp[^"\'<>\s]*', 'gi');
                        scontentMatches = pageSource.match(pattern3);
                    }
                    // 패턴 4: cdninstagram URL (scontent 대신)
                    if (!scontentMatches || scontentMatches.length === 0) {
                        var pattern4 = new RegExp('https?://[^"\'<>\s]*cdninstagram[^"\'<>\s]+\.(jpg|jpeg|webp|png)[^"\'<>\s]*', 'gi');
                        scontentMatches = pageSource.match(pattern4);
                    }
                    // 패턴 5: 일반 scontent URL (더 포괄적)
                    if (!scontentMatches || scontentMatches.length === 0) {
                        var pattern5 = new RegExp('https?://[^"\'<>\s]*scontent[^"\'<>\s]+', 'gi');
                        scontentMatches = pageSource.match(pattern5);
                    }
                } catch (e) {
                    console.log('정규식 오류:', e);
                    scontentMatches = null;
                }
                console.log('페이지 소스에서 scontent URL 개수:', scontentMatches ? scontentMatches.length : 0);
                if (scontentMatches && scontentMatches.length > 0) {
                    console.log('첫 번째 URL 샘플:', scontentMatches[0].substring(0, 100));
                    // 처음 5개 URL 전체 출력 (디버깅)
                    for (var d = 0; d < Math.min(5, scontentMatches.length); d++) {
                        console.log('URL[' + d + ']:', scontentMatches[d]);
                    }
                }
                
                // 모든 이미지 수집
                var allImgElements = [];
                
                // _aagv 안의 이미지 (또는 _aagv 자체가 이미지일 수도 있음)
                for (var i = 0; i < containers1.length; i++) {
                    var container = containers1[i];
                    // _aagv 안의 img 태그 찾기
                    var img = container.querySelector('img');
                    if (img) {
                        allImgElements.push(img);
                    } else {
                        // img 태그가 없으면 _aagv 자체가 이미지 요소일 수 있음
                        // background-image 스타일 확인
                        try {
                            var style = window.getComputedStyle(container);
                            var bgImage = style.backgroundImage;
                            if (bgImage && bgImage !== 'none') {
                                // background-image에서 URL 추출 (정규식 수정)
                                var urlMatch = bgImage.match(/url\(["']?([^"']+)["']?\)/);
                                if (urlMatch && urlMatch[1]) {
                                    // 가상 img 객체 생성
                                    var virtualImg = { src: urlMatch[1], alt: '' };
                                    allImgElements.push(virtualImg);
                                }
                            }
                        } catch (e) {
                            // 스타일 가져오기 실패 시 무시
                        }
                    }
                }
                
                // article 안의 이미지 (중복 제거)
                for (var i = 0; i < containers2.length; i++) {
                    var img = containers2[i];
                    // 프로필 이미지 제외
                    if (img.src && !img.src.includes('_19/') && !img.src.includes('profile')) {
                        allImgElements.push(img);
                    }
                }
                
                // article 링크 내 이미지 추가
                for (var i = 0; i < articleLinkImages.length; i++) {
                    var img = articleLinkImages[i];
                    if (allImgElements.indexOf(img) === -1) {
                        allImgElements.push(img);
                    }
                }
                
                // 로드된 이미지 추가
                for (var i = 0; i < loadedImages.length; i++) {
                    var img = loadedImages[i];
                    if (allImgElements.indexOf(img) === -1) {
                        allImgElements.push(img);
                    }
                }
                
                // 각 이미지에서 URL 추출
                for (var i = 0; i < allImgElements.length; i++) {
                    var img = allImgElements[i];
                    // 가상 img 객체인 경우 src 속성 직접 사용
                    var src = img.src || (img.getAttribute ? img.getAttribute('srcset') : '') || '';
                    
                    if (!src || src.trim() === '') {
                        continue;
                    }
                    
                    // srcset에서 첫 번째 URL 추출
                    if (src.includes(',')) {
                        src = src.split(',')[0].trim().split(' ')[0];
                    }
                    
                    // data URL이나 빈 URL 스킵
                    if (src.startsWith('data:') || src.trim() === '') {
                        continue;
                    }
                    
                    // static.cdninstagram.com은 스킵 (아이콘 등)
                    if (src.includes('static.cdninstagram.com')) {
                        continue;
                    }
                    
                    // 프로필 이미지 제외
                    if (src.includes('_19/') || src.includes('/t51.2885-19/') || src.includes('profile')) {
                        continue;
                    }
                    
                    // scontent 또는 cdninstagram 포함 (실제 게시물 이미지)
                    // scontent가 없어도 cdninstagram이면 포함 (최신 Instagram 구조)
                    if (!src.includes('scontent') && !src.includes('cdninstagram')) {
                        continue;
                    }
                    
                    // static.cdninstagram.com은 제외 (이미 위에서 처리했지만 다시 확인)
                    if (src.includes('static.cdninstagram.com')) {
                        continue;
                    }
                    
                    // 중복 제거 (기본 URL 기준)
                    var baseUrl = src.split('?')[0];
                    if (seen.has(baseUrl)) {
                        continue;
                    }
                    seen.add(baseUrl);
                    
                    // 이미지 크기 정보 가져오기
                    var width = (img.naturalWidth !== undefined) ? (img.naturalWidth || img.width || 0) : 0;
                    var height = (img.naturalHeight !== undefined) ? (img.naturalHeight || img.height || 0) : 0;
                    
                    results.push({
                        url: src,
                        width: width,
                        height: height,
                        aspectRatio: (width > 0 && height > 0) ? (width / height) : 1,
                        alt: (img.alt !== undefined) ? (img.alt || '') : ''
                    });
                }
                
                // 페이지 소스에서 직접 찾은 URL도 추가 (이미지 요소를 찾지 못한 경우)
                if (scontentMatches && scontentMatches.length > 0) {
                    console.log('페이지 소스에서 찾은 scontent URL 처리 시작:', scontentMatches.length);
                    for (var i = 0; i < scontentMatches.length && results.length < 10; i++) {
                        var originalUrl = scontentMatches[i];
                        var url = originalUrl.replace(/\\\\/g, '').replace(/\\u002F/g, '/').replace(/\\\//g, '/');
                        console.log('원본 URL:', originalUrl.substring(0, 150));
                        console.log('정제된 URL:', url.substring(0, 150));
                        
                        // 프로필 이미지 제외
                        if (url.includes('_19/') || url.includes('/t51.2885-19/') || url.includes('profile')) {
                            console.log('프로필 이미지로 제외:', url.substring(0, 50));
                            continue;
                        }
                        // static 제외
                        if (url.includes('static.cdninstagram.com')) {
                            console.log('static 이미지로 제외:', url.substring(0, 50));
                            continue;
                        }
                        // scontent 또는 cdninstagram 포함 확인
                        if (!url.includes('scontent') && !url.includes('cdninstagram')) {
                            console.log('scontent/cdninstagram가 없어서 제외:', url.substring(0, 50));
                            continue;
                        }
                        // 중복 제거
                        var baseUrl = url.split('?')[0];
                        if (seen.has(baseUrl)) {
                            console.log('중복으로 제외:', baseUrl.substring(0, 50));
                            continue;
                        }
                        seen.add(baseUrl);
                        
                        console.log('URL 추가됨:', url.substring(0, 100));
                        results.push({
                            url: url,
                            width: 0,
                            height: 0,
                            aspectRatio: 1,
                            alt: ''
                        });
                    }
                    console.log('페이지 소스에서 추가된 URL 개수:', results.length);
                }
                
                return {
                    _aagu_count: aaguContainers.length,
                    _aagv_count: containers1.length,
                    article_img_count: containers2.length,
                    total_img_count: allImages.length,
                    scontent_urls_in_source: scontentMatches ? scontentMatches.length : 0,
                    results: results
                };
            """)
            except Exception as js_error:
                logger.error(f"JavaScript 실행 오류: {js_error}")
                logger.warning("JavaScript 실행 실패 - 현재 페이지 소스로 보조 파싱 시도...")
                try:
                    html_content = driver.page_source or ""
                except:
                    html_content = ""
                extracted = self._extract_image_urls_from_html(html_content)
                if extracted:
                    logger.info(f"보조 파싱으로 {len(extracted)}개 URL 추출")
                    return extracted[:4]
                logger.warning("보조 파싱 실패, 간단한 방법으로 최종 폴백...")
                return self.get_thumbnails_simple(username)
            
            # JavaScript에서 반환한 결과 파싱
            if isinstance(image_data, dict):
                _aagu_count = image_data.get('_aagu_count', 0)
                _aagv_count = image_data.get('_aagv_count', 0)
                article_img_count = image_data.get('article_img_count', 0)
                total_img_count = image_data.get('total_img_count', 0)
                scontent_urls_in_source = image_data.get('scontent_urls_in_source', 0)
                image_data = image_data.get('results', [])
                logger.info(f"디버깅 정보 - _aagu: {_aagu_count}개, _aagv: {_aagv_count}개, article img: {article_img_count}개, 전체 img: {total_img_count}개, 소스 내 scontent URL: {scontent_urls_in_source}개")
                logger.info(f"추출된 이미지: {len(image_data)}개")
            else:
                logger.warning(f"예상치 못한 데이터 형식: {type(image_data)}")
                image_data = []
            
            # 이미지 크기 정보 출력 (디버깅)
            logger.info(f"추출된 이미지 상세 정보:")
            for i, img in enumerate(image_data[:10]):  # 처음 10개만 로그
                alt_text = img.get('alt', '')[:50] if img.get('alt') else ''
                logger.info(f"  [{i+1}] {img['url'][:100]}... 크기: {img['width']}x{img['height']}, 비율: {img['aspectRatio']:.2f}")
            
            # 이미 JavaScript에서 필터링했으므로 추가 필터링은 최소화
            filtered_images = []
            for img in image_data:
                url = img['url']
                
                # 추가 안전 체크: 프로필 이미지 제외
                if '_19/' in url or '/t51.2885-19/' in url or 'profile' in url.lower():
                    logger.debug(f"프로필 이미지 제외: {url[:80]}...")
                    continue
                
                # static 제외
                if 'static.cdninstagram.com' in url:
                    logger.debug(f"static 이미지 제외: {url[:80]}...")
                    continue
                
                # scontent 또는 cdninstagram 포함 (게시물 이미지)
                if 'scontent' in url or ('cdninstagram' in url and 'static.cdninstagram.com' not in url):
                    filtered_images.append(img)
                    logger.info(f"✓ 게시글 썸네일 추가: {url[:100]}...")
            
            logger.info(f"필터링 후 {len(filtered_images)}개 이미지 남음")
            
            # URL만 추출 (중복 제거는 이미 JavaScript에서 처리했지만 다시 확인)
            unique_urls = []
            seen_urls = set()
            for img in filtered_images:
                url = img['url']
                base_url = url.split('?')[0]
                if base_url not in seen_urls:
                    seen_urls.add(base_url)
                    unique_urls.append(url)
            
            logger.info(f"최종 {len(unique_urls)}개 이미지 URL 추출")
            
            # 처음 4개의 썸네일만 반환
            thumbnail_urls = unique_urls[:4]
            
            logger.info(f"Selenium으로 게시물 썸네일 {len(thumbnail_urls)}개 추출 완료 (최대 4개)")
            
            if len(thumbnail_urls) > 0:
                logger.info(f"첫 번째 URL: {thumbnail_urls[0][:100]}...")
            else:
                logger.warning("썸네일을 찾을 수 없습니다. 페이지 구조를 확인해주세요.")
            
            return thumbnail_urls
            
        except Exception as e:
            logger.error(f"Selenium 크롤링 실패: {e}", exc_info=True)
            return []
        finally:
            # driver는 이미지 다운로드를 위해 유지하지 않음 (메모리 절약)
            if driver:
                driver.quit()
            # 임시 프로필 디렉토리 정리
            try:
                import shutil
                # 사용자 지정 프로필을 사용한 경우에는 삭제하지 않음
                if 'profile_dir' in locals() and profile_dir and not used_custom_profile:
                    shutil.rmtree(profile_dir, ignore_errors=True)
                    logger.info(f"임시 프로필 디렉토리 삭제: {profile_dir}")
            except:
                pass
    
    def _filter_by_size_pattern(self, urls: List[str]) -> List[str]:
        """크기 패턴 기반 필터링: 게시글 썸네일은 보통 같은 크기"""
        logger.info(f"크기 패턴 필터링 시작: {len(urls)}개 URL")
        if len(urls) <= 2:
            logger.info("URL이 2개 이하라 필터링하지 않습니다.")
            return urls
        
        # 1단계: 프로필 이미지와 스토리 커버 제외
        filtered_urls = []
        excluded_count = 0
        
        for url in urls:
            url_lower = url.lower()
            # 프로필 이미지 제외
            if ('_19/' in url or '/t51.2885-19/' in url or 
                'profile' in url_lower or 
                '100x100' in url or '150x150' in url or
                re.search(r'[^0-9]1[0-5][0-9]x1[0-5][0-9]', url)):  # 100-159x100-159 크기
                excluded_count += 1
                logger.debug(f"프로필 이미지 제외: {url[:80]}...")
                continue
            
            # 스토리 커버 제외
            if ('stories' in url_lower or 'story' in url_lower or
                'highlight' in url_lower):
                excluded_count += 1
                logger.debug(f"스토리 커버 제외: {url[:80]}...")
                continue
            
            filtered_urls.append(url)
        
        if excluded_count > 0:
            logger.info(f"프로필/스토리 이미지 {excluded_count}개 제외")
        
        if len(filtered_urls) <= 2:
            return filtered_urls
        
        # 2단계: 각 URL에서 크기 정보 추출
        size_groups = {}
        no_size_urls = []
        
        for url in filtered_urls:
            # URL에서 크기 정보 추출 (예: 640x640, 1080x1080, p640x640)
            # 쿼리 파라미터에서 p640x640 패턴 찾기
            p_size_match = re.search(r'p(\d+)x(\d+)', url)
            if p_size_match:
                width = int(p_size_match.group(1))
                height = int(p_size_match.group(2))
            else:
                # 일반 크기 패턴 찾기
                size_match = re.search(r'(\d+)x(\d+)', url)
                if size_match:
                    width = int(size_match.group(1))
                    height = int(size_match.group(2))
                else:
                    width = height = None
            
            if width and height:
                # 정사각형이거나 비슷한 크기만 고려 (게시글 썸네일은 보통 정사각형)
                # 작은 크기 제외 (200x200 미만은 프로필 이미지일 가능성 높음)
                if width >= 200 and height >= 200 and abs(width - height) <= 50:
                    size_key = f"{width}x{height}"
                    if size_key not in size_groups:
                        size_groups[size_key] = []
                    size_groups[size_key].append(url)
                else:
                    no_size_urls.append(url)
            else:
                # 크기 정보가 없는 경우, URL 패턴으로 판단
                # 게시글 이미지 패턴인 경우
                if '/t51.2885-15/' in url or '/t51.29350-15/' in url:
                    # 크기가 명시되지 않은 게시글 이미지는 별도 그룹
                    if 'no_size_post' not in size_groups:
                        size_groups['no_size_post'] = []
                    size_groups['no_size_post'].append(url)
                else:
                    no_size_urls.append(url)
        
        # 3단계: 가장 많이 나타나는 크기 패턴 찾기
        if not size_groups:
            logger.warning("크기 패턴을 찾을 수 없어 필터링하지 않습니다.")
            return filtered_urls
        
        # 크기별 개수 정렬
        sorted_groups = sorted(size_groups.items(), key=lambda x: len(x[1]), reverse=True)
        most_common_size, most_common_urls = sorted_groups[0]
        
        logger.info(f"크기 패턴 분석: {len(size_groups)}개 그룹 발견")
        for size_key, group_urls in sorted_groups:
            logger.info(f"  - {size_key}: {len(group_urls)}개")
        
        # 가장 많이 나타나는 크기 패턴이 2개 이상이고, 다른 그룹보다 많으면 필터링
        if len(most_common_urls) >= 2 and len(most_common_urls) > len(no_size_urls):
            logger.info(f"가장 많이 나타나는 크기 패턴 '{most_common_size}' ({len(most_common_urls)}개) 선택")
            return most_common_urls
        else:
            # 필터링하지 않고 중복 제거된 URL 반환
            logger.info(f"명확한 크기 패턴이 없어 필터링하지 않습니다. (가장 많은 그룹: {len(most_common_urls)}개, 크기 없는 URL: {len(no_size_urls)}개)")
            # 크기 없는 URL도 포함하여 반환 (게시글 이미지일 수 있음)
            if len(no_size_urls) > 0 and 'no_size_post' in size_groups:
                logger.info(f"크기 없는 게시글 이미지 {len(size_groups['no_size_post'])}개 포함")
                return filtered_urls
            return filtered_urls
    
    def get_thumbnails_official_api(self, username: str) -> List[str]:
        """Instagram 공식 Graph API를 사용한 썸네일 추출"""
        try:
            if not self.access_token:
                logger.warning("Instagram Graph API 액세스 토큰이 없습니다. 환경 변수 INSTAGRAM_ACCESS_TOKEN을 설정하거나 access_token을 전달하세요.")
                return []
            
            logger.info(f"Instagram Graph API로 {username} 썸네일 크롤링 시작")
            
            # 1단계: username으로 Instagram User ID 찾기
            # Facebook Graph API를 통해 username으로 user_id 조회
            fb_graph_url = f"https://graph.facebook.com/v18.0/{username}"
            params = {
                'fields': 'id,username',
                'access_token': self.access_token
            }
            
            response = self.session.get(fb_graph_url, params=params, timeout=15)
            logger.info(f"User ID 조회 응답 코드: {response.status_code}")
            
            if response.status_code != 200:
                logger.warning(f"User ID 조회 실패: {response.status_code}")
                logger.warning(f"응답 내용: {response.text[:500]}")
                # username이 Instagram Business Account가 아닐 수 있음
                # Instagram Basic Display API 또는 다른 방법 시도
                return []
            
            user_data = response.json()
            user_id = user_data.get('id')
            
            if not user_id:
                logger.error("User ID를 찾을 수 없습니다")
                return []
            
            logger.info(f"Instagram User ID: {user_id}")
            
            # 2단계: 사용자의 미디어 목록 가져오기
            media_url = f"{self.graph_api_base}/{user_id}/media"
            params = {
                'fields': 'id,media_type,media_url,thumbnail_url,permalink',
                'limit': 4,  # 처음 4개만
                'access_token': self.access_token
            }
            
            response = self.session.get(media_url, params=params, timeout=15)
            logger.info(f"미디어 목록 조회 응답 코드: {response.status_code}")
            
            if response.status_code != 200:
                logger.warning(f"미디어 목록 조회 실패: {response.status_code}")
                logger.warning(f"응답 내용: {response.text[:500]}")
                return []
            
            media_data = response.json()
            media_list = media_data.get('data', [])
            
            logger.info(f"미디어 개수: {len(media_list)}개")
            
            thumbnail_urls = []
            
            for i, media in enumerate(media_list):
                media_type = media.get('media_type', '')
                
                # 이미지인 경우
                if media_type == 'IMAGE':
                    image_url = media.get('media_url') or media.get('thumbnail_url')
                    if image_url:
                        thumbnail_urls.append(image_url)
                        logger.info(f"✓ 이미지 썸네일 추가 [{i+1}]: {image_url[:100]}...")
                
                # 비디오인 경우 썸네일 사용
                elif media_type == 'VIDEO':
                    thumbnail_url = media.get('thumbnail_url')
                    if thumbnail_url:
                        thumbnail_urls.append(thumbnail_url)
                        logger.info(f"✓ 비디오 썸네일 추가 [{i+1}]: {thumbnail_url[:100]}...")
                
                # 캐러셀(여러 이미지)인 경우
                elif media_type == 'CAROUSEL_ALBUM':
                    # 캐러셀의 첫 번째 이미지 가져오기
                    media_id = media.get('id')
                    if media_id:
                        children_url = f"{self.graph_api_base}/{media_id}/children"
                        children_params = {
                            'fields': 'media_url,thumbnail_url',
                            'access_token': self.access_token
                        }
                        children_response = self.session.get(children_url, params=children_params, timeout=15)
                        if children_response.status_code == 200:
                            children_data = children_response.json()
                            children = children_data.get('data', [])
                            if children and len(children) > 0:
                                first_child = children[0]
                                child_url = first_child.get('media_url') or first_child.get('thumbnail_url')
                                if child_url:
                                    thumbnail_urls.append(child_url)
                                    logger.info(f"✓ 캐러셀 썸네일 추가 [{i+1}]: {child_url[:100]}...")
            
            logger.info(f"총 {len(thumbnail_urls)}개 썸네일 추출 완료 (Instagram Graph API)")
            return thumbnail_urls[:4]
            
        except Exception as e:
            logger.error(f"Instagram Graph API 크롤링 실패: {e}", exc_info=True)
            return []
    
    def get_thumbnails_graphql(self, username: str) -> List[str]:
        """인스타그램 GraphQL API를 사용한 썸네일 추출 - Instagram Web Profile API 사용"""
        try:
            logger.info(f"GraphQL API로 {username} 썸네일 크롤링 시작")
            
            # Instagram Web Profile API 사용 (인증 없이 공개 프로필 접근 가능)
            api_url = f"https://www.instagram.com/api/v1/users/web_profile_info/?username={username}"
            headers = {
                'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
                'Accept': 'application/json',
                'Accept-Language': 'ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7',
                'Referer': f'https://www.instagram.com/{username}/',
                'X-Requested-With': 'XMLHttpRequest',
                'X-IG-App-ID': '936619743392459',
            }
            
            response = self.session.get(api_url, headers=headers, timeout=15)
            logger.info(f"API 응답 코드: {response.status_code}")
            
            if response.status_code != 200:
                logger.warning(f"API 응답 실패: {response.status_code}")
                logger.warning(f"응답 내용: {response.text[:500]}")
                # API 실패 시 Selenium으로 폴백
                logger.info("API 실패, Selenium으로 폴백 시도...")
                return self.get_thumbnails_selenium(username)
            
            try:
                data = response.json()
            except json.JSONDecodeError:
                logger.error("JSON 파싱 실패")
                logger.error(f"응답 내용: {response.text[:1000]}")
                return self.get_thumbnails_selenium(username)
            
            thumbnail_urls = []
            seen_urls = set()
            
            # 게시물 데이터 추출
            try:
                user = data.get('data', {}).get('user', {})
                edge_owner_to_timeline_media = user.get('edge_owner_to_timeline_media', {})
                edges = edge_owner_to_timeline_media.get('edges', [])
                
                logger.info(f"게시물 개수: {len(edges)}개")
                
                for i, edge in enumerate(edges[:4]):  # 처음 4개만
                    node = edge.get('node', {})
                    
                    # 이미지 URL 추출 (여러 방법 시도)
                    image_url = None
                    
                    # 방법 1: display_url (일반 이미지)
                    if 'display_url' in node:
                        image_url = node['display_url']
                        logger.info(f"게시물 {i+1}: display_url 사용")
                    
                    # 방법 2: thumbnail_src (썸네일)
                    elif 'thumbnail_src' in node:
                        image_url = node['thumbnail_src']
                        logger.info(f"게시물 {i+1}: thumbnail_src 사용")
                    
                    # 방법 3: thumbnail_resources에서 가장 큰 이미지
                    elif 'thumbnail_resources' in node:
                        resources = node['thumbnail_resources']
                        if resources and len(resources) > 0:
                            # 가장 큰 크기의 이미지 선택
                            largest = max(resources, key=lambda x: x.get('config_width', 0) * x.get('config_height', 0))
                            image_url = largest.get('src')
                            logger.info(f"게시물 {i+1}: thumbnail_resources에서 선택")
                    
                    # 방법 4: edge_sidecar_to_children (여러 이미지 게시물)
                    elif 'edge_sidecar_to_children' in node:
                        children = node['edge_sidecar_to_children'].get('edges', [])
                        if children and len(children) > 0:
                            first_child = children[0].get('node', {})
                            image_url = first_child.get('display_url') or first_child.get('thumbnail_src')
                            logger.info(f"게시물 {i+1}: edge_sidecar_to_children에서 첫 번째 이미지 사용")
                    
                    if image_url:
                        # 프로필 이미지 제외
                        if '_19/' in image_url or '/t51.2885-19/' in image_url or 'profile' in image_url.lower():
                            logger.debug(f"프로필 이미지로 제외: {image_url[:80]}...")
                            continue
                        
                        # static 제외
                        if 'static.cdninstagram.com' in image_url:
                            logger.debug(f"static 이미지로 제외: {image_url[:80]}...")
                            continue
                        
                        # 중복 제거
                        base_url = image_url.split('?')[0]
                        if base_url not in seen_urls:
                            seen_urls.add(base_url)
                            thumbnail_urls.append(image_url)
                            logger.info(f"✓ 썸네일 추가 [{i+1}]: {image_url[:100]}...")
                    else:
                        logger.warning(f"게시물 {i+1}: 이미지 URL을 찾을 수 없음")
                
                logger.info(f"총 {len(thumbnail_urls)}개 썸네일 추출 완료")
                return thumbnail_urls[:4]
                
            except Exception as e:
                logger.error(f"데이터 파싱 오류: {e}", exc_info=True)
                logger.error(f"응답 데이터 구조: {str(data)[:1000]}")
                return self.get_thumbnails_selenium(username)
                
        except Exception as e:
            logger.error(f"GraphQL API 크롤링 실패: {e}", exc_info=True)
            # 실패 시 Selenium으로 폴백
            logger.info("Selenium으로 폴백 시도...")
            return self.get_thumbnails_selenium(username)
    
    def get_thumbnails(self, username: str, use_selenium: bool = True, use_api: bool = False, use_official_api: bool = False) -> List[str]:
        """썸네일 URL 조회 (메인 메서드)
        
        Args:
            username: Instagram 사용자명
            use_selenium: Selenium 사용 여부
            use_api: Web Profile API 사용 여부 (기본값: False)
            use_official_api: Instagram 공식 Graph API 사용 여부 (기본값: False, 액세스 토큰 필요)
        
        우선순위:
        1. use_official_api=True (공식 API)
        2. use_selenium=True (Selenium)
        3. use_api=True (Web Profile API)
        4. 기본값: get_thumbnails_simple (개선된 간단한 방법)
        """
        # 공식 API가 우선순위가 가장 높음
        if use_official_api and self.access_token:
            result = self.get_thumbnails_official_api(username)
            if result:
                return result
            logger.warning("공식 API 실패, 다른 방법으로 폴백...")
        
        if use_selenium:
            result = self.get_thumbnails_selenium(username)
            if result:
                return result
            logger.warning("Selenium 실패, 다른 방법으로 폴백...")
        # Selenium 실패 또는 비활성화 시, Web Profile API를 항상 시도
        result = self.get_thumbnails_graphql(username)
        if result:
            return result
        logger.warning("Web Profile API 실패, 간단한 방법으로 폴백...")
        # 기본값: 개선된 간단한 방법 (가장 안정적)
        return self.get_thumbnails_simple(username)
    
    def download_image_with_selenium(self, image_url: str, driver: webdriver.Chrome) -> Optional[bytes]:
        """기존 Selenium 세션을 사용하여 이미지 다운로드 (403 방지)"""
        try:
            # Selenium으로 이미지 URL 접근
            driver.get(image_url)
            time.sleep(2)
            
            # JavaScript로 이미지를 base64로 변환
            base64_image = driver.execute_script("""
                var img = document.querySelector('img');
                if (!img) return null;
                var canvas = document.createElement('canvas');
                canvas.width = img.naturalWidth || img.width;
                canvas.height = img.naturalHeight || img.height;
                var ctx = canvas.getContext('2d');
                ctx.drawImage(img, 0, 0);
                return canvas.toDataURL('image/jpeg').split(',')[1];
            """)
            
            if base64_image:
                return base64.b64decode(base64_image)
            
            # 실패 시 requests로 시도 (Selenium 쿠키 사용)
            cookies = driver.get_cookies()
            session = requests.Session()
            for cookie in cookies:
                session.cookies.set(cookie['name'], cookie['value'])
            
            session.headers.update({
                'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
                'Referer': 'https://www.instagram.com/'
            })
            
            response = session.get(image_url, timeout=10)
            if response.status_code == 200:
                return response.content
                
        except Exception as e:
            logger.error(f"이미지 다운로드 실패: {e}")
            return None
        
        return None
    