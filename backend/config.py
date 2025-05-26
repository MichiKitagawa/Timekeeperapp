"""
Timekeeper Backend Configuration
Firestoreクライアントの設定と初期化を管理
"""
import os
from typing import Optional
import firebase_admin
from firebase_admin import credentials, firestore
from google.cloud.firestore import Client
import stripe # Stripeライブラリをインポート


class StripeConfig:
    """Stripe設定クラス"""

    def __init__(self):
        self.api_key: Optional[str] = os.getenv('STRIPE_API_KEY')
        self.webhook_secret: Optional[str] = os.getenv('STRIPE_WEBHOOK_SECRET') # 将来的にWebhookで使用する可能性を考慮
        self._initialized: bool = False
        self.initialize_stripe()

    def initialize_stripe(self) -> bool:
        """Stripe APIキーを設定"""
        if self.api_key:
            stripe.api_key = self.api_key
            self._initialized = True
            print("Stripe initialized successfully")
            return True
        print("Warning: STRIPE_API_KEY not found in environment variables.")
        return False

    def is_initialized(self) -> bool:
        """Stripeが初期化済みかどうかを確認"""
        return self._initialized


class FirestoreConfig:
    """Firestore設定クラス"""
    
    def __init__(self):
        self.service_account_path: str = os.getenv(
            'FIREBASE_SERVICE_ACCOUNT_PATH',
            'timekee-b5863-firebase-adminsdk-fbsvc-9ad9aa5ac6.json'
        )
        self.environment: str = os.getenv('ENVIRONMENT', 'development')
        self._client: Optional[Client] = None
        self._initialized: bool = False
    
    def initialize_firestore(self) -> bool:
        """
        Firestoreクライアントを初期化
        
        Returns:
            bool: 初期化が成功した場合True、失敗した場合False
        """
        if self._initialized:
            return True
            
        try:
            # サービスアカウントファイルのパスを解決
            service_account_file = os.path.join(
                os.path.dirname(__file__), 
                self.service_account_path
            )
            
            if not os.path.exists(service_account_file):
                print(f"Error: Service account file not found at {service_account_file}")
                print(f"Current working directory: {os.getcwd()}")
                print(f"Files in backend directory: {os.listdir(os.path.dirname(__file__))}")
                return False
            
            # Firebase Admin SDKの初期化（既に初期化されている場合はスキップ）
            if not firebase_admin._apps:
                cred = credentials.Certificate(service_account_file)
                firebase_admin.initialize_app(cred)
            
            # Firestoreクライアントの取得
            self._client = firestore.client()
            self._initialized = True
            
            print(f"Firestore initialized successfully in {self.environment} environment")
            return True
            
        except Exception as e:
            print(f"Failed to initialize Firestore: {str(e)}")
            return False
    
    def get_client(self) -> Optional[Client]:
        """
        Firestoreクライアントを取得
        
        Returns:
            Optional[Client]: 初期化済みのFirestoreクライアント、未初期化の場合None
        """
        if not self._initialized:
            if not self.initialize_firestore():
                return None
        return self._client
    
    def is_initialized(self) -> bool:
        """
        Firestoreクライアントが初期化済みかどうかを確認
        
        Returns:
            bool: 初期化済みの場合True
        """
        return self._initialized
    
    def test_connection(self) -> dict:
        """
        Firestore接続をテスト
        
        Returns:
            dict: テスト結果
        """
        client = self.get_client()
        if not client:
            return {
                "status": "error",
                "message": "Firestore client not initialized"
            }
        
        try:
            # テスト用のドキュメントを作成・読み取り
            test_collection = 'test_connection'
            test_doc_id = 'connection_test'
            
            doc_ref = client.collection(test_collection).document(test_doc_id)
            test_data = {
                'timestamp': firestore.SERVER_TIMESTAMP,
                'message': 'Connection test successful',
                'environment': self.environment
            }
            
            # ドキュメントを書き込み
            doc_ref.set(test_data)
            
            # ドキュメントを読み取り
            doc = doc_ref.get()
            if doc.exists:
                # テスト用ドキュメントを削除
                doc_ref.delete()
                return {
                    "status": "success",
                    "message": "Firestore connection test successful",
                    "data": doc.to_dict()
                }
            else:
                return {
                    "status": "error",
                    "message": "Test document not found after write"
                }
                
        except Exception as e:
            return {
                "status": "error",
                "message": f"Firestore connection test failed: {str(e)}"
            }


# グローバルなFirestore設定インスタンス
firestore_config = FirestoreConfig()

# グローバルなStripe設定インスタンス
stripe_config = StripeConfig() 