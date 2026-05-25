from __future__ import annotations

import json
import os
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any, Dict, Optional

try:
    from dotenv import load_dotenv
    _env_path = Path(__file__).resolve().parents[2] / ".env"
    if _env_path.exists():
        load_dotenv(_env_path, override=True)
except Exception:
    pass


_DEFAULT_BASE_URL = "http://agents-api-sze.paic.com.cn"
_DEFAULT_APP_ID = "ZHJYPT"
_DEFAULT_APP_KEY = "08a11d80ff624036863e22dd5b5d4a0a"
_DEFAULT_BOT_ID = "1089944"


class LLMClient:
    def __init__(
        self,
        base_url: Optional[str] = None,
        app_id: Optional[str] = None,
        app_key: Optional[str] = None,
        bot_id: Optional[str] = None,
        timeout: int = 120,
    ) -> None:
        self.base_url = (
            base_url
            or os.getenv("INTERNAL_LLM_BASE_URL", "")
            or _DEFAULT_BASE_URL
        ).rstrip("/")
        self.app_id = app_id or os.getenv("INTERNAL_LLM_APP_ID", "") or _DEFAULT_APP_ID
        self.app_key = app_key or os.getenv("INTERNAL_LLM_APP_KEY", "") or _DEFAULT_APP_KEY
        self.bot_id = bot_id or os.getenv("INTERNAL_LLM_BOT_ID", "") or _DEFAULT_BOT_ID
        self.timeout = timeout

        if not self.base_url:
            raise ValueError("缺少 base_url")
        if not self.app_id or not self.app_key or not self.bot_id:
            raise ValueError("缺少 app_id / app_key / bot_id")

    def chat(self, prompt: str, system: str = "") -> str:
        token = self._login()
        conversation_id = self._create_conversation(token)
        message = f"{system}\n\n{prompt}" if system else prompt
        body = self._post_json(
            f"/api/v1/conversations/{conversation_id}/messages",
            {
                "bot_id": self.bot_id,
                "content": message,
                "message": message,
                "query": message,
                "stream": False,
            },
            token=token,
        )
        content = _extract(
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
            raise RuntimeError(f"LLM 响应缺少 content: {body}")
        return content

    def chat_json(self, prompt: str, system: str = "") -> Dict[str, Any]:
        content = self.chat(prompt, system)
        return _loads_json_object(content)

    def _login(self) -> str:
        body = self._post_json(
            f"/{self.app_id}/auth/login",
            {
                "app_id": self.app_id,
                "appId": self.app_id,
                "app_key": self.app_key,
                "appKey": self.app_key,
            },
        )
        token = _extract(
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
            raise RuntimeError(f"登录响应缺少 token: {body}")
        return token

    def _create_conversation(self, token: str) -> str:
        body = self._post_json(
            "/api/v1/conversations",
            {
                "bot_id": self.bot_id,
                "botId": self.bot_id,
                "name": "llm-client",
            },
            token=token,
        )
        cid = _extract(
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
        if not cid:
            raise RuntimeError(f"创建会话响应缺少 id: {body}")
        return cid

    def _post_json(
        self,
        path: str,
        payload: Dict[str, Any],
        token: Optional[str] = None,
    ) -> Dict[str, Any]:
        headers: Dict[str, str] = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = f"Bearer {token}"
            headers["X-Auth-Token"] = token

        url = f"{self.base_url}{path}"
        request = urllib.request.Request(
            url,
            data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
            headers=headers,
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                return json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as exc:
            raise RuntimeError(f"请求失败 HTTP {exc.code}: {path}") from exc
        except (urllib.error.URLError, TimeoutError) as exc:
            raise RuntimeError(f"请求失败 {path}: {exc}") from exc
        except json.JSONDecodeError as exc:
            raise RuntimeError(f"响应非合法 JSON {path}: {exc}") from exc


_default_client: Optional[LLMClient] = None


def _get_client() -> LLMClient:
    global _default_client
    if _default_client is None:
        _default_client = LLMClient()
    return _default_client


def chat(prompt: str, system: str = "") -> str:
    return _get_client().chat(prompt, system)


def chat_json(prompt: str, system: str = "") -> Dict[str, Any]:
    return _get_client().chat_json(prompt, system)


def _extract(body: Any, paths: list[tuple[Any, ...]]) -> Optional[str]:
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
        raise RuntimeError("LLM 返回的 JSON 不是对象")
    return parsed


if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print("用法: python llm_client.py <prompt> [system_prompt]")
        sys.exit(1)

    user_prompt = sys.argv[1]
    system_prompt = sys.argv[2] if len(sys.argv) > 2 else ""

    try:
        result = chat(user_prompt, system_prompt)
        print(result)
    except Exception as e:
        print(f"错误: {e}", file=sys.stderr)
        sys.exit(1)
