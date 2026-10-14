from fastapi.testclient import TestClient
from app.main import app
client = TestClient(app)
def test_health_contract():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "ai-service"}
def test_no_fake_evaluation_endpoint():
    assert client.post("/evaluations", json={}).status_code == 404
