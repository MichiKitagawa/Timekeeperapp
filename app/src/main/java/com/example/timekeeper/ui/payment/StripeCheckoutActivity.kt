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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
        Log.i(TAG, "=== StripeCheckoutActivity onCreate() called ===")

        val checkoutUrl = intent.getStringExtra(EXTRA_CHECKOUT_URL)
        Log.i(TAG, "Checkout URL received: $checkoutUrl")
        
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
                    onClose = { 
                        Log.i(TAG, "=== onClose callback triggered ===")
                        finish() 
                    },
                    onPaymentComplete = { sessionId, productType ->
                        Log.i(TAG, "=== onPaymentComplete callback triggered: sessionId=$sessionId, productType=$productType ===")
                        // MainActivityにディープリンクを送信
                        val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("app://com.example.timekeeper/checkout-success?session_id=$sessionId&product_type=$productType")
                            // 既存のMainActivityタスクを使用し、新しいタスクは作らない
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            // パッケージを明示的に指定して、確実にこのアプリ内でIntent が処理されるようにする
                            setPackage(packageName)
                        }
                        startActivity(deepLinkIntent)
                        finish()
                    },
                    onPaymentCancelled = {
                        Log.i(TAG, "=== onPaymentCancelled callback triggered ===")
                        val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("app://com.example.timekeeper/checkout-cancel")
                            // 既存のMainActivityタスクを使用し、新しいタスクは作らない
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            // パッケージを明示的に指定
                            setPackage(packageName)
                        }
                        startActivity(deepLinkIntent)
                        finish()
                    }
                )
            }
        }
        
        Log.i(TAG, "=== StripeCheckoutActivity onCreate() completed ===")
    }
    
    override fun onStart() {
        super.onStart()
        Log.i(TAG, "=== StripeCheckoutActivity onStart() ===")
    }
    
    override fun onResume() {
        super.onResume()
        Log.i(TAG, "=== StripeCheckoutActivity onResume() ===")
    }
    
    override fun onPause() {
        super.onPause()
        Log.i(TAG, "=== StripeCheckoutActivity onPause() ===")
    }
    
    override fun onStop() {
        super.onStop()
        Log.i(TAG, "=== StripeCheckoutActivity onStop() ===")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "=== StripeCheckoutActivity onDestroy() ===")
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
    var pageLoadCompleted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("決済") },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
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
                                pageLoadCompleted = false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                Log.d("StripeCheckoutActivity", "Page finished loading: $url")
                                isLoading = false
                                
                                // ページ読み込み完了後、少し待機してからディープリンク処理を有効にする
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    pageLoadCompleted = true
                                    Log.i("StripeCheckoutActivity", "=== Deep link processing enabled ===")
                                }, 2000) // 2秒待機
                            }

                            @Suppress("OVERRIDE_DEPRECATION")
                            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                                super.onReceivedError(view, errorCode, description, failingUrl)
                                Log.e("StripeCheckoutActivity", "WebView error: $errorCode - $description for URL: $failingUrl")
                                
                                // ERR_UNKNOWN_URL_SCHEME (-10) でapp://スキームの場合は最後のチャンスとして処理
                                if (errorCode == -10 && failingUrl?.startsWith("app://com.example.timekeeper") == true) {
                                    Log.i("StripeCheckoutActivity", "=== Handling app:// scheme in onReceivedError as fallback ===")
                                    
                                    val uri = Uri.parse(failingUrl)
                                    val pathSegments = uri.pathSegments
                                    
                                    if (pathSegments.contains("checkout-success")) {
                                        val sessionId = uri.getQueryParameter("session_id")
                                        val productType = uri.getQueryParameter("product_type")
                                        Log.i("StripeCheckoutActivity", "Fallback processing: Session ID: $sessionId, Product Type: $productType")
                                        
                                        if (sessionId != null && productType != null) {
                                            Log.i("StripeCheckoutActivity", "=== Calling onPaymentComplete from fallback ===")
                                            onPaymentComplete(sessionId, productType)
                                            return
                                        }
                                    } else if (pathSegments.contains("checkout-cancel")) {
                                        Log.i("StripeCheckoutActivity", "=== Calling onPaymentCancelled from fallback ===")
                                        onPaymentCancelled()
                                        return
                                    }
                                }
                                
                                isLoading = false
                            }

                            @Suppress("OVERRIDE_DEPRECATION")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                Log.i("StripeCheckoutActivity", "=== shouldOverrideUrlLoading called ===")
                                Log.i("StripeCheckoutActivity", "URL loading: $url")
                                Log.i("StripeCheckoutActivity", "Page load completed: $pageLoadCompleted")
                                
                                url?.let { urlString ->
                                    val uri = Uri.parse(urlString)
                                    Log.i("StripeCheckoutActivity", "Parsed URI - scheme: ${uri.scheme}, host: ${uri.host}, path: ${uri.path}")
                                    Log.i("StripeCheckoutActivity", "Path segments: ${uri.pathSegments}")
                                    
                                    // app://スキームのディープリンクは常に処理（pageLoadCompletedに関係なく）
                                    if (uri.scheme == "app" && uri.host == "com.example.timekeeper") {
                                        Log.i("StripeCheckoutActivity", "=== Deep link detected! Processing immediately ===")
                                        val pathSegments = uri.pathSegments
                                        
                                        if (pathSegments.contains("checkout-success")) {
                                            Log.i("StripeCheckoutActivity", "=== Checkout success deep link ===")
                                            val sessionId = uri.getQueryParameter("session_id")
                                            val productType = uri.getQueryParameter("product_type")
                                            Log.i("StripeCheckoutActivity", "Session ID: $sessionId, Product Type: $productType")
                                            
                                            if (sessionId != null && productType != null) {
                                                Log.i("StripeCheckoutActivity", "=== Calling onPaymentComplete ===")
                                                onPaymentComplete(sessionId, productType)
                                                return true
                                            }
                                        } else if (pathSegments.contains("checkout-cancel")) {
                                            Log.i("StripeCheckoutActivity", "=== Checkout cancel deep link ===")
                                            Log.i("StripeCheckoutActivity", "=== Calling onPaymentCancelled ===")
                                            onPaymentCancelled()
                                            return true
                                        }
                                    }
                                    
                                    // 通常のHTTPSページの場合はページ読み込み完了を待つ
                                    if (uri.scheme == "https") {
                                        // ページ読み込みが完了していない場合はディープリンク処理をスキップ
                                        if (!pageLoadCompleted) {
                                            Log.i("StripeCheckoutActivity", "=== HTTPS page not fully loaded yet, allowing normal loading ===")
                                            return false
                                        }
                                    }
                                }
                                
                                Log.i("StripeCheckoutActivity", "=== No deep link detected, proceeding with normal URL loading ===")
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