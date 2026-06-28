-- ============================================================
-- Flyway V3: 游戏性增强字段
-- 对应方案 1.1 礼物影响直播收益 / 1.2 弹幕影响直播质量 /
--          1.3 每日行动点系统 / 2.1 每日中途存档 / 2.4 难度选择
-- H2 在 MODE=MySQL 下可正常执行本脚本。
-- ============================================================

-- 1.1 礼物影响直播收益：本日累积礼物数与折算币值
ALTER TABLE day_session ADD COLUMN gift_count INT NOT NULL DEFAULT 0;
ALTER TABLE day_session ADD COLUMN gift_coin_value INT NOT NULL DEFAULT 0;

-- 1.2 弹幕影响直播质量：本日弹幕数与累积热度
ALTER TABLE day_session ADD COLUMN danmaku_count INT NOT NULL DEFAULT 0;
ALTER TABLE day_session ADD COLUMN danmaku_heat INT NOT NULL DEFAULT 0;

-- 1.3 每日行动点系统：当日剩余行动点与上限
ALTER TABLE day_session ADD COLUMN action_points INT NOT NULL DEFAULT 4;
ALTER TABLE day_session ADD COLUMN max_action_points INT NOT NULL DEFAULT 4;

-- 2.4 难度选择：EASY / STANDARD / HARD
ALTER TABLE vup ADD COLUMN difficulty VARCHAR(16) NOT NULL DEFAULT 'STANDARD';

-- 2.1 每日中途存档：manual_save_snapshot.phase 列已在 V1 中定义
-- （phase VARCHAR(64) NOT NULL），无需重复添加。
