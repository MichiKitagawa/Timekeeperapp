package com.example.timekeeper.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timekeeper.data.MonitoredAppRepository
import com.example.timekeeper.data.AppUsageRepository
import com.example.timekeeper.data.AccessibilityServiceMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val monitoredAppRepository: MonitoredAppRepository,
    private val appUsageRepository: AppUsageRepository,
    private val accessibilityServiceMonitor: AccessibilityServiceMonitor
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"
    }

    data class AppUsageInfo(
        val packageName: String,
        val appName: String,
        val usedMinutes: Int,
        val limitMinutes: Int,
        val hasDayPass: Boolean = false
    )

    // アクセシビリティサービスの状態
    val isAccessibilityServiceEnabled = accessibilityServiceMonitor.isServiceEnabled

    // 監視対象アプリと使用状況を組み合わせたデータ
    val appUsageInfoList: StateFlow<List<AppUsageInfo>> = combine(
        monitoredAppRepository.monitoredApps,
        appUsageRepository.usageData,
        accessibilityServiceMonitor.isServiceEnabled
    ) { monitoredApps, usageData, isServiceEnabled ->
        Log.i(TAG, "=== Data combination triggered ===")
        Log.d(TAG, "Monitored apps: ${monitoredApps.size}, Usage data entries: ${usageData.size}, Service enabled: $isServiceEnabled")
        
        val result = monitoredApps.map { monitoredApp ->
            val usageInfo = usageData[monitoredApp.packageName]
            val usedMinutes = usageInfo?.todayUsageMinutes ?: 0
            val limitMinutes = monitoredApp.currentLimitMinutes
            val hasDayPass = appUsageRepository.hasDayPass(monitoredApp.packageName)
            
            Log.i(TAG, "App ${monitoredApp.appName} (${monitoredApp.packageName}): used=$usedMinutes, limit=$limitMinutes, dayPass=$hasDayPass")
            
            AppUsageInfo(
                packageName = monitoredApp.packageName,
                appName = monitoredApp.appName,
                usedMinutes = usedMinutes,
                limitMinutes = limitMinutes,
                hasDayPass = hasDayPass
            )
        }
        
        Log.i(TAG, "=== Data combination completed: ${result.size} apps ===")
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        Log.d(TAG, "DashboardViewModel initialized")
        
        // アクセシビリティサービスの状態変化を監視
        viewModelScope.launch {
            accessibilityServiceMonitor.isServiceEnabled.collect { isEnabled ->
                Log.i(TAG, "Accessibility service status changed: $isEnabled")
                
                if (!isEnabled) {
                    Log.w(TAG, "⚠️ Accessibility service disabled - forcing data refresh")
                    // データを強制的に再読み込み
                    refreshData()
                }
            }
        }
        
        // デバッグ用：データの状態を定期的にログ出力
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // 2秒待機
            val monitoredApps = monitoredAppRepository.monitoredApps.value
            val usageData = appUsageRepository.usageData.value
            Log.d(TAG, "Debug - Monitored apps: ${monitoredApps.size}")
            monitoredApps.forEach { app ->
                Log.d(TAG, "Debug - Monitored app: ${app.appName} (${app.packageName})")
            }
            Log.d(TAG, "Debug - Usage data entries: ${usageData.size}")
            usageData.forEach { (packageName, data) ->
                Log.d(TAG, "Debug - Usage data: $packageName = ${data.todayUsageMinutes}/${data.currentLimitMinutes} minutes")
            }
        }
    }

    /**
     * データを強制的に再読み込み
     */
    private fun refreshData() {
        viewModelScope.launch {
            try {
                // MonitoredAppRepositoryのデータを再読み込み
                monitoredAppRepository.loadMonitoredApps()
                Log.i(TAG, "✅ Monitored apps data refreshed")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to refresh data", e)
            }
        }
    }

    /**
     * アクセシビリティサービスの状態を強制チェック
     */
    fun forceCheckAccessibilityService() {
        accessibilityServiceMonitor.forceCheck()
    }
} 