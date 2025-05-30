package com.example.timekeeper.ui.screens

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.timekeeper.viewmodel.DayPassPurchaseViewModel
import com.example.timekeeper.viewmodel.StripeViewModel
import com.example.timekeeper.ui.navigation.TimekeeperRoutes
import kotlin.math.pow

@Composable
fun DayPassPurchaseScreen(
    onCancelClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
    stripeViewModel: StripeViewModel,
    onPurchaseDaypassClick: (Int?) -> Unit = {},
    viewModel: DayPassPurchaseViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("TimekeeperPrefs", Context.MODE_PRIVATE)
    }
    val unlockCount = sharedPreferences.getInt("UNLOCK_COUNT", 0)

    // 価格計算: ¥(200×1.2^unlock_count)
    val price = (200 * 1.2.pow(unlockCount.toDouble())).toInt()

    var isLoading by remember { mutableStateOf(false) }
    var showWebView by remember { mutableStateOf(false) }
    var checkoutUrl by remember { mutableStateOf<String?>(null) }
    
    // ViewModelの状態を監視
    val purchaseState by viewModel.purchaseState.collectAsState()
    val checkoutUrlFlow by stripeViewModel.checkoutUrlFlow.collectAsState()
    
    // Checkout URLが取得されたらWebViewを表示
    LaunchedEffect(checkoutUrlFlow) {
        checkoutUrlFlow?.let { url ->
            checkoutUrl = url
            showWebView = true
            stripeViewModel.consumeCheckoutUrl()
        }
    }
    
    // 購入状態の変化を監視
    LaunchedEffect(purchaseState) {
        when (purchaseState) {
            is DayPassPurchaseViewModel.PurchaseState.Success -> {
                isLoading = false
                showWebView = false
                // ダッシュボードに戻る
                navController.navigate(TimekeeperRoutes.DASHBOARD) {
                    popUpTo(TimekeeperRoutes.DAY_PASS_PURCHASE) { inclusive = true }
                }
            }
            is DayPassPurchaseViewModel.PurchaseState.Error -> {
                isLoading = false
                showWebView = false
                // エラー処理（必要に応じてSnackbarなどで表示）
            }
            is DayPassPurchaseViewModel.PurchaseState.Loading -> {
                isLoading = true
            }
            is DayPassPurchaseViewModel.PurchaseState.Idle -> {
                isLoading = false
            }
        }
    }
    
    if (showWebView && checkoutUrl != null) {
        // Stripe Checkout WebView
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            // ヘッダー
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "決済画面",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        showWebView = false
                        checkoutUrl = null
                    }
                ) {
                    Text("キャンセル")
                }
            }
            
            // WebView
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            @Suppress("OVERRIDE_DEPRECATION")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                url?.let { urlString ->
                                    when {
                                        urlString.contains("checkout-success") -> {
                                            // 決済成功
                                            // val sessionId = extractSessionId(urlString)
                                            // if (sessionId != null) {
                                            //     stripeViewModel.confirmStripePayment(sessionId, "daypass")
                                            // }
                                            showWebView = false // WebViewを閉じるだけ
                                            // MainActivityのonNewIntent -> handleDeepLink で処理される想定
                                            return true // URLの読み込みはWebViewにさせない
                                        }
                                        urlString.contains("checkout-cancel") -> {
                                            // 決済キャンセル
                                            showWebView = false
                                            return true
                                        }
                                        else -> {
                                            // その他のURL
                                            return false
                                        }
                                    }
                                }
                                return false
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                    }
                },
                update = { webView ->
                    checkoutUrl?.let { url ->
                        webView.loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // 通常の購入画面
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
            
            // Stripe決済ボタン
            Button(
                onClick = {
                    // isLoading = true
                    // stripeViewModel.confirmStripePayment(
                    //     purchaseToken = "test_session_daypass_${System.currentTimeMillis()}",
                    //     productType = "daypass"
                    // ) // モック呼び出しからMainActivity経由のコールバック呼び出しへ戻す
                    onPurchaseDaypassClick(unlockCount) // MainActivityから渡されたコールバックを実行
                },
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
                        text = "Stripe決済で購入",
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // 開発時のテスト用ボタン
            OutlinedButton(
                onClick = {
                    // テスト用：直接アンロック処理を実行
                    // stripeViewModel.confirmStripePayment(
                    //     "test_session_${System.currentTimeMillis()}", 
                    //     "daypass"
                    // ) // deviceId が不足しているため、一時的にコメントアウト
                    android.util.Log.d("DayPassPurchaseScreen", "Test button (confirmStripePayment) is temporarily disabled.")
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "テスト用（開発時のみ）",
                    fontSize = 16.sp
                )
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

/**
 * URLからセッションIDを抽出
 */
private fun extractSessionId(url: String): String? {
    return try {
        val uri = android.net.Uri.parse(url)
        uri.getQueryParameter("session_id")
    } catch (e: Exception) {
        null
    }
}

 