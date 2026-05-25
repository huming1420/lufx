CREATE TABLE IF NOT EXISTS metric_snapshot (
    id BIGSERIAL PRIMARY KEY,
    period VARCHAR(40) NOT NULL,
    metric_version VARCHAR(80) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_label VARCHAR(100) NOT NULL,
    source_file VARCHAR(200),
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(period, metric_version)
);

CREATE TABLE IF NOT EXISTS business_metric (
    id BIGSERIAL PRIMARY KEY,
    period VARCHAR(40) NOT NULL,
    metric_version VARCHAR(80) NOT NULL,
    metric_scope VARCHAR(20) NOT NULL,
    metric_code VARCHAR(80) NOT NULL,
    metric_name VARCHAR(120),
    module VARCHAR(40),
    dimension_key VARCHAR(120) NOT NULL DEFAULT '',
    segment VARCHAR(120),
    metric_value DOUBLE PRECISION,
    display_value VARCHAR(80),
    unit VARCHAR(30),
    yoy DOUBLE PRECISION,
    display_yoy VARCHAR(80),
    mom DOUBLE PRECISION,
    budget DOUBLE PRECISION,
    budget_gap DOUBLE PRECISION,
    display_budget_gap VARCHAR(80),
    redline DOUBLE PRECISION,
    yellowline DOUBLE PRECISION,
    distance_to_redline DOUBLE PRECISION,
    display_distance_to_redline VARCHAR(80),
    direction VARCHAR(40),
    source_status VARCHAR(20),
    product_type VARCHAR(80),
    channel_type VARCHAR(80),
    customer_type VARCHAR(80),
    vintage VARCHAR(40),
    mob VARCHAR(40),
    UNIQUE(period, metric_version, metric_scope, metric_code, dimension_key)
);

CREATE TABLE IF NOT EXISTS card_definition (
    card_id VARCHAR(80) PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    module VARCHAR(40),
    prompt_version VARCHAR(40) DEFAULT 'v2'
);

CREATE TABLE IF NOT EXISTS card_metric_binding (
    card_id VARCHAR(80) NOT NULL,
    metric_code VARCHAR(80) NOT NULL,
    PRIMARY KEY(card_id, metric_code)
);

CREATE TABLE IF NOT EXISTS scenario_baseline (
    period VARCHAR(40) NOT NULL,
    metric_version VARCHAR(80) NOT NULL,
    scenario VARCHAR(20) NOT NULL,
    scenario_label VARCHAR(80),
    inputs_json TEXT NOT NULL,
    PRIMARY KEY(period, metric_version, scenario)
);

CREATE TABLE IF NOT EXISTS insight_result (
    id BIGSERIAL PRIMARY KEY,
    period VARCHAR(40),
    metric_version VARCHAR(80),
    prompt_version VARCHAR(40),
    insight_type VARCHAR(30),
    card_id VARCHAR(100),
    scenario VARCHAR(50),
    metric_date VARCHAR(40),
    scenario_hash VARCHAR(64),
    insight_json TEXT,
    score DOUBLE PRECISION,
    offset_factor DOUBLE PRECISION,
    version VARCHAR(64),
    status VARCHAR(20) DEFAULT 'pending',
    raw_llm_output TEXT,
    raw_llm_output_json TEXT,
    raw_fact_pack_json TEXT,
    schema_name VARCHAR(80),
    traffic_light VARCHAR(20),
    analysis_labels_json TEXT,
    result_json TEXT,
    error_message TEXT,
    validated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(period, metric_version, prompt_version, insight_type, card_id)
);

CREATE TABLE IF NOT EXISTS insight_job (
    job_id BIGSERIAL PRIMARY KEY,
    period VARCHAR(40),
    metric_version VARCHAR(80),
    prompt_version VARCHAR(40),
    job_type VARCHAR(30),
    status VARCHAR(20) DEFAULT 'pending',
    total_count INTEGER DEFAULT 0,
    finished_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    total_items INTEGER DEFAULT 0,
    force_refresh BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    error_message TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS insight_job_item (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT REFERENCES insight_job(job_id),
    insight_type VARCHAR(30),
    card_id VARCHAR(100),
    scenario VARCHAR(50),
    metric_date VARCHAR(40),
    input_data JSON,
    status VARCHAR(20) DEFAULT 'pending',
    result_id BIGINT,
    result_json TEXT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
