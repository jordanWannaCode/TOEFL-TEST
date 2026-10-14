from fastapi import FastAPI
from pydantic import BaseModel
from typing import Literal
app = FastAPI(title="TOEFL AI Service", version="0.1.0", docs_url=None, redoc_url=None)
class HealthResponse(BaseModel):
    status: Literal["ok"]
    service: Literal["ai-service"]
@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok", service="ai-service")
