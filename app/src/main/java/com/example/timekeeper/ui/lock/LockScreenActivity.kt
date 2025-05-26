package com.example.timekeeper.ui.lock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.timekeeper.R
// import com.example.timekeeper.ui.daypass.DayPassPurchaseActivity // P05画面 (P05未実装のためコメントアウト)

class LockScreenActivity : AppCompatActivity() {

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        messageTextView.text = "使用時間を超過しました" // P04 UI: メッセージ

        // P04 UI: アンロックボタン
        // 価格表示はP05で行うため、ここでは遷移のみ
        unlockButton.text = "1日アンロック" // ボタンテキストは仮
        unlockButton.setOnClickListener {
            // P05: デイパス決済画面へ遷移
            // val intent = Intent(this, DayPassPurchaseActivity::class.java) // P05未実装のためコメントアウト
            // startActivity(intent) // P05未実装のためコメントアウト
            // finish() // ロック画面を閉じる // P05未実装のためコメントアウト
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                // 権限が付与されなかった場合の処理 (例: アプリ終了、再度リクエストなど)
                // ここでは簡略化のため何もしない
            }
        }
    }

    // P04: バックボタン無効化
    override fun onBackPressed() {
        // 何もしないことでバックボタンを無効化
    }

    // 他のアプリの上に表示するための設定
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // WindowManager.LayoutParamsを設定してオーバーレイ表示を実現する
        // この部分はAndroidManifest.xmlでのwindowIsFloatingやthemeの設定、
        // またはWindowManagerを直接操作することでより細かく制御可能
    }
} 