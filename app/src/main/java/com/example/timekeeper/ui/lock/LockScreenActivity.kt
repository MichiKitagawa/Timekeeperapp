package com.example.timekeeper.ui.lock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.timekeeper.R
import com.example.timekeeper.MainActivity
import com.example.timekeeper.service.MyAccessibilityService
// import com.example.timekeeper.ui.daypass.DayPassPurchaseActivity // P05画面 (P05未実装のためコメントアウト)

class LockScreenActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LockScreenActivity"
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
        var accessibilityService: MyAccessibilityService? = null
    }
    
    private var lockedAppPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "LockScreenActivity created")
        
        // フルスクリーン表示の設定
        setupFullscreen()
        
        setContentView(R.layout.activity_lock_screen)

        // SYSTEM_ALERT_WINDOW権限の確認とリクエスト
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }

        val messageTextView: TextView = findViewById(R.id.lock_message_textview)
        val unlockButton: Button = findViewById(R.id.unlock_button)

        // 制限されたアプリ名を取得
        lockedAppPackage = intent.getStringExtra("locked_app_package")
        val appName = getAppName(lockedAppPackage)
        messageTextView.text = "使用時間を超過しました\n$appName"
        
        Log.i(TAG, "Lock screen displayed for app: $appName ($lockedAppPackage)")

        // P04 UI: アンロックボタン
        // 価格表示はP05で行うため、ここでは遷移のみ
        unlockButton.text = "1日アンロック (¥200)" // ボタンテキストは仮
        unlockButton.setOnClickListener {
            Log.i(TAG, "Unlock button clicked for $lockedAppPackage")
            
            // デイパス購入画面に遷移
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                // デイパス購入画面に直接遷移するためのフラグを追加
                putExtra("navigate_to", "day_pass_purchase")
                putExtra("locked_app_package", lockedAppPackage)
            }
            startActivity(intent)
            finish()
        }
    }
    
    /**
     * フルスクリーン表示の設定
     */
    private fun setupFullscreen() {
        // ウィンドウフラグを設定
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // システムバーを隠す（Android 11以降）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            // Android 10以前
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
        
        Log.d(TAG, "Fullscreen mode configured")
    }
    
    /**
     * パッケージ名からアプリ名を取得
     */
    private fun getAppName(packageName: String?): String {
        if (packageName == null) return "Unknown App"
        
        return try {
            val packageManager = packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get app name for $packageName", e)
            packageName
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                // 権限が付与されなかった場合の処理 (例: アプリ終了、再度リクエストなど)
                // ここでは簡略化のため何もしない
                Log.w(TAG, "Overlay permission not granted")
            }
        }
    }

    // P04: バックボタン無効化
    override fun onBackPressed() {
        // 何もしないことでバックボタンを無効化
        Log.d(TAG, "Back button pressed but ignored")
    }

    // 他のアプリの上に表示するための設定
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "LockScreenActivity attached to window")
        // WindowManager.LayoutParamsを設定してオーバーレイ表示を実現する
        // この部分はAndroidManifest.xmlでのwindowIsFloatingやthemeの設定、
        // またはWindowManagerを直接操作することでより細かく制御可能
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "LockScreenActivity resumed")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "LockScreenActivity paused")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LockScreenActivity destroyed")
    }
} 