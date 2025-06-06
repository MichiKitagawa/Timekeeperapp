package com.example.timekeeper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.timekeeper.data.AppUsageRepository
import com.example.timekeeper.data.MonitoredAppRepository
import com.example.timekeeper.util.SecurityManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MyAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var appUsageRepository: AppUsageRepository
    
    @Inject
    lateinit var monitoredAppRepository: MonitoredAppRepository
    
    @Inject
    lateinit var securityManager: SecurityManager
    
    private val handler = Handler(Looper.getMainLooper())
    private var currentForegroundApp: String? = null
    private var usageTrackingRunnable: Runnable? = null
    private val blockedApps = mutableSetOf<String>() // 制限中のアプリ一覧
    private var continuousBlockingRunnable: Runnable? = null
    private var dailyResetCheckRunnable: Runnable? = null // 日次リセットチェック用

    companion object {
        private const val TAG = "MyAccessibilityService"
        private const val USAGE_TRACKING_INTERVAL = 60000L // 1分間隔（ミリ秒）
        private const val CONTINUOUS_BLOCKING_INTERVAL = 500L // 0.5秒間隔（ミリ秒）
        private const val DAILY_RESET_CHECK_INTERVAL = 1800000L // 30分間隔（ミリ秒）
        
        @Volatile
        private var instance: MyAccessibilityService? = null
        
        fun getInstance(): MyAccessibilityService? = instance
        
        /**
         * 外部から日次リセット通知を行う静的メソッド
         * AppUsageRepositoryから確実に呼び出せるように用意
         */
        fun notifyDailyReset() {
            Log.i(TAG, "🔄 notifyDailyReset static method called")
            val serviceInstance = getInstance()
            if (serviceInstance != null) {
                serviceInstance.clearAllBlockedApps()
                Log.i(TAG, "🔄 Successfully notified service instance about daily reset")
            } else {
                Log.w(TAG, "🔄 Service instance is null, cannot clear blocked apps")
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        Log.i(TAG, "Accessibility service connected")
        
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
        
        // 日次リセット処理を実行
        appUsageRepository.performDailyReset()
        
        // 定期的な日次リセットチェックを開始（1時間ごと）
        startDailyResetChecker()
        
        // 監視対象アプリを確認し、既に制限超過しているアプリをブロック
        val monitoredApps = monitoredAppRepository.monitoredApps.value
        Log.i(TAG, "Monitored apps: ${monitoredApps.size}")
        
        monitoredApps.forEach { app ->
            if (appUsageRepository.isUsageExceededWithDayPass(app.packageName)) {
                blockApp(app.packageName)
                Log.w(TAG, "Pre-blocked app due to usage exceeded: ${app.packageName}")
            }
        }
        
        Log.i(TAG, "Accessibility service initialization completed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()

            // Timekeeperアプリ自体は監視・ブロック対象外
            if (packageName == "com.example.timekeeper") {
                if (currentForegroundApp != packageName) {
                    stopUsageTracking()
                    currentForegroundApp = packageName
                }
                return
            }

            // デイパス状態をチェック - いずれかの監視対象アプリでデイパスが有効なら監視を停止
            val monitoredApps = monitoredAppRepository.monitoredApps.value
            val hasAnyDayPass = monitoredApps.any { app -> appUsageRepository.hasDayPass(app.packageName) }
            
            if (hasAnyDayPass) {
                Log.d(TAG, "Day Pass is active. Skipping all monitoring and blocking logic.")
                if (packageName != null && packageName != currentForegroundApp) {
                    currentForegroundApp = packageName
                    stopUsageTracking()
                }
                return
            }

            // ブロック対象アプリかどうかを確認
            if (packageName != null && blockedApps.contains(packageName)) {
                Log.w(TAG, "Blocked app $packageName detected, blocking access")
                blockAppAccess(packageName)
                currentForegroundApp = packageName
                stopUsageTracking()
                return
            }
            
            if (packageName != null && packageName != currentForegroundApp) {
                Log.i(TAG, "Foreground app changed: $packageName")
                
                // 前のアプリの使用時間追跡を停止
                stopUsageTracking()
                
                // 制限中のアプリかどうかをチェック
                if (blockedApps.contains(packageName)) {
                    Log.w(TAG, "Blocked app $packageName detected, blocking access")
                    blockAppAccess(packageName)
                    currentForegroundApp = packageName
                    return
                }
                
                // 新しいアプリが監視対象かチェック
                val isMonitored = monitoredAppRepository.isAppMonitored(packageName)
                
                if (isMonitored) {
                    Log.i(TAG, "Monitored app detected: $packageName")
                    
                    val currentLimit = appUsageRepository.getCurrentLimit(packageName)
                    
                    if (currentLimit == Int.MAX_VALUE) {
                        Log.d(TAG, "App $packageName has unlimited usage, starting tracking")
                        startUsageTracking(packageName)
                        return
                    }
                    
                    if (appUsageRepository.isUsageExceededWithDayPass(packageName)) {
                        Log.w(TAG, "Usage limit reached for $packageName")
                        blockApp(packageName)
                        blockAppAccess(packageName)
                        currentForegroundApp = packageName
                    } else {
                        Log.i(TAG, "Usage within limit for $packageName, starting tracking")
                        startUsageTracking(packageName)
                    }
                } else {
                    Log.d(TAG, "Non-monitored app: $packageName")
                    currentForegroundApp = packageName
                }
            }
        }
    }

    /**
     * 使用時間追跡を開始
     */
    private fun startUsageTracking(packageName: String) {
        currentForegroundApp = packageName
        Log.i(TAG, "⏱️ Starting usage tracking for $packageName (interval: ${USAGE_TRACKING_INTERVAL}ms)")
        
        usageTrackingRunnable = object : Runnable {
            override fun run() {
                // 現在もそのアプリがフォアグラウンドにいるかチェック
                if (currentForegroundApp == packageName) {
                    Log.i(TAG, "⏰ Adding 1 minute usage for $packageName")
                    appUsageRepository.addUsageMinute(packageName)
                    
                    val newUsage = appUsageRepository.getTodayUsage(packageName)
                    val currentLimit = appUsageRepository.getCurrentLimit(packageName)
                    
                    Log.i(TAG, "📈 Usage updated for $packageName: $newUsage/$currentLimit minutes")
                    
                    // 制限を超えたかチェック
                    if (appUsageRepository.isUsageExceeded(packageName)) {
                        Log.w(TAG, "🚫 Usage limit reached for $packageName ($newUsage >= $currentLimit)")
                        blockApp(packageName)
                        blockAppAccess(packageName)
                    } else {
                        Log.d(TAG, "✅ Still within limit for $packageName, continuing tracking")
                        // まだ制限内なので継続追跡
                        handler.postDelayed(this, USAGE_TRACKING_INTERVAL)
                    }
                } else {
                    Log.d(TAG, "⏹️ App $packageName no longer in foreground, stopping tracking")
                }
            }
        }
        
        handler.postDelayed(usageTrackingRunnable!!, USAGE_TRACKING_INTERVAL)
    }

    /**
     * 使用時間追跡を停止
     */
    private fun stopUsageTracking() {
        usageTrackingRunnable?.let {
            handler.removeCallbacks(it)
            usageTrackingRunnable = null
        }
    }

    /**
     * アプリをブロック状態にする
     */
    private fun blockApp(packageName: String) {
        blockedApps.add(packageName)
        Log.i(TAG, "App $packageName added to blocked list")
    }
    
    /**
     * アプリのブロックを解除する
     */
    private fun unblockApp(packageName: String) {
        blockedApps.remove(packageName)
        Log.i(TAG, "App $packageName removed from blocked list")
    }

    /**
     * 制限超過アプリへのアクセスを完全にブロック
     */
    private fun blockAppAccess(packageName: String) {
        Log.w(TAG, "🚫 blockAppAccess called for $packageName - forcing return to home screen")
        
        // デバッグ: 現在の状態を確認
        Log.d(TAG, "🔍 Current foreground app: $currentForegroundApp")
        Log.d(TAG, "🔍 Blocked apps before: ${blockedApps.toList()}")
        
        // 即座にホーム画面に戻す
        Log.d(TAG, "🏠 Calling goToHomeScreen()...")
        goToHomeScreen()
        Log.d(TAG, "🏠 goToHomeScreen() completed")
        
        // 継続的な監視を開始（ユーザーがアプリに戻ろうとするのを防ぐ）
        Log.d(TAG, "🔄 Starting continuous blocking for $packageName...")
        startContinuousBlocking(packageName)
        Log.d(TAG, "🔄 startContinuousBlocking() completed")
    }
    
    /**
     * 継続的なブロック監視を開始
     */
    private fun startContinuousBlocking(packageName: String) {
        Log.d(TAG, "🔄 startContinuousBlocking called for $packageName")
        
        // 既存の継続ブロックがあれば停止
        stopContinuousBlocking()
        
        continuousBlockingRunnable = object : Runnable {
            override fun run() {
                // デイパス状態をチェック - いずれかの監視対象アプリでデイパスが有効なら継続ブロックを停止
                val monitoredApps = monitoredAppRepository.monitoredApps.value
                val hasAnyDayPass = monitoredApps.any { app -> appUsageRepository.hasDayPass(app.packageName) }
                
                if (hasAnyDayPass) {
                    Log.i(TAG, "🎉 Day Pass detected during continuous blocking. Stopping continuous blocking for $packageName.")
                    // ブロックリストからアプリを削除
                    unblockApp(packageName)
                    // 継続的ブロックを停止（このrunnableを再スケジュールしない）
                    return
                }

                Log.d(TAG, "🔄 Continuous blocking check for $packageName - current foreground: $currentForegroundApp")
                
                // ブロック対象アプリがフォアグラウンドにいるかチェック
                if (currentForegroundApp == packageName && blockedApps.contains(packageName)) {
                    Log.w(TAG, "🚫 Blocked app $packageName still in foreground, forcing home screen")
                    goToHomeScreen()
                } else {
                    Log.d(TAG, "🔄 Blocked app $packageName not in foreground (current: $currentForegroundApp) or not in blocked list (${blockedApps.contains(packageName)})")
                }
                
                // ブロック状態が続く限り継続監視
                if (blockedApps.contains(packageName)) {
                    Log.d(TAG, "🔄 Scheduling next continuous blocking check for $packageName in 1.5 seconds")
                    handler.postDelayed(this, 1500) // 1.5秒間隔で監視
                } else {
                    Log.d(TAG, "🔄 App $packageName no longer in blocked list, stopping continuous blocking")
                }
            }
        }
        
        handler.postDelayed(continuousBlockingRunnable!!, 1500)
        Log.d(TAG, "🔄 Started continuous blocking for $packageName")
    }
    
    /**
     * 継続的なブロック監視を停止
     */
    private fun stopContinuousBlocking() {
        continuousBlockingRunnable?.let {
            handler.removeCallbacks(it)
            continuousBlockingRunnable = null
            Log.d(TAG, "Stopped continuous blocking")
        }
    }

    /**
     * ホーム画面に戻す
     */
    private fun goToHomeScreen() {
        Log.d(TAG, "🏠 goToHomeScreen() called")
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP // FLAG_ACTIVITY_CLEAR_TOP を追加
            
            Log.d(TAG, "🏠 Starting home screen activity with intent: $homeIntent")
            startActivity(homeIntent)
            Log.d(TAG, "🏠 Successfully navigated to home screen")
        } catch (e: SecurityException) { // より具体的な例外をキャッチ
            Log.e(TAG, "🚨 Failed to navigate to home screen due to SecurityException. This might be due to restrictions on starting activities from background on some Android versions.", e)
            // TODO: ユーザーへの通知など、代替手段を検討
        } catch (e: Exception) {
            Log.e(TAG, "🚨 Failed to navigate to home screen due to generic Exception", e)
            // TODO: ユーザーへの通知など、代替手段を検討
        }
    }

    /**
     * デイパス購入後にアプリのブロックを解除
     */
    fun onDayPassPurchased(packageName: String) {
        unblockApp(packageName)
        
        // 継続的なブロック監視も停止
        if (blockedApps.isEmpty()) {
            stopContinuousBlocking()
        }
        
        Log.i(TAG, "Day pass purchased, unblocked app: $packageName")
    }

    /**
     * 全ての監視対象アプリのブロックを解除
     */
    fun onDayPassPurchasedForAllApps() {
        Log.i(TAG, "🎉 onDayPassPurchasedForAllApps called - starting block removal process")
        
        val monitoredApps = monitoredAppRepository.monitoredApps.value
        Log.i(TAG, "🎉 Monitored apps count: ${monitoredApps.size}")
        Log.i(TAG, "🎉 Current blocked apps before removal: ${blockedApps.toList()}")
        
        monitoredApps.forEach { app ->
            val wasBlocked = blockedApps.contains(app.packageName)
            unblockApp(app.packageName)
            Log.i(TAG, "🎉 Day pass purchased, unblocked app: ${app.packageName} (was blocked: $wasBlocked)")
            
            // デバッグ：デイパス状態を確認
            val hasDayPass = appUsageRepository.hasDayPass(app.packageName)
            val isExceeded = appUsageRepository.isUsageExceededWithDayPass(app.packageName)
            Log.i(TAG, "🎉 App ${app.packageName}: dayPass=$hasDayPass, exceeded=$isExceeded")
        }
        
        Log.i(TAG, "🎉 Blocked apps after removal: ${blockedApps.toList()}")
        
        // 継続的なブロック監視も停止
        stopContinuousBlocking()
        Log.i(TAG, "🎉 Stopped continuous blocking")
        
        Log.i(TAG, "🎉 Day pass purchased for all ${monitoredApps.size} monitored apps")
    }

    /**
     * 日次リセット時に全てのブロック状態をクリア
     */
    fun clearAllBlockedApps() {
        Log.i(TAG, "🔄 clearAllBlockedApps called - daily reset detected")
        Log.i(TAG, "🔄 Current blocked apps before reset: ${blockedApps.toList()}")
        
        // 全てのブロック状態をクリア
        blockedApps.clear()
        
        // 継続的なブロック監視も停止
        stopContinuousBlocking()
        
        Log.i(TAG, "🔄 All blocked apps cleared due to daily reset")
        Log.i(TAG, "🔄 Blocked apps after reset: ${blockedApps.toList()}")
        Log.i(TAG, "🔄 Users can now access all apps again")
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
        stopUsageTracking()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i(TAG, "🛑 Accessibility service destroyed - delegating to SecurityManager")
        
        try {
            // SecurityManagerに処理を委譲
            securityManager.handleAccessibilityDisabled()
            Log.i(TAG, "✅ SecurityManager.handleAccessibilityDisabled() called successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to call SecurityManager.handleAccessibilityDisabled()", e)
        }
        
        stopUsageTracking()
        stopContinuousBlocking()
        stopDailyResetChecker()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "🔧 Accessibility service disconnecting")
        
        // 他のリソースクリーンアップ
        stopUsageTracking()
        stopContinuousBlocking()
        stopDailyResetChecker()
        
        instance = null
        return super.onUnbind(intent)
    }

    /**
     * 日次リセットの定期チェックを開始
     */
    private fun startDailyResetChecker() {
        Log.i(TAG, "Starting daily reset checker (interval: ${DAILY_RESET_CHECK_INTERVAL}ms)")
        
        dailyResetCheckRunnable = object : Runnable {
            override fun run() {
                Log.d(TAG, "Performing scheduled daily reset check")
                try {
                    appUsageRepository.performDailyReset()
                    Log.d(TAG, "Daily reset check completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during daily reset check", e)
                }
                
                // 次のチェックをスケジュール
                handler.postDelayed(this, DAILY_RESET_CHECK_INTERVAL)
            }
        }
        
        handler.postDelayed(dailyResetCheckRunnable!!, DAILY_RESET_CHECK_INTERVAL)
        Log.i(TAG, "Daily reset checker started")
    }

    /**
     * 日次リセットの定期チェックを停止
     */
    private fun stopDailyResetChecker() {
        dailyResetCheckRunnable?.let {
            handler.removeCallbacks(it)
            dailyResetCheckRunnable = null
            Log.d(TAG, "Stopped daily reset checker")
        }
    }
} 