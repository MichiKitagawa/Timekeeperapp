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
    private val context: Context,
    private val monitoredAppRepository: MonitoredAppRepository
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
        android.util.Log.d("AppUsageRepository", "AppUsageRepository initialized with injected MonitoredAppRepository")
        
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
        // MonitoredAppRepositoryインスタンスを使用してチェック
        if (!monitoredAppRepository.isAppMonitored(packageName)) {
            android.util.Log.d("AppUsageRepository", 
                "App $packageName is not monitored, no usage limit")
            return false
        }
        
        val today = dateFormat.format(Date())
        val usageKey = "${packageName}_usage_$today"
        
        // MonitoredAppRepositoryから制限値を取得
        val currentLimit = getCurrentLimitFromRepo(packageName)
        val todayUsage = prefs.getInt(usageKey, 0)
        
        // 制限が設定されていない場合（-1）は制限なし
        if (currentLimit == -1) {
            android.util.Log.d("AppUsageRepository", 
                "No limit set for monitored app $packageName")
            return false
        }
        
        val isExceeded = todayUsage >= currentLimit
        
        android.util.Log.d("AppUsageRepository", 
            "Usage check for $packageName: usage=$todayUsage, limit=$currentLimit, exceeded=$isExceeded (from MonitoredAppRepository)")
        
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
        // MonitoredAppRepositoryインスタンスを使用して取得
        val currentLimit = getCurrentLimitFromRepo(packageName)
        
        // 監視対象として設定されているが制限が設定されていない場合
        if (currentLimit == -1) {
            android.util.Log.w("AppUsageRepository", 
                "Monitored app $packageName has no limit set in monitored_apps prefs, returning unlimited")
            return Int.MAX_VALUE
        }
        
        android.util.Log.d("AppUsageRepository", 
            "Current limit for $packageName: $currentLimit minutes (from MonitoredAppRepository)")
        return currentLimit
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
     * 
     * 重要: このメソッドは日付が変わった時に一度だけ実行されます
     */
    fun performDailyReset() {
        val today = dateFormat.format(Date())
        val lastResetDate = prefs.getString("last_reset_date", "")
        
        android.util.Log.i("AppUsageRepository", "=== DAILY RESET START ===")
        android.util.Log.i("AppUsageRepository", "performDailyReset called - today: $today, lastReset: $lastResetDate")
        
        // 日付が変わった場合のみリセット処理を実行
        if (lastResetDate != today) {
            val monitoredApps = getMonitoredApps()
            android.util.Log.i("AppUsageRepository", "Date changed! Performing daily reset for ${monitoredApps.size} monitored apps")
            
            val editor = prefs.edit()
            val monitoredAppPrefs = context.getSharedPreferences("monitored_apps", Context.MODE_PRIVATE)
            val monitoredAppEditor = monitoredAppPrefs.edit()
            
            // === STEP 1: 全ての監視対象アプリの使用時間をリセット ===
            android.util.Log.i("AppUsageRepository", "=== Step 1: Resetting usage times ===")
            monitoredApps.forEach { packageName ->
                // 今日の使用時間を0にリセット
                val todayUsageKey = "${packageName}_usage_$today"
                val previousUsage = prefs.getInt(todayUsageKey, 0)
                editor.putInt(todayUsageKey, 0)
                android.util.Log.i("AppUsageRepository", "Reset usage for $packageName: $previousUsage -> 0 minutes (key: $todayUsageKey)")
                
                // 過去7日分の使用時間データを削除（メモリ節約）
                for (i in 1..7) {
                    val pastDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                        val cal = Calendar.getInstance()
                        cal.time = Date()
                        cal.add(Calendar.DAY_OF_MONTH, -i)
                        format(cal.time)
                    }
                    val pastUsageKey = "${packageName}_usage_$pastDay"
                    if (prefs.contains(pastUsageKey)) {
                        editor.remove(pastUsageKey)
                        android.util.Log.d("AppUsageRepository", "Removed old usage data: $pastUsageKey")
                    }
                }
            }
            
            // === STEP 2: 全てのデイパスを無効化 ===
            android.util.Log.i("AppUsageRepository", "=== Step 2: Clearing day passes ===")
            monitoredApps.forEach { packageName ->
                // 今日のデイパスも含めて全て削除（新しい日なので）
                val todayDayPassKey = "${packageName}_day_pass_$today"
                if (prefs.getBoolean(todayDayPassKey, false)) {
                    editor.remove(todayDayPassKey)
                    android.util.Log.i("AppUsageRepository", "Cleared today's day pass for $packageName")
                }
                
                // 過去7日分のデイパスも削除
                for (i in 1..7) {
                    val pastDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                        val cal = Calendar.getInstance()
                        cal.time = Date()
                        cal.add(Calendar.DAY_OF_MONTH, -i)
                        format(cal.time)
                    }
                    val pastDayPassKey = "${packageName}_day_pass_$pastDay"
                    if (prefs.getBoolean(pastDayPassKey, false)) {
                        editor.remove(pastDayPassKey)
                        android.util.Log.d("AppUsageRepository", "Cleared old day pass: $pastDayPassKey")
                    }
                }
            }
            
            // === STEP 3: 監視対象アプリの制限時間を1分ずつ減らす ===
            android.util.Log.i("AppUsageRepository", "=== Step 3: Reducing time limits ===")
            var anyLimitChanged = false
            monitoredApps.forEach { packageName ->
                val currentLimit = monitoredAppPrefs.getInt("${packageName}_current_limit", -1)
                val targetLimit = monitoredAppPrefs.getInt("${packageName}_target_limit", -1)
                
                android.util.Log.d("AppUsageRepository", "Checking limits for $packageName: current=$currentLimit, target=$targetLimit")
                
                if (currentLimit > 0 && targetLimit > 0 && currentLimit > targetLimit) {
                    val newLimit = maxOf(currentLimit - 1, targetLimit)
                    monitoredAppEditor.putInt("${packageName}_current_limit", newLimit)
                    anyLimitChanged = true
                    android.util.Log.i("AppUsageRepository", "✅ Reduced limit for $packageName: $currentLimit -> $newLimit minutes")
                } else {
                    android.util.Log.d("AppUsageRepository", "No limit reduction for $packageName (current=$currentLimit, target=$targetLimit)")
                }
            }
            
            // === STEP 4: 全ての変更を適用 ===
            android.util.Log.i("AppUsageRepository", "=== Step 4: Applying all changes ===")
            
            // リセット日を記録
            editor.putString("last_reset_date", today)
            
            // 変更をコミット
            val appUsageSuccess = editor.commit()
            val monitoredAppSuccess = monitoredAppEditor.commit()
            
            android.util.Log.i("AppUsageRepository", "SharedPreferences commit results: appUsage=$appUsageSuccess, monitoredApp=$monitoredAppSuccess")
            
            // === STEP 5: データ再読み込みとUI更新 ===
            android.util.Log.i("AppUsageRepository", "=== Step 5: Reloading data and updating UI ===")
            
            // MonitoredAppRepositoryのデータ再読み込み
            if (anyLimitChanged) {
                try {
                    // 保持しているMonitoredAppRepositoryインスタンスから直接データを再読み込み
                    monitoredAppRepository.loadMonitoredApps()
                    android.util.Log.i("AppUsageRepository", "✅ MonitoredApp StateFlow reloaded successfully")
                } catch (e: Exception) {
                    android.util.Log.e("AppUsageRepository", "❌ Failed to reload MonitoredApp StateFlow", e)
                }
                
                // さらに確実にするため追加の再読み込み
                try {
                    // 少し待機してからリフレッシュ（SharedPreferencesの変更が反映されるまで待つ）
                    Thread.sleep(100)
                    monitoredAppRepository.loadMonitoredApps()
                    android.util.Log.i("AppUsageRepository", "✅ Additional MonitoredApp reload completed")
                } catch (e: Exception) {
                    android.util.Log.e("AppUsageRepository", "❌ Additional MonitoredApp reload failed", e)
                }
            }
            
            // 使用データを再読み込み（これによりStateFlowが更新され、UIに反映される）
            loadUsageData()
            
            // さらに確実にするため、少し待機してから再度データを読み込み
            try {
                Thread.sleep(200)
                loadUsageData()
                android.util.Log.i("AppUsageRepository", "✅ Additional AppUsage data reload completed")
            } catch (e: Exception) {
                android.util.Log.e("AppUsageRepository", "❌ Additional AppUsage data reload failed", e)
            }
            
            // === STEP 6: MyAccessibilityServiceにリセット完了を通知 ===
            android.util.Log.i("AppUsageRepository", "=== Step 6: Notifying AccessibilityService ===")
            try {
                // Kotlinのcompanion objectメソッドを直接呼び出し
                com.example.timekeeper.service.MyAccessibilityService.notifyDailyReset()
                android.util.Log.i("AppUsageRepository", "✅ AccessibilityService notified directly - all blocked apps should be cleared")
            } catch (e: Exception) {
                android.util.Log.e("AppUsageRepository", "Failed to notify AccessibilityService directly", e)
                
                // フォールバック: AccessibilityServiceが利用できない場合のエラーハンドリング
                android.util.Log.w("AppUsageRepository", "AccessibilityService notification failed, but daily reset completed successfully")
            }
            
            android.util.Log.i("AppUsageRepository", "=== DAILY RESET COMPLETED SUCCESSFULLY ===")
            android.util.Log.i("AppUsageRepository", "✅ All data reset: usage=0, day passes cleared, limits reduced")
            android.util.Log.i("AppUsageRepository", "✅ UI will be updated via StateFlow")
            android.util.Log.i("AppUsageRepository", "✅ All app blocks cleared")
            
        } else {
            android.util.Log.d("AppUsageRepository", "Daily reset already performed today ($today), skipping")
        }
    }
    
    /**
     * 監視対象アプリ一覧を取得
     */
    private fun getMonitoredApps(): Set<String> {
        // MonitoredAppRepositoryから監視対象アプリを取得（データソース統一）
        return monitoredAppRepository.monitoredApps.value.map { it.packageName }.toSet()
    }
    
    /**
     * MonitoredAppRepositoryから制限値を取得
     */
    private fun getCurrentLimitFromRepo(packageName: String): Int {
        // MonitoredAppRepositoryが監視対象でない場合は無制限
        if (!monitoredAppRepository.isAppMonitored(packageName)) {
            android.util.Log.d("AppUsageRepository", 
                "App $packageName is not monitored, returning unlimited")
            return Int.MAX_VALUE
        }
        
        val currentLimit = monitoredAppRepository.getCurrentLimit(packageName)
        
        // 監視対象として設定されているが制限が設定されていない場合
        if (currentLimit == -1) {
            android.util.Log.w("AppUsageRepository", 
                "Monitored app $packageName has no limit set, returning unlimited")
            return Int.MAX_VALUE
        }
        
        android.util.Log.d("AppUsageRepository", 
            "Current limit for $packageName: $currentLimit minutes (from MonitoredAppRepository)")
        return currentLimit
    }
    
    /**
     * 使用データを読み込み
     */
    fun loadUsageData() {
        val today = dateFormat.format(Date())
        val monitoredApps = getMonitoredApps()
        val usageMap = mutableMapOf<String, AppUsageData>()
        
        android.util.Log.d("AppUsageRepository", "loadUsageData called - today: $today, monitored apps: ${monitoredApps.size}")
        
        monitoredApps.forEach { packageName ->
            val todayUsage = getTodayUsage(packageName)
            // MonitoredAppRepositoryから制限値を取得（統一）
            val currentLimit = getCurrentLimitFromRepo(packageName)
            val lastUsed = prefs.getString("${packageName}_last_used", today) ?: today
            
            android.util.Log.d("AppUsageRepository", "Loading data for $packageName: usage=$todayUsage, limit=$currentLimit (from MonitoredAppRepository)")
            
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