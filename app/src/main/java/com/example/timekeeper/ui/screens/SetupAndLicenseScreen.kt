package com.example.timekeeper.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.content.ComponentName
import android.text.TextUtils
import com.example.timekeeper.service.MyAccessibilityService
import com.example.timekeeper.viewmodel.StripeViewModel

@Composable
fun SetupAndLicenseScreen(
    stripeViewModel: StripeViewModel,
    onNavigateToMonitoringSetup: () -> Unit,
    onPurchaseLicenseClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var showWebView by remember { mutableStateOf(false) }
    var checkoutUrl by remember { mutableStateOf<String?>(null) }
    
    // チェックリスト状態
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var hasReadPermissions by remember { mutableStateOf(false) }
    var hasReadUsageStatsInfo by remember { mutableStateOf(false) }
    var hasReadAppInfo by remember { mutableStateOf(false) }
    
    // ViewModelの状態を監視
    val checkoutUrlFlow by stripeViewModel.checkoutUrlFlow.collectAsState()
    val paymentUiState by stripeViewModel.paymentUiState.collectAsState()
    
    // アクセシビリティサービスの状態を定期的にチェック
    LaunchedEffect(Unit) {
        isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
    }
    
    // Checkout URLが取得されたらWebViewを表示
    LaunchedEffect(checkoutUrlFlow) {
        checkoutUrlFlow?.let { url ->
            checkoutUrl = url
            showWebView = true
            stripeViewModel.consumeCheckoutUrl()
        }
    }
    
    // 決済状態の変化を監視
    LaunchedEffect(paymentUiState) {
        when (val state = paymentUiState) {
            is com.example.timekeeper.viewmodel.PaymentUiState.Success -> {
                if (state.message.contains("license")) {
                    isLoading = false
                    showWebView = false
                    onNavigateToMonitoringSetup()
                }
            }
            is com.example.timekeeper.viewmodel.PaymentUiState.Error -> {
                isLoading = false
                showWebView = false
            }
            is com.example.timekeeper.viewmodel.PaymentUiState.Loading -> {
                isLoading = true
            }
            is com.example.timekeeper.viewmodel.PaymentUiState.Idle -> {
                isLoading = false
            }
        }
    }
    
    if (showWebView && checkoutUrl != null) {
        // Stripe Checkout WebView
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ヘッダー
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "決済画面",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        showWebView = false
                        checkoutUrl = null
                    }
                ) {
                    Text("キャンセル")
                }
            }
            
            // WebView
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            @Suppress("OVERRIDE_DEPRECATION")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                url?.let { urlString ->
                                    when {
                                        urlString.contains("checkout-success") -> {
                                            showWebView = false
                                            return true
                                        }
                                        urlString.contains("checkout-cancel") -> {
                                            showWebView = false
                                            return true
                                        }
                                        else -> {
                                            return false
                                        }
                                    }
                                }
                                return false
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                    }
                },
                update = { webView ->
                    checkoutUrl?.let { url ->
                        webView.loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // 統合されたセットアップ・ライセンス購入画面
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Timekeeper セットアップ",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            
            item {
                Text(
                    text = "アプリを使用するには、以下の設定を完了してからライセンスを購入してください。",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
            
            // 設定チェックリスト
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "必要な設定",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // チェックリスト項目 1: アクセシビリティサービス
                        ChecklistItem(
                            title = "アクセシビリティサービスの有効化",
                            description = "アプリの使用時間を監視するために必要です",
                            isChecked = isAccessibilityEnabled,
                            onToggle = { /* 自動で更新される */ },
                            actionButton = {
                                if (!isAccessibilityEnabled) {
                                    Button(
                                        onClick = {
                                            openAccessibilitySettings(context)
                                        }
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("設定を開く")
                                    }
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // チェックリスト項目 2: 権限についての理解
                        ChecklistItem(
                            title = "必要な権限について理解しました",
                            description = "オーバーレイ表示権限とシステムアラートウィンドウ権限が必要です",
                            isChecked = hasReadPermissions,
                            onToggle = { hasReadPermissions = !hasReadPermissions }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // チェックリスト項目 3: 使用状況統計について
                        ChecklistItem(
                            title = "使用状況統計について理解しました",
                            description = "アプリの使用時間計測のためにデバイスの使用状況にアクセスします",
                            isChecked = hasReadUsageStatsInfo,
                            onToggle = { hasReadUsageStatsInfo = !hasReadUsageStatsInfo }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // チェックリスト項目 4: アプリ情報について
                        ChecklistItem(
                            title = "監視対象アプリの設定について理解しました",
                            description = "購入後にどのアプリを監視するか設定できます",
                            isChecked = hasReadAppInfo,
                            onToggle = { hasReadAppInfo = !hasReadAppInfo }
                        )
                    }
                }
            }
            
            // 設定完了状況の表示
            item {
                val allRequirementsMet = isAccessibilityEnabled && hasReadPermissions && hasReadUsageStatsInfo && hasReadAppInfo
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (allRequirementsMet) {
                        CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                    } else {
                        CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (allRequirementsMet) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (allRequirementsMet) {
                                "すべての設定が完了しました！"
                            } else {
                                "上記の項目をすべて完了してください"
                            },
                            fontWeight = FontWeight.Medium,
                            color = if (allRequirementsMet) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                    }
                }
            }
            
            // 価格とライセンス購入
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "¥10,000で利用開始",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Button(
                    onClick = {
                        onPurchaseLicenseClick()
                    },
                    enabled = !isLoading && isAccessibilityEnabled && hasReadPermissions && hasReadUsageStatsInfo && hasReadAppInfo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "ライセンスを購入",
                            fontSize = 18.sp
                        )
                    }
                }
            }
            
            // アクセシビリティ設定チェック用のボタン
            item {
                if (!isAccessibilityEnabled) {
                    TextButton(
                        onClick = {
                            isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
                        }
                    ) {
                        Text("設定状況を更新")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistItem(
    title: String,
    description: String,
    isChecked: Boolean,
    onToggle: () -> Unit,
    actionButton: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            actionButton?.let {
                Spacer(modifier = Modifier.height(8.dp))
                it()
            }
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