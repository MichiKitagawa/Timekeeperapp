package com.example.timekeeper.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timekeeper.data.MonitoredAppRepository
import com.example.timekeeper.data.AppUsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val monitoredAppRepository: MonitoredAppRepository,
    private val appUsageRepository: AppUsageRepository
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

    // 監視対象アプリと使用状況を組み合わせたデータ
    val appUsageInfoList: StateFlow<List<AppUsageInfo>> = combine(
        monitoredAppRepository.monitoredApps,
        appUsageRepository.usageData
    ) { monitoredApps, usageData ->
        Log.i(TAG, "=== Data combination triggered ===")
        Log.d(TAG, "Monitored apps: ${monitoredApps.size}, Usage data entries: ${usageData.size}")
        
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
                Log.d(TAG, "Debug - Usage data: $packageName = ${data.todayUsageMinutes}/${data.currentLimitMinutes}")
            }
        }
        

    }
} 