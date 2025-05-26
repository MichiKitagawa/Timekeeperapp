"""
Timekeeper Backend Middleware
共通エラーハンドリングミドルウェア
"""
from fastapi import Request, HTTPException
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware
from validation import ValidationError
import traceback
import logging

# ログ設定
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class ErrorHandlingMiddleware(BaseHTTPMiddleware):
    """共通エラーハンドリングミドルウェア"""
    
    async def dispatch(self, request: Request, call_next):
        """
        リクエスト処理とエラーハンドリング
        
        Args:
            request: HTTPリクエスト
            call_next: 次の処理
            
        Returns:
            Response: HTTPレスポンス
        """
        try:
            response = await call_next(request)
            return response
            
        except ValidationError as e:
            # バリデーションエラーの処理
            logger.warning(f"Validation error: {e.error_code} - {e.message}")
            return JSONResponse(
                status_code=e.status_code,
                content={
                    "error": e.error_code,
                    "message": e.message
                }
            )
            
        except HTTPException as e:
            # FastAPIのHTTPExceptionの処理
            logger.warning(f"HTTP exception: {e.status_code} - {e.detail}")
            
            # detailが辞書の場合はそのまま返す、文字列の場合は標準形式に変換
            if isinstance(e.detail, dict):
                content = e.detail
            else:
                content = {
                    "error": "http_error",
                    "message": str(e.detail)
                }
            
            return JSONResponse(
                status_code=e.status_code,
                content=content
            )
            
        except Exception as e:
            # 予期しないエラーの処理
            logger.error(f"Unexpected error: {str(e)}")
            logger.error(f"Traceback: {traceback.format_exc()}")
            
            return JSONResponse(
                status_code=500,
                content={
                    "error": "internal_server_error",
                    "message": "予期しないエラーが発生しました"
                }
            ) 