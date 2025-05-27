package com.example.timekeeper.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageRepository @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_usage", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private val _usageData = MutableStateFlow<Map<String, AppUsageData>>(emptyMap())
    val usageData: StateFlow<Map<String, AppUsageData>> = _usageData
    
    data class AppUsageData(
        val packageName: String,
        val todayUsageMinutes: Int,
        val currentLimitMinutes: Int,
        val lastUsedDate: String
    )
    
    init {
        loadUsageData()
    }
    
    /**
     * アプリの使用時間が制限を超えているかチェック
     */
    fun isUsageExceeded(packageName: String): Boolean {
        val today = dateFormat.format(Date())
        val usageKey = "${packageName}_usage_$today"
        val limitKey = "${packageName}_current_limit"
        
        val todayUsage = prefs.getInt(usageKey, 0)
        val currentLimit = prefs.getInt(limitKey, Int.MAX_VALUE)
        
        return todayUsage >= currentLimit
    }
    
    /**
     * アプリの使用時間を1分追加
     */
    fun addUsageMinute(packageName: String) {
        val today = dateFormat.format(Date())
        val usageKey = "${packageName}_usage_$today"
        val currentUsage = prefs.getInt(usageKey, 0)
        
        prefs.edit()
            .putInt(usageKey, currentUsage + 1)
            .putString("${packageName}_last_used", today)
            .apply()
        
        loadUsageData()
    }
    
    /**
     * アプリの現在の制限時間を取得
     */
    fun getCurrentLimit(packageName: String): Int {
        return prefs.getInt("${packageName}_current_limit", Int.MAX_VALUE)
    }
    
    /**
     * アプリの今日の使用時間を取得
     */
    fun getTodayUsage(packageName: String): Int {
        val today = dateFormat.format(Date())
        return prefs.getInt("${packageName}_usage_$today", 0)
    }
    
    /**
     * 日次リセット処理（0時に実行）
     */
    fun performDailyReset() {
        val today = dateFormat.format(Date())
        val lastResetDate = prefs.getString("last_reset_date", "")
        
        if (lastResetDate != today) {
            // 監視対象アプリの制限時間を1分ずつ減らす
            val monitoredApps = getMonitoredApps()
            monitoredApps.forEach { packageName ->
                val currentLimit = getCurrentLimit(packageName)
                val targetLimit = prefs.getInt("${packageName}_target_limit", currentLimit)
                
                if (currentLimit > targetLimit) {
                    val newLimit = maxOf(currentLimit - 1, targetLimit)
                    prefs.edit()
                        .putInt("${packageName}_current_limit", newLimit)
                        .apply()
                }
            }
            
            prefs.edit()
                .putString("last_reset_date", today)
                .apply()
            
            loadUsageData()
        }
    }
    
    /**
     * 監視対象アプリ一覧を取得
     */
    private fun getMonitoredApps(): Set<String> {
        return prefs.getStringSet("monitored_apps", emptySet()) ?: emptySet()
    }
    
    /**
     * 使用データを読み込み
     */
    private fun loadUsageData() {
        val today = dateFormat.format(Date())
        val monitoredApps = getMonitoredApps()
        val usageMap = mutableMapOf<String, AppUsageData>()
        
        monitoredApps.forEach { packageName ->
            val todayUsage = getTodayUsage(packageName)
            val currentLimit = getCurrentLimit(packageName)
            val lastUsed = prefs.getString("${packageName}_last_used", today) ?: today
            
            usageMap[packageName] = AppUsageData(
                packageName = packageName,
                todayUsageMinutes = todayUsage,
                currentLimitMinutes = currentLimit,
                lastUsedDate = lastUsed
            )
        }
        
        _usageData.value = usageMap
    }
} 