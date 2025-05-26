"""
Firestore クライアント初期化・接続設定のテスト
"""
import pytest
import os
from unittest.mock import patch, MagicMock
from config import FirestoreConfig, firestore_config


class TestFirestoreConfig:
    """FirestoreConfig クラスのテスト"""
    
    def test_init_default_values(self):
        """初期化時のデフォルト値をテスト"""
        config = FirestoreConfig()
        assert config.service_account_path == 'timekee-b5863-firebase-adminsdk-fbsvc-9ad9aa5ac6.json'
        assert config.environment == 'development'
        assert config._client is None
        assert config._initialized is False
    
    @patch.dict(os.environ, {
        'FIREBASE_SERVICE_ACCOUNT_PATH': 'custom_path.json',
        'ENVIRONMENT': 'production'
    })
    def test_init_with_env_vars(self):
        """環境変数が設定されている場合の初期化をテスト"""
        config = FirestoreConfig()
        assert config.service_account_path == 'custom_path.json'
        assert config.environment == 'production'
    
    def test_is_initialized_false_initially(self):
        """初期状態では初期化されていないことをテスト"""
        config = FirestoreConfig()
        assert config.is_initialized() is False
    
    @patch('config.os.path.exists')
    def test_initialize_firestore_file_not_found(self, mock_exists):
        """サービスアカウントファイルが見つからない場合のテスト"""
        mock_exists.return_value = False
        config = FirestoreConfig()
        
        result = config.initialize_firestore()
        assert result is False
        assert config.is_initialized() is False
    
    @patch('config.firebase_admin._apps', [])
    @patch('config.os.path.exists')
    @patch('config.credentials.Certificate')
    @patch('config.firebase_admin.initialize_app')
    @patch('config.firestore.client')
    def test_initialize_firestore_success(self, mock_client, mock_init_app, 
                                        mock_certificate, mock_exists):
        """Firestore初期化が成功する場合のテスト"""
        mock_exists.return_value = True
        mock_firestore_client = MagicMock()
        mock_client.return_value = mock_firestore_client
        
        config = FirestoreConfig()
        result = config.initialize_firestore()
        
        assert result is True
        assert config.is_initialized() is True
        assert config.get_client() == mock_firestore_client
        mock_certificate.assert_called_once()
        mock_init_app.assert_called_once()
        mock_client.assert_called_once()
    
    @patch('config.firebase_admin._apps', ['existing_app'])
    @patch('config.os.path.exists')
    @patch('config.firebase_admin.initialize_app')
    @patch('config.firestore.client')
    def test_initialize_firestore_already_initialized_firebase(self, mock_client, 
                                                             mock_init_app, mock_exists):
        """Firebase Admin SDKが既に初期化されている場合のテスト"""
        mock_exists.return_value = True
        mock_firestore_client = MagicMock()
        mock_client.return_value = mock_firestore_client
        
        config = FirestoreConfig()
        result = config.initialize_firestore()
        
        assert result is True
        # Firebase Admin SDKの初期化は呼ばれない
        mock_init_app.assert_not_called()
        mock_client.assert_called_once()
    
    def test_initialize_firestore_twice(self):
        """初期化を2回呼んでも問題ないことをテスト"""
        config = FirestoreConfig()
        config._initialized = True
        
        result = config.initialize_firestore()
        assert result is True
    
    @patch('config.firebase_admin._apps', [])
    @patch('config.os.path.exists')
    @patch('config.credentials.Certificate')
    def test_initialize_firestore_exception(self, mock_certificate, mock_exists):
        """初期化中に例外が発生した場合のテスト"""
        mock_exists.return_value = True
        mock_certificate.side_effect = Exception("Test exception")
        
        config = FirestoreConfig()
        
        # 強制的に初期化されていない状態にする
        config._initialized = False
        config._client = None
        
        result = config.initialize_firestore()
        
        assert result is False
        assert config.is_initialized() is False
    
    def test_get_client_not_initialized(self):
        """初期化されていない状態でクライアントを取得しようとした場合のテスト"""
        config = FirestoreConfig()
        
        with patch.object(config, 'initialize_firestore', return_value=False):
            client = config.get_client()
            assert client is None
    
    @patch('config.firestore.SERVER_TIMESTAMP', 'mock_timestamp')
    def test_test_connection_not_initialized(self):
        """初期化されていない状態で接続テストを実行した場合のテスト"""
        config = FirestoreConfig()
        
        # 強制的に初期化されていない状態にする
        with patch.object(config, 'get_client', return_value=None):
            result = config.test_connection()
            assert result["status"] == "error"
            assert "not initialized" in result["message"]
    
    @patch('config.firestore.SERVER_TIMESTAMP', 'mock_timestamp')
    def test_test_connection_success(self):
        """接続テストが成功する場合のテスト"""
        config = FirestoreConfig()
        
        # モッククライアントを設定
        mock_client = MagicMock()
        mock_doc_ref = MagicMock()
        mock_doc = MagicMock()
        mock_doc.exists = True
        mock_doc.to_dict.return_value = {"test": "data"}
        
        mock_client.collection.return_value.document.return_value = mock_doc_ref
        mock_doc_ref.get.return_value = mock_doc
        
        config._client = mock_client
        config._initialized = True
        
        result = config.test_connection()
        
        assert result["status"] == "success"
        assert "successful" in result["message"]
        assert result["data"] == {"test": "data"}
        
        # ドキュメントの作成、読み取り、削除が呼ばれることを確認
        mock_doc_ref.set.assert_called_once()
        mock_doc_ref.get.assert_called_once()
        mock_doc_ref.delete.assert_called_once()
    
    @patch('config.firestore.SERVER_TIMESTAMP', 'mock_timestamp')
    def test_test_connection_document_not_found(self):
        """接続テストでドキュメントが見つからない場合のテスト"""
        config = FirestoreConfig()
        
        # モッククライアントを設定
        mock_client = MagicMock()
        mock_doc_ref = MagicMock()
        mock_doc = MagicMock()
        mock_doc.exists = False
        
        mock_client.collection.return_value.document.return_value = mock_doc_ref
        mock_doc_ref.get.return_value = mock_doc
        
        config._client = mock_client
        config._initialized = True
        
        result = config.test_connection()
        
        assert result["status"] == "error"
        assert "not found after write" in result["message"]
    
    @patch('config.firestore.SERVER_TIMESTAMP', 'mock_timestamp')
    def test_test_connection_exception(self):
        """接続テスト中に例外が発生した場合のテスト"""
        config = FirestoreConfig()
        
        # モッククライアントを設定
        mock_client = MagicMock()
        mock_client.collection.side_effect = Exception("Connection error")
        
        config._client = mock_client
        config._initialized = True
        
        result = config.test_connection()
        
        assert result["status"] == "error"
        assert "Connection error" in result["message"]


class TestGlobalFirestoreConfig:
    """グローバルなfirestore_configインスタンスのテスト"""
    
    def test_global_instance_exists(self):
        """グローバルインスタンスが存在することをテスト"""
        assert firestore_config is not None
        assert isinstance(firestore_config, FirestoreConfig)


@pytest.mark.asyncio
async def test_firestore_integration():
    """
    実際のFirestore接続の統合テスト
    注意: このテストは実際のFirestoreサービスアカウントファイルが必要
    """
    # サービスアカウントファイルが存在する場合のみテストを実行
    service_account_file = os.path.join(
        os.path.dirname(__file__), 
        'timekee-b5863-firebase-adminsdk-fbsvc-9ad9aa5ac6.json'
    )
    
    if not os.path.exists(service_account_file):
        pytest.skip("Service account file not found, skipping integration test")
    
    config = FirestoreConfig()
    
    # 初期化テスト
    success = config.initialize_firestore()
    assert success is True
    assert config.is_initialized() is True
    
    # クライアント取得テスト
    client = config.get_client()
    assert client is not None
    
    # 接続テスト
    result = config.test_connection()
    assert result["status"] == "success"
    assert "successful" in result["message"] 