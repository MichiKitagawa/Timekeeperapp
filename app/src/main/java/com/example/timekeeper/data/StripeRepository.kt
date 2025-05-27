package com.example.timekeeper.data

import android.util.Log
// data.api パッケージからインポートするように変更
import com.example.timekeeper.data.api.CreateCheckoutSessionRequest
import com.example.timekeeper.data.api.CreateCheckoutSessionResponse
import com.example.timekeeper.data.api.LicenseConfirmRequest
import com.example.timekeeper.data.api.UnlockDaypassRequest
import com.example.timekeeper.data.api.TimekeeperApiService // YourApiService から TimekeeperApiService に変更
// UnlockDaypassResponse は confirmPayment内で直接使われていないが、apiServiceのシグネチャとしては存在するので念のため
// import com.example.timekeeper.data.api.UnlockDaypassResponse 
// LicenseConfirmResponse も同様
// import com.example.timekeeper.data.api.LicenseConfirmResponse 

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// ファイル冒頭にあったデータクラス定義は削除

class StripeRepository(
    private val apiService: TimekeeperApiService,
    private val purchaseStateManager: PurchaseStateManager
) { // YourApiService から TimekeeperApiService に変更

    companion object {
        private const val TAG = "StripeRepository"
    }

    /**
     * バックエンドにStripe Checkoutセッションの作成をリクエストし、決済ページのURLを取得します。
     */
    suspend fun createCheckoutSession(deviceId: String, productType: String, unlockCount: Int?): String? {
        return withContext(Dispatchers.IO) {
            try {
                // テスト用のモックレスポンス（ネットワーク接続問題を回避）
                Log.i(TAG, "Mock: Creating checkout session for device=$deviceId, product=$productType, count=$unlockCount")
                
                // 実際のStripe Checkout URLの代わりにテスト用URLを返す
                val mockCheckoutUrl = "https://checkout.stripe.com/c/pay/test_session_mock_${System.currentTimeMillis()}"
                Log.i(TAG, "Mock checkout URL generated: $mockCheckoutUrl")
                
                return@withContext mockCheckoutUrl
                
                /* 実際のAPI呼び出し（ネットワーク問題解決後に復元）
                val requestBody = CreateCheckoutSessionRequest(
                    device_id = deviceId,
                    product_type = productType,
                    unlock_count = unlockCount
                )
                val response = apiService.createCheckoutSession(requestBody)
                if (response.isSuccessful) {
                    response.body()?.checkout_url
                } else {
                    Log.e(TAG, "Failed to create checkout session: ${response.code()} - ${response.errorBody()?.string()}")
                    null
                }
                */
            } catch (e: Exception) {
                Log.e(TAG, "Exception in createCheckoutSession", e)
                null
            }
        }
    }

    /**
     * バックエンドに決済の最終確認をリクエストします。
     */
    suspend fun confirmPayment(deviceId: String, purchaseToken: String, productType: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // テスト用のモックレスポンス（ネットワーク接続問題を回避）
                Log.i(TAG, "Mock: Confirming payment for device=$deviceId, token=$purchaseToken, product=$productType")
                
                // アプリ内の購入状態を更新
                if (productType == "license") {
                    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    purchaseStateManager.setLicensePurchased(true, currentDate)
                    Log.i(TAG, "Mock: License purchase state updated locally")
                } else if (productType == "daypass") {
                    // モックのデイパス情報
                    purchaseStateManager.updateDaypassUnlockCount(5, "2024-05-28")
                    Log.i(TAG, "Mock: Daypass unlock state updated locally: count=5, date=2024-05-28")
                }
                
                Log.i(TAG, "Mock: Payment confirmed successfully for $productType")
                return@withContext true
                
                /* 実際のAPI呼び出し（ネットワーク問題解決後に復元）
                val response = if (productType == "license") {
                    apiService.confirmLicense(LicenseConfirmRequest(device_id = deviceId, purchase_token = purchaseToken))
                } else {
                    apiService.unlockDaypass(UnlockDaypassRequest(device_id = deviceId, purchase_token = purchaseToken))
                }

                if (response.isSuccessful) {
                    Log.i(TAG, "Payment confirmed successfully for $productType")
                    
                    // アプリ内の購入状態を更新
                    if (productType == "license") {
                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        purchaseStateManager.setLicensePurchased(true, currentDate)
                        Log.i(TAG, "License purchase state updated locally")
                    } else if (productType == "daypass") {
                        // デイパスの場合、レスポンスからunlock_countとlast_unlock_dateを取得
                        val responseBody = response.body()
                        if (responseBody is com.example.timekeeper.data.api.UnlockDaypassResponse) {
                            purchaseStateManager.updateDaypassUnlockCount(
                                responseBody.unlock_count,
                                responseBody.last_unlock_date
                            )
                            Log.i(TAG, "Daypass unlock state updated locally: count=${responseBody.unlock_count}, date=${responseBody.last_unlock_date}")
                        }
                    }
                    
                    true
                } else {
                    Log.e(TAG, "Failed to confirm payment for $productType: ${response.code()} - ${response.errorBody()?.string()}")
                    false
                }
                */
            } catch (e: Exception) {
                Log.e(TAG, "Exception in confirmPayment for $productType", e)
                false
            }
        }
    }
}

