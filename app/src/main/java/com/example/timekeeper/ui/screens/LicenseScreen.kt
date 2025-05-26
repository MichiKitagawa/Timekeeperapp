package com.example.timekeeper.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import com.example.timekeeper.ui.navigation.TimekeeperRoutes
import com.example.timekeeper.ui.theme.TimekeeperTheme
import com.example.timekeeper.ui.viewmodels.LicenseViewModel

// 仮のStripe Checkout テストURL
private const val STRIPE_CHECKOUT_TEST_URL = "https://checkout.stripe.com/pay/cs_test_a1bb22cc33dd44ee55ff66ABCDEF1234567890" // Stripeのテスト用URLに置き換える

@Composable
fun LicenseScreen(
    viewModel: LicenseViewModel = viewModel(),
    navController: NavController, // P06画面遷移のために追加
    onNavigateToMonitoringSetup: () -> Unit,
) {
    val licensePurchased by viewModel.licensePurchased.collectAsState()
    val errorState by viewModel.errorState.collectAsState()
    var showWebView by remember { mutableStateOf(false) }

    LaunchedEffect(licensePurchased) {
        if (licensePurchased && !showWebView) { // WebViewが表示中でない場合のみ遷移
            onNavigateToMonitoringSetup()
        }
    }

    LaunchedEffect(errorState) {
        errorState?.let {
            navController.navigate("${TimekeeperRoutes.ERROR_SCREEN}/${it.name}") {
                // popUpTo(TimekeeperRoutes.LICENSE_PURCHASE) { inclusive = true } // エラー内容による
            }
            viewModel.clearErrorState() // エラー表示後にViewModelの状態をクリア
        }
    }

    if (showWebView) {
        StripeCheckoutWebView(
            url = STRIPE_CHECKOUT_TEST_URL,
            onPaymentSuccess = { purchaseToken ->
                // WebView内で成功URLにリダイレクトされたら、ViewModelのAPI呼び出し関数をコール
                viewModel.confirmLicensePurchase(purchaseToken)
                // WebViewは errorState または licensePurchased の変化により閉じるか、
                // API成功後に直接 showWebView = false としても良い。
                // ここでは ViewModel の状態変化に任せる。
            },
            onPaymentCancel = {
                showWebView = false
                // キャンセル時のユーザーフィードバックがあればここに
            },
            onClose = { // WebViewを閉じる手段 (例: 物理バックボタン対応などで使う)
                 showWebView = false
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "¥10,000で利用開始")
            Button(
                onClick = { showWebView = true },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("購入")
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StripeCheckoutWebView(
    url: String,
    onPaymentSuccess: (purchaseToken: String?) -> Unit,
    onPaymentCancel: () -> Unit,
    onClose: () -> Unit // onCloseコールバック追加
) {
    val successUrlPrefix = "app://timekeeper/payment/success"
    val cancelUrl = "app://timekeeper/payment/cancel"

    // 物理バックボタンでWebViewを閉じる (オプション)
    // androidx.activity.compose.BackHandler { onClose() }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, loadedUrl: String?): Boolean {
                        if (loadedUrl == null) return false
                        println("WebView Navigating to: $loadedUrl")

                        if (loadedUrl.startsWith(successUrlPrefix)) {
                            val purchaseToken = Uri.parse(loadedUrl).getQueryParameter("purchase_token") 
                                                ?: "dummy_purchase_token_from_webview" // 仮
                            onPaymentSuccess(purchaseToken)
                            return true
                        } else if (loadedUrl.startsWith(cancelUrl)) {
                            onPaymentCancel()
                            return true
                        }
                        return false
                    }
                }
                settings.javaScriptEnabled = true
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}


// Previewの引数にNavControllerが必要になるが、Previewでは通常モックを使う
// 簡単のためPreviewからは一旦削除するか、モックを渡す
@Preview(showBackground = true)
@Composable
fun LicenseScreenPreview_NotPurchased() {
    TimekeeperTheme {
        val mockNavController = NavController(LocalContext.current) // モックNavController
        LicenseScreen(
            navController = mockNavController, 
            onNavigateToMonitoringSetup = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LicenseScreenPreview_Purchased_Effect() {
    TimekeeperTheme {
        val previewViewModel = LicenseViewModel()
        previewViewModel.confirmLicensePurchase("dummy_purchase_token_preview") // ViewModelの状態を直接変更
        val mockNavController = NavController(LocalContext.current) // モックNavController
        LicenseScreen(
            viewModel = previewViewModel,
            navController = mockNavController,
            onNavigateToMonitoringSetup = {}
        )
    }
} 