"""
Timekeeper Backend Validation
共通リクエストバリデーション機能
"""
import re
import uuid
from typing import Dict, Any, Optional
from fastapi import HTTPException


class ValidationError(Exception):
    """バリデーションエラー"""
    def __init__(self, error_code: str, message: str, status_code: int = 400):
        self.error_code = error_code
        self.message = message
        self.status_code = status_code
        super().__init__(message)


class RequestValidator:
    """リクエストバリデーター"""
    
    @staticmethod
    def validate_device_id(device_id: Optional[str]) -> str:
        """
        device_idのバリデーション
        
        Args:
            device_id: バリデーション対象のdevice_id
            
        Returns:
            str: バリデーション済みのdevice_id
            
        Raises:
            ValidationError: バリデーションエラー
        """
        if not device_id:
            raise ValidationError(
                error_code="missing_device_id",
                message="device_id が必須です",
                status_code=400
            )
        
        if not isinstance(device_id, str):
            raise ValidationError(
                error_code="invalid_device_id_type",
                message="device_id は文字列である必要があります",
                status_code=400
            )
        
        # UUID形式のバリデーション
        try:
            # UUIDとして解析可能かチェック
            uuid.UUID(device_id)
        except ValueError:
            raise ValidationError(
                error_code="invalid_device_id_format",
                message="device_id はUUID形式である必要があります",
                status_code=400
            )
        
        return device_id
    
    @staticmethod
    def validate_purchase_token(purchase_token: Optional[str]) -> str:
        """
        purchase_tokenのバリデーション
        
        Args:
            purchase_token: バリデーション対象のpurchase_token
            
        Returns:
            str: バリデーション済みのpurchase_token
            
        Raises:
            ValidationError: バリデーションエラー
        """
        if not purchase_token:
            raise ValidationError(
                error_code="missing_purchase_token",
                message="purchase_token が必須です",
                status_code=400
            )
        
        if not isinstance(purchase_token, str):
            raise ValidationError(
                error_code="invalid_purchase_token_type",
                message="purchase_token は文字列である必要があります",
                status_code=400
            )
        
        # Stripe セッション ID の形式チェック（cs_test_ または cs_live_ で始まる）
        if not re.match(r'^cs_(test|live)_[a-zA-Z0-9]+$', purchase_token):
            raise ValidationError(
                error_code="invalid_purchase_token_format",
                message="purchase_token はStripeセッションID形式である必要があります",
                status_code=400
            )
        
        return purchase_token
    
    @staticmethod
    def validate_common_request(request_data: Dict[str, Any]) -> Dict[str, str]:
        """
        共通リクエストフィールドのバリデーション
        
        Args:
            request_data: リクエストデータ
            
        Returns:
            Dict[str, str]: バリデーション済みのリクエストデータ
            
        Raises:
            ValidationError: バリデーションエラー
        """
        validated_data = {}
        
        # device_idのバリデーション
        validated_data['device_id'] = RequestValidator.validate_device_id(
            request_data.get('device_id')
        )
        
        return validated_data
    
    @staticmethod
    def validate_license_confirm_request(request_data: Dict[str, Any]) -> Dict[str, str]:
        """
        /license/confirm リクエストのバリデーション
        
        Args:
            request_data: リクエストデータ
            
        Returns:
            Dict[str, str]: バリデーション済みのリクエストデータ
            
        Raises:
            ValidationError: バリデーションエラー
        """
        validated_data = RequestValidator.validate_common_request(request_data)
        
        # purchase_tokenのバリデーション
        validated_data['purchase_token'] = RequestValidator.validate_purchase_token(
            request_data.get('purchase_token')
        )
        
        return validated_data
    
    @staticmethod
    def validate_unlock_daypass_request(request_data: Dict[str, Any]) -> Dict[str, str]:
        """
        /unlock/daypass リクエストのバリデーション
        
        Args:
            request_data: リクエストデータ
            
        Returns:
            Dict[str, str]: バリデーション済みのリクエストデータ
            
        Raises:
            ValidationError: バリデーションエラー
        """
        validated_data = RequestValidator.validate_common_request(request_data)
        
        # purchase_tokenのバリデーション
        validated_data['purchase_token'] = RequestValidator.validate_purchase_token(
            request_data.get('purchase_token')
        )
        
        return validated_data


def validation_error_handler(validation_error: ValidationError) -> HTTPException:
    """
    ValidationErrorをHTTPExceptionに変換
    
    Args:
        validation_error: ValidationError
        
    Returns:
        HTTPException: FastAPI用のHTTPException
    """
    return HTTPException(
        status_code=validation_error.status_code,
        detail={
            "error": validation_error.error_code,
            "message": validation_error.message
        }
    ) 