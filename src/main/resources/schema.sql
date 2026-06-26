CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    coin INT NOT NULL DEFAULT 0,
    restart_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vup (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    slot_number INT NOT NULL DEFAULT 1,
    name VARCHAR(64) NOT NULL,
    persona VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    day_count INT NOT NULL,
    run_seed VARCHAR(64) NOT NULL,
    song_power INT NOT NULL,
    dance_power INT NOT NULL,
    talk_power INT NOT NULL,
    meme_power INT NOT NULL,
    plan_power INT NOT NULL,
    stress_power INT NOT NULL,
    stamina INT NOT NULL,
    max_stamina INT NOT NULL,
    coin INT NOT NULL,
    inspiration INT NOT NULL,
    fans INT NOT NULL,
    true_fans INT NOT NULL,
    fun_fans INT NOT NULL,
    unicorn_fans INT NOT NULL,
    dd_fans INT NOT NULL,
    popularity INT NOT NULL,
    watch_heat INT NOT NULL,
    reputation INT NOT NULL,
    meme_level INT NOT NULL,
    commercial_level INT NOT NULL,
    current_route VARCHAR(64) NOT NULL,
    expectation_json TEXT NOT NULL,
    route_score_json TEXT NOT NULL,
    tutorial_flags_json TEXT NOT NULL,
    previous_ending_id BIGINT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vup_user_status ON vup(user_id, status);

CREATE TABLE IF NOT EXISTS save_slot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    slot_number INT NOT NULL DEFAULT 1,
    display_name VARCHAR(64) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_save_slot_user_slot ON save_slot(user_id, slot_number);

CREATE TABLE IF NOT EXISTS manual_save_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    slot_number INT NOT NULL,
    vup_id BIGINT NOT NULL,
    day INT NOT NULL,
    phase VARCHAR(64) NOT NULL,
    current_route VARCHAR(64) NOT NULL,
    summary VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_manual_save_snapshot_slot
    ON manual_save_snapshot(user_id, slot_number);

CREATE TABLE IF NOT EXISTS day_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vup_id BIGINT NOT NULL,
    day INT NOT NULL,
    phase VARCHAR(64) NOT NULL,
    selected_action VARCHAR(64),
    selected_plan_id BIGINT,
    selected_title_template_id BIGINT,
    title_candidates_json TEXT NOT NULL,
    title_reroll_count INT NOT NULL DEFAULT 0,
    stream_plan_cancelled BOOLEAN NOT NULL DEFAULT FALSE,
    risk_tool_used BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_plan_snapshot_json TEXT,
    pending_interaction_event_id BIGINT,
    pending_formal_event_id BIGINT,
    formal_event_slot_status VARCHAR(32) NOT NULL DEFAULT 'EMPTY',
    formal_event_source VARCHAR(64),
    formal_event_priority INT NOT NULL DEFAULT 0,
    formal_event_roll_detail_json TEXT,
    pending_action_result_json TEXT,
    pending_event_result_json TEXT,
    report_id BIGINT,
    ending_review_id BIGINT,
    random_seed VARCHAR(64) NOT NULL,
    rng_cursor INT NOT NULL DEFAULT 0,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    off_stream_action VARCHAR(64),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_day_session_vup_day UNIQUE (vup_id, day)
);

CREATE TABLE IF NOT EXISTS api_idempotency_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vup_id BIGINT,
    day INT,
    api_path VARCHAR(255) NOT NULL,
    phase VARCHAR(64),
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_code VARCHAR(64),
    expire_time DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_idempotency_user_path_key
    ON api_idempotency_record(user_id, api_path, idempotency_key);

CREATE TABLE IF NOT EXISTS business_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vup_id BIGINT NOT NULL,
    day_session_id BIGINT,
    day INT NOT NULL,
    phase VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128),
    action VARCHAR(64),
    meme_subtype VARCHAR(64),
    plan_id BIGINT,
    title_template_id BIGINT,
    interaction_event_id BIGINT,
    event_id BIGINT,
    risk_tool_id VARCHAR(64),
    fan_group_topic_id VARCHAR(64),
    result TEXT NOT NULL,
    raw_fan_gain INT NOT NULL DEFAULT 0,
    final_fan_gain INT NOT NULL DEFAULT 0,
    multiplier_detail TEXT,
    cap_detail TEXT,
    clamp_detail TEXT,
    weight_detail TEXT,
    rng_detail TEXT,
    fan_change INT NOT NULL DEFAULT 0,
    true_fan_change INT NOT NULL DEFAULT 0,
    fun_fan_change INT NOT NULL DEFAULT 0,
    unicorn_fan_change INT NOT NULL DEFAULT 0,
    dd_fan_change INT NOT NULL DEFAULT 0,
    popularity_change INT NOT NULL DEFAULT 0,
    watch_heat_change INT NOT NULL DEFAULT 0,
    reputation_change INT NOT NULL DEFAULT 0,
    meme_change INT NOT NULL DEFAULT 0,
    commercial_change INT NOT NULL DEFAULT 0,
    coin_change INT NOT NULL DEFAULT 0,
    inspiration_change INT NOT NULL DEFAULT 0,
    expectation_change TEXT,
    route_score_change TEXT,
    debt_ids TEXT,
    accident_material_ids TEXT,
    report_id BIGINT,
    ending_ref_flag BOOLEAN NOT NULL DEFAULT FALSE,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_business_log_vup_day ON business_log(vup_id, day);
