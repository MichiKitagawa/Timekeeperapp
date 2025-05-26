"""
FastAPI アプリケーションのテスト
"""
import pytest
import httpx
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock
from main import app


@pytest.fixture
def client():
    """テスト用のFastAPIクライアント"""
    with TestClient(app) as test_client:
        yield test_client


class TestHealthEndpoint:
    """ヘルスチェックエンドポイントのテスト"""
    
    def test_health_check_success(self, client):
        """ヘルスチェックが成功することをテスト"""
        with patch('main.firestore_config.is_initialized', return_value=True):
            response = client.get("/health")
            assert response.status_code == 200
            data = response.json()
            assert data["status"] == "ok"
            assert data["firestore_initialized"] is True
    
    def test_health_check_firestore_not_initialized(self, client):
        """Firestoreが初期化されていない場合のヘルスチェック"""
        with patch('main.firestore_config.is_initialized', return_value=False):
            response = client.get("/health")
            assert response.status_code == 200
            data = response.json()
            assert data["status"] == "ok"
            assert data["firestore_initialized"] is False


class TestFirestoreStatusEndpoint:
    """Firestore状態確認エンドポイントのテスト"""
    
    def test_firestore_status_initialized(self, client):
        """Firestoreが初期化されている場合の状態確認"""
        with patch('main.firestore_config.is_initialized', return_value=True), \
             patch('main.firestore_config.environment', 'development'):
            response = client.get("/firestore/status")
            assert response.status_code == 200
            data = response.json()
            assert data["initialized"] is True
            assert data["environment"] == "development"
    
    def test_firestore_status_not_initialized(self, client):
        """Firestoreが初期化されていない場合の状態確認"""
        with patch('main.firestore_config.is_initialized', return_value=False), \
             patch('main.firestore_config.environment', 'development'):
            response = client.get("/firestore/status")
            assert response.status_code == 200
            data = response.json()
            assert data["initialized"] is False
            assert data["environment"] == "development"


class TestFirestoreTestEndpoint:
    """Firestore接続テストエンドポイントのテスト"""
    
    def test_test_firestore_not_initialized(self, client):
        """Firestoreが初期化されていない場合のテスト"""
        with patch('main.firestore_config.is_initialized', return_value=False):
            response = client.get("/test_firestore")
            assert response.status_code == 503
            assert "Firestore not initialized" in response.json()["detail"]
    
    def test_test_firestore_success(self, client):
        """Firestore接続テストが成功する場合のテスト"""
        mock_result = {
            "status": "success",
            "message": "Firestore connection test successful",
            "data": {"test": "data"}
        }
        
        with patch('main.firestore_config.is_initialized', return_value=True), \
             patch('main.firestore_config.test_connection', return_value=mock_result):
            response = client.get("/test_firestore")
            assert response.status_code == 200
            data = response.json()
            assert data["status"] == "success"
            assert "successful" in data["message"]
            assert data["data"] == {"test": "data"}
    
    def test_test_firestore_connection_error(self, client):
        """Firestore接続テストでエラーが発生する場合のテスト"""
        mock_result = {
            "status": "error",
            "message": "Connection failed"
        }
        
        with patch('main.firestore_config.is_initialized', return_value=True), \
             patch('main.firestore_config.test_connection', return_value=mock_result):
            response = client.get("/test_firestore")
            assert response.status_code == 500
            assert "Connection failed" in response.json()["detail"]


class TestStartupEvent:
    """アプリケーション起動イベントのテスト"""
    
    @patch('main.firestore_config.initialize_firestore')
    def test_startup_event_success(self, mock_initialize):
        """起動時のFirestore初期化が成功する場合のテスト"""
        mock_initialize.return_value = True
        
        # ライフサイクルイベントを直接呼び出し
        import asyncio
        from main import lifespan, app
        
        async def test_lifespan():
            async with lifespan(app):
                pass
        
        asyncio.run(test_lifespan())
        mock_initialize.assert_called_once()
    
    @patch('main.firestore_config.initialize_firestore')
    @patch('builtins.print')
    def test_startup_event_failure(self, mock_print, mock_initialize):
        """起動時のFirestore初期化が失敗する場合のテスト"""
        mock_initialize.return_value = False
        
        # ライフサイクルイベントを直接呼び出し
        import asyncio
        from main import lifespan, app
        
        async def test_lifespan():
            async with lifespan(app):
                pass
        
        asyncio.run(test_lifespan())
        mock_initialize.assert_called_once()
        mock_print.assert_called_with("Warning: Firestore initialization failed") 