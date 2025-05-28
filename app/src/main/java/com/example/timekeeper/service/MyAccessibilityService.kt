package com.example.timekeeper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.timekeeper.data.AppUsageRepository
import com.example.timekeeper.data.MonitoredAppRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MyAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var appUsageRepository: AppUsageRepository
    
    @Inject
    lateinit var monitoredAppRepository: MonitoredAppRepository
    
    private val handler = Handler(Looper.getMainLooper())
    private var currentForegroundApp: String? = null
    private var usageTrackingRunnable: Runnable? = null
    private val blockedApps = mutableSetOf<String>() // 制限中のアプリ一覧
    private var continuousBlockingRunnable: Runnable? = null

    companion object {
        private const val TAG = "MyAccessibilityService"
        private const val USAGE_TRACKING_INTERVAL = 10000L // テスト用: 10秒間隔
        
        // サービスのstatic参照
        @Volatile
        private var instance: MyAccessibilityService? = null
        
        fun getInstance(): MyAccessibilityService? = instance
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
        
        // デバッグ用：監視対象アプリを確認
        val monitoredApps = monitoredAppRepository.monitoredApps.value
        Log.d(TAG, "Service connected - Monitored apps: ${monitoredApps.size}")
        monitoredApps.forEach { app ->
            Log.d(TAG, "Monitored app: ${app.appName} (${app.packageName})")
            
            // 既に制限超過しているアプリをブロック
            if (appUsageRepository.isUsageExceededWithDayPass(app.packageName)) {
                blockApp(app.packageName)
                Log.i(TAG, "Pre-blocked app due to usage exceeded: ${app.packageName}")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null && packageName != currentForegroundApp) {
                Log.d(TAG, "Foreground app changed: $packageName")
                
                // Timekeeperアプリ自体は監視対象外
                if (packageName == "com.example.timekeeper") {
                    Log.d(TAG, "Timekeeper app detected, skipping monitoring")
                    currentForegroundApp = packageName
                    stopUsageTracking()
                    return
                }
                
                // 前のアプリの使用時間追跡を停止
                stopUsageTracking()
                
                // 制限中のアプリかどうかを最初にチェック
                if (blockedApps.contains(packageName)) {
                    Log.w(TAG, "Blocked app $packageName detected, immediately blocking access")
                    blockAppAccess(packageName)
                    return
                }
                
                // 新しいアプリが監視対象かチェック
                if (monitoredAppRepository.isAppMonitored(packageName)) {
                    Log.d(TAG, "Monitored app detected: $packageName")
                    
                    // 今日の使用時間と制限を確認
                    val todayUsage = appUsageRepository.getTodayUsage(packageName)
                    val currentLimit = appUsageRepository.getCurrentLimit(packageName)
                    
                    Log.d(TAG, "App $packageName: usage=$todayUsage minutes, limit=$currentLimit minutes")
                    
                    // 制限が無制限（Int.MAX_VALUE）の場合はスキップ
                    if (currentLimit == Int.MAX_VALUE) {
                        Log.d(TAG, "App $packageName has unlimited usage, starting tracking")
                        startUsageTracking(packageName)
                        return
                    }
                    
                    // 使用時間が制限を超えているかチェック
                    if (appUsageRepository.isUsageExceededWithDayPass(packageName)) {
                        Log.i(TAG, "Usage limit reached for $packageName ($todayUsage >= $currentLimit)")
                        blockApp(packageName)
                        blockAppAccess(packageName)
                    } else {
                        Log.d(TAG, "Usage within limit for $packageName ($todayUsage < $currentLimit), starting tracking")
                        // 制限内の場合は使用時間追跡を開始
                        startUsageTracking(packageName)
                    }
                } else {
                    // 監視対象外のアプリの場合
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
        
        usageTrackingRunnable = object : Runnable {
            override fun run() {
                // 1分経過したので使用時間を追加
                Log.d(TAG, "Adding 1 minute usage for $packageName")
                appUsageRepository.addUsageMinute(packageName)
                
                val newUsage = appUsageRepository.getTodayUsage(packageName)
                val currentLimit = appUsageRepository.getCurrentLimit(packageName)
                
                Log.i(TAG, "Usage updated for $packageName: $newUsage/$currentLimit minutes")
                
                // 制限を超えたかチェック
                if (appUsageRepository.isUsageExceeded(packageName)) {
                    Log.i(TAG, "Usage limit reached for $packageName ($newUsage >= $currentLimit)")
                    blockApp(packageName)
                    blockAppAccess(packageName)
                } else {
                    Log.d(TAG, "Still within limit for $packageName, continuing tracking")
                    // まだ制限内なので継続追跡
                    handler.postDelayed(this, USAGE_TRACKING_INTERVAL)
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
        Log.w(TAG, "Blocking access to $packageName - forcing return to home screen")
        
        // 即座にホーム画面に戻す
        goToHomeScreen()
        
        // 継続的な監視を開始（ユーザーがアプリに戻ろうとするのを防ぐ）
        startContinuousBlocking(packageName)
    }
    
    /**
     * 継続的なブロック監視を開始
     */
    private fun startContinuousBlocking(packageName: String) {
        // 既存の継続ブロックがあれば停止
        stopContinuousBlocking()
        
        continuousBlockingRunnable = object : Runnable {
            override fun run() {
                // ブロック対象アプリがフォアグラウンドにいるかチェック
                if (currentForegroundApp == packageName && blockedApps.contains(packageName)) {
                    Log.w(TAG, "Blocked app $packageName still in foreground, forcing home screen")
                    goToHomeScreen()
                }
                
                // ブロック状態が続く限り継続監視
                if (blockedApps.contains(packageName)) {
                    handler.postDelayed(this, 1500) // 1.5秒間隔で監視
                }
            }
        }
        
        handler.postDelayed(continuousBlockingRunnable!!, 1500)
        Log.d(TAG, "Started continuous blocking for $packageName")
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
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(homeIntent)
            Log.d(TAG, "Navigated to home screen")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to navigate to home screen", e)
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
        val monitoredApps = monitoredAppRepository.monitoredApps.value
        monitoredApps.forEach { app ->
            unblockApp(app.packageName)
            Log.i(TAG, "Day pass purchased, unblocked app: ${app.packageName}")
        }
        
        // 継続的なブロック監視も停止
        stopContinuousBlocking()
        
        Log.i(TAG, "Day pass purchased for all ${monitoredApps.size} monitored apps")
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
        stopUsageTracking()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i(TAG, "Accessibility service destroyed")
        stopUsageTracking()
        stopContinuousBlocking()
    }
} 