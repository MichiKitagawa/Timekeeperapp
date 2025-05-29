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
        android.util.Log.d("AppUsageRepository", "AppUsageRepository initialized")
        
        // 初期化時に強制制限状態をチェック・クリア
        checkAndClearForceBlocks()
        
        loadUsageData()
    }
    
    /**
     * 初期化時に強制制限状態をチェック・クリア（デバッグ用）
     */
    private fun checkAndClearForceBlocks() {
        // 新しい実装では強制制限は使用しないため、この処理は不要
        android.util.Log.i("AppUsageRepository", "Force block check skipped - using new security model")
    }
    
    /**
     * アプリの使用時間が制限を超えているかチェック
     */
    fun isUsageExceeded(packageName: String): Boolean {
        // まず監視対象アプリかどうかをチェック
        val monitoredApps = getMonitoredApps()
        if (!monitoredApps.contains(packageName)) {
            android.util.Log.d("AppUsageRepository", 
                "App $packageName is not monitored, no usage limit")
            return false
        }
        
        val today = dateFormat.format(Date())
        val usageKey = "${packageName}_usage_$today"
        
        // MonitoredAppRepositoryのSharedPreferencesから制限値を取得
        val monitoredAppPrefs = context.getSharedPreferences("monitored_apps", Context.MODE_PRIVATE)
        val currentLimit = monitoredAppPrefs.getInt("${packageName}_current_limit", -1)
        
        val todayUsage = prefs.getInt(usageKey, 0)
        
        // 制限が設定されていない場合（-1）は制限なし
        if (currentLimit == -1) {
            android.util.Log.d("AppUsageRepository", 
                "No limit set for monitored app $packageName in monitored_apps prefs")
            return false
        }
        
        val isExceeded = todayUsage >= currentLimit
        
        android.util.Log.d("AppUsageRepository", 
            "Usage check for $packageName: usage=$todayUsage, limit=$currentLimit, exceeded=$isExceeded (from monitored_apps prefs)")
        
        return isExceeded
    }
    
    /**
     * アプリの使用時間を1分追加
     */
    fun addUsageMinute(packageName: String) {
        val today = dateFormat.format(Date())
        val usageKey = "${packageName}_usage_$today"
        val currentUsage = prefs.getInt(usageKey, 0)
        val newUsage = currentUsage + 1
        
        android.util.Log.i("AppUsageRepository", 
            "Adding usage minute for $packageName: $currentUsage -> $newUsage")
        
        prefs.edit()
            .putInt(usageKey, newUsage)
            .putString("${packageName}_last_used", today)
            .apply()
        
        android.util.Log.d("AppUsageRepository", 
            "SharedPreferences updated for $packageName, calling loadUsageData()")
        
        loadUsageData()
        
        // StateFlowの更新を確認
        val updatedData = _usageData.value[packageName]
        android.util.Log.i("AppUsageRepository", 
            "StateFlow updated for $packageName: ${updatedData?.todayUsageMinutes} minutes")
    }
    
    /**
     * アプリの現在の制限時間を取得
     */
    fun getCurrentLimit(packageName: String): Int {
        // まず監視対象アプリかどうかをチェック
        val monitoredApps = getMonitoredApps()
        if (!monitoredApps.contains(packageName)) {
            android.util.Log.d("AppUsageRepository", 
                "App $packageName is not monitored, returning unlimited")
            return Int.MAX_VALUE
        }
        
        // MonitoredAppRepositoryのSharedPreferencesから制限値を取得
        val monitoredAppPrefs = context.getSharedPreferences("monitored_apps", Context.MODE_PRIVATE)
        val limit = monitoredAppPrefs.getInt("${packageName}_current_limit", -1)
        
        // 監視対象として設定されているが制限が設定されていない場合
        if (limit == -1) {
            android.util.Log.w("AppUsageRepository", 
                "Monitored app $packageName has no limit set in monitored_apps prefs, returning unlimited")
            return Int.MAX_VALUE
        }
        
        android.util.Log.d("AppUsageRepository", 
            "Current limit for $packageName: $limit minutes (from monitored_apps prefs)")
        return limit
    }
    
    /**
     * アプリの今日の使用時間を取得
     */
    fun getTodayUsage(packageName: String): Int {
        val today = dateFormat.format(Date())
        return prefs.getInt("${packageName}_usage_$today", 0)
    }
    
    /**
     * アプリの今日の使用時間をリセット
     */
    fun resetTodayUsage(packageName: String) {
        val today = dateFormat.format(Date())
        val usageKey = "${packageName}_usage_$today"
        
        prefs.edit()
            .putInt(usageKey, 0)
            .apply()
        
        android.util.Log.d("AppUsageRepository", 
            "Reset today usage for $packageName")
        
        loadUsageData()
    }
    
    /**
     * デイパスを購入（今日の制限を無制限にする）
     */
    fun purchaseDayPass(packageName: String) {
        val today = dateFormat.format(Date())
        val dayPassKey = "${packageName}_day_pass_$today"
        
        prefs.edit()
            .putBoolean(dayPassKey, true)
            .apply()
        
        android.util.Log.i("AppUsageRepository", 
            "Day pass purchased for $packageName on $today")
        
        loadUsageData()
    }
    
    /**
     * 全ての監視対象アプリにデイパスを適用
     */
    fun purchaseDayPassForAllApps() {
        val today = dateFormat.format(Date())
        val monitoredApps = getMonitoredApps()
        
        val editor = prefs.edit()
        monitoredApps.forEach { packageName ->
            val dayPassKey = "${packageName}_day_pass_$today"
            editor.putBoolean(dayPassKey, true)
            android.util.Log.i("AppUsageRepository", 
                "Day pass applied to $packageName on $today")
        }
        editor.apply()
        
        android.util.Log.i("AppUsageRepository", 
            "Day pass purchased for all ${monitoredApps.size} monitored apps on $today")
        
        loadUsageData()
    }
    
    /**
     * デイパスが購入されているかチェック
     */
    fun hasDayPass(packageName: String): Boolean {
        val today = dateFormat.format(Date())
        val dayPassKey = "${packageName}_day_pass_$today"
        return prefs.getBoolean(dayPassKey, false)
    }
    
    /**
     * アプリの使用時間が制限を超えているかチェック（デイパス考慮）
     */
    fun isUsageExceededWithDayPass(packageName: String): Boolean {
        // デイパスが購入されている場合は制限なし
        if (hasDayPass(packageName)) {
            android.util.Log.d("AppUsageRepository", 
                "Day pass active for $packageName, no limit")
            return false
        }
        
        return isUsageExceeded(packageName)
    }
    
    /**
     * 全ての監視対象アプリの今日の使用時間をリセット
     */
    fun resetAllTodayUsage() {
        val monitoredApps = getMonitoredApps()
        monitoredApps.forEach { packageName ->
            resetTodayUsage(packageName)
        }
        
        android.util.Log.d("AppUsageRepository", 
            "Reset today usage for all monitored apps")
    }
    
    /**
     * 日次リセット処理（0時に実行）
     */
    fun performDailyReset() {
        val today = dateFormat.format(Date())
        val lastResetDate = prefs.getString("last_reset_date", "")
        
        android.util.Log.d("AppUsageRepository", "performDailyReset called - today: $today, lastReset: $lastResetDate")
        
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
                    android.util.Log.d("AppUsageRepository", "Reduced limit for $packageName from $currentLimit to $newLimit")
                }
            }
            
            prefs.edit()
                .putString("last_reset_date", today)
                .apply()
            
            android.util.Log.d("AppUsageRepository", "Daily reset completed")
            loadUsageData()
        }
    }
    
    /**
     * 監視対象アプリ一覧を取得
     */
    private fun getMonitoredApps(): Set<String> {
        // MonitoredAppRepository専用のSharedPreferencesから取得
        val monitoredAppPrefs = context.getSharedPreferences("monitored_apps", Context.MODE_PRIVATE)
        return monitoredAppPrefs.getStringSet("monitored_apps", emptySet()) ?: emptySet()
    }
    
    /**
     * 使用データを読み込み
     */
    private fun loadUsageData() {
        val today = dateFormat.format(Date())
        val monitoredApps = getMonitoredApps()
        val usageMap = mutableMapOf<String, AppUsageData>()
        
        // MonitoredAppRepositoryのSharedPreferencesを取得
        val monitoredAppPrefs = context.getSharedPreferences("monitored_apps", Context.MODE_PRIVATE)
        
        android.util.Log.d("AppUsageRepository", "loadUsageData called - today: $today, monitored apps: ${monitoredApps.size}")
        
        monitoredApps.forEach { packageName ->
            val todayUsage = getTodayUsage(packageName)
            val currentLimit = monitoredAppPrefs.getInt("${packageName}_current_limit", Int.MAX_VALUE)
            val lastUsed = prefs.getString("${packageName}_last_used", today) ?: today
            
            android.util.Log.d("AppUsageRepository", "Loading data for $packageName: usage=$todayUsage, limit=$currentLimit (from monitored_apps prefs)")
            
            usageMap[packageName] = AppUsageData(
                packageName = packageName,
                todayUsageMinutes = todayUsage,
                currentLimitMinutes = currentLimit,
                lastUsedDate = lastUsed
            )
        }
        
        android.util.Log.i("AppUsageRepository", "Updating StateFlow: ${_usageData.value.size} -> ${usageMap.size} entries")
        _usageData.value = usageMap
        android.util.Log.i("AppUsageRepository", "StateFlow updated successfully with ${usageMap.size} entries")
        
        // 各エントリの詳細をログ出力
        usageMap.forEach { (packageName, data) ->
            android.util.Log.d("AppUsageRepository", "StateFlow entry: $packageName = ${data.todayUsageMinutes}/${data.currentLimitMinutes} minutes")
        }
    }
    
    /**
     * 全ての使用データをクリア（デバッグ用）
     */
    fun clearAllData() {
        android.util.Log.d("AppUsageRepository", "Clearing all usage data")
        prefs.edit().clear().apply()
        _usageData.value = emptyMap()
        android.util.Log.d("AppUsageRepository", "All usage data cleared")
    }
} 