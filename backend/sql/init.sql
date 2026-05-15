-- Insight result cache table
CREATE TABLE IF NOT EXISTS insight_result (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    period TEXT NOT NULL,
    metric_version TEXT NOT NULL DEFAULT '',
    prompt_version TEXT NOT NULL DEFAULT 'v2',
    schema_name TEXT NOT NULL DEFAULT '',
    insight_type TEXT NOT NULL,
    card_id TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'pending',
    title TEXT NOT NULL DEFAULT '',
    standard_insight TEXT NOT NULL DEFAULT '',
    standard_diagnosis_json TEXT NOT NULL DEFAULT '',
    standard_explanation_json TEXT NOT NULL DEFAULT '',
    drivers_json TEXT NOT NULL DEFAULT '[]',
    watch_items_json TEXT NOT NULL DEFAULT '[]',
    evidence_json TEXT NOT NULL DEFAULT '[]',
    exploratory_insights_json TEXT NOT NULL DEFAULT '[]',
    emerging_findings_json TEXT NOT NULL DEFAULT '[]',
    scenario_exploration_json TEXT NOT NULL DEFAULT '{}',
    deep_dive_questions_json TEXT NOT NULL DEFAULT '[]',
    management_questions_json TEXT NOT NULL DEFAULT '[]',
    raw_fact_pack_json TEXT NOT NULL DEFAULT '{}',
    raw_llm_output_json TEXT NOT NULL DEFAULT '{}',
    validated INTEGER NOT NULL DEFAULT 0,
    validation_error TEXT NOT NULL DEFAULT '',
    error_message TEXT NOT NULL DEFAULT '',
    started_at TEXT,
    finished_at TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(period, metric_version, prompt_version, insight_type, card_id)
);

-- Insight job table
CREATE TABLE IF NOT EXISTS insight_job (
    job_id TEXT PRIMARY KEY,
    period TEXT NOT NULL,
    metric_version TEXT NOT NULL DEFAULT '',
    prompt_version TEXT NOT NULL DEFAULT 'v2',
    job_type TEXT NOT NULL DEFAULT 'all',
    status TEXT NOT NULL DEFAULT 'pending',
    total_count INTEGER NOT NULL DEFAULT 0,
    finished_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    force_refresh INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    started_at TEXT,
    finished_at TEXT,
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    error_message TEXT NOT NULL DEFAULT ''
);

-- Insight job item table
CREATE TABLE IF NOT EXISTS insight_job_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id TEXT NOT NULL,
    insight_type TEXT NOT NULL,
    card_id TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'pending',
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT NOT NULL DEFAULT '',
    started_at TEXT,
    finished_at TEXT,
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (job_id) REFERENCES insight_job(job_id)
);

CREATE INDEX IF NOT EXISTS idx_insight_result_lookup
    ON insight_result(period, metric_version, prompt_version, insight_type, card_id);

CREATE INDEX IF NOT EXISTS idx_insight_result_status
    ON insight_result(status);

CREATE INDEX IF NOT EXISTS idx_insight_job_period_status
    ON insight_job(period, status);

CREATE INDEX IF NOT EXISTS idx_insight_job_item_job
    ON insight_job_item(job_id);
