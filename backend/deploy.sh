#!/bin/bash

# Timekeeper Backend - Cloud Run デプロイスクリプト

# 変数設定（実際の値に変更してください）
PROJECT_ID="your-project-id"
SERVICE_NAME="timekeeper-backend"
REGION="asia-northeast1"  # 東京リージョン
IMAGE_NAME="gcr.io/${PROJECT_ID}/${SERVICE_NAME}"

echo "🚀 Timekeeper Backend を Cloud Run にデプロイ中..."

# 1. Dockerイメージをビルドしてプッシュ
echo "📦 Dockerイメージをビルド中..."
gcloud builds submit --tag ${IMAGE_NAME}

# 2. Cloud Run にデプロイ
echo "🌐 Cloud Run にデプロイ中..."
gcloud run deploy ${SERVICE_NAME} \
  --image ${IMAGE_NAME} \
  --platform managed \
  --region ${REGION} \
  --allow-unauthenticated \
  --port 8080 \
  --memory 512Mi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 10 \
  --set-env-vars ENVIRONMENT=production \
  --timeout 300

echo "✅ デプロイ完了！"
echo "📍 サービスURL:"
gcloud run services describe ${SERVICE_NAME} --region=${REGION} --format="value(status.url)"

echo ""
echo "⚠️  次の手順を実行してください："
echo "1. Stripe API キーを設定:"
echo "   gcloud run services update ${SERVICE_NAME} --region=${REGION} --set-env-vars STRIPE_API_KEY=sk_live_xxx"
echo ""
echo "2. Firebase サービスアカウントキーを設定:"
echo "   gcloud secrets create firebase-service-account --data-file=timekee-b5863-firebase-adminsdk-fbsvc-9ad9aa5ac6.json"
echo "   gcloud run services update ${SERVICE_NAME} --region=${REGION} --set-env-vars FIREBASE_SERVICE_ACCOUNT_PATH=/secrets/firebase-service-account" 