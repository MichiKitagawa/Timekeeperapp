# test_checkout.py

import requests, webbrowser

res = requests.post("http://localhost:8000/create-checkout-session", json={
    "device_id": "test-device-123",
    "product_type": "daypass",
    "unlock_count": 3
})

if res.status_code == 200:
    checkout_url = res.json()["checkout_url"]
    print("✅ Stripe Checkoutページを開きます:", checkout_url)
    webbrowser.open(checkout_url)
else:
    print("❌ エラー:", res.status_code, res.text)
