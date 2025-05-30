package com.example.timekeeper.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.ComponentName
import android.text.TextUtils
import com.example.timekeeper.service.MyAccessibilityService

@Composable
fun AccessibilityPromptScreen(
    onAccessibilityEnabled: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "アクセシビリティサービスを有効にしてください",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "このアプリの機能を利用するには、アクセシビリティサービスの有効化が必要です。\n\n設定画面でTimekeeperのアクセシビリティサービスをONにしてください。"
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        // アクセシビリティ設定ボタン
        Button(onClick = {
            openAccessibilitySettings(context)
        }) {
            Text("アクセシビリティ設定を開く")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 設定完了確認ボタン
        Button(onClick = {
            val isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
            
            if (isAccessibilityEnabled) {
                onAccessibilityEnabled()
            } else {
                android.widget.Toast.makeText(
                    context, 
                    "アクセシビリティサービスの有効化が必要です。", 
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }) {
            Text("設定完了")
        }
    }
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    context.startActivity(intent)
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = ComponentName(context, MyAccessibilityService::class.java)
    val accessibilityEnabled = Settings.Secure.getInt(
        context.contentResolver,
        Settings.Secure.ACCESSIBILITY_ENABLED,
        0
    )
    if (accessibilityEnabled == 0) {
        return false
    }
    val settingValue = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    if (settingValue != null) {
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(settingValue)
        while (splitter.hasNext()) {
            val accessibilityService = splitter.next()
            if (accessibilityService.equals(service.flattenToString(), ignoreCase = true)) {
                return true
            }
        }
    }
    return false
} 