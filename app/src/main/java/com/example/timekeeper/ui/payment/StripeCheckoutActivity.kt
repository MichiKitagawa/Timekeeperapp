package com.example.timekeeper.ui.payment

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.timekeeper.ui.theme.TimekeeperTheme

class StripeCheckoutActivity : ComponentActivity() {

    companion object {
        private const val TAG = "StripeCheckoutActivity"
        private const val EXTRA_CHECKOUT_URL = "checkout_url"

        fun createIntent(context: Context, checkoutUrl: String): Intent {
            return Intent(context, StripeCheckoutActivity::class.java).apply {
                putExtra(EXTRA_CHECKOUT_URL, checkoutUrl)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val checkoutUrl = intent.getStringExtra(EXTRA_CHECKOUT_URL)
        if (checkoutUrl == null) {
            Log.e(TAG, "Checkout URL not provided")
            Toast.makeText(this, "決済URLが見つかりません", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            TimekeeperTheme {
                StripeCheckoutScreen(
                    checkoutUrl = checkoutUrl,
                    onClose = { finish() },
                    onPaymentComplete = { sessionId, productType ->
                        Log.i(TAG, "Payment completed: sessionId=$sessionId, productType=$productType")
                        // MainActivityにディープリンクを送信
                        val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("app://com.example.timekeeper/checkout-success?session_id=$sessionId&product_type=$productType")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(deepLinkIntent)
                        finish()
                    },
                    onPaymentCancelled = {
                        Log.i(TAG, "Payment cancelled")
                        val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("app://com.example.timekeeper/checkout-cancel")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(deepLinkIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StripeCheckoutScreen(
    checkoutUrl: String,
    onClose: () -> Unit,
    onPaymentComplete: (sessionId: String, productType: String) -> Unit,
    onPaymentCancelled: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var paymentProcessed by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("決済") },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                }
            },
            actions = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "閉じる")
                }
            }
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                Log.d("StripeCheckoutActivity", "Page started loading: $url")
                                isLoading = true
                                
                                // テスト用モックURLの場合は自動的に成功処理（重複防止）
                                if (url?.contains("test_session_mock") == true && !paymentProcessed) {
                                    Log.i("StripeCheckoutActivity", "Mock URL detected, simulating successful payment")
                                    paymentProcessed = true
                                    // 少し遅延を入れてリアルな感じにする
                                    view?.postDelayed({
                                        val mockSessionId = "test_session_${System.currentTimeMillis()}"
                                        onPaymentComplete(mockSessionId, "license")
                                    }, 2000) // 2秒後に成功処理
                                    return
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                Log.d("StripeCheckoutActivity", "Page finished loading: $url")
                                isLoading = false
                            }

                            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                super.onReceivedError(view, errorCode, description, failingUrl)
                                Log.e("StripeCheckoutActivity", "WebView error: $errorCode - $description for URL: $failingUrl")
                                
                                // モックURLでエラーが発生した場合は成功処理を実行（重複防止）
                                if (failingUrl?.contains("test_session_mock") == true && !paymentProcessed) {
                                    Log.i("StripeCheckoutActivity", "Mock URL error detected, simulating successful payment")
                                    paymentProcessed = true
                                    val mockSessionId = "test_session_${System.currentTimeMillis()}"
                                    onPaymentComplete(mockSessionId, "license")
                                    return
                                }
                                
                                isLoading = false
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                Log.d("StripeCheckoutActivity", "URL loading: $url")
                                
                                url?.let { urlString ->
                                    val uri = Uri.parse(urlString)
                                    
                                    // ディープリンクをチェック
                                    if (uri.scheme == "app" && uri.host == "com.example.timekeeper") {
                                        val pathSegments = uri.pathSegments
                                        
                                        if (pathSegments.contains("checkout-success")) {
                                            val sessionId = uri.getQueryParameter("session_id")
                                            val productType = uri.getQueryParameter("product_type")
                                            
                                            if (sessionId != null && productType != null) {
                                                onPaymentComplete(sessionId, productType)
                                                return true
                                            }
                                        } else if (pathSegments.contains("checkout-cancel")) {
                                            onPaymentCancelled()
                                            return true
                                        }
                                    }
                                }
                                
                                return false
                            }
                        }

                        loadUrl(checkoutUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
} 