/*
// --- MainActivity.kt または関連するActivity/Fragmentでの処理コンセプト ---

// 1. ディープリンクの処理 (onCreate または onNewIntent)
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    handleDeepLink(intent)
}

private fun handleDeepLink(intent: Intent?) {
    intent?.data?.let { uri ->
        if (uri.scheme == "app" && uri.host == "com.example.timekeeper") {
            if (uri.pathSegments.contains("checkout-success")) {
                val sessionId = uri.getQueryParameter("session_id")
                if (sessionId != null) {
                    Log.i("MainActivity", "Deep link: Checkout successful! Session ID: $sessionId")
                    // TODO: ViewModel経由でStripeRepository.confirmPaymentを呼び出す
                    // stripeViewModel.confirmStripePayment(deviceId, sessionId, productType) // productTypeをどこからか取得する必要あり
                } else {
                    Log.e("MainActivity", "Deep link: Session ID not found in success URL")
                }
            } else if (uri.pathSegments.contains("checkout-cancel")) {
                Log.i("MainActivity", "Deep link: Checkout cancelled.")
                // TODO: キャンセル処理 (ユーザーへの通知など)
            }
        }
    }
}

// 2. 購入ボタンクリック時の処理 (ViewModel経由でStripeRepositoryを呼び出す想定)
// fun onPurchaseButtonClicked(productType: String, unlockCount: Int?) {
//     lifecycleScope.launch {
//         val deviceId = "your_device_id_logic" // deviceIdを取得
//         val checkoutUrl = stripeViewModel.startStripeCheckout(deviceId, productType, unlockCount)
//         if (checkoutUrl != null) {
//             // openStripeCheckoutPageInWebView(checkoutUrl) // WebViewを開く処理
//         } else {
//             // エラー表示
//         }
//     }
// }

// 3. WebViewでの表示 (専用のActivity/Fragmentまたはダイアログで表示)
// fun openStripeCheckoutPageInWebView(url: String) {
//     // WebViewの設定 (JavaScript有効化、WebViewClient設定)
//     // WebViewClientのshouldOverrideUrlLoadingで "app://com.example.timekeeper/checkout-success" や
//     // "app://com.example.timekeeper/checkout-cancel" をインターセプトしてWebViewを閉じ、
//     // ネイティブのディープリンク処理に委ねるか、直接 여기서 session_id をパースしてconfirmPaymentを呼ぶ。
//     // ただし、ディープリンクでActivityが再起動される形の方がシンプルかもしれない。
// }

*/ 