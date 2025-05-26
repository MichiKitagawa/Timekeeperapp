"""
Timekeeper Backend Validation Tests
バリデーション機能のテスト
"""
import pytest
import uuid
from validation import RequestValidator, ValidationError


class TestRequestValidator:
    """RequestValidatorのテストクラス"""
    
    def test_validate_device_id_success(self):
        """device_idバリデーション成功テスト"""
        # 有効なUUID
        valid_uuid = str(uuid.uuid4())
        result = RequestValidator.validate_device_id(valid_uuid)
        assert result == valid_uuid
    
    def test_validate_device_id_missing(self):
        """device_id未指定エラーテスト"""
        with pytest.raises(ValidationError) as exc_info:
            RequestValidator.validate_device_id(None)
        
        assert exc_info.value.error_code == "missing_device_id"
        assert exc_info.value.status_code == 400
        
        with pytest.raises(ValidationError) as exc_info:
            RequestValidator.validate_device_id("")
        
        assert exc_info.value.error_code == "missing_device_id"
        assert exc_info.value.status_code == 400
    
    def test_validate_device_id_invalid_type(self):
        """device_id型エラーテスト"""
        with pytest.raises(ValidationError) as exc_info:
            RequestValidator.validate_device_id(123)
        
        assert exc_info.value.error_code == "invalid_device_id_type"
        assert exc_info.value.status_code == 400
    
    def test_validate_device_id_invalid_format(self):
        """device_id形式エラーテスト"""
        with pytest.raises(ValidationError) as exc_info:
            RequestValidator.validate_device_id("invalid-uuid")
        
        assert exc_info.value.error_code == "invalid_device_id_format"
        assert exc_info.value.status_code == 400
    
    def test_validate_purchase_token_success(self):
        """purchase_tokenバリデーション成功テスト"""
        # 有効なStripeセッションID
        valid_tokens = [
            "cs_test_1234567890abcdef",
            "cs_live_abcdef1234567890"
        ]
        
        for token in valid_tokens:
            result = RequestValidator.validate_purchase_token(token)
            assert result == token
    
    def test_validate_purchase_token_missing(self):
        """purchase_token未指定エラーテスト"""
        with pytest.raises(ValidationError) as exc_info:
            RequestValidator.validate_purchase_token(None)
        
        assert exc_info.value.error_code == "missing_purchase_token"
        assert exc_info.value.status_code == 400
        
        with pytest.raises(ValidationError) as exc_info:
            RequestValidator.validate_purchase_token("")
        
        assert exc_info.value.error_code == "missing_purchase_token"
        assert exc_info.value.status_code == 400
    
    def test_validate_purchase_token_invalid_type(self):
        """purchase_token型エラーテスト"""
        with pytest.raises(ValidationError) as exc_info:
            RequestValidator.validate_purchase_token(123)
        
        assert exc_info.value.error_code == "invalid_purchase_token_type"
        assert exc_info.value.status_code == 400
    
    def test_validate_purchase_token_invalid_format(self):
        """purchase_token形式エラーテスト"""
        invalid_tokens = [
            "invalid-token",
            "cs_invalid_token",
            "pi_test_1234567890",  # PaymentIntentのID
            "cs_1234567890"  # test/liveが含まれていない
        ]
        
        for token in invalid_tokens:
            with pytest.raises(ValidationError) as exc_info:
                RequestValidator.validate_purchase_token(token)
            
            assert exc_info.value.error_code == "invalid_purchase_token_format"
            assert exc_info.value.status_code == 400
    
    def test_validate_common_request_success(self):
        """共通リクエストバリデーション成功テスト"""
        valid_uuid = str(uuid.uuid4())
        request_data = {
            "device_id": valid_uuid
        }
        
        result = RequestValidator.validate_common_request(request_data)
        assert result["device_id"] == valid_uuid
    
    def test_validate_common_request_missing_device_id(self):
        """共通リクエストバリデーション - device_id未指定エラーテスト"""
        request_data = {}
        
        with pytest.raises(ValidationError) as exc_info:
            RequestValidator.validate_common_request(request_data)
        
        assert exc_info.value.error_code == "missing_device_id"
        assert exc_info.value.status_code == 400
    
    def test_validate_license_confirm_request_success(self):
        """ライセンス確認リクエストバリデーション成功テスト"""
        valid_uuid = str(uuid.uuid4())
        request_data = {
            "device_id": valid_uuid,
            "purchase_token": "cs_test_1234567890abcdef"
        }
        
        result = RequestValidator.validate_license_confirm_request(request_data)
        assert result["device_id"] == valid_uuid
        assert result["purchase_token"] == "cs_test_1234567890abcdef"
    
    def test_validate_license_confirm_request_missing_fields(self):
        """ライセンス確認リクエストバリデーション - フィールド未指定エラーテスト"""
        # device_id未指定
        with pytest.raises(ValidationError) as exc_info:
            RequestValidator.validate_license_confirm_request({
                "purchase_token": "cs_test_1234567890abcdef"
            })
        assert exc_info.value.error_code == "missing_device_id"
        
        # purchase_token未指定
        with pytest.raises(ValidationError) as exc_info:
            RequestValidator.validate_license_confirm_request({
                "device_id": str(uuid.uuid4())
            })
        assert exc_info.value.error_code == "missing_purchase_token"
    
    def test_validate_unlock_daypass_request_success(self):
        """デイパスアンロックリクエストバリデーション成功テスト"""
        valid_uuid = str(uuid.uuid4())
        request_data = {
            "device_id": valid_uuid,
            "purchase_token": "cs_live_abcdef1234567890"
        }
        
        result = RequestValidator.validate_unlock_daypass_request(request_data)
        assert result["device_id"] == valid_uuid
        assert result["purchase_token"] == "cs_live_abcdef1234567890"
    
    def test_validate_unlock_daypass_request_missing_fields(self):
        """デイパスアンロックリクエストバリデーション - フィールド未指定エラーテスト"""
        # device_id未指定
        with pytest.raises(ValidationError) as exc_info:
            RequestValidator.validate_unlock_daypass_request({
                "purchase_token": "cs_test_1234567890abcdef"
            })
        assert exc_info.value.error_code == "missing_device_id"
        
        # purchase_token未指定
        with pytest.raises(ValidationError) as exc_info:
            RequestValidator.validate_unlock_daypass_request({
                "device_id": str(uuid.uuid4())
            })
        assert exc_info.value.error_code == "missing_purchase_token"


if __name__ == "__main__":
    pytest.main([__file__]) 