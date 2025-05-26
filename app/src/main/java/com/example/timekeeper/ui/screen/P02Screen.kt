package com.example.timekeeper.ui.screen

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.timekeeper.ui.theme.TimekeeperTheme
import com.example.timekeeper.viewmodel.P02ViewModel
import kotlinx.coroutines.launch

data class AppInfo(val name: String, val packageName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun P02Screen(
    onNavigateToP03: () -> Unit,
    viewModel: P02ViewModel = hiltViewModel()
) {
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var initialTimeText by remember { mutableStateOf("") }
    var targetTimeText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val packageManager = context.packageManager
    val appList = remember {
        val pm = packageManager
        val flags = PackageManager.GET_META_DATA
        val applications = pm.getInstalledApplications(flags)
        applications.filter { appInfo ->
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                    pm.getLaunchIntentForPackage(appInfo.packageName) != null
        }.map { appInfo ->
            AppInfo(pm.getApplicationLabel(appInfo).toString(), appInfo.packageName)
        }.sortedBy { it.name }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun validateInputs(): String? {
        val initialTime = initialTimeText.toIntOrNull()
        val targetTime = targetTimeText.toIntOrNull()

        if (selectedApp == null) {
            return "監視対象アプリを選択してください。"
        }
        if (initialTime == null || initialTime !in 1..1440) {
            return "初期時間を1〜1440の整数で入力してください。"
        }
        if (targetTime == null || targetTime !in 1..1440) {
            return "目標時間を1〜1440の整数で入力してください。"
        }
        if (targetTime >= initialTime) {
            return "目標時間は初期時間より短く設定してください。"
        }
        return null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("監視対象設定", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedApp?.name ?: "",
                    onValueChange = { /* ReadOnly */ },
                    label = { Text("監視対象アプリ") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    appList.forEach { appInfo ->
                        DropdownMenuItem(
                            text = { Text(appInfo.name) },
                            onClick = {
                                selectedApp = appInfo
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = initialTimeText,
                onValueChange = { initialTimeText = it },
                label = { Text("初期時間 (分)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = targetTimeText,
                onValueChange = { targetTimeText = it },
                label = { Text("目標時間 (分)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val errorMessage = validateInputs()
                    if (errorMessage != null) {
                        scope.launch {
                            snackbarHostState.showSnackbar(errorMessage)
                        }
                    } else {
                        selectedApp?.let { app ->
                            val initialTime = initialTimeText.toInt() // validateInputsでnullチェック済み
                            val targetTime = targetTimeText.toInt()   // validateInputsでnullチェック済み
                            viewModel.addOrUpdateApp(
                                packageName = app.packageName,
                                label = app.name,
                                initialTimeMinutes = initialTime,
                                targetTimeMinutes = targetTime
                            )
                            onNavigateToP03() // 保存後P03へ遷移
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("追加/更新")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun P02ScreenPreview() {
    TimekeeperTheme {
        // PreviewではViewModelを直接渡せないので、ダミーのViewModelを渡すか、プレビュー用の処理を工夫する必要がある
        // ここでは一旦そのままにしておく
        P02Screen(onNavigateToP03 = {})
    }
} 