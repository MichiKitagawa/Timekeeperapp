#!/bin/bash

echo "=== Timekeeper アクセシビリティサービス デバッグテスト ==="
echo ""

# アプリをインストール
echo "📱 アプリをインストール中..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo ""
echo "🔍 現在のアクセシビリティサービス状態を確認..."
adb shell settings get secure enabled_accessibility_services

echo ""
echo "📊 現在実行中のサービスを確認..."
adb shell dumpsys activity services | grep -E "(timekeeper|BackgroundAppMonitoringService|MyAccessibilityService)"

echo ""
echo "🚀 アプリを起動..."
adb shell am start -n com.example.timekeeper/.MainActivity

echo ""
echo "⏳ 5秒待機..."
sleep 5

echo ""
echo "📋 ログを確認 (最新50行)..."
adb logcat -d | grep -E "(BackgroundAppMonitoringService|AppUsageRepository|AccessibilityServiceMonitor|DashboardViewModel)" | tail -50

echo ""
echo "=== テスト完了 ==="
echo ""
echo "次の手順:"
echo "1. アクセシビリティサービスをオンにする"
echo "2. 監視対象アプリを設定する"
echo "3. アクセシビリティサービスをオフにする"
echo "4. 監視対象アプリを起動してブロックされるかテストする"
echo ""
echo "リアルタイムログ監視:"
echo "adb logcat | grep -E '(BackgroundAppMonitoringService|AppUsageRepository|AccessibilityServiceMonitor|DashboardViewModel)'" 