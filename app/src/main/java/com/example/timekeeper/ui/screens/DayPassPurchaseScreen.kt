package com.example.timekeeper.ui.screens

import android.content.Context // SharedPreferencesのために追加
import android.webkit.WebView // Stripe Checkoutのために追加
import android.webkit.WebViewClient // Stripe Checkoutのために追加
import android.net.Uri // Stripe CheckoutでのURLパースのために追加
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // SharedPreferencesのために追加
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView // Stripe Checkoutのために追加
import androidx.navigation.NavController
import com.example.timekeeper.data.api.ApiResponse
import com.example.timekeeper.data.api.TimekeeperRepository
import com.example.timekeeper.utils.ErrorHandler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat // last_unlock_dateのパースのために追加
import java.util.Date // last_unlock_dateのために追加
import java.util.Locale // SimpleDateFormatのために追加
import kotlin.math.pow

// 仮のStripe Checkout テストURL
private const val STRIPE_CHECKOUT_TEST_URL = "https://checkout.stripe.com/pay/cs_test_a1bb22cc33dd44ee55ff66ABCDEF1234567890" // これは実際のStripeサーバーとの連携時に置き換える

@Composable
fun DayPassPurchaseScreen(
    onPurchaseSuccess: () -> Unit, // 成功時のコールバック名を変更
    onCancelClick: () -> Unit,
    navController: NavController, // rememberNavController() を削除したのでデフォルト値も削除
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("TimekeeperPrefs", Context.MODE_PRIVATE)
    }
    val unlockCount = sharedPreferences.getInt("UNLOCK_COUNT", 0)

    // 価格計算: ¥(200×1.2^unlock_count)
    val price = (200 * 1.2.pow(unlockCount.toDouble())).toInt()

    val scope = rememberCoroutineScope()
    val repository = remember { TimekeeperRepository() }
    var isLoading by remember { mutableStateOf(false) }
    var showWebView by remember { mutableStateOf(false) }

    val handlePurchaseAttempt: () -> Unit = { // 名前をhandlePurchaseAttemptに変更
        // ここでStripe CheckoutのWebViewを表示する
        showWebView = true
    }

    if (showWebView) {
        StripeCheckoutWebView(
            url = STRIPE_CHECKOUT_TEST_URL, // 後で動的に生成するように変更する可能性あり
            onPaymentSuccess = { purchaseToken ->
                showWebView = false
                scope.launch {
                    isLoading = true
                    try {
                        val deviceId = sharedPreferences.getString("DEVICE_ID", "") ?: ""
                        if (deviceId.isEmpty()) {
                            // deviceIdがない場合はエラー処理 (P06へ遷移など)
                            ErrorHandler.handleException(navController, IllegalStateException("Device ID not found"))
                            return@launch
                        }

                        val response = repository.unlockDaypass(deviceId, purchaseToken)

                        when (response) {
                            is ApiResponse.Success -> {
                                // 成功したらSharedPreferencesに保存
                                val newUnlockCount = response.data.unlock_count
                                val lastUnlockDateStr = response.data.last_unlock_date
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                try {
                                    val lastUnlockDate = dateFormat.parse(lastUnlockDateStr)
                                    if (lastUnlockDate != null) {
                                        sharedPreferences.edit()
                                            .putInt("UNLOCK_COUNT", newUnlockCount)
                                            .putLong("LAST_UNLOCK_DATE", lastUnlockDate.time) // DateをLongに変換
                                            .apply()
                                        onPurchaseSuccess() // 呼び出し元に成功を通知
                                    } else {
                                        // 日付パース失敗エラー
                                        ErrorHandler.handleException(navController, IllegalStateException("Failed to parse last_unlock_date"))
                                    }
                                } catch (e: java.text.ParseException) {
                                    // 日付パース失敗エラー
                                    ErrorHandler.handleException(navController, e)
                                }
                            }
                            is ApiResponse.Error -> {
                                // Stripe決済成功後のFirestore更新失敗の場合など
                                ErrorHandler.handleApiError(navController, response)
                            }
                            is ApiResponse.Exception -> {
                                ErrorHandler.handleException(navController, response.exception)
                            }
                        }
                    } catch (e: Exception) {
                        ErrorHandler.handleException(navController, e)
                    } finally {
                        isLoading = false
                    }
                }
            },
            onPaymentCancel = {
                showWebView = false
                // キャンセル時のユーザーフィードバックがあればここに
            },
            onClose = {
                showWebView = false
            },
            navController = navController
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "デイパス購入",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "今日1日だけ制限解除",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Text(
                text = "¥$price",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = handlePurchaseAttempt, // handlePurchaseから変更
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "今すぐアンロック",
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "キャンセル",
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun StripeCheckoutWebView(
    url: String,
    onPaymentSuccess: (purchaseToken: String) -> Unit,
    onPaymentCancel: () -> Unit,
    onClose: () -> Unit, // WebViewを閉じるためのコールバック
    navController: NavController // エラーハンドリングのために NavController を追加
) {
    // AndroidViewを使用してWebViewを表示
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        if (url != null) {
                            // 成功URLのパターンに合わせて調整が必要
                            if (url.startsWith("YOUR_SUCCESS_URL")) {
                                // URLからpurchase_tokenを抽出するロジック
                                val purchaseToken = Uri.parse(url).getQueryParameter("purchase_token")
                                if (purchaseToken != null) {
                                    onPaymentSuccess(purchaseToken)
                                } else {
                                    // purchase_token が取得できないエラー
                                    ErrorHandler.handleException(navController, IllegalStateException("Purchase token not found in success URL"))
                                    onClose() // WebViewを閉じる
                                }
                                return true
                            }
                            // キャンセルURLのパターンに合わせて調整が必要
                            if (url.startsWith("YOUR_CANCEL_URL")) {
                                onPaymentCancel()
                                return true
                            }
                        }
                        return super.shouldOverrideUrlLoading(view, url)
                    }
                }
                settings.javaScriptEnabled = true // JavaScriptを有効にする
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize(),
        // WebViewが不要になった際に呼び出される
        onRelease = { webView ->
             // WebViewのリソースを解放する処理があれば記述
             // webView.destroy() // 必要に応じて
        }
    )
    // WebView表示中に物理バックボタンで閉じられるようにする対応（例）
    // BackHandler(enabled = true) {
    //     onClose()
    // }
} 