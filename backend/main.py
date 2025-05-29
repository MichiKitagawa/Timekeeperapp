"""
Timekeeper Backend API
FastAPIを使用したバックエンドサーバー
"""
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Request as FastAPIRequest
from config import firestore_config, stripe_config
from middleware import ErrorHandlingMiddleware
from validation import RequestValidator, ValidationError
from models import (
    LicenseConfirmRequest, LicenseConfirmResponse,
    UnlockDaypassRequest, UnlockDaypassResponse,
    ErrorResponse,
    CreateCheckoutSessionRequest, CreateCheckoutSessionResponse
)
import stripe
from datetime import datetime, timezone

# 定数を定義
# YOUR_APP_DOMAIN = "https://example.com" # HTTP/HTTPSのダミードメインに変更
YOUR_HOSTED_DOMAIN = "https://timekeeper-redirect.example.com" # Stripe決済後の中間ページをホストするドメイン (実際にデプロイするドメインに変更してください)
LICENSE_PRICE_ID = "price_1RSoQKCplaJfZ2mW9cv8EVSw" # Stripeダッシュボードで設定したライセンス商品の価格ID

# ngrokのURL (開発時に一時的に使用) - 実行の都度変更が必要な場合があります
YOUR_NGROK_URL = "https://9068-240b-c020-490-84ae-55b2-1437-165b-1acd.ngrok-free.app"


@asynccontextmanager
async def lifespan(app: FastAPI):
    """アプリケーションのライフサイクル管理"""
    # 起動時の処理
    success = firestore_config.initialize_firestore()
    if not success:
        print("Warning: Firestore initialization failed")
    
    yield
    
    # 終了時の処理（必要に応じて追加）


app = FastAPI(
    title="Timekeeper API",
    description="TimekeeperアプリのバックエンドAPI",
    version="1.0.0",
    lifespan=lifespan
)

# 共通エラーハンドリングミドルウェアを追加
app.add_middleware(ErrorHandlingMiddleware)

@app.get("/health")
async def health_check():
    """ヘルスチェックエンドポイント"""
    return {
        "status": "ok",
        "firestore_initialized": firestore_config.is_initialized()
    }

@app.get("/test_firestore")
async def test_firestore():
    """Firestore接続テストエンドポイント"""
    if not firestore_config.is_initialized():
        raise HTTPException(
            status_code=503,
            detail="Firestore not initialized"
        )
    
    result = firestore_config.test_connection()
    
    if result["status"] == "error":
        raise HTTPException(
            status_code=500,
            detail=result["message"]
        )
    
    return result

@app.get("/firestore/status")
async def firestore_status():
    """Firestore初期化状態を確認"""
    return {
        "initialized": firestore_config.is_initialized(),
        "environment": firestore_config.environment
    }

@app.post("/create-checkout-session", response_model=CreateCheckoutSessionResponse, responses={400: {"model": ErrorResponse}, 500: {"model": ErrorResponse}})
async def create_checkout_session(request: CreateCheckoutSessionRequest):
    """
    Stripe Checkoutセッションを作成し、決済ページURLを返却するAPI
    """
    if not stripe_config.is_initialized():
        raise HTTPException(
            status_code=503,
            detail={"error_code": "stripe_not_initialized", "message": "Stripe is not initialized. Check API key."}
        )

    line_items = []
    if request.product_type == "license":
        if not LICENSE_PRICE_ID:
             raise HTTPException(status_code=500, detail={"error_code": "license_price_id_not_configured", "message": "License Price ID is not configured."})
        line_items.append({"price": LICENSE_PRICE_ID, "quantity": 1})
    elif request.product_type == "daypass":
        unlock_count = request.unlock_count if request.unlock_count is not None and request.unlock_count >= 0 else 0
        price_amount = int(200 * (1.2 ** unlock_count))
        product_name = f"デイパス ({unlock_count + 1}回目)"

        line_items.append({
            "price_data": {
                "currency": "jpy",
                "product_data": {
                    "name": product_name,
                },
                "unit_amount": price_amount,
            },
            "quantity": 1,
        })
    else:
        raise HTTPException(status_code=400, detail={"error_code": "invalid_product_type", "message": "Invalid product type provided."})

    if not line_items:
         raise HTTPException(status_code=500, detail={"error_code": "line_items_empty", "message": "No items to purchase."})

    try:
        # success_url と cancel_url を ngrok 経由の中間ページのURLに変更
        # device_id もクエリパラメータに追加
        success_url_intermediate = f"{YOUR_NGROK_URL}/checkout-success.html?session_id={{CHECKOUT_SESSION_ID}}&deviceId={request.device_id}&product_type={request.product_type}&status=success"
        cancel_url_intermediate = f"{YOUR_NGROK_URL}/checkout-cancel.html?deviceId={request.device_id}&product_type={request.product_type}&status=cancel"

        checkout_session = stripe.checkout.Session.create(
            payment_method_types=['card'],
            line_items=line_items,
            mode='payment',
            success_url=success_url_intermediate, # 修正
            cancel_url=cancel_url_intermediate,   # 修正
            metadata={
                'device_id': request.device_id,
                'product_type': request.product_type
            }
        )
        return CreateCheckoutSessionResponse(checkout_url=checkout_session.url)
    except stripe.error.StripeError as e:
        error_message = str(e)
        if hasattr(e, 'user_message') and e.user_message: # Check if user_message exists
            error_message = e.user_message
        print(f"Stripe API error: {str(e)}")
        raise HTTPException(
            status_code=e.http_status or 500,
            detail={"error_code": "stripe_api_error", "message": f"Stripe API error: {error_message}"}
        )
    except Exception as e:
        print(f"Unexpected error creating checkout session: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail={"error_code": "checkout_session_creation_failed", "message": f"An unexpected error occurred while creating the checkout session: {str(e)}"}
        )

