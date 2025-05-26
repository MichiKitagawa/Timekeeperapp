package com.example.timekeeper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.timekeeper.ui.lock.LockScreenActivity
// import com.example.timekeeper.data.AppUsageRepository // 仮。実際には使用状況を管理するリポジトリ
// import com.example.timekeeper.data.MonitoredAppRepository // 仮。実際には監視対象アプリを管理するリポジトリ

class MyAccessibilityService : AccessibilityService() {

    // private lateinit var appUsageRepository: AppUsageRepository
    // private lateinit var monitoredAppRepository: MonitoredAppRepository

    override fun onServiceConnected() {
        super.onServiceConnected()
        // appUsageRepository = AppUsageRepository(applicationContext)
        // monitoredAppRepository = MonitoredAppRepository(applicationContext)

        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null) {
                // Log.d("MyAccessibilityService", "Foreground app: $packageName")

                // val isMonitored = monitoredAppRepository.isAppMonitored(packageName)
                // val usageExceeded = appUsageRepository.isUsageExceeded(packageName)

                // 仮のロジック: "com.example.targetapp" の場合にロック画面を表示
                val isMonitoredAndExceeded = packageName == "com.example.targetapp" // この部分は実際の判定ロジックに置き換える

                if (isMonitoredAndExceeded) {
                    // 既にロック画面が表示されているかなどをチェックするロジックも必要
                    val intent = Intent(this, LockScreenActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    // intent.putExtra("locked_app_package", packageName) // 必要に応じて情報を渡す
                    startActivity(intent)
                }
            }
        }
    }

    override fun onInterrupt() {
        // サービスが中断されたときの処理
    }
} 