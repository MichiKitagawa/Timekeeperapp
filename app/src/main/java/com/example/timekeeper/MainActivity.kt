package com.example.timekeeper

import android.content.ComponentName
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
import com.example.timekeeper.service.MyAccessibilityService
import android.provider.Settings
import android.text.TextUtils

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

    private lateinit var appSpecificDeviceId: String

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
        val storedDeviceId = sharedPreferences.getString("DEVICE_ID", null)
        Log.d("MainActivity", "Device ID from SharedPreferences before check: $storedDeviceId")
        if (storedDeviceId == null) {
            val newDeviceId = UUID.randomUUID().toString()
            Log.d("MainActivity", "New Device ID generated: $newDeviceId, because stored was null.")
            sharedPreferences.edit().putString("DEVICE_ID", newDeviceId).apply()
            return newDeviceId
        } else {
            Log.d("MainActivity", "Retrieved Device ID from SharedPreferences: $storedDeviceId")
            return storedDeviceId
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val service = ComponentName(context, MyAccessibilityService::class.java)
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )
        if (accessibilityEnabled == 0) {
            return false
        }
        val settingValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (settingValue != null) {
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(settingValue)
            while (splitter.hasNext()) {
                val accessibilityService = splitter.next()
                if (accessibilityService.equals(service.flattenToString(), ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    private fun determineStartDestination(): String {
        if (!isAccessibilityServiceEnabled(this)) {
            Log.d("MainActivity", "Accessibility service not enabled, navigating to prompt.")
            return TimekeeperRoutes.ACCESSIBILITY_PROMPT
        }

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
        // clearOldSampleData() // 常にクリアするのではなく、必要に応じて手動で呼び出すか、一度きりの処理にする

        appSpecificDeviceId = retrieveAppSpecificDeviceId()
        Log.d("MainActivity", "Device ID set for Stripe calls: $appSpecificDeviceId") // ここでのログも重要

        handleDeepLink(intent)

        setContent {
            TimekeeperTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val startDestination = determineStartDestination()

                // LockScreenActivityからのインテント処理
                LaunchedEffect(Unit) {
                    intent?.getStringExtra("navigate_to")?.let { destination ->
                        if (destination == "day_pass_purchase") {
                            Log.i("MainActivity", "Navigating to day pass purchase from lock screen")
                            navController.navigate(TimekeeperRoutes.DAY_PASS_PURCHASE)
                        }
                    }
                }

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
                                Log.i("MainActivity", "🎉 Day pass purchase successful, applying unlock")
                                
                                // 全ての監視対象アプリにデイパスを適用
                                Log.i("MainActivity", "🎉 Calling appUsageRepository.purchaseDayPassForAllApps()")
                                appUsageRepository.purchaseDayPassForAllApps()
                                Log.i("MainActivity", "🎉 appUsageRepository.purchaseDayPassForAllApps() completed")
                                
                                // デバッグ：デイパス状態を確認
                                val monitoredApps = monitoredAppRepository.monitoredApps.value
                                Log.i("MainActivity", "🎉 Verifying day pass state for ${monitoredApps.size} monitored apps")
                                monitoredApps.forEach { app ->
                                    val hasDayPass = appUsageRepository.hasDayPass(app.packageName)
                                    val isExceeded = appUsageRepository.isUsageExceededWithDayPass(app.packageName)
                                    Log.i("MainActivity", "🎉 App ${app.packageName}: dayPass=$hasDayPass, exceeded=$isExceeded")
                                }
                                
                                // アクセシビリティサービスにブロック解除を通知
                                Log.i("MainActivity", "🎉 Starting accessibility service notification process")
                                try {
                                    Log.d("MainActivity", "🎉 Attempting to get MyAccessibilityService class")
                                    val serviceClass = Class.forName("com.example.timekeeper.service.MyAccessibilityService")
                                    Log.d("MainActivity", "🎉 Got service class: $serviceClass")
                                    
                                    Log.d("MainActivity", "🎉 Attempting to get getInstance method")
                                    val getInstanceMethod = serviceClass.getMethod("getInstance")
                                    Log.d("MainActivity", "🎉 Got getInstance method: $getInstanceMethod")
                                    
                                    Log.d("MainActivity", "🎉 Calling getInstance method")
                                    val serviceInstance = getInstanceMethod.invoke(null)
                                    Log.d("MainActivity", "🎉 Got service instance: $serviceInstance")
                                    
                                    if (serviceInstance != null) {
                                        Log.d("MainActivity", "🎉 Service instance is not null, getting onDayPassPurchasedForAllApps method")
                                        val onDayPassPurchasedMethod = serviceClass.getMethod("onDayPassPurchasedForAllApps")
                                        Log.d("MainActivity", "🎉 Got method: $onDayPassPurchasedMethod")
                                        
                                        Log.i("MainActivity", "🎉 Calling onDayPassPurchasedForAllApps on service instance")
                                        onDayPassPurchasedMethod.invoke(serviceInstance)
                                        Log.i("MainActivity", "🎉 Successfully notified accessibility service about day pass purchase")
                                    } else {
                                        Log.w("MainActivity", "🚨 Accessibility service instance is null - service may not be running")
                                        Log.w("MainActivity", "🚨 This means the user needs to restart the app or re-enable accessibility service")
                                    }
                                } catch (e: ClassNotFoundException) {
                                    Log.e("MainActivity", "🚨 Failed to find MyAccessibilityService class", e)
                                } catch (e: NoSuchMethodException) {
                                    Log.e("MainActivity", "🚨 Failed to find method in accessibility service", e)
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "🚨 Failed to notify accessibility service", e)
                                }
                                
                                Log.i("MainActivity", "🎉 Navigating back to dashboard")
                                // ダッシュボードに戻る
                                navController.navigate(com.example.timekeeper.ui.navigation.TimekeeperRoutes.DASHBOARD) {
                                    popUpTo(com.example.timekeeper.ui.navigation.TimekeeperRoutes.DAY_PASS_PURCHASE) { inclusive = true }
                                }
                                Log.i("MainActivity", "🎉 Day pass purchase processing completed")
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
                        stripeViewModel = stripeViewModel,
                        onPurchaseLicenseClick = {
                            stripeViewModel.startStripeCheckout(appSpecificDeviceId, "license", null)
                        },
                        onPurchaseDaypassClick = { unlockCount ->
                            stripeViewModel.startStripeCheckout(appSpecificDeviceId, "daypass", unlockCount ?: 1)
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
                        stripeViewModel.confirmStripePayment(appSpecificDeviceId, sessionId, productType)
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