@app.post("/license/confirm", response_model=LicenseConfirmResponse, responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}, 500: {"model": ErrorResponse}})
async def license_confirm(request: LicenseConfirmRequest):
    """
    ライセンス購入確認API
    
    Args:
        request: ライセンス確認リクエスト
        
    Returns:
        LicenseConfirmResponse: 確認結果
        
    Raises:
        ValidationError: バリデーションエラー
        HTTPException: その他のエラー
    """
    # リクエストバリデーション
    try:
        validated_data = RequestValidator.validate_license_confirm_request(request.model_dump())
        device_id = validated_data['device_id']
        purchase_token = validated_data['purchase_token']
    except ValidationError as e:
        # validation_error_handler を使う代わりに、ミドルウェアに処理を任せるか、
        # ここで直接HTTPExceptionをraiseする
        raise HTTPException(
            status_code=e.status_code,
            detail={"error_code": e.error_code, "message": e.message}
        )

    if not stripe_config.is_initialized():
        raise HTTPException(
            status_code=503,
            detail={"error_code": "stripe_not_initialized", "message": "Stripe is not initialized. Check API key."}
        )

    db = firestore_config.get_client()
    if not db:
        raise HTTPException(
            status_code=503,
            detail={"error_code": "firestore_not_initialized", "message": "Firestore is not initialized."}
        )

    # Stripe API と連携し purchase_token を検証
    try:
        # purchase_tokenはStripeのCheckout Session IDであることを想定
        session = stripe.checkout.Session.retrieve(purchase_token)
        if session.payment_status != 'paid':
            raise HTTPException(
                status_code=400,
                detail={"error_code": "payment_not_completed", "message": "Payment not completed or failed."}
            )
        # ここで顧客情報や支払い金額を検証することも可能 (session.amount_total, session.currencyなど)
        # 例: session.metadata['device_id'] と device_id が一致するかなど

    except stripe.error.StripeError as e:
        # Stripe APIエラー
        raise HTTPException(
            status_code=e.http_status or 500,
            detail={"error_code": "stripe_api_error", "message": str(e)}
        )
    except Exception as e: # その他の予期せぬエラー
        raise HTTPException(
            status_code=500,
            detail={"error_code": "stripe_validation_failed", "message": f"Stripe purchase_token validation failed: {str(e)}"}
        )

    # Firestore の devices コレクションに device_id とライセンス購入情報を記録/更新
    try:
        device_ref = db.collection('devices').document(device_id)
        device_doc = device_ref.get()

        # TC3: 未登録 device_id でリクエストがあった場合にエラーレスポンスが返却されること
        # Firestoreにdevice_idが存在しない場合、新規作成するかエラーとするかは仕様による。
        # ここでは、P01のフローではdevice_idが既に存在している前提とし、
        # もし存在しなければエラーとする。あるいは、ここで新規作成しても良い。
        # 今回は、技術仕様書に「認証はUUID (device_id) による匿名識別のみ」とあり、
        # device_idはクライアントが生成するため、ここでは存在チェックは必須ではないかもしれない。
        # ただし、不正なdevice_idによる書き込みを防ぐ意図があればチェックは有効。
        # 今回は「未登録device_id」のテストケースがあるので、ドキュメントが存在しない場合は作成する方針で進める。

        doc_data = {
            'license_purchased': True,
            'license_purchase_date': datetime.now(timezone.utc), # ISO 8601形式で保存
            # purchase_tokens 配列に今回のトークンを追加することも検討できる
            # 'purchase_tokens': firestore.ArrayUnion([purchase_token]) # 既存の配列に追加する場合
        }
        if device_doc.exists:
            # 既に購入済みの場合の処理も考慮 (例: エラーとするか、上書きするか)
            # ここでは上書きする
            device_ref.update(doc_data)
            print(f"License information updated for device_id: {device_id}")
        else:
            # ドキュメントが存在しない場合は新規作成
            device_ref.set(doc_data)
            print(f"License information created for device_id: {device_id}")


    except Exception as e:
        # Firestoreエラー
        # P06のケース: 課金は成功したが、Firestore更新に失敗した場合のハンドリング
        # この場合、ユーザーには課金成功・アプリ側処理失敗を通知する必要がある
        # ここでは汎用的な500エラーを返し、クライアント側でP06に誘導する想定
        print(f"Error updating Firestore for device_id {device_id}: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail={"error_code": "firestore_update_failed", "message": f"Failed to update license information in Firestore: {str(e)}"}
        )

    # 成功レスポンス返却
    return LicenseConfirmResponse(status="ok")


