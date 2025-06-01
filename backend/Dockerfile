# Cloud Run用 Dockerfile
FROM python:3.11-slim

# 作業ディレクトリを設定
WORKDIR /app

# システムの依存関係をインストール
RUN apt-get update && apt-get install -y \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Pythonの依存関係をコピーしてインストール
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# アプリケーションファイルをコピー
COPY . .

# 本番用環境変数を設定
ENV ENVIRONMENT=production
ENV PYTHONPATH=/app

# Cloud Runのポート設定（環境変数PORTを使用）
ENV PORT=8080
EXPOSE $PORT

# 非rootユーザーでの実行（セキュリティ向上）
RUN useradd --create-home --shell /bin/bash app
USER app

# アプリケーションを起動
CMD exec uvicorn main:app --host 0.0.0.0 --port $PORT --workers 1 