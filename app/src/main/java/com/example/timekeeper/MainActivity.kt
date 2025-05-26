package com.example.timekeeper

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.timekeeper.ui.theme.TimekeeperTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val sharedPreferences by lazy {
        getSharedPreferences("TimekeeperPrefs", Context.MODE_PRIVATE)
    }

    private fun retrieveAppSpecificDeviceId(): String {
        var deviceId = sharedPreferences.getString("DEVICE_ID", null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            sharedPreferences.edit().putString("DEVICE_ID", deviceId).apply()
        }
        return deviceId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appSpecificDeviceId = retrieveAppSpecificDeviceId()
        // Log.d("MainActivity", "Device ID: $appSpecificDeviceId") // For testing

        setContent {
            TimekeeperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Device ID: $appSpecificDeviceId", // Display device ID for now
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TimekeeperTheme {
        Greeting("Android")
    }
}