from __future__ import annotations

import json
import os
import urllib.error
import urllib.request
from abc import ABC, abstractmethod
from pathlib import Path
from typing import Any, Dict, Optional

try:
    from dotenv import load_dotenv
    _env_path = Path(__file__).resolve().parents[2] / ".env"
    if _env_path.exists():
        load_dotenv(_env_path, override=True)
except Exception:
    pass

try:
    from . import mock_llm
except ImportError:  # pragma: no cover
    import mock_llm  # type: ignore


class BaseLLMAdapter(ABC):
    name = "base"

    @abstractmethod
    def generate_json(self, prompt: str, fact_pack: Dict[str, Any], schema: Dict[str, Any]) -> Dict[str, Any]:
        raise NotImplementedError


class MockLLMAdapter(BaseLLMAdapter):
    name = "mock"

    def generate_json(self, prompt: str, fact_pack: Dict[str, Any], schema: Dict[str, Any]) -> Dict[str, Any]:
        return mock_llm.generate(fact_pack)


class InternalLLMAdapter(BaseLLMAdapter):
    name = "internal"

    def __init__(self) -> None:
        _oai_key = os.getenv("OPENAI_API_KEY", "")
        _oai_url = os.getenv("OPENAI_BASE_URL", "")
        _oai_model = os.getenv("OPENAI_MODEL", "")
        self.base_url = (_oai_url or os.getenv("INTERNAL_LLM_BASE_URL", "")).rstrip("/")
        self.api_key = _oai_key or os.getenv("INTERNAL_LLM_API_KEY", "")
        self.model = _oai_model or os.getenv("INTERNAL_LLM_MODEL", "")
        self.app_id = os.getenv("INTERNAL_LLM_APP_ID", "")
        self.app_key = os.getenv("INTERNAL_LLM_APP_KEY", "")
        self.bot_id = os.getenv("INTERNAL_LLM_BOT_ID", "")
        self.protocol = os.getenv("INTERNAL_LLM_PROTOCOL", "paic_agents").lower()
        if _oai_key and _oai_url:
            self.protocol = "openai"
        self.login_path = os.getenv("INTERNAL_LLM_LOGIN_PATH", "/{app_id}/auth/login")
        self.conversation_path = os.getenv("INTERNAL_LLM_CONVERSATION_PATH", "/api/v1/conversations")
        self.message_path = os.getenv(
            "INTERNAL_LLM_MESSAGE_PATH",
            "/api/v1/conversations/{conversation_id}/messages",
        )
        if not self.base_url:
            raise ValueError("INTERNAL_LLM_BASE_URL is required")
        if self.protocol == "openai":
            if not self.api_key or not self.model:
                raise ValueError("INTERNAL_LLM_API_KEY and INTERNAL_LLM_MODEL are required for openai protocol")
        elif self.protocol == "paic_agents":
            if not self.app_id or not self.app_key or not self.bot_id:
                raise ValueError("INTERNAL_LLM_APP_ID, INTERNAL_LLM_APP_KEY, and INTERNAL_LLM_BOT_ID are required")
        else:
            raise ValueError(f"Unsupported INTERNAL_LLM_PROTOCOL: {self.protocol}")

    def generate_json(self, prompt: str, fact_pack: Dict[str, Any], schema: Dict[str, Any]) -> Dict[str, Any]:
        if self.protocol == "openai":
            return self._generate_openai_json(prompt, fact_pack, schema)
        return self._generate_paic_agents_json(prompt, fact_pack, schema)

    def _generate_openai_json(self, prompt: str, fact_pack: Dict[str, Any], schema: Dict[str, Any]) -> Dict[str, Any]:
        payload = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": prompt},
                {"role": "user", "content": json.dumps({"fact_pack": fact_pack, "json_schema": schema}, ensure_ascii=False)},
            ],
            "temperature": 0,
        }
        request = urllib.request.Request(
            f"{self.base_url}/chat/completions",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                body = json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as exc:
            raise RuntimeError(f"Internal LLM request failed: HTTP {exc.code}") from exc
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
            raise RuntimeError(f"Internal LLM request failed: {exc}") from exc

        content = body.get("choices", [{}])[0].get("message", {}).get("content")
        if not isinstance(content, str):
            raise RuntimeError("Internal LLM response missing choices[0].message.content")
        return _loads_json_object(content)

    def _generate_paic_agents_json(self, prompt: str, fact_pack: Dict[str, Any], schema: Dict[str, Any]) -> Dict[str, Any]:
        token = self._login_paic_agents()
        conversation_id = self._create_paic_conversation(token)
        message = (
            f"{prompt}\n\n"
            "请严格按 JSON Schema 输出一个 JSON 对象，不要输出 Markdown。\n\n"
            f"JSON_SCHEMA:\n{json.dumps(schema, ensure_ascii=False)}\n\n"
            f"FACT_PACK:\n{json.dumps(fact_pack, ensure_ascii=False)}"
        )
        body = self._post_json(
            self.message_path.format(
                app_id=self.app_id,
                bot_id=self.bot_id,
                conversation_id=conversation_id,
            ),
            {
                "bot_id": self.bot_id,
                "content": message,
                "message": message,
                "query": message,
                "stream": False,
            },
            token=token,
        )
        content = _extract_first_string(
            body,
            [
                ("data", "content"),
                ("data", "answer"),
                ("data", "message"),
                ("content",),
                ("answer",),
                ("message",),
                ("choices", 0, "message", "content"),
            ],
        )
        if not content:
            raise RuntimeError(f"Internal LLM response missing JSON content: {body}")
        return _loads_json_object(content)

    def _login_paic_agents(self) -> str:
        body = self._post_json(
            self.login_path.format(app_id=self.app_id, bot_id=self.bot_id),
            {
                "app_id": self.app_id,
                "appId": self.app_id,
                "app_key": self.app_key,
                "appKey": self.app_key,
            },
        )
        token = _extract_first_string(
            body,
            [
                ("data", "token"),
                ("data", "access_token"),
                ("token",),
                ("access_token",),
                ("data", "accessToken"),
                ("accessToken",),
            ],
        )
        if not token:
            raise RuntimeError(f"Internal LLM login response missing token: {body}")
        return token

    def _create_paic_conversation(self, token: str) -> str:
        body = self._post_json(
            self.conversation_path.format(app_id=self.app_id, bot_id=self.bot_id),
            {
                "bot_id": self.bot_id,
                "botId": self.bot_id,
                "name": "lufax-dashboard-insight",
            },
            token=token,
        )
        conversation_id = _extract_first_string(
            body,
            [
                ("data", "conversation_id"),
                ("data", "conversationId"),
                ("data", "id"),
                ("conversation_id",),
                ("conversationId",),
                ("id",),
            ],
        )
        if not conversation_id:
            raise RuntimeError(f"Internal LLM conversation response missing id: {body}")
        return conversation_id

    def _post_json(self, path: str, payload: Dict[str, Any], token: Optional[str] = None) -> Dict[str, Any]:
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = f"Bearer {token}"
            headers["X-Auth-Token"] = token
        request = urllib.request.Request(
            f"{self.base_url}{path}",
            data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
            headers=headers,
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return json.loads(response.read().decode("utf-8"))
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
            raise RuntimeError(f"Internal LLM request failed at {path}: {exc}") from exc


def get_llm_adapter() -> BaseLLMAdapter:
    try:
        return InternalLLMAdapter()
    except ValueError:
        return MockLLMAdapter()


def _extract_first_string(body: Any, paths: list[tuple[Any, ...]]) -> Optional[str]:
    for path in paths:
        value = body
        for part in path:
            if isinstance(part, int):
                if not isinstance(value, list) or len(value) <= part:
                    value = None
                    break
                value = value[part]
            else:
                if not isinstance(value, dict):
                    value = None
                    break
                value = value.get(part)
        if isinstance(value, str) and value:
            return value
        if isinstance(value, (int, float)):
            return str(value)
    return None


def _loads_json_object(content: str) -> Dict[str, Any]:
    text = content.strip()
    if text.startswith("```"):
        text = text.strip("`")
        if text.lower().startswith("json"):
            text = text[4:].strip()
    try:
        parsed = json.loads(text)
    except json.JSONDecodeError:
        start = text.find("{")
        end = text.rfind("}")
        if start < 0 or end <= start:
            raise
        parsed = json.loads(text[start : end + 1])
    if not isinstance(parsed, dict):
        raise RuntimeError("Internal LLM returned JSON that is not an object")
    return parsed
