from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict, List, Optional

try:
    from . import fact_builder
    from .llm_adapter import BaseLLMAdapter, get_llm_adapter
except ImportError:  # pragma: no cover
    import fact_builder  # type: ignore
    from llm_adapter import BaseLLMAdapter, get_llm_adapter  # type: ignore


BACKEND_DIR = Path(__file__).resolve().parents[1]
PROMPT_DIR = BACKEND_DIR / "prompts"
SCHEMA_DIR = BACKEND_DIR / "schemas"

try:
    import jsonschema  # type: ignore

    JSONSCHEMA_AVAILABLE = True
except ImportError:  # pragma: no cover - exercised only in minimal envs
    jsonschema = None  # type: ignore
    JSONSCHEMA_AVAILABLE = False


class InsightValidationError(ValueError):
    pass


def get_dashboard_data() -> Dict[str, Any]:
    data = fact_builder.load_dashboard_data()
    cards = fact_builder.load_card_config()
    all_metrics = data.get("core_metrics", []) + data.get("segment_metrics", [])
    fact_pack = fact_builder.build_dashboard_fact_pack(data.get("period"))
    try:
        from . import insight_repo as repo  # type: ignore
    except ImportError:  # pragma: no cover
        import insight_repo as repo  # type: ignore
    metric_version = repo.get_metric_version(data.get("period", ""))
    return {
        "period": data.get("period"),
        "metric_version": metric_version,
        "core_metrics": data.get("core_metrics", []),
        "segment_metrics": data.get("segment_metrics", []),
        "scenario_defaults": data.get("scenario_defaults", []),
        "rules": data.get("rules", {}),
        "cards": cards,
        "fact_pack_rules": fact_pack.get("rule_result", {}),
    }


def build_card_fact_pack(card_id: str, period: Optional[str] = None) -> Dict[str, Any]:
    return fact_builder.build_card_fact_pack(card_id, period)


def build_dashboard_fact_pack(period: Optional[str] = None) -> Dict[str, Any]:
    return fact_builder.build_dashboard_fact_pack(period)


def build_scenario_fact_pack(
    scenario_id: str = "base",
    overrides: Optional[Dict[str, Any]] = None,
    period: Optional[str] = None,
) -> Dict[str, Any]:
    return fact_builder.build_scenario_fact_pack(scenario_id, overrides, period)


def generate_card_insight(card_id: str, period: Optional[str] = None, adapter: Optional[BaseLLMAdapter] = None) -> Dict[str, Any]:
    fact_pack = fact_builder.build_card_fact_pack(card_id, period)
    return _generate("card_insight_v2.txt", "card_insight.schema.json", fact_pack, adapter)


def generate_dashboard_insight(period: Optional[str] = None, adapter: Optional[BaseLLMAdapter] = None) -> Dict[str, Any]:
    fact_pack = fact_builder.build_dashboard_fact_pack(period)
    return _generate("dashboard_insight_v2.txt", "dashboard_insight.schema.json", fact_pack, adapter)


def generate_scenario_insight(
    scenario_id: str = "base",
    overrides: Optional[Dict[str, Any]] = None,
    period: Optional[str] = None,
    adapter: Optional[BaseLLMAdapter] = None,
) -> Dict[str, Any]:
    fact_pack = fact_builder.build_scenario_fact_pack(scenario_id, overrides, period)
    return _generate("scenario_insight_v2.txt", "scenario_insight.schema.json", fact_pack, adapter)


def validation_status() -> Dict[str, Any]:
    llm = get_llm_adapter()
    return {
        "jsonschema_available": JSONSCHEMA_AVAILABLE,
        "validator": "jsonschema" if JSONSCHEMA_AVAILABLE else "fallback_required_keys",
        "llm": {
            "adapter": llm.name,
            "protocol": getattr(llm, "protocol", "mock"),
            "base_url_configured": bool(getattr(llm, "base_url", "")),
            "app_id_configured": bool(getattr(llm, "app_id", "")),
            "bot_id_configured": bool(getattr(llm, "bot_id", "")),
        },
    }


def _generate(prompt_file: str, schema_file: str, fact_pack: Dict[str, Any], adapter: Optional[BaseLLMAdapter]) -> Dict[str, Any]:
    schema = _load_json(SCHEMA_DIR / schema_file)
    prompt = _load_text(PROMPT_DIR / prompt_file)
    llm = adapter or get_llm_adapter()
    output = llm.generate_json(prompt, fact_pack, schema)
    errors = _validate_output(output, schema)
    if errors:
        raise InsightValidationError(f"LLM output failed schema validation for {schema_file}: {'; '.join(errors)}")
    return output


def _validate_output(output: Dict[str, Any], schema: Dict[str, Any]) -> List[str]:
    if JSONSCHEMA_AVAILABLE:
        validator = jsonschema.Draft7Validator(schema)  # type: ignore[union-attr]
        return [f"{'/'.join(str(p) for p in err.path) or '$'}: {err.message}" for err in validator.iter_errors(output)]
    return _fallback_validate(output, schema)


def _fallback_validate(output: Dict[str, Any], schema: Dict[str, Any]) -> List[str]:
    errors: List[str] = []
    if not isinstance(output, dict):
        return ["$ must be an object"]
    for key in schema.get("required", []):
        if key not in output:
            errors.append(f"{key}: required property missing")
    properties = schema.get("properties", {})
    for key, expected in properties.items():
        if key not in output:
            continue
        expected_type = expected.get("type")
        if expected_type and not _matches_type(output[key], expected_type):
            errors.append(f"{key}: expected {expected_type}")
    return errors


def _matches_type(value: Any, expected_type: Any) -> bool:
    type_map = {
        "object": dict,
        "array": list,
        "string": str,
        "number": (int, float),
        "integer": int,
        "boolean": bool,
    }
    if isinstance(expected_type, list):
        return any(_matches_type(value, one) for one in expected_type)
    if expected_type == "null":
        return value is None
    py_type = type_map.get(expected_type)
    return True if py_type is None else isinstance(value, py_type)


def _load_json(path: Path) -> Dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def _load_text(path: Path) -> str:
    with path.open("r", encoding="utf-8") as handle:
        return handle.read()