@app.post("/unlock/daypass", response_model=UnlockDaypassResponse, responses={400: {"model": ErrorResponse}, 404: {"model": ErrorResponse}, 500: {"model": ErrorResponse}})
async def unlock_daypass(request: UnlockDaypassRequest):
    """
    デイパスアンロックAPI
    
    Args:
        request: デイパスアンロックリクエスト
        
    Returns:
        UnlockDaypassResponse: アンロック結果
        
    Raises:
        ValidationError: バリデーションエラー
        HTTPException: その他のエラー
    """
    # リクエストバリデーション
    try:
        validated_data = RequestValidator.validate_unlock_daypass_request(request.model_dump())
        device_id = validated_data['device_id']
        purchase_token = validated_data['purchase_token']
    except ValidationError as e:
        raise HTTPException(
            status_code=e.status_code,
            detail={"error_code": e.error_code, "message": e.message}
        )

    if not stripe_config.is_initialized():
        raise HTTPException(
            status_code=503,
            detail={"error_code": "stripe_not_initialized", "message": "Stripe is not initialized. Check API key."}
        )

    db = firestore_config.get_client()
    if not db:
        raise HTTPException(
            status_code=503,
            detail={"error_code": "firestore_not_initialized", "message": "Firestore is not initialized."}
        )

    # Stripe API と連携し purchase_token を検証
    try:
        session = stripe.checkout.Session.retrieve(purchase_token)
        if session.payment_status != 'paid':
            raise HTTPException(
                status_code=400,
                detail={"error_code": "payment_not_completed", "message": "Payment not completed or failed."}
            )
        # ここで顧客情報や支払い金額を検証することも可能
    except stripe.error.StripeError as e:
        raise HTTPException(
            status_code=e.http_status or 500,
            detail={"error_code": "stripe_api_error", "message": str(e)}
        )
    except Exception as e: # その他の予期せぬエラー
        raise HTTPException(
            status_code=500,
            detail={"error_code": "payment_verification_failed", "message": f"Stripe purchase_token validation failed: {str(e)}"}
        )

    # Firestore の devices コレクションで unlock_count をインクリメント、last_unlock_date を更新
    try:
        device_ref = db.collection('devices').document(device_id)
        device_doc = device_ref.get()

        if not device_doc.exists:
            # TC6: 未登録 device_id でリクエストがあった場合にエラーレスポンスが返却されること
            raise HTTPException(
                status_code=404, # Not Found
                detail={"error_code": "device_not_found", "message": "device_id が未登録です"}
            )

        current_unlock_count = device_doc.to_dict().get('unlock_count', 0)
        new_unlock_count = current_unlock_count + 1
        today_str = datetime.now(timezone.utc).strftime("%Y-%m-%d")

        update_data = {
            'unlock_count': new_unlock_count,
            'last_unlock_date': today_str # FirestoreのDate型ではなく文字列で保存（仕様書と合わせる）
            # 'purchase_tokens': firestore.ArrayUnion([purchase_token]) # 既存の配列に追加する場合
        }
        device_ref.update(update_data)
        print(f"Daypass unlock information updated for device_id: {device_id}. New unlock_count: {new_unlock_count}")

    except HTTPException as e: # 上でraiseされたHTTPExceptionをそのまま再throw
        raise e
    except Exception as e:
        # Firestoreエラー (TC9のケースに該当しうる)
        # P06のケース: 課金は成功したが、Firestore更新に失敗した場合のハンドリング
        print(f"Error updating Firestore for daypass, device_id {device_id}: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail={"error_code": "firestore_update_failed", "message": f"Failed to update daypass information in Firestore: {str(e)}"}
        )

    # 成功レスポンス返却 (TC4)
    return UnlockDaypassResponse(
        status="ok",
        unlock_count=new_unlock_count,
        last_unlock_date=today_str
    )


