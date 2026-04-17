from __future__ import annotations

import json
import os
from dataclasses import dataclass
from typing import Optional
from urllib import error as urlerror
from urllib import request as urlrequest

from flask import Flask, jsonify, render_template, request


@dataclass
class ModerationResult:
    ok: bool
    filter_name: Optional[str] = None
    reason: Optional[str] = None
    system_error: bool = False


class AIServiceError(Exception):
    """Raised when AI moderation backend is unavailable or returns invalid data."""


class OllamaClient:
    """Minimal client for local free LLM moderation with Ollama."""

    def __init__(self) -> None:
        self.base_url = os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434").strip().rstrip("/")
        self.endpoint = f"{self.base_url}/api/chat"
        self.tags_endpoint = f"{self.base_url}/api/tags"
        self.model = os.getenv("OLLAMA_MODEL", "gemini-3-flash-preview:cloud").strip()
        self.timeout = int(os.getenv("OLLAMA_TIMEOUT_SEC", "30"))

    def list_models(self) -> list[str]:
        req = urlrequest.Request(self.tags_endpoint, method="GET")

        try:
            with urlrequest.urlopen(req, timeout=self.timeout) as response:
                body = response.read().decode("utf-8")
        except urlerror.HTTPError as exc:
            details = exc.read().decode("utf-8", errors="replace") if exc.fp else str(exc)
            raise AIServiceError(f"Ollama HTTP {exc.code}: {details[:160]}") from exc
        except Exception as exc:  # noqa: BLE001
            raise AIServiceError(f"Ошибка запроса к Ollama: {exc}") from exc

        try:
            data = json.loads(body)
            models = data.get("models", [])
            model_names = []

            for item in models:
                name = str(item.get("name", "")).strip()
                if name:
                    model_names.append(name)

            return model_names
        except Exception as exc:  # noqa: BLE001
            raise AIServiceError(f"Некорректный ответ Ollama: {exc}") from exc

    def moderate_for_filter(self, text: str, filter_name: str, policy: str, model: Optional[str] = None) -> tuple[bool, str]:
        selected_model = (model or self.model).strip()
        prompt = (
            "Ты модератор контента. Проверь сообщение только по одному правилу. "
            "Отвечай строго JSON без пояснений. Формат: "
            '{"violation": true/false, "reason": "короткая причина на русском"}. '\
            f"Правило: {policy}. Сообщение: {text}"
        )

        payload = json.dumps(
            {
                "model": selected_model,
                "stream": False,
                "format": "json",
                "messages": [
                    {"role": "system", "content": "Ты строгий модератор контента."},
                    {"role": "user", "content": prompt},
                ],
            }
        ).encode("utf-8")

        req = urlrequest.Request(
            self.endpoint,
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST",
        )

        try:
            with urlrequest.urlopen(req, timeout=self.timeout) as response:
                body = response.read().decode("utf-8")
        except urlerror.HTTPError as exc:
            details = exc.read().decode("utf-8", errors="replace") if exc.fp else str(exc)
            raise AIServiceError(f"Ollama HTTP {exc.code}: {details[:160]}") from exc
        except Exception as exc:  # noqa: BLE001
            raise AIServiceError(f"Ошибка запроса к Ollama: {exc}") from exc

        try:
            data = json.loads(body)
            content = data["message"]["content"]
            decision = json.loads(content)
            violation = bool(decision.get("violation", False))
            reason = str(decision.get("reason", "")).strip()
        except Exception as exc:  # noqa: BLE001
            raise AIServiceError(f"Некорректный ответ Ollama: {exc}") from exc

        if violation:
            return True, reason or f"Нарушение найдено ({filter_name.lower()})"
        return False, ""


class ContentFilter:
    """Base link in a moderation chain (Chain of Responsibility)."""

    def __init__(self, name: str, ai_client: OllamaClient, policy: str, model: Optional[str] = None) -> None:
        self.name = name
        self._next: Optional[ContentFilter] = None
        self._client = ai_client
        self._policy = policy
        self._model = model
        self._reason_text = ""

    def set_next(self, next_filter: ContentFilter) -> ContentFilter:
        self._next = next_filter
        return next_filter

    def handle(self, text: str) -> ModerationResult:
        if self._violates(text):
            return ModerationResult(ok=False, filter_name=self.name, reason=self._reason())
        if self._next is not None:
            return self._next.handle(text)
        return ModerationResult(ok=True)

    def _violates(self, text: str) -> bool:
        violation, reason = self._client.moderate_for_filter(
            text=text,
            filter_name=self.name,
            policy=self._policy,
            model=self._model,
        )
        self._reason_text = reason
        return violation

    def _reason(self) -> str:
        return self._reason_text or "Контент не прошел проверку ИИ"

