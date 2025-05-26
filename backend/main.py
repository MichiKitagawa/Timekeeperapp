"""
Timekeeper Backend API
FastAPIを使用したバックエンドサーバー
"""
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from config import firestore_config


@asynccontextmanager
async def lifespan(app: FastAPI):
    """アプリケーションのライフサイクル管理"""
    # 起動時の処理
    success = firestore_config.initialize_firestore()
    if not success:
        print("Warning: Firestore initialization failed")
    
    yield
    
    # 終了時の処理（必要に応じて追加）


app = FastAPI(
    title="Timekeeper API",
    description="TimekeeperアプリのバックエンドAPI",
    version="1.0.0",
    lifespan=lifespan
)

@app.get("/health")
async def health_check():
    """ヘルスチェックエンドポイント"""
    return {
        "status": "ok",
        "firestore_initialized": firestore_config.is_initialized()
    }

@app.get("/test_firestore")
async def test_firestore():
    """Firestore接続テストエンドポイント"""
    if not firestore_config.is_initialized():
        raise HTTPException(
            status_code=503,
            detail="Firestore not initialized"
        )
    
    result = firestore_config.test_connection()
    
    if result["status"] == "error":
        raise HTTPException(
            status_code=500,
            detail=result["message"]
        )
    
    return result

@app.get("/firestore/status")
async def firestore_status():
    """Firestore初期化状態を確認"""
    return {
        "initialized": firestore_config.is_initialized(),
        "environment": firestore_config.environment
    } 