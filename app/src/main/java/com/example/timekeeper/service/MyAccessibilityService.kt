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
import com.example.timekeeper.ui.lock.LockScreenActivity
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
    private var isLockScreenShowing = false

    companion object {
        private const val TAG = "MyAccessibilityService"
        private const val USAGE_TRACKING_INTERVAL = 60000L // 1分間隔
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")
        
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
        
        // 日次リセット処理を実行
        appUsageRepository.performDailyReset()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null && packageName != currentForegroundApp) {
                Log.d(TAG, "Foreground app changed: $packageName")
                
                // 前のアプリの使用時間追跡を停止
                stopUsageTracking()
                
                // 新しいアプリが監視対象かチェック
                if (monitoredAppRepository.isAppMonitored(packageName)) {
                    Log.d(TAG, "Monitored app detected: $packageName")
                    
                    // 使用時間が制限を超えているかチェック
                    if (appUsageRepository.isUsageExceeded(packageName)) {
                        Log.i(TAG, "Usage exceeded for $packageName, showing lock screen")
                        showLockScreen(packageName)
                    } else {
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
                appUsageRepository.addUsageMinute(packageName)
                Log.d(TAG, "Added 1 minute usage for $packageName")
                
                // 制限を超えたかチェック
                if (appUsageRepository.isUsageExceeded(packageName)) {
                    Log.i(TAG, "Usage limit reached for $packageName")
                    showLockScreen(packageName)
                } else {
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
     * ロック画面を表示
     */
    private fun showLockScreen(packageName: String) {
        if (isLockScreenShowing) {
            Log.d(TAG, "Lock screen already showing, skipping")
            return
        }
        
        isLockScreenShowing = true
        stopUsageTracking()
        
        val intent = Intent(this, LockScreenActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("locked_app_package", packageName)
        startActivity(intent)
        
        Log.i(TAG, "Lock screen started for $packageName")
    }
    
    /**
     * ロック画面が閉じられた時に呼ばれる
     */
    fun onLockScreenDismissed() {
        isLockScreenShowing = false
        Log.d(TAG, "Lock screen dismissed")
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
        stopUsageTracking()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Accessibility service destroyed")
        stopUsageTracking()
    }
} 