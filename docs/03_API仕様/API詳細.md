# 📘 API詳細 - Timekeeper（v2仕様）

---

## 共通仕様

- **ベースURL**: `https://api.timekeeper.example.com`
- **認証**: JSONボディ内に必ず `device_id` を含める（匿名識別）
- **コンテンツ型**: `application/json`
- **HTTPステータス**:

|コード|用途|
|---|---|
|200|正常処理|
|400|リクエスト形式／バリデーションエラー|
|404|`device_id` 未登録|
|500|サーバー内部エラー|

- **エラー形式**:

```json
{
  "error": "<エラーコード>",
  "message": "<エラーメッセージ>"
}

```

---

### POST `/license/confirm`

### 🔹 概要

初回ライセンス購入完了の通知と状態登録／更新

- **呼び出し元**: P01（ライセンス購入画面）

### 🔸 リクエスト

```json
{
  "device_id": "abc123-uuid",
  "purchase_token": "cs_test_XXXXXXXXXXXXX"
}

```

### 🔸 バリデーション

|フィールド|型|必須|制約|
|---|---|---|---|
|`device_id`|string|✓|UUID形式|
|`purchase_token`|string|✓|Stripe セッション ID|

### 🔸 成功レスポンス 200

```json
{ "status": "ok" }

```

### 🔸 エラーレスポンス例

|ステータス|レスポンス|
|---|---|
|400|`{ "error": "invalid_purchase_token", "message": "purchase_token が不正です" }`|
|404|`{ "error": "device_not_found", "message": "device_id が未登録です" }`|
|500|`{ "error": "stripe_verification_failed", "message": "決済検証に失敗しました" }`|

---

### POST `/unlock/daypass`

### 🔹 概要

デイパス（1日アンロック）課金完了の通知と状態更新

- **呼び出し元**: P05（デイパス決済画面）

### 🔸 リクエスト

```json
{
  "device_id": "abc123-uuid",
  "purchase_token": "cs_test_YYYYYYYYYYYYY"
}

```

### 🔸 バリデーション

|フィールド|型|必須|制約|
|---|---|---|---|
|`device_id`|string|✓|UUID形式|
|`purchase_token`|string|✓|Stripe セッション ID|

### 🔸 成功レスポンス 200

```json
{
  "status": "ok",
  "unlock_count": 4,
  "last_unlock_date": "2025-05-25"
}

```

### 🔸 エラーレスポンス例

| ステータス | レスポンス                                                                      |
| ----- | -------------------------------------------------------------------------- |
| 400   | `{ "error": "invalid_purchase_token", "message": "purchase_token が不正です" }` |
| 404   | `{ "error": "device_not_found", "message": "device_id が未登録です" }`           |
| 500   | `{ "error": "payment_verification_failed", "message": "決済検証に失敗しました" }`     |