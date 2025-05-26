#!/usr/bin/env python3
"""
Firestore接続テストスクリプト
"""
import sys
import os

# 現在のディレクトリをPythonパスに追加
sys.path.insert(0, os.path.dirname(__file__))

from config import firestore_config

def test_firestore_connection():
    """Firestore接続をテスト"""
    print("=== Firestore接続テスト開始 ===")
    
    # 初期化テスト
    print("1. Firestore初期化テスト...")
    success = firestore_config.initialize_firestore()
    if success:
        print("   ✓ Firestore初期化成功")
    else:
        print("   ✗ Firestore初期化失敗")
        return False
    
    # 初期化状態確認
    print("2. 初期化状態確認...")
    if firestore_config.is_initialized():
        print("   ✓ Firestore初期化済み")
    else:
        print("   ✗ Firestore未初期化")
        return False
    
    # クライアント取得テスト
    print("3. クライアント取得テスト...")
    client = firestore_config.get_client()
    if client:
        print("   ✓ Firestoreクライアント取得成功")
    else:
        print("   ✗ Firestoreクライアント取得失敗")
        return False
    
    # 接続テスト
    print("4. 接続テスト...")
    result = firestore_config.test_connection()
    if result["status"] == "success":
        print("   ✓ Firestore接続テスト成功")
        print(f"   メッセージ: {result['message']}")
        if "data" in result:
            print(f"   データ: {result['data']}")
    else:
        print("   ✗ Firestore接続テスト失敗")
        print(f"   エラー: {result['message']}")
        return False
    
    print("=== 全てのテストが成功しました ===")
    return True

def test_firestore_connection_pytest():
    """pytest用のテスト関数"""
    assert test_firestore_connection() is True

if __name__ == "__main__":
    success = test_firestore_connection()
    sys.exit(0 if success else 1) 