@app.post("/stripe-webhook", include_in_schema=False) # APIドキュメントには表示しない
async def stripe_webhook(request: FastAPIRequest):
    """
    StripeからのWebhookイベントを受信し処理するAPI
    checkout.session.completed イベントを主に処理する
    """
    if not stripe_config.webhook_secret:
        print("Warning: STRIPE_WEBHOOK_SECRET is not set. Webhook validation will be skipped (unsafe for production).")

    payload_body = await request.body()
    sig_header = request.headers.get('stripe-signature')

    event = None # イベントオブジェクトを初期化
    try:
        if stripe_config.webhook_secret and sig_header:
            event = stripe.Webhook.construct_event(
                payload_body, sig_header, stripe_config.webhook_secret
            )
        elif sig_header: # ヘッダーはあるがシークレットがない場合（設定ミスなど）
            print("Error: Stripe webhook secret is not configured, but signature header was received. Signature validation failed.")
            raise HTTPException(status_code=500, detail="Webhook secret not configured for signature validation.")
        else: # ローカルテスト等でシグネチャヘッダーもシークレットもない場合
            event = stripe.Event.construct_from(
                stripe.util.json.loads(payload_body.decode('utf-8')), stripe_config.api_key
            )
            print("Warning: Webhook signature validation skipped (no secret or signature header).")

    except ValueError as e:
        print(f"Webhook ValueError (Invalid payload): {str(e)}")
        raise HTTPException(status_code=400, detail="Invalid payload")
    except stripe.error.SignatureVerificationError as e:
        print(f"Webhook SignatureVerificationError (Invalid signature): {str(e)}")
        raise HTTPException(status_code=400, detail="Invalid signature")
    except HTTPException as e: # 内部でraiseしたHTTPExceptionをキャッチして再throw
        raise e
    except Exception as e:
        print(f"Webhook construction/validation error: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Webhook event construction/validation error: {str(e)}")

    if not event:
        print("Error: Webhook event object is None after construction attempts.")
        raise HTTPException(status_code=500, detail="Failed to construct webhook event object.")

    if event.type == 'checkout.session.completed':
        session = event.data.object
        print(f"Received checkout.session.completed event for session: {session.id}")

        metadata = session.get('metadata', {})
        device_id = metadata.get('device_id')
        product_type = metadata.get('product_type')

        if not device_id or not product_type:
            print(f"Error: Missing device_id or product_type in webhook metadata for session {session.id}")
            return {"status": "error", "message": "Missing metadata, event not processed further."}

        db = firestore_config.get_client()
        if not db:
            print(f"Error: Firestore not initialized. Cannot process webhook for session {session.id}")
            return {"status": "error", "message": "Firestore not initialized, event not processed further."}

        try:
            if product_type == "license":
                device_ref = db.collection('devices').document(device_id)
                doc_data = {
                    'license_purchased': True,
                    'license_purchase_date': datetime.now(timezone.utc),
                    'last_successful_payment_intent': session.get('payment_intent')
                }
                current_doc = device_ref.get()
                if current_doc.exists:
                    device_ref.update(doc_data)
                    print(f"Webhook: License updated for device_id: {device_id}")
                else:
                    device_ref.set(doc_data)
                    print(f"Webhook: License created for device_id: {device_id}")

            elif product_type == "daypass":
                device_ref = db.collection('devices').document(device_id)
                device_doc = device_ref.get()
                if not device_doc.exists:
                    print(f"Webhook Error: Device_id {device_id} not found for daypass purchase (session: {session.id})")
                    return {"status": "error", "message": "Device not found for daypass, event not processed further."}
                
                current_data = device_doc.to_dict()
                current_unlock_count = current_data.get('unlock_count', 0) if current_data else 0
                new_unlock_count = current_unlock_count + 1
                today_str = datetime.now(timezone.utc).strftime("%Y-%m-%d")
                update_data = {
                    'unlock_count': new_unlock_count,
                    'last_unlock_date': today_str,
                    'last_successful_payment_intent': session.get('payment_intent')
                }
                device_ref.update(update_data)
                print(f"Webhook: Daypass updated for device_id: {device_id}. New unlock_count: {new_unlock_count}")
            else:
                print(f"Warning: Unknown product_type '{product_type}' in webhook for session {session.id}")
        
        except Exception as e:
            print(f"Error processing webhook event (Firestore update failed) for session {session.id}: {str(e)}")
            return {"status": "error", "message": "Firestore update failed during webhook processing"}

    else:
        print(f"Received unhandled event type: {event.type}")

    return {"status": "received"}


# アプリケーションの実行（開発用）
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000) 