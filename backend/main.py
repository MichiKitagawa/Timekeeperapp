import os
from fastapi import FastAPI
import firebase_admin
from firebase_admin import credentials, firestore

app = FastAPI()

# Firestoreの初期化
SERVICE_ACCOUNT_FILE = os.path.join(os.path.dirname(__file__), "timekee-b5863-firebase-adminsdk-fbsvc-9ad9aa5ac6.json")

if os.path.exists(SERVICE_ACCOUNT_FILE):
    cred = credentials.Certificate(SERVICE_ACCOUNT_FILE)
    firebase_admin.initialize_app(cred)
    db = firestore.client()
    print("Firestore initialized successfully")
else:
    db = None
    print(f"Error: Service account file not found at {SERVICE_ACCOUNT_FILE}")
    print(f"Current working directory: {os.getcwd()}")
    print(f"Files in backend directory: {os.listdir(os.path.dirname(__file__))}")

@app.get("/health")
async def health_check():
    return {"status": "ok"}

@app.get("/test_firestore")
async def test_firestore():
    if not db:
        return {"status": "error", "message": "Firestore not initialized"}
    try:
        doc_ref = db.collection(u'test_collection').document(u'test_doc')
        doc_ref.set({
            u'message': u'Hello from FastAPI!'
        })
        doc = doc_ref.get()
        if doc.exists:
            return {"status": "success", "data": doc.to_dict()}
        else:
            return {"status": "error", "message": "Test document not found after write."}
    except Exception as e:
        return {"status": "error", "message": str(e)} 