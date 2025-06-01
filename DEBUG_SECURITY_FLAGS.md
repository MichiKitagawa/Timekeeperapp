# 🔧 Timekeeper セキュリティ機能デバッグ設定

## 概要
Timekeeperアプリには日付・時間変更検知によるセキュリティ機能が実装されていますが、開発・テスト時には一時的に無効化することができます。

## デバッグフラグ一覧

### 1. SecurityManager
**ファイル**: `app/src/main/java/com/example/timekeeper/util/SecurityManager.kt`
**フラグ**: `SECURITY_CHECKS_DISABLED_FOR_DEBUG`
**現在の状態**: `false` (✅本番モード：セキュリティ有効)
**機能**: アプリ初期化処理を無効化

### 2. GapDetector  
**ファイル**: `app/src/main/java/com/example/timekeeper/util/GapDetector.kt`
**フラグ**: `GAP_DETECTION_DISABLED_FOR_DEBUG`
**現在の状態**: `false` (✅本番モード：ギャップ検知有効)
**機能**: ハートビートギャップ検知を無効化

### 3. HeartbeatService
**ファイル**: `app/src/main/java/com/example/timekeeper/service/HeartbeatService.kt`
**フラグ**: `HEARTBEAT_SERVICE_DISABLED_FOR_DEBUG`
**現在の状態**: `false` (✅本番モード：セキュリティチェック有効)
**機能**: セキュリティチェックを無効化（ハートビート記録は継続）

### 4. MainActivity
**ファイル**: `app/src/main/java/com/example/timekeeper/MainActivity.kt`
**フラグ**: `MAIN_ACTIVITY_SECURITY_DISABLED_FOR_DEBUG`
**現在の状態**: `false` (✅本番モード：起動時セキュリティ有効)
**機能**: 起動時のセキュリティチェックを無効化

## 🎉 本番リリース準備完了

**現在の状態**: 全てのセキュリティ機能が有効化されており、本番リリースの準備が整いました。

### セキュリティ機能の動作
- ✅ **時間変更検知**: システム時刻が変更されると自動的にアプリデータがリセット
- ✅ **ハートビート監視**: バックグラウンドサービスの異常停止を検知
- ✅ **ギャップ検知**: アプリ使用の不自然な間隔を検知してセキュリティ違反を判定
- ✅ **起動時チェック**: アプリ起動時にセキュリティ状態を検証

## 使用方法

### デバッグモードを有効にする場合（現在の状態）
```kotlin
private const val SECURITY_CHECKS_DISABLED_FOR_DEBUG = true
```

### 本番モード（セキュリティ機能を有効）にする場合
```kotlin
private const val SECURITY_CHECKS_DISABLED_FOR_DEBUG = false
```

## ⚠️ 重要な注意事項

1. **本番リリース前には必ず全てのフラグを`false`に戻してください**
2. デバッグモード中はセキュリティ機能が無効になるため、制限回避が可能になります
3. ログに警告メッセージが表示されます：
   ```
   ⚠️ SECURITY CHECKS ARE DISABLED FOR DEBUG! This should only be used in development.
   ```

## 確認方法

アプリを起動してLogcatで以下のメッセージを確認：
```
🔧 DEBUG MODE: Security checks skipped
🔧 GAP DETECTION IS DISABLED FOR DEBUG!
🔧 HEARTBEAT SERVICE IS DISABLED FOR DEBUG!
```

## テスト用途での推奨設定

### 時間変更テストを行う場合
- 全てのフラグを`true`に設定
- エミュレータで時間を変更しても初期化されない

### セキュリティ機能のテストを行う場合  
- 全てのフラグを`false`に設定
- 時間変更により期待通りに初期化されるかを確認

## 元に戻す方法

全てのファイルで該当する行を以下のように変更：
```kotlin
private const val [フラグ名] = false
``` 