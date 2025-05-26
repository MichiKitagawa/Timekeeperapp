"""
Timekeeper Backend Models
APIリクエスト・レスポンス用のPydanticモデル
"""
from pydantic import BaseModel, Field
from typing import Optional
from datetime import date


class LicenseConfirmRequest(BaseModel):
    """ライセンス確認リクエスト"""
    device_id: str = Field(..., description="デバイスID（UUID形式）")
    purchase_token: str = Field(..., description="Stripe購入トークン")


class LicenseConfirmResponse(BaseModel):
    """ライセンス確認レスポンス"""
    status: str = Field(..., description="処理ステータス")


class UnlockDaypassRequest(BaseModel):
    """デイパスアンロックリクエスト"""
    device_id: str = Field(..., description="デバイスID（UUID形式）")
    purchase_token: str = Field(..., description="Stripe購入トークン")


class UnlockDaypassResponse(BaseModel):
    """デイパスアンロックレスポンス"""
    status: str = Field(..., description="処理ステータス")
    unlock_count: int = Field(..., description="アンロック回数")
    last_unlock_date: str = Field(..., description="最終アンロック日（YYYY-MM-DD形式）")


class ErrorResponse(BaseModel):
    """エラーレスポンス"""
    error: str = Field(..., description="エラーコード")
    message: str = Field(..., description="エラーメッセージ") 