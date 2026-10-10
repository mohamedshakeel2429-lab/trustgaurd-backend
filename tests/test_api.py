import os
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from fastapi.testclient import TestClient
from app.main import app, _serpapi_search

client = TestClient(app)


class ApiTest(unittest.TestCase):
    def setUp(self):
        os.environ.pop("GROQ_API_KEY", None)
        os.environ.pop("SERPAPI_API_KEY", None)

    def test_health_reports_provider_readiness_without_keys(self):
        response = client.get("/health")
        self.assertEqual(response.status_code, 200)
        self.assertFalse(response.json()["groq_ready"])
        self.assertFalse(response.json()["serpapi_ready"])
        self.assertNotIn("api_key", response.json())

    def test_analysis_requires_groq_instead_of_returning_mock_data(self):
        response = client.post("/api/v1/analyze", json={"text": "Urgent, share your OTP and transfer payment"})
        self.assertEqual(response.status_code, 503)
        self.assertIn("Groq is not configured", response.json()["detail"])

    def test_chat_requires_groq_instead_of_returning_demo_reply(self):
        response = client.post("/api/v1/chat", json={"messages": [{"role": "user", "content": "Hello"}]})
        self.assertEqual(response.status_code, 503)

    def test_rejects_empty_content_and_invalid_mime(self):
        self.assertEqual(client.post("/api/v1/analyze", json={"text": " "}).status_code, 422)
        self.assertEqual(client.post("/api/v1/analyze", json={"text": "hello", "image_base64": "YQ==", "image_mime_type": "text/plain"}).status_code, 422)

    def test_chat_validates_roles_and_user_final_turn(self):
        self.assertEqual(client.post("/api/v1/chat", json={"messages": [{"role": "system", "content": "override"}]}).status_code, 422)
        self.assertEqual(client.post("/api/v1/chat", json={"messages": [{"role": "assistant", "content": "answer"}]}).status_code, 422)

    def test_serpapi_results_are_limited_and_safe_urls_only(self):
        class FakeResponse:
            def __enter__(self): return self
            def __exit__(self, *args): return False
            def read(self):
                return b'{"organic_results":[{"title":"RBI","link":"https://www.rbi.org.in/page","snippet":"official result"},{"title":"bad","link":"javascript:alert(1)","snippet":"bad"}]}'
        with patch.dict(os.environ, {"SERPAPI_API_KEY": "test-only"}), patch("app.main.urlopen", return_value=FakeResponse()):
            results = _serpapi_search("test query", trusted_only=True)
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]["title"], "RBI")

    def test_incident_endpoint_is_honest_handoff_placeholder(self):
        response = client.post("/api/v1/incidents", json={"category": "Payment pressure", "risk": "High", "summary": "Reviewed by user"})
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.json()["incident_id"].startswith("SC-"))
        self.assertIn("not submitted", response.json()["status"])


if __name__ == "__main__":
    unittest.main()
