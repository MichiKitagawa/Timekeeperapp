package com.example.timekeeper.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class HeartbeatLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "HeartbeatLogger"
        private const val PREFS_NAME = "heartbeat_log"
        private const val KEY_LAST_HEARTBEAT = "last_heartbeat"
        private const val KEY_HEARTBEAT_HISTORY = "heartbeat_history"
        private const val MAX_HISTORY_ENTRIES = 100 // 最大100エントリ（約8時間分）
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * ハートビートを記録
     */
    fun recordHeartbeat() {
        val currentTime = System.currentTimeMillis()
        val formattedTime = dateFormat.format(Date(currentTime))
        
        // 最新のハートビートタイムスタンプを保存
        prefs.edit()
            .putLong(KEY_LAST_HEARTBEAT, currentTime)
            .putString(KEY_HEARTBEAT_HISTORY, appendToHistory(currentTime))
            .apply()
        
        Log.d(TAG, "💓 Heartbeat recorded: $formattedTime")
    }

    /**
     * 最後のハートビートタイムスタンプを取得
     */
    fun getLastHeartbeat(): Long {
        return prefs.getLong(KEY_LAST_HEARTBEAT, 0)
    }

    /**
     * ハートビート履歴に追加
     */
    private fun appendToHistory(timestamp: Long): String {
        val existing = prefs.getString(KEY_HEARTBEAT_HISTORY, "") ?: ""
        val entries = if (existing.isNotEmpty()) {
            existing.split(",").filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
        
        // 新しいエントリを追加
        val newEntries = (entries + timestamp.toString()).takeLast(MAX_HISTORY_ENTRIES)
        
        return newEntries.joinToString(",")
    }

    /**
     * ハートビート履歴を取得
     */
    fun getHeartbeatHistory(): List<Long> {
        val historyString = prefs.getString(KEY_HEARTBEAT_HISTORY, "") ?: ""
        return if (historyString.isNotEmpty()) {
            historyString.split(",").mapNotNull { it.toLongOrNull() }
        } else {
            emptyList()
        }
    }

    /**
     * ハートビート履歴をクリア
     */
    fun clearHeartbeatHistory() {
        prefs.edit()
            .remove(KEY_LAST_HEARTBEAT)
            .remove(KEY_HEARTBEAT_HISTORY)
            .apply()
        
        Log.i(TAG, "🗑️ Heartbeat history cleared")
    }

    /**
     * ハートビート状態の詳細ログ出力
     */
    fun logHeartbeatStatus() {
        val lastHeartbeat = getLastHeartbeat()
        val history = getHeartbeatHistory()
        
        if (lastHeartbeat > 0) {
            val lastTime = dateFormat.format(Date(lastHeartbeat))
            val gapMinutes = (System.currentTimeMillis() - lastHeartbeat) / (60 * 1000)
            
            Log.i(TAG, "📊 Heartbeat Status:")
            Log.i(TAG, "   Last: $lastTime (${gapMinutes}分前)")
            Log.i(TAG, "   History entries: ${history.size}")
            
            if (history.size >= 2) {
                val recent = history.takeLast(5)
                Log.i(TAG, "   Recent heartbeats:")
                recent.forEach { timestamp ->
                    Log.i(TAG, "     ${dateFormat.format(Date(timestamp))}")
                }
            }
        } else {
            Log.i(TAG, "📊 No heartbeat records found")
        }
    }
} 