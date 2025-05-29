Write-Host "=== Timekeeper アクセシビリティサービス デバッグテスト ===" -ForegroundColor Cyan
Write-Host ""

# アプリをインストール
Write-Host "📱 アプリをインストール中..." -ForegroundColor Yellow
adb install -r app/build/outputs/apk/debug/app-debug.apk

Write-Host ""
Write-Host "🔍 現在のアクセシビリティサービス状態を確認..." -ForegroundColor Yellow
adb shell settings get secure enabled_accessibility_services

Write-Host ""
Write-Host "📊 現在実行中のサービスを確認..." -ForegroundColor Yellow
adb shell dumpsys activity services | Select-String -Pattern "(timekeeper|BackgroundAppMonitoringService|MyAccessibilityService)"

Write-Host ""
Write-Host "🚀 アプリを起動..." -ForegroundColor Yellow
adb shell am start -n com.example.timekeeper/.MainActivity

Write-Host ""
Write-Host "⏳ 5秒待機..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

Write-Host ""
Write-Host "📋 ログを確認 (最新50行)..." -ForegroundColor Yellow
adb logcat -d | Select-String -Pattern "(BackgroundAppMonitoringService|AppUsageRepository|AccessibilityServiceMonitor|DashboardViewModel)" | Select-Object -Last 50

Write-Host ""
Write-Host "=== テスト完了 ===" -ForegroundColor Green
Write-Host ""
Write-Host "次の手順:" -ForegroundColor Cyan
Write-Host "1. アクセシビリティサービスをオンにする"
Write-Host "2. 監視対象アプリを設定する"
Write-Host "3. アクセシビリティサービスをオフにする"
Write-Host "4. 監視対象アプリを起動して強制終了されるかテストする"
Write-Host ""
Write-Host "強制終了機能のテスト:" -ForegroundColor Cyan
Write-Host "- 監視対象アプリが0.2秒間隔で検知される"
Write-Host "- killBackgroundProcesses, am force-stop, Process.killProcess が実行される"
Write-Host "- 継続的な監視により再起動も即座にブロックされる"
Write-Host ""
Write-Host "リアルタイムログ監視:" -ForegroundColor Cyan
Write-Host "adb logcat | Select-String -Pattern '(BackgroundAppMonitoringService|AppUsageRepository|AccessibilityServiceMonitor|DashboardViewModel)'"
Write-Host ""
Write-Host "強制終了ログの確認:" -ForegroundColor Cyan
Write-Host "adb logcat | Select-String -Pattern '(💀|🔪|��|🎯|force|kill)'" 