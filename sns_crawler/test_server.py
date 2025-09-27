from fastapi import FastAPI
import uvicorn

app = FastAPI()

@app.get("/")
async def root():
    return {"message": "Test server is running"}

@app.get("/health")
async def health():
    return {"status": "healthy"}

@app.get("/instagram/thumbnails/urls/{username}")
async def get_thumbnails(username: str):
    # 테스트용 더미 데이터
    return {
        "username": username,
        "thumbnail_urls": [
            "https://example.com/thumb1.jpg",
            "https://example.com/thumb2.jpg",
            "https://example.com/thumb3.jpg",
            "https://example.com/thumb4.jpg",
            "https://example.com/thumb5.jpg"
        ],
        "count": 5,
        "crawled_at": "2024-01-15T10:30:00",
        "success": True
    }

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)