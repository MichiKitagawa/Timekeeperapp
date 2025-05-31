package com.example.timekeeper.util

import android.content.Context
import android.util.Log
import com.example.timekeeper.data.PurchaseStateManager
import com.example.timekeeper.data.MonitoredAppRepository
import com.example.timekeeper.data.AppUsageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val purchaseStateManager: PurchaseStateManager,
    private val monitoredAppRepository: MonitoredAppRepository,
    private val appUsageRepository: AppUsageRepository,
    private val heartbeatLogger: HeartbeatLogger
) {
    companion object {
        private const val TAG = "SecurityManager"
    }

    /**
     * バックグラウンドでの強制初期化処理
     * 不正検知時に即座に実行される
     */
    fun performBackgroundDataReset(reason: String) {
        try {
            Log.w(TAG, "🚨 Performing BACKGROUND data reset. Reason: $reason")
            
            // デバイスIDを保存（初期化後も保持）
            val timekeeperPrefs = context.getSharedPreferences("TimekeeperPrefs", Context.MODE_PRIVATE)
            val deviceId = timekeeperPrefs.getString("DEVICE_ID", null)
            
            // 全SharedPreferencesをクリア
            val prefsToClear = listOf(
                "TimekeeperPrefs", 
                "monitored_apps", 
                "app_usage", 
                "heartbeat_log", 
                "purchase_state"
            )
            
            prefsToClear.forEach { prefName ->
                context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    .edit().clear().apply()
                Log.d(TAG, "Cleared SharedPreferences: $prefName")
            }
            
            // Repository データもクリア
            try {
                purchaseStateManager.clearPurchaseState()
                monitoredAppRepository.clearAllData()
                appUsageRepository.clearAllData()
                heartbeatLogger.clearHeartbeatHistory()
                Log.d(TAG, "Repository data cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing repository data", e)
            }
            
            // デバイスIDを復元
            if (deviceId != null) {
                timekeeperPrefs.edit().putString("DEVICE_ID", deviceId).apply()
                Log.d(TAG, "Device ID restored: $deviceId")
            }
            
            Log.w(TAG, "🚨 BACKGROUND data reset completed successfully. Reason: $reason")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during background data reset", e)
        }
    }
    
    /**
     * アクセシビリティサービス無効化検知時の処理
     */
    fun handleAccessibilityDisabled() {
        Log.w(TAG, "🚨 Accessibility service disabled - initiating background reset")
        performBackgroundDataReset("Accessibility service disabled")
    }
    
    /**
     * ハートビートギャップ検知時の処理
     */
    fun handleHeartbeatGap(gapMinutes: Long) {
        Log.w(TAG, "🚨 Heartbeat gap detected: ${gapMinutes} minutes - initiating background reset")
        performBackgroundDataReset("Heartbeat gap: ${gapMinutes} minutes")
    }
} 