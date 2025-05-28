package com.example.timekeeper

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.timekeeper.ui.navigation.TimekeeperNavigation
import com.example.timekeeper.ui.navigation.TimekeeperRoutes
import com.example.timekeeper.ui.theme.TimekeeperTheme
import com.example.timekeeper.ui.payment.StripeCheckoutActivity
import com.example.timekeeper.viewmodel.StripeViewModel
import com.example.timekeeper.data.PurchaseStateManager
import com.example.timekeeper.data.MonitoredAppRepository
import com.example.timekeeper.data.AppUsageRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var purchaseStateManager: PurchaseStateManager

    @Inject
    lateinit var monitoredAppRepository: MonitoredAppRepository

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    private val sharedPreferences by lazy {
        getSharedPreferences("TimekeeperPrefs", Context.MODE_PRIVATE)
    }

    private val stripeViewModel: StripeViewModel by viewModels()

    private fun clearOldSampleData() {
        // 古いサンプルデータが残っている場合はクリア
        val hasOldData = sharedPreferences.getBoolean("has_cleared_sample_data", false)
        if (!hasOldData) {
            Log.d("MainActivity", "Clearing old sample data")
            monitoredAppRepository.clearAllData()
            appUsageRepository.clearAllData()
            sharedPreferences.edit().putBoolean("has_cleared_sample_data", true).apply()
            Log.d("MainActivity", "Old sample data cleared")
        }
    }

    private fun retrieveAppSpecificDeviceId(): String {
        var deviceId = sharedPreferences.getString("DEVICE_ID", null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            sharedPreferences.edit().putString("DEVICE_ID", deviceId).apply()
        }
        return deviceId
    }

    private fun determineStartDestination(): String {
        return if (purchaseStateManager.isLicensePurchased()) {
            // ライセンス購入済みの場合、監視対象アプリが設定されているかチェック
            val monitoredApps = monitoredAppRepository.monitoredApps.value
            if (monitoredApps.isEmpty()) {
                Log.d("MainActivity", "License purchased but no monitored apps, starting with Monitoring Setup")
                TimekeeperRoutes.MONITORING_SETUP
            } else {
                Log.d("MainActivity", "License purchased and monitored apps configured, starting with Dashboard")
            TimekeeperRoutes.DASHBOARD
            }
        } else {
            Log.d("MainActivity", "License not purchased, starting with License Purchase")
            TimekeeperRoutes.LICENSE_PURCHASE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // デバッグ用：古いサンプルデータをクリア
        clearOldSampleData()

        val appSpecificDeviceId = retrieveAppSpecificDeviceId()
        stripeViewModel.setDeviceId(appSpecificDeviceId)
        Log.d("MainActivity", "Device ID: $appSpecificDeviceId")

        handleDeepLink(intent)

        setContent {
            TimekeeperTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val startDestination = determineStartDestination()

                val checkoutUrl by stripeViewModel.checkoutUrlFlow.collectAsState()
                LaunchedEffect(checkoutUrl) {
                    checkoutUrl?.let { url ->
                        Log.i("MainActivity", "Opening Stripe Checkout URL: $url")
                        val intent = StripeCheckoutActivity.createIntent(this@MainActivity, url)
                        startActivity(intent)
                        stripeViewModel.consumeCheckoutUrl()
                    }
                }

                val paymentUiState by stripeViewModel.paymentUiState.collectAsState()
                LaunchedEffect(paymentUiState) {
                    when (val state = paymentUiState) {
                        is com.example.timekeeper.viewmodel.PaymentUiState.Success -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                            if (state.message.contains("license")) {
                                navController.navigate(com.example.timekeeper.ui.navigation.TimekeeperRoutes.MONITORING_SETUP) {
                                    popUpTo(com.example.timekeeper.ui.navigation.TimekeeperRoutes.LICENSE_PURCHASE) { inclusive = true }
                                }
                            } else if (state.message.contains("daypass")) {
                                // デイパス購入成功時の処理
                                Log.i("MainActivity", "Day pass purchase successful, applying unlock")
                                
                                // 全ての監視対象アプリにデイパスを適用
                                appUsageRepository.purchaseDayPassForAllApps()
                                
                                // アクセシビリティサービスにブロック解除を通知
                                try {
                                    val serviceClass = Class.forName("com.example.timekeeper.service.MyAccessibilityService")
                                    val getInstanceMethod = serviceClass.getMethod("getInstance")
                                    val serviceInstance = getInstanceMethod.invoke(null)
                                    
                                    if (serviceInstance != null) {
                                        val onDayPassPurchasedMethod = serviceClass.getMethod("onDayPassPurchasedForAllApps")
                                        onDayPassPurchasedMethod.invoke(serviceInstance)
                                        Log.i("MainActivity", "Successfully notified accessibility service about day pass purchase")
                                    } else {
                                        Log.w("MainActivity", "Accessibility service instance not available")
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to notify accessibility service", e)
                                }
                                
                                // ダッシュボードに戻る
                                navController.navigate(com.example.timekeeper.ui.navigation.TimekeeperRoutes.DASHBOARD) {
                                    popUpTo(com.example.timekeeper.ui.navigation.TimekeeperRoutes.DAY_PASS_PURCHASE) { inclusive = true }
                                }
                            }
                        }
                        is com.example.timekeeper.viewmodel.PaymentUiState.Error -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        }
                        else -> { /* Idle or Loading - no action needed */ }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TimekeeperNavigation(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding),
                        onPurchaseLicenseClick = {
                            stripeViewModel.startStripeCheckout("license", null)
                        },
                        onPurchaseDaypassClick = { unlockCount ->
                            stripeViewModel.startStripeCheckout("daypass", unlockCount ?: 1)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent called with: $intent")
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            Log.i("MainActivity", "Handling deep link: $uri")
            if (uri.scheme == "app" && uri.host == "com.example.timekeeper") {
                val pathSegments = uri.pathSegments
                if (pathSegments.contains("checkout-success")) {
                    val sessionId = uri.getQueryParameter("session_id")
                    val productType = uri.getQueryParameter("product_type")
                    if (sessionId != null && productType != null) {
                        Log.i("MainActivity", "Deep link: Checkout successful! Session ID: $sessionId, Product Type: $productType")
                        stripeViewModel.confirmStripePayment(sessionId, productType)
                    } else {
                        Log.e("MainActivity", "Deep link: Session ID or Product Type not found in success URL")
                    }
                } else if (pathSegments.contains("checkout-cancel")) {
                    Log.i("MainActivity", "Deep link: Checkout cancelled.")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TimekeeperTheme {
        Greeting("Android")
    }
}