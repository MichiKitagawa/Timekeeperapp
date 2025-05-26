"""
Timekeeper Backend API
FastAPIを使用したバックエンドサーバー
"""
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from config import firestore_config
from middleware import ErrorHandlingMiddleware
from validation import RequestValidator, ValidationError
from models import (
    LicenseConfirmRequest, LicenseConfirmResponse,
    UnlockDaypassRequest, UnlockDaypassResponse,
    ErrorResponse
)


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

# 共通エラーハンドリングミドルウェアを追加
app.add_middleware(ErrorHandlingMiddleware)

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


@app.post("/license/confirm", response_model=LicenseConfirmResponse, responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}, 500: {"model": ErrorResponse}})
async def license_confirm(request: LicenseConfirmRequest):
    """
    ライセンス購入確認API
    
    Args:
        request: ライセンス確認リクエスト
        
    Returns:
        LicenseConfirmResponse: 確認結果
        
    Raises:
        ValidationError: バリデーションエラー
        HTTPException: その他のエラー
    """
    # リクエストバリデーション
    validated_data = RequestValidator.validate_license_confirm_request(request.model_dump())
    
    # TODO: Stripe検証とFirestore更新の実装
    # 現在は基本的なバリデーションのみ実装
    
    return LicenseConfirmResponse(status="ok")


@app.post("/unlock/daypass", response_model=UnlockDaypassResponse, responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}, 500: {"model": ErrorResponse}})
async def unlock_daypass(request: UnlockDaypassRequest):
    """
    デイパスアンロックAPI
    
    Args:
        request: デイパスアンロックリクエスト
        
    Returns:
        UnlockDaypassResponse: アンロック結果
        
    Raises:
        ValidationError: バリデーションエラー
        HTTPException: その他のエラー
    """
    # リクエストバリデーション
    validated_data = RequestValidator.validate_unlock_daypass_request(request.model_dump())
    
    # TODO: Stripe検証とFirestore更新の実装
    # 現在は基本的なバリデーションのみ実装
    
    return UnlockDaypassResponse(
        status="ok",
        unlock_count=1,
        last_unlock_date="2025-01-27"
    ) 