CREATE INDEX IF NOT EXISTS idx_business_log_vup_phase ON business_log(vup_id, phase);

CREATE TABLE IF NOT EXISTS daily_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vup_id BIGINT NOT NULL,
    day INT NOT NULL,
    summary TEXT NOT NULL,
    selected_title VARCHAR(255),
    report_tone VARCHAR(64) NOT NULL,
    platform_trend_id VARCHAR(64),
    fan_delta INT NOT NULL DEFAULT 0,
    coin_delta INT NOT NULL DEFAULT 0,
    popularity_delta INT NOT NULL DEFAULT 0,
    watch_heat_delta INT NOT NULL DEFAULT 0,
    reputation_delta INT NOT NULL DEFAULT 0,
    meme_delta INT NOT NULL DEFAULT 0,
    commercial_delta INT NOT NULL DEFAULT 0,
    highlight_event_id BIGINT,
    debt_summary TEXT,
    risk_hint TEXT NOT NULL,
    visible_items_json TEXT NOT NULL,
    evidence_refs_json TEXT NOT NULL,
    template_refs_json TEXT NOT NULL,
    render_version VARCHAR(32) NOT NULL,
    strategy_panel_json TEXT NOT NULL DEFAULT '{}',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_daily_report_vup_day UNIQUE (vup_id, day)
);

CREATE INDEX IF NOT EXISTS idx_daily_report_vup_day ON daily_report(vup_id, day);

CREATE TABLE IF NOT EXISTS ending_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vup_id BIGINT NOT NULL,
    final_report_id BIGINT NOT NULL,
    ending_type VARCHAR(64) NOT NULL,
    final_title VARCHAR(128) NOT NULL,
    subtitle VARCHAR(255) NOT NULL,
    ending_tags_json TEXT NOT NULL,
    ending_reason TEXT NOT NULL,
    ending_reason_json TEXT NOT NULL DEFAULT '{}',
    summary TEXT NOT NULL,
    fan_profile_json TEXT NOT NULL,
    key_events_json TEXT NOT NULL,
    debt_refs_json TEXT NOT NULL,
    route_review_json TEXT NOT NULL,
    restart_hint TEXT NOT NULL,
    timeline_json TEXT NOT NULL DEFAULT '[]',
    career_stats_json TEXT NOT NULL DEFAULT '{}',
    player_profile_json TEXT NOT NULL DEFAULT '{}',
    comparison_json TEXT NOT NULL DEFAULT '{}',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ending_review_vup ON ending_review(vup_id);

CREATE TABLE IF NOT EXISTS risk_debt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vup_id BIGINT NOT NULL,
    source_log_id BIGINT,
    debt_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    severity INT NOT NULL,
    create_day INT NOT NULL,
    due_day INT NOT NULL,
    source_action VARCHAR(64),
    source_title VARCHAR(255),
    summary TEXT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_risk_debt_vup_status ON risk_debt(vup_id, status);

CREATE TABLE IF NOT EXISTS game_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    content_key VARCHAR(100) NOT NULL,
    sub_key VARCHAR(100),
    content_text TEXT NOT NULL,
    content_json TEXT,
    sort_order INT DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    UNIQUE KEY uk_content_category_key (category, content_key, sub_key)
);

CREATE TABLE IF NOT EXISTS game_unlock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    unlock_type VARCHAR(64) NOT NULL,
    unlock_key VARCHAR(128) NOT NULL,
    source_ending_type VARCHAR(64),
    source_run_score INT DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_game_unlock_user_key UNIQUE (user_id, unlock_type, unlock_key)
);

CREATE INDEX IF NOT EXISTS idx_game_unlock_user ON game_unlock(user_id);

CREATE TABLE IF NOT EXISTS game_stats_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vup_id BIGINT NOT NULL,
    ending_type VARCHAR(64),
    final_score INT DEFAULT 0,
    final_grade VARCHAR(4),
    final_fans INT DEFAULT 0,
    final_reputation INT DEFAULT 0,
    run_days INT DEFAULT 30,
    route_key VARCHAR(64),
    ng_plus_level INT DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_game_stats_user ON game_stats_history(user_id);

CREATE TABLE IF NOT EXISTS p0_seed_manifest (
    seed_key VARCHAR(64) PRIMARY KEY,
    seed_category VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    expected_count INT NOT NULL,
    detail VARCHAR(255) NOT NULL
);
