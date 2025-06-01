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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.navigation.compose.rememberNavController
import com.example.timekeeper.ui.navigation.TimekeeperNavigation
import com.example.timekeeper.ui.navigation.TimekeeperRoutes
import com.example.timekeeper.ui.theme.TimekeeperTheme
import com.example.timekeeper.ui.payment.StripeCheckoutActivity
import com.example.timekeeper.viewmodel.StripeViewModel
import com.example.timekeeper.data.PurchaseStateManager
import com.example.timekeeper.data.MonitoredAppRepository
import com.example.timekeeper.data.AppUsageRepository
import com.example.timekeeper.service.HeartbeatService
import com.example.timekeeper.util.GapDetector
import com.example.timekeeper.util.HeartbeatLogger
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject
import com.example.timekeeper.service.MyAccessibilityService
import android.provider.Settings
import android.text.TextUtils
import com.example.timekeeper.util.SecurityManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var purchaseStateManager: PurchaseStateManager

    @Inject
    lateinit var monitoredAppRepository: MonitoredAppRepository

    @Inject
    lateinit var appUsageRepository: AppUsageRepository

    @Inject
    lateinit var gapDetector: GapDetector

    @Inject
    lateinit var heartbeatLogger: HeartbeatLogger

    @Inject
    lateinit var securityManager: SecurityManager

    private val stripeViewModel: StripeViewModel by viewModels()
    private lateinit var appSpecificDeviceId: String

    // 🔧 デバッグ用フラグ - 本番リリース前にfalseに戻すこと！
    private val MAIN_ACTIVITY_SECURITY_DISABLED_FOR_DEBUG = true

    private val sharedPreferences by lazy {
        getSharedPreferences("TimekeeperPrefs", Context.MODE_PRIVATE)
    }

    private fun retrieveAppSpecificDeviceId(): String {
        val storedDeviceId = sharedPreferences.getString("DEVICE_ID", null)
        return if (storedDeviceId == null) {
            val newDeviceId = UUID.randomUUID().toString()
            sharedPreferences.edit().putString("DEVICE_ID", newDeviceId).apply()
            Log.d("MainActivity", "New Device ID generated: $newDeviceId")
            newDeviceId
        } else {
            Log.d("MainActivity", "Retrieved Device ID: $storedDeviceId")
            storedDeviceId
        }
    }

    private fun determineStartDestination(): String {
        // ライセンス購入済みかチェック
        if (!purchaseStateManager.isLicensePurchased()) {
            return TimekeeperRoutes.SETUP_AND_LICENSE
        }

        // 監視対象アプリが設定されているかチェック
        val monitoredApps = monitoredAppRepository.monitoredApps.value
        if (monitoredApps.isEmpty()) {
            return TimekeeperRoutes.MONITORING_SETUP
        }

        // 全て設定済みならダッシュボード
        return TimekeeperRoutes.DASHBOARD
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // セキュリティチェック：heartbeatによるギャップ検知
        performHeartbeatSecurityCheck()

        // HeartbeatServiceを開始
        startHeartbeatService()

        appSpecificDeviceId = retrieveAppSpecificDeviceId()
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
                            Log.i("MainActivity", "Payment success state detected: ${state.message}")
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                            
                            if (state.message.contains("license")) {
                                Log.i("MainActivity", "License purchase successful, navigating to monitoring setup")
                                // ライセンス購入成功時はアプリ設定画面へ
                                navController.navigate(TimekeeperRoutes.MONITORING_SETUP) {
                                    popUpTo(TimekeeperRoutes.SETUP_AND_LICENSE) { inclusive = true }
                                }
                            } else if (state.message.contains("daypass")) {
                                // デイパス購入成功時の処理
                                Log.i("MainActivity", "Day pass purchase successful, applying unlock")
                                
                                // 全ての監視対象アプリにデイパスを適用
                                appUsageRepository.purchaseDayPassForAllApps()
                                Log.i("MainActivity", "Day pass applied to all monitored apps")
                                
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
                                        Log.w("MainActivity", "Accessibility service instance is null - service may not be running")
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to notify accessibility service about day pass purchase", e)
                                }
                                
                                Log.i("MainActivity", "Navigating to dashboard after successful day pass purchase")
                                // ダッシュボードに戻る
                                navController.navigate(TimekeeperRoutes.DASHBOARD) {
                                    popUpTo(TimekeeperRoutes.DAY_PASS_PURCHASE) { inclusive = true }
                                }
                            }
                            
                            // 状態をリセット（一度きりの処理として）
                            // Note: ViewModelにリセットメソッドがある場合は呼び出し
                            stripeViewModel.resetPaymentState()
                        }
                        is com.example.timekeeper.viewmodel.PaymentUiState.Error -> {
                            Log.e("MainActivity", "Payment error state detected: ${state.message}")
                            Toast.makeText(context, "決済エラー: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                        is com.example.timekeeper.viewmodel.PaymentUiState.Loading -> {
                            Log.d("MainActivity", "Payment loading state detected")
                        }
                        else -> { 
                            Log.d("MainActivity", "Payment state: Idle")
                        }
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
        Log.i("MainActivity", "=== onNewIntent called ===")
        Log.i("MainActivity", "Intent action: ${intent.action}")
        Log.i("MainActivity", "Intent data: ${intent.data}")
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        Log.i("MainActivity", "=== handleDeepLink called ===")
        intent?.data?.let { uri ->
            Log.i("MainActivity", "Deep link URI: $uri")
            Log.i("MainActivity", "URI scheme: ${uri.scheme}, host: ${uri.host}")
            Log.i("MainActivity", "URI path segments: ${uri.pathSegments}")
            
            if (uri.scheme == "app" && uri.host == "com.example.timekeeper") {
                val pathSegments = uri.pathSegments
                Log.i("MainActivity", "Valid deep link detected with path segments: $pathSegments")
                
                if (pathSegments.contains("checkout-success")) {
                    val sessionId = uri.getQueryParameter("session_id")
                    val productType = uri.getQueryParameter("product_type")
                    Log.i("MainActivity", "Checkout success deep link - sessionId: $sessionId, productType: $productType")
                    
                    if (sessionId != null && productType != null) {
                        Log.i("MainActivity", "Confirming Stripe payment with sessionId: $sessionId, productType: $productType")
                        stripeViewModel.confirmStripePayment(appSpecificDeviceId, sessionId, productType)
                    } else {
                        Log.e("MainActivity", "Missing sessionId or productType in checkout success deep link")
                    }
                } else if (pathSegments.contains("checkout-cancel")) {
                    Log.i("MainActivity", "Checkout cancel deep link detected")
                    // キャンセル時の処理があれば追加
                    Toast.makeText(this, "決済がキャンセルされました", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w("MainActivity", "Unknown deep link path: $pathSegments")
                }
            } else {
                Log.w("MainActivity", "Invalid deep link scheme or host: ${uri.scheme}://${uri.host}")
            }
        } ?: run {
            Log.d("MainActivity", "No URI data in intent")
        }
    }

    /**
     * Heartbeatによるセキュリティチェック
     * 不正なサービス停止を検知してアプリを初期化
     */
    private fun performHeartbeatSecurityCheck() {
        // デバッグモード時はセキュリティチェックをスキップ
        if (MAIN_ACTIVITY_SECURITY_DISABLED_FOR_DEBUG) {
            Log.w("MainActivity", "🔧 DEBUG MODE: MainActivity security check skipped")
            return
        }
        
        try {
            Log.i("MainActivity", "Performing heartbeat security check")
            
            val breach = gapDetector.checkForSuspiciousGaps()
            if (breach != null && breach.severity == GapDetector.SecurityBreach.Severity.SECURITY_BREACH) {
                Log.w("MainActivity", "SECURITY BREACH DETECTED: Suspicious gap of ${breach.gapMinutes} minutes")
                
                // Toast表示
                Toast.makeText(
                    this,
                    "制限回避が検知されました。アプリを初期化します。\n停止期間: ${breach.gapMinutes}分",
                    Toast.LENGTH_LONG
                ).show()
                
                // SecurityManagerによるバックグラウンド初期化
                securityManager.handleHeartbeatGap(breach.gapMinutes)
                
                Log.w("MainActivity", "App data has been reset due to security breach")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during security check", e)
        }
    }
    
    /**
     * セキュリティ違反によるアプリデータ完全初期化
     * @deprecated SecurityManager.performBackgroundDataResetを使用してください
     */
    @Deprecated("Use SecurityManager.performBackgroundDataReset instead")
    private fun resetAppDataDueToSecurityBreach() {
        // 廃止予定：SecurityManagerにロジックを移行済み
        securityManager.performBackgroundDataReset("Legacy MainActivity call")
    }
    
    /**
     * HeartbeatServiceを開始
     */
    private fun startHeartbeatService() {
        try {
            val intent = Intent(this, HeartbeatService::class.java)
            startForegroundService(intent)
            Log.i("MainActivity", "HeartbeatService started")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting HeartbeatService", e)
        }
    }
}