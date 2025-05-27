package com.example.timekeeper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import com.example.timekeeper.ui.theme.TimekeeperTheme

@Composable
fun LicenseScreen(
    navController: NavController, // P06画面遷移のために追加
    onNavigateToMonitoringSetup: () -> Unit,
    onPurchaseLicenseClick: () -> Unit = {}
) {
    // 簡単な実装のため、ViewModelは使用せずに直接購入ボタンを表示
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "¥10,000で利用開始")
            Button(
                onClick = { onPurchaseLicenseClick() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("購入")
            }
        }
        }


@Preview(showBackground = true)
@Composable
fun LicenseScreenPreview() {
    TimekeeperTheme {
        val mockNavController = NavController(LocalContext.current) // モックNavController
        LicenseScreen(
            navController = mockNavController, 
            onNavigateToMonitoringSetup = {},
            onPurchaseLicenseClick = {}
        )
    }
} 