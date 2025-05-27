package com.example.timekeeper.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.math.pow

@Composable
fun DayPassPurchaseScreen(
    onPurchaseSuccess: () -> Unit, // 成功時のコールバック名を変更
    onCancelClick: () -> Unit,
    navController: NavController, // rememberNavController() を削除したのでデフォルト値も削除
    modifier: Modifier = Modifier,
    onPurchaseDaypassClick: (Int?) -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("TimekeeperPrefs", Context.MODE_PRIVATE)
    }
    val unlockCount = sharedPreferences.getInt("UNLOCK_COUNT", 0)

    // 価格計算: ¥(200×1.2^unlock_count)
    val price = (200 * 1.2.pow(unlockCount.toDouble())).toInt()

    var isLoading by remember { mutableStateOf(false) }
    val handlePurchaseAttempt: () -> Unit = { // 名前をhandlePurchaseAttemptに変更
        // MainActivityのStripe連携を使用
        onPurchaseDaypassClick(unlockCount)
    }
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "デイパス購入",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "今日1日だけ制限解除",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Text(
                text = "¥$price",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = handlePurchaseAttempt, // handlePurchaseから変更
                enabled = !isLoading,
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
                        text = "今すぐアンロック",
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "キャンセル",
                    fontSize = 18.sp
                )
            }
        }
}

 