class ProfanityFilter(ContentFilter):
    def __init__(self, ai_client: OllamaClient, model: Optional[str] = None) -> None:
        super().__init__(
            name="Проверка на мат",
            ai_client=ai_client,
            policy="Определи, есть ли ненормативная лексика, мат или грубые ругательства.",
            model=model,
        )

class SpamFilter(ContentFilter):
    def __init__(self, ai_client: OllamaClient, model: Optional[str] = None) -> None:
        super().__init__(
            name="Проверка на спам",
            ai_client=ai_client,
            policy=(
                "Определи, является ли сообщение спамом: навязчивая реклама, массовые повторы, "
                "бессмысленные повторы, агрессивный призыв купить."
            ),
            model=model,
        )

class BannedTopicsFilter(ContentFilter):
    def __init__(self, ai_client: OllamaClient, model: Optional[str] = None) -> None:
        super().__init__(
            name="Проверка на запрещённые темы",
            ai_client=ai_client,
            policy=(
                "Определи, затрагивает ли сообщение запрещенные темы: пропаганда насилия, "
                "терроризма, наркотиков, незаконной деятельности."
            ),
            model=model,
        )

class LinksFilter(ContentFilter):
    def __init__(self, ai_client: OllamaClient, model: Optional[str] = None) -> None:
        super().__init__(
            name="Проверка на ссылки",
            ai_client=ai_client,
            policy=(
                "Определи, содержит ли сообщение ссылку, URL, домен, приглашение перейти "
                "на внешний сайт или мессенджер."
            ),
            model=model,
        )

class InsultsFilter(ContentFilter):
    def __init__(self, ai_client: OllamaClient, model: Optional[str] = None) -> None:
        super().__init__(
            name="Проверка на оскорбления",
            ai_client=ai_client,
            policy="Определи, есть ли в сообщении прямые оскорбления, унижения, токсичные личные нападки.",
            model=model,
        )


class Moderator:
    FILTER_CLASSES = {
        "profanity": ProfanityFilter,
        "spam": SpamFilter,
        "banned_topics": BannedTopicsFilter,
        "links": LinksFilter,
        "insults": InsultsFilter,
    }

    ORDER = ["profanity", "spam", "banned_topics", "links", "insults"]

    def __init__(self) -> None:
        self._ai_client = OllamaClient()

    @property
    def default_model(self) -> str:
        return self._ai_client.model

    def available_models(self) -> list[str]:
        return self._ai_client.list_models()

    def _build_filter(self, key: str, model: Optional[str] = None) -> ContentFilter:
        filter_class = self.FILTER_CLASSES[key]
        return filter_class(ai_client=self._ai_client, model=model)

    def moderate(self, text: str, enabled_filters: dict[str, bool], model: Optional[str] = None) -> ModerationResult:
        chain_start: Optional[ContentFilter] = None
        chain_current: Optional[ContentFilter] = None

        for key in self.ORDER:
            if not enabled_filters.get(key, False):
                continue

            next_filter = self._build_filter(key, model=model)
            if chain_start is None:
                chain_start = next_filter
                chain_current = chain_start
                continue

            assert chain_current is not None
            chain_current = chain_current.set_next(next_filter)

        if chain_start is None:
            return ModerationResult(ok=True, filter_name="Нет активных фильтров", reason="Проверка пропущена")

        try:
            return chain_start.handle(text)
        except AIServiceError as exc:
            return ModerationResult(
                ok=False,
                reason=f"{exc}",
                system_error=True,
            )


app = Flask(__name__)
moderator = Moderator()


@app.get("/")
def index() -> str:
    return render_template("index.html")


@app.get("/api/models")
def get_models():
    try:
        models = moderator.available_models()
    except AIServiceError as exc:
        return jsonify({"models": [], "default_model": moderator.default_model, "error": str(exc)}), 503

    return jsonify({"models": models, "default_model": moderator.default_model})


@app.post("/api/moderate")
def moderate_message():
    payload = request.get_json(silent=True) or {}
    text = str(payload.get("message", "")).strip()
    enabled_filters = payload.get("filters", {})
    selected_model = payload.get("model", None)

    if not text:
        return jsonify({"ok": False, "filter": "Валидация формы", "reason": "Введите сообщение для проверки"}), 400

    if not isinstance(enabled_filters, dict):
        return jsonify({"ok": False, "filter": "Валидация формы", "reason": "Некорректный список фильтров"}), 400

    if selected_model is not None and not isinstance(selected_model, str):
        return jsonify({"ok": False, "filter": "Валидация формы", "reason": "Некорректная модель"}), 400

    if isinstance(selected_model, str):
        selected_model = selected_model.strip() or None

    result = moderator.moderate(text, enabled_filters, model=selected_model)
    status_code = 503 if result.system_error else 200
    return jsonify(
        {
            "ok": result.ok,
            "filter": result.filter_name,
            "reason": result.reason,
            "system_error": result.system_error,
        }
    ), status_code


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080, debug=True)
