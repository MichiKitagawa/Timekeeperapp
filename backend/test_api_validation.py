"""
Timekeeper Backend API Validation Tests
APIエンドポイントのバリデーション機能テスト
"""
import pytest
import uuid
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


class TestAPIValidation:
    """APIバリデーションのテストクラス"""
    
    def test_license_confirm_success(self):
        """ライセンス確認API成功テスト"""
        valid_uuid = str(uuid.uuid4())
        request_data = {
            "device_id": valid_uuid,
            "purchase_token": "cs_test_1234567890abcdef"
        }
        
        response = client.post("/license/confirm", json=request_data)
        assert response.status_code == 200
        
        data = response.json()
        assert data["status"] == "ok"
    
    def test_license_confirm_missing_device_id(self):
        """ライセンス確認API - device_id未指定エラーテスト"""
        request_data = {
            "purchase_token": "cs_test_1234567890abcdef"
        }
        
        response = client.post("/license/confirm", json=request_data)
        assert response.status_code == 422  # Pydanticバリデーションエラー
    
    def test_license_confirm_invalid_device_id(self):
        """ライセンス確認API - device_id形式エラーテスト"""
        request_data = {
            "device_id": "invalid-uuid",
            "purchase_token": "cs_test_1234567890abcdef"
        }
        
        response = client.post("/license/confirm", json=request_data)
        assert response.status_code == 400
        
        data = response.json()
        assert data["error"] == "invalid_device_id_format"
        assert "UUID形式" in data["message"]
    
    def test_license_confirm_missing_purchase_token(self):
        """ライセンス確認API - purchase_token未指定エラーテスト"""
        request_data = {
            "device_id": str(uuid.uuid4())
        }
        
        response = client.post("/license/confirm", json=request_data)
        assert response.status_code == 422  # Pydanticバリデーションエラー
    
    def test_license_confirm_invalid_purchase_token(self):
        """ライセンス確認API - purchase_token形式エラーテスト"""
        request_data = {
            "device_id": str(uuid.uuid4()),
            "purchase_token": "invalid-token"
        }
        
        response = client.post("/license/confirm", json=request_data)
        assert response.status_code == 400
        
        data = response.json()
        assert data["error"] == "invalid_purchase_token_format"
        assert "Stripeセッション" in data["message"]
    
    def test_unlock_daypass_success(self):
        """デイパスアンロックAPI成功テスト"""
        valid_uuid = str(uuid.uuid4())
        request_data = {
            "device_id": valid_uuid,
            "purchase_token": "cs_live_abcdef1234567890"
        }
        
        response = client.post("/unlock/daypass", json=request_data)
        assert response.status_code == 200
        
        data = response.json()
        assert data["status"] == "ok"
        assert "unlock_count" in data
        assert "last_unlock_date" in data
    
    def test_unlock_daypass_missing_device_id(self):
        """デイパスアンロックAPI - device_id未指定エラーテスト"""
        request_data = {
            "purchase_token": "cs_test_1234567890abcdef"
        }
        
        response = client.post("/unlock/daypass", json=request_data)
        assert response.status_code == 422  # Pydanticバリデーションエラー
    
    def test_unlock_daypass_invalid_device_id(self):
        """デイパスアンロックAPI - device_id形式エラーテスト"""
        request_data = {
            "device_id": "not-a-uuid",
            "purchase_token": "cs_test_1234567890abcdef"
        }
        
        response = client.post("/unlock/daypass", json=request_data)
        assert response.status_code == 400
        
        data = response.json()
        assert data["error"] == "invalid_device_id_format"
        assert "UUID形式" in data["message"]
    
    def test_unlock_daypass_missing_purchase_token(self):
        """デイパスアンロックAPI - purchase_token未指定エラーテスト"""
        request_data = {
            "device_id": str(uuid.uuid4())
        }
        
        response = client.post("/unlock/daypass", json=request_data)
        assert response.status_code == 422  # Pydanticバリデーションエラー
    
    def test_unlock_daypass_invalid_purchase_token(self):
        """デイパスアンロックAPI - purchase_token形式エラーテスト"""
        request_data = {
            "device_id": str(uuid.uuid4()),
            "purchase_token": "pi_test_1234567890"  # PaymentIntentのID（無効）
        }
        
        response = client.post("/unlock/daypass", json=request_data)
        assert response.status_code == 400
        
        data = response.json()
        assert data["error"] == "invalid_purchase_token_format"
        assert "Stripeセッション" in data["message"]
    
    def test_empty_request_body(self):
        """空のリクエストボディテスト"""
        response = client.post("/license/confirm", json={})
        assert response.status_code == 422  # Pydanticバリデーションエラー
        
        response = client.post("/unlock/daypass", json={})
        assert response.status_code == 422  # Pydanticバリデーションエラー
    
    def test_invalid_json(self):
        """不正なJSONテスト"""
        response = client.post(
            "/license/confirm",
            data="invalid json",
            headers={"Content-Type": "application/json"}
        )
        assert response.status_code == 422


if __name__ == "__main__":
    pytest.main([__file__]) 