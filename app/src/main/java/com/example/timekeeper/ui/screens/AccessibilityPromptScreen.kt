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
import androidx.navigation.NavController

@Composable
fun AccessibilityPromptScreen(navController: NavController) {
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
            text = "このアプリの全ての機能を利用するには、アクセシビリティサービスを有効にする必要があります。「設定を開く」ボタンをタップして、表示されるリストから「Timekeeper」を選択し、サービスをONにしてください。"
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            openAccessibilitySettings(context)
        }) {
            Text("設定を開く")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            // アプリを終了するか、MainActivityに戻って再チェックさせるか検討
            // ここではMainActivityに戻り、アクセシビリティが有効になっていれば次の画面へ進むことを期待
            // (MainActivity側でonResume等で再チェックするロジックが必要になる可能性)
            // または、この画面にとどまり、ユーザーが手動で有効化するのを待つ
            // 今回は、ユーザーが設定後、アプリに手動で戻ることを想定し、何もしないか、
            // 最小化するなどの挙動も考えられる。
            // 一旦、MainActivityのonResumeでの再チェックを期待して、ここでは何もしない。
            // または、特定のルートに飛ばさず、navController.popBackStack() で前の画面に戻るなど。
            // 今回はユーザーが設定から戻ってきた際に MainActivity の再開でチェックされることを期待。
        }) {
            Text("設定しました (アプリに戻る)")
        }
    }
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    context.startActivity(intent)
} 