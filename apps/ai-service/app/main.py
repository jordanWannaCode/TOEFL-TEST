from fastapi import FastAPI
from pydantic import BaseModel
from typing import Literal
app = FastAPI(title="TOEFL AI Service", version="0.1.0", docs_url=None, redoc_url=None, openapi_url=None)
class HealthResponse(BaseModel):
    status: Literal["ok"]
    service: Literal["ai-service"]
@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok", service="ai-service")

import os
import secrets
from fastapi import Header, HTTPException, Depends
from pydantic import ValidationError
from .evaluation.schema import EvaluationRequest, Feedback
from .evaluation.providers import get_provider, ProviderUnavailable, ProviderTransient, ProviderInvalid

def internal_auth(authorization: str = Header(default='')) -> None:
    token = os.environ.get('AI_INTERNAL_TOKEN')
    if not token or not secrets.compare_digest(authorization, 'Bearer '+token):
        raise HTTPException(401, 'Internal authentication required')

@app.post('/api/v1/ai/evaluations/writing', dependencies=[Depends(internal_auth)])
def evaluate_writing(body: EvaluationRequest) -> dict:
    try:
        value, metadata = get_provider().evaluate(body)
        feedback = Feedback.model_validate(value).validate_against(body)
        return {'evaluationId':body.evaluationId,'promptVersion':body.promptVersion,'rubricVersion':body.rubric.id,'feedback':feedback,**metadata}
    except ProviderUnavailable:
        raise HTTPException(503, 'EVALUATION_UNAVAILABLE') from None
    except ProviderTransient:
        raise HTTPException(502, 'PROVIDER_TRANSIENT') from None
    except (ProviderInvalid, ValueError, ValidationError):
        raise HTTPException(502, 'INVALID_EVALUATION') from None

@app.get('/internal/openapi.json', dependencies=[Depends(internal_auth)])
def internal_openapi() -> dict:
    return app.openapi()

from starlette.responses import JSONResponse
@app.middleware('http')
async def limit_request_size(request, call_next):
    if request.method == 'POST':
        body = bytearray()
        async for chunk in request.stream():
            body.extend(chunk)
            if len(body) > 131072:
                return JSONResponse(status_code=413, content={'detail':'Request size limit exceeded'})
        request._body = bytes(body)
    return await call_next(request)
