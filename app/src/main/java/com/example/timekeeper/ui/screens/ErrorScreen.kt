package com.example.timekeeper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ErrorType {
    LICENSE_REQUIRED,
    UNEXPECTED_ERROR,
    PAYMENT_SUCCESS_BUT_UNLOCK_FAILED,
    PAYMENT_VERIFICATION_FAILED
}

@Composable
fun ErrorScreen(
    errorType: ErrorType,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (message, buttonText) = when (errorType) {
        ErrorType.LICENSE_REQUIRED -> Pair(
            "ライセンス購入が必要です",
            "購入画面へ"
        )
        ErrorType.UNEXPECTED_ERROR -> Pair(
            "予期しないエラーが発生しました",
            "再試行"
        )
        ErrorType.PAYMENT_SUCCESS_BUT_UNLOCK_FAILED -> Pair(
            "課金は成功しましたが、ロック解除に失敗しました",
            "問い合わせる"
        )
        ErrorType.PAYMENT_VERIFICATION_FAILED -> Pair(
            "決済の検証に失敗しました。",
            "やり直す"
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "エラー",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Red,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = message,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Button(
            onClick = onActionClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = 18.sp
            )
        }
    }
} 