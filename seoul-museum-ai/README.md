## 서울시 미술관 정문 인식 AI

이 프로젝트는 서울에 있는 약 70개 미술관의 정문 사진을 분류해 어느 미술관인지 식별하는 모델과 추론 API를 구축하기 위한 템플릿입니다.  
Colab에서 데이터 수집과 학습을 진행하고, FastAPI 기반 추론 마이크로서비스로 배포할 수 있도록 구성했습니다.

### 폴더 구조

```
seoul-museum-ai/
├── data/                   # 정문 이미지 데이터셋 (70개 × 40장 목표)
│   └── seoul_museums/
├── models/                 # best_model.pth, class_mapping.json 저장
├── notebooks/              # Colab/VS Code용 Jupyter 노트북
│   ├── 01_data_crawling.ipynb
│   ├── 02_train_model.ipynb
│   └── 03_test_model.ipynb
├── src/                    # 추론 서비스 코드
│   ├── api.py              # FastAPI 애플리케이션
│   └── predict.py          # 모델 로딩 및 예측 유틸
└── requirements.txt        # 통합 의존성
```

### 빠른 시작

```bash
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

1. **01_data_crawling.ipynb** – Bing/기타 이미지 크롤링, 중복 제거, 외부 스토리지 백업 가이드  
2. **02_train_model.ipynb** – EfficientNet 기반 파인튜닝, 체크포인트 저장  
3. **03_test_model.ipynb** – Top-3 예측 검증, 시각화

학습이 끝나면 `models/best_model.pth`, `models/class_mapping.json` 파일이 생성됩니다.

### 추론 API 실행

```bash
uvicorn src.api:app --host 0.0.0.0 --port 8000
```

- `POST /predict` – multipart/form-data로 정문 이미지를 업로드하면 Top-K 예측을 반환합니다.  
- `GET /health` – 모델 로딩 상태를 확인합니다.

환경 변수:
- `MODEL_PATH`, `CLASS_MAP_PATH` – 커스텀 경로 지정 가능  
- `DEFAULT_TOPK` – 기본 Top-K 개수 (기본 3)  
- `CORS_ALLOW_ORIGINS` – CORS 허용 도메인 목록 (쉼표 구분)

### Colab 활용 팁

- 노트북에서 `!pip install -r requirements.txt` 명령으로 의존성을 맞출 수 있습니다.  
- 데이터는 Google Drive 또는 S3 호환 스토리지에 백업해 세션 만료에 대비하세요.  
- 학습 후 `models` 디렉터리를 통째로 다운로드하여 FastAPI 서버에 배포합니다.

### 향후 확장 아이디어

- DVC 또는 Git LFS로 이미지/모델 버전 관리  
- ONNX/TensorRT로 추론 최적화  
- 유사 이미지 검색(Top-K 이미지 반환) 기능 추가  
- Spring 또는 Node 서비스와의 REST 연동 예시 스크립트 작성

