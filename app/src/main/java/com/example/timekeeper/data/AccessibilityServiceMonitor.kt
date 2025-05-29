package com.example.timekeeper.data

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityServiceMonitor @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AccessibilityServiceMonitor"
        private const val CHECK_INTERVAL = 3000L // 3秒間隔でチェック
        private const val SERVICE_NAME = "com.example.timekeeper/com.example.timekeeper.service.MyAccessibilityService"
    }

    private val _isServiceEnabled = MutableStateFlow(false)
    val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled

    private var monitoringJob: Job? = null
    private val monitoringScope = CoroutineScope(Dispatchers.IO)

    init {
        // 初期状態をチェック
        val initialStatus = checkServiceStatus()
        Log.i(TAG, "Initial accessibility service status: $initialStatus")
        
        // 継続的な監視を開始
        startMonitoring()
    }

    /**
     * アクセシビリティサービスが有効かどうかをチェック
     */
    private fun checkServiceStatus(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            val isEnabled = !TextUtils.isEmpty(enabledServices) && 
                           enabledServices.contains(SERVICE_NAME)
            
            Log.d(TAG, "Accessibility service status: enabled=$isEnabled")
            
            // 現在の状態を更新
            val previousState = _isServiceEnabled.value
            _isServiceEnabled.value = isEnabled
            
            // 状態変化をログ出力
            if (previousState != isEnabled) {
                Log.i(TAG, "Accessibility service status changed: $previousState -> $isEnabled")
            }
            
            isEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check accessibility service status", e)
            false
        }
    }

    /**
     * 継続的な監視を開始
     */
    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = monitoringScope.launch {
            while (true) {
                checkServiceStatus()
                delay(CHECK_INTERVAL)
            }
        }
        Log.i(TAG, "Started continuous monitoring")
    }

    /**
     * 監視を停止
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        Log.i(TAG, "Stopped monitoring")
    }

    /**
     * 現在のサービス状態を強制的にチェック
     */
    fun forceCheck(): Boolean {
        return checkServiceStatus()
    }
} 