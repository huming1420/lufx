-- PostgreSQL initialization script (migrated from SQLite)
-- SQLite -> PostgreSQL adaptations:
--   AUTOINCREMENT -> SERIAL
--   INTEGER (boolean) -> BOOLEAN
--   datetime('now') -> now()
--   TEXT -> TEXT (PostgreSQL supports TEXT natively)
--   INSERT OR REPLACE -> ON CONFLICT ... DO UPDATE (see note below)

CREATE TABLE IF NOT EXISTS metric_snapshot (
    id              BIGSERIAL PRIMARY KEY,
    period          VARCHAR(40) NOT NULL,
    metric_version  VARCHAR(80) NOT NULL,
    source_type     VARCHAR(30) NOT NULL,
    source_label    VARCHAR(100) NOT NULL,
    source_file     VARCHAR(200),
    imported_at     TIMESTAMP DEFAULT now(),
    UNIQUE(period, metric_version)
);

CREATE TABLE IF NOT EXISTS business_metric (
    id              BIGSERIAL PRIMARY KEY,
    period          VARCHAR(40) NOT NULL,
    metric_version  VARCHAR(80) NOT NULL,
    metric_scope    VARCHAR(20) NOT NULL,
    metric_code     VARCHAR(80) NOT NULL,
    metric_name     VARCHAR(120),
    module          VARCHAR(40),
    dimension_key   VARCHAR(120) NOT NULL DEFAULT '',
    segment         VARCHAR(120),
    metric_value    DOUBLE PRECISION,
    display_value   VARCHAR(80),
    unit            VARCHAR(30),
    yoy             DOUBLE PRECISION,
    display_yoy     VARCHAR(80),
    mom             DOUBLE PRECISION,
    budget          DOUBLE PRECISION,
    budget_gap      DOUBLE PRECISION,
    display_budget_gap VARCHAR(80),
    redline         DOUBLE PRECISION,
    yellowline      DOUBLE PRECISION,
    distance_to_redline DOUBLE PRECISION,
    display_distance_to_redline VARCHAR(80),
    direction       VARCHAR(40),
    source_status   VARCHAR(20),
    product_type    VARCHAR(80),
    channel_type    VARCHAR(80),
    customer_type   VARCHAR(80),
    vintage         VARCHAR(40),
    mob             VARCHAR(40),
    UNIQUE(period, metric_version, metric_scope, metric_code, dimension_key)
);

CREATE TABLE IF NOT EXISTS card_definition (
    card_id         VARCHAR(80) PRIMARY KEY,
    title           VARCHAR(120) NOT NULL,
    module          VARCHAR(40),
    prompt_version  VARCHAR(40) DEFAULT 'v2'
);

CREATE TABLE IF NOT EXISTS card_metric_binding (
    card_id         VARCHAR(80) NOT NULL,
    metric_code     VARCHAR(80) NOT NULL,
    PRIMARY KEY(card_id, metric_code)
);

CREATE TABLE IF NOT EXISTS scenario_baseline (
    period          VARCHAR(40) NOT NULL,
    metric_version  VARCHAR(80) NOT NULL,
    scenario        VARCHAR(20) NOT NULL,
    scenario_label  VARCHAR(80),
    inputs_json     TEXT NOT NULL,
    PRIMARY KEY(period, metric_version, scenario)
);

CREATE TABLE IF NOT EXISTS insight_result (
    id              SERIAL PRIMARY KEY,
    period          TEXT NOT NULL,
    metric_version  TEXT NOT NULL DEFAULT 'unknown',
    prompt_version  TEXT NOT NULL DEFAULT 'v2',
    insight_type    TEXT NOT NULL,
    card_id         TEXT NOT NULL DEFAULT '',
    status          TEXT NOT NULL DEFAULT 'pending',
    raw_llm_output  TEXT,
    raw_llm_output_json TEXT,
    raw_fact_pack_json TEXT,
    schema_name     TEXT,
    traffic_light   TEXT,
    analysis_labels_json TEXT,
    result_json     TEXT,
    error_message   TEXT,
    validated       BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now(),
    UNIQUE(period, metric_version, prompt_version, insight_type, card_id)
);

CREATE INDEX IF NOT EXISTS idx_insight_result_lookup
    ON insight_result(period, metric_version, prompt_version, insight_type, card_id);

CREATE INDEX IF NOT EXISTS idx_insight_result_status
    ON insight_result(status);

-- NOTE: SQLite INSERT OR REPLACE can be emulated in PostgreSQL as:
--   INSERT INTO insight_result (...) VALUES (...)
--   ON CONFLICT (period, metric_version, prompt_version, insight_type, card_id)
--   DO UPDATE SET ... ;

CREATE TABLE IF NOT EXISTS insight_job (
    job_id          TEXT PRIMARY KEY,
    period          TEXT NOT NULL,
    metric_version  TEXT NOT NULL DEFAULT 'unknown',
    prompt_version  TEXT NOT NULL DEFAULT 'v2',
    job_type        TEXT NOT NULL DEFAULT 'batch',
    status          TEXT NOT NULL DEFAULT 'pending',
    total_count     INTEGER DEFAULT 0,
    finished_count  INTEGER DEFAULT 0,
    failed_count    INTEGER DEFAULT 0,
    force_refresh   BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT now(),
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    error_message   TEXT
);

CREATE INDEX IF NOT EXISTS idx_insight_job_period_status
    ON insight_job(period, status);

CREATE TABLE IF NOT EXISTS insight_job_item (
    id              SERIAL PRIMARY KEY,
    job_id          TEXT NOT NULL,
    insight_type    TEXT NOT NULL,
    card_id         TEXT NOT NULL DEFAULT '',
    status          TEXT NOT NULL DEFAULT 'pending',
    result_id       INTEGER,
    error_message   TEXT,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now(),
    FOREIGN KEY (job_id) REFERENCES insight_job(job_id)
);

CREATE INDEX IF NOT EXISTS idx_insight_job_item_job
    ON insight_job_item(job_id);
