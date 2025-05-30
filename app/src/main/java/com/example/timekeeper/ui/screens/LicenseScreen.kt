package com.example.timekeeper.ui.screens

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
import com.example.timekeeper.viewmodel.StripeViewModel
import com.example.timekeeper.ui.theme.TimekeeperTheme

@Composable
fun LicenseScreen(
    stripeViewModel: StripeViewModel,
    onNavigateToMonitoringSetup: () -> Unit,
    onPurchaseLicenseClick: () -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }
    var showWebView by remember { mutableStateOf(false) }
    var checkoutUrl by remember { mutableStateOf<String?>(null) }
    
    // ViewModelの状態を監視
    val checkoutUrlFlow by stripeViewModel.checkoutUrlFlow.collectAsState()
    val paymentUiState by stripeViewModel.paymentUiState.collectAsState()
    
    // Checkout URLが取得されたらWebViewを表示
    LaunchedEffect(checkoutUrlFlow) {
        checkoutUrlFlow?.let { url ->
            checkoutUrl = url
            showWebView = true
            stripeViewModel.consumeCheckoutUrl()
        }
    }
    
    // 決済状態の変化を監視
    LaunchedEffect(paymentUiState) {
        when (val state = paymentUiState) {
            is com.example.timekeeper.viewmodel.PaymentUiState.Success -> {
                if (state.message.contains("license")) {
                    isLoading = false
                    showWebView = false
                    onNavigateToMonitoringSetup()
                }
            }
            is com.example.timekeeper.viewmodel.PaymentUiState.Error -> {
                isLoading = false
                showWebView = false
            }
            is com.example.timekeeper.viewmodel.PaymentUiState.Loading -> {
                isLoading = true
            }
            is com.example.timekeeper.viewmodel.PaymentUiState.Idle -> {
                isLoading = false
            }
        }
    }
    
    if (showWebView && checkoutUrl != null) {
        // Stripe Checkout WebView
        Column(
            modifier = Modifier.fillMaxSize()
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
                                            // val sessionId = extractSessionId(urlString) // sessionIdの抽出はMainActivityのDeepLink処理に任せる
                                            // if (sessionId != null) { // ViewModelへの通知もMainActivity経由
                                            //     stripeViewModel.confirmStripePayment(sessionId, "license")
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
        // 通常のライセンス購入画面
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¥10,000で利用開始",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Button(
                onClick = {
                    // isLoading = true // isLoadingの管理はViewModelのStateを見て行うので、直接操作は不要になることが多い
                    // stripeViewModel.confirmStripePayment(
                    //     purchaseToken = "test_session_license_${System.currentTimeMillis()}", 
                    //     productType = "license"
                    // ) // モック呼び出しからMainActivity経由のコールバック呼び出しへ戻す
                    onPurchaseLicenseClick() // MainActivityから渡されたコールバックを実行
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
                        text = "購入",
                        fontSize = 18.sp
                    )
                }
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