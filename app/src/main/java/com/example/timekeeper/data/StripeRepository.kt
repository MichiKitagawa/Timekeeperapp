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
     * Stripe Checkoutセッションを作成し、決済ページのURLを返します。
     */
    suspend fun createCheckoutSession(deviceId: String, productType: String, unlockCount: Int?): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Creating Stripe checkout session for device=$deviceId, product=$productType, unlockCount=$unlockCount")
                
                // 実際のAPI呼び出しを試行
                try {
                    val response = apiService.createCheckoutSession(
                        CreateCheckoutSessionRequest(
                            device_id = deviceId,
                            product_type = productType,
                            unlock_count = unlockCount
                        )
                    )
                    
                    if (response.isSuccessful) {
                        val checkoutUrl = response.body()?.checkout_url
                        Log.i(TAG, "Stripe checkout session created successfully: $checkoutUrl")
                        return@withContext checkoutUrl
                    } else {
                        Log.e(TAG, "Failed to create checkout session: ${response.code()} - ${response.errorBody()?.string()}")
                        return@withContext null
                    }
                } catch (networkException: Exception) {
                    Log.e(TAG, "Network error during checkout session creation", networkException)
                    return@withContext null
                }
                
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
                Log.i(TAG, "Confirming payment for device=$deviceId, token=$purchaseToken, product=$productType")
                
                // テスト用セッションIDの場合はモック処理
                if (purchaseToken.startsWith("test_session_")) {
                    Log.i(TAG, "Test session detected, using mock payment confirmation")
                    
                    // アプリ内の購入状態を更新
                    if (productType == "license") {
                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        purchaseStateManager.setLicensePurchased(true, currentDate)
                        Log.i(TAG, "Test: License purchase state updated locally")
                    } else if (productType == "daypass") {
                        // テスト用のデイパス情報
                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        // サーバー側でもカウントアップされるため、実際のAPIコールをシミュレートして
                        // 現在のカウント+1の値を直接設定（二重カウントアップを避ける）
                        val currentUnlockCount = purchaseStateManager.getDaypassUnlockCount()
                        val newUnlockCount = currentUnlockCount + 1
                        purchaseStateManager.updateDaypassUnlockCount(newUnlockCount, currentDate)
                        Log.i(TAG, "Test: Daypass unlock state updated locally: count=$newUnlockCount, date=$currentDate")
                    }
                    
                    Log.i(TAG, "Test: Payment confirmed successfully for $productType")
                    return@withContext true
                }
                
                // 実際のAPI呼び出しを試行
                try {
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
                        
                        return@withContext true
                    } else {
                        Log.e(TAG, "Failed to confirm payment for $productType: ${response.code()} - ${response.errorBody()?.string()}")
                        return@withContext false
                    }
                } catch (networkException: Exception) {
                    Log.e(TAG, "Network error during payment confirmation", networkException)
                    return@withContext false
                }
                
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