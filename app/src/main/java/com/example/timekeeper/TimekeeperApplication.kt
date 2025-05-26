package com.example.timekeeper

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TimekeeperApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 必要であればここで初期化処理を行う
    }
} 