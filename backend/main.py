from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, Dict, Any
from engine import evaluate_chama_loan, get_model

app = FastAPI(
    title="Chama Loan Underwriting & Risk Engine API",
    description="Microservice evaluating loan applications via Kenyan Chama bylaws (3x Savings, DTI <= 45%) and trained Random Forest model.",
    version="1.0.0"
)

# Enable CORS for local development and Android emulator
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class LoanApplicationRequest(BaseModel):
    Gender: Optional[str] = "Male"
    Married: Optional[str] = "Yes"
    Dependents: Optional[int] = 0
    Education: Optional[str] = "Graduate"
    Self_Employed: Optional[str] = "No"
    ApplicantIncome: float
    CoapplicantIncome: Optional[float] = 0.0
    LoanAmount: float
    MemberSavings: Optional[float] = None
    Loan_Amount_Term: Optional[float] = 12.0
    Credit_History: Optional[float] = 1.0
    Property_Area: Optional[str] = "Urban"

@app.on_event("startup")
def startup_event():
    # Warm up ML model in memory
    try:
        get_model()
        print(" [FastAPI] Random Forest ML model loaded and cached in memory.")
    except Exception as e:
        print(f" [FastAPI] Warning: Model failed to load: {e}")

@app.get("/")
def root_health():
    """
    Health check endpoint utilized by Android app (MainActivity Quick Menu Ping).
    """
    return {
        "status": "online",
        "service": "Chama AI Underwriting Engine",
        "version": "1.0.0",
        "model_loaded": get_model() is not None,
        "message": "Chama AI Backend is ready to process loan requests."
    }

@app.get("/health")
def health_check():
    return root_health()

@app.post("/predict")
def predict_loan(payload: LoanApplicationRequest):
    """
    Underwriting endpoint called by Retrofit in ChamaLoanApp.
    Processes demographic and financial inputs through domain bylaws and ML model.
    """
    try:
        data = payload.dict()
        result = evaluate_chama_loan(data)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Inference error: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
