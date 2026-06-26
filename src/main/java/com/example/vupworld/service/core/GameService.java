package com.example.vupworld.service.core;

import com.example.vupworld.common.GameException;
import com.example.vupworld.domain.VupStatus;
import com.example.vupworld.dto.AuthDtos.UserDTO;
import com.example.vupworld.dto.DayDtos.DaySessionDTO;
import com.example.vupworld.dto.GameDtos.GameBootstrapDTO;
import com.example.vupworld.dto.GameDtos.GameStartDTO;
import com.example.vupworld.dto.GameDtos.SaveArchiveDTO;
import com.example.vupworld.dto.GameDtos.SaveSlotDTO;
import com.example.vupworld.dto.VupDtos.CreateVupRequest;
import com.example.vupworld.dto.VupDtos.VupStateDTO;
import com.example.vupworld.mapper.DaySessionMapper;
import com.example.vupworld.mapper.EndingReviewMapper;
import com.example.vupworld.mapper.ManualSaveSnapshotMapper;
import com.example.vupworld.mapper.SaveSlotMapper;
import com.example.vupworld.mapper.VupMapper;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.EndingReview;
import com.example.vupworld.model.ManualSaveSnapshot;
import com.example.vupworld.model.SaveSlot;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.AuthService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class GameService {
    private static final int LOCAL_SLOT = 1;
    private static final int MAX_LOCAL_SLOT = 3;
    private static final int SAVE_SLOT_NAME_MAX_LENGTH = 64;
    private static final String SAVE_VERSION = "singleplayer-save-v1";
    private static final List<String> VUP_IMPORT_COLUMNS = List.of(
            "user_id", "slot_number", "name", "persona", "status", "day_count", "run_seed",
            "song_power", "dance_power", "talk_power", "meme_power", "plan_power", "stress_power",
            "stamina", "max_stamina", "coin", "inspiration", "fans", "true_fans", "fun_fans",
            "unicorn_fans", "dd_fans", "popularity", "watch_heat", "reputation", "meme_level",
            "commercial_level", "current_route", "expectation_json", "route_score_json",
            "tutorial_flags_json", "previous_ending_id"
    );
    private static final List<String> DAY_SESSION_IMPORT_COLUMNS = List.of(
            "vup_id", "day", "phase", "selected_action", "selected_plan_id", "selected_title_template_id",
            "title_candidates_json", "title_reroll_count", "stream_plan_cancelled", "risk_tool_used",
            "cancelled_plan_snapshot_json", "pending_interaction_event_id", "pending_formal_event_id",
            "formal_event_slot_status", "formal_event_source", "formal_event_priority",
            "formal_event_roll_detail_json", "pending_action_result_json", "pending_event_result_json",
            "report_id", "ending_review_id", "random_seed", "rng_cursor", "locked", "off_stream_action"
    );
    private static final List<String> BUSINESS_LOG_IMPORT_COLUMNS = List.of(
            "vup_id", "day_session_id", "day", "phase", "idempotency_key", "action", "meme_subtype",
            "plan_id", "title_template_id", "interaction_event_id", "event_id", "risk_tool_id",
            "fan_group_topic_id", "result", "raw_fan_gain", "final_fan_gain", "multiplier_detail",
            "cap_detail", "clamp_detail", "weight_detail", "rng_detail", "fan_change",
            "true_fan_change", "fun_fan_change", "unicorn_fan_change", "dd_fan_change",
            "popularity_change", "watch_heat_change", "reputation_change", "meme_change",
            "commercial_change", "coin_change", "inspiration_change", "expectation_change",
            "route_score_change", "debt_ids", "accident_material_ids", "report_id", "ending_ref_flag"
    );
    private static final List<String> DAILY_REPORT_IMPORT_COLUMNS = List.of(
            "vup_id", "day", "summary", "selected_title", "report_tone", "platform_trend_id",
            "fan_delta", "coin_delta", "popularity_delta", "watch_heat_delta", "reputation_delta",
            "meme_delta", "commercial_delta", "highlight_event_id", "debt_summary", "risk_hint",
            "visible_items_json", "evidence_refs_json", "template_refs_json", "render_version",
            "strategy_panel_json"
    );
    private static final List<String> ENDING_REVIEW_IMPORT_COLUMNS = List.of(
            "vup_id", "final_report_id", "ending_type", "final_title", "subtitle", "ending_tags_json",
            "ending_reason", "ending_reason_json", "summary", "fan_profile_json", "key_events_json",
            "debt_refs_json", "route_review_json", "restart_hint", "timeline_json", "career_stats_json",
            "player_profile_json", "comparison_json"
    );
    private static final List<String> RISK_DEBT_IMPORT_COLUMNS = List.of(
            "vup_id", "source_log_id", "debt_type", "status", "severity", "create_day", "due_day",
            "source_action", "source_title", "summary"
    );
    private static final List<String> GAME_UNLOCK_IMPORT_COLUMNS = List.of(
            "user_id", "unlock_type", "unlock_key", "source_ending_type", "source_run_score"
    );
    private static final List<String> GAME_STATS_IMPORT_COLUMNS = List.of(
            "user_id", "vup_id", "ending_type", "final_score", "final_grade", "final_fans",
            "final_reputation", "run_days", "route_key", "ng_plus_level"
    );
    private static final List<String> SAVE_SLOT_IMPORT_COLUMNS = List.of(
            "user_id", "slot_number", "display_name"
    );
    private static final List<String> MANUAL_SAVE_IMPORT_COLUMNS = List.of(
            "user_id", "slot_number", "vup_id", "day", "phase", "current_route", "summary"
    );
    private static final CreateVupRequest DEFAULT_LOCAL_VUP = new CreateVupRequest(
            "露米",
            "B站直播间出道新人，主打杂谈、切片和一点点黑红体质。"
    );

    private final AuthService authService;
    private final VupService vupService;
    private final VupMapper vupMapper;
    private final SaveSlotMapper saveSlotMapper;
    private final ManualSaveSnapshotMapper manualSaveSnapshotMapper;
    private final DaySessionMapper daySessionMapper;
    private final EndingReviewMapper endingReviewMapper;
    private final VupStateMapper vupStateMapper;
    private final JdbcTemplate jdbcTemplate;

    public GameService(
            AuthService authService,
            VupService vupService,
            VupMapper vupMapper,
            SaveSlotMapper saveSlotMapper,
            ManualSaveSnapshotMapper manualSaveSnapshotMapper,
            DaySessionMapper daySessionMapper,
            EndingReviewMapper endingReviewMapper,
            VupStateMapper vupStateMapper,
            JdbcTemplate jdbcTemplate
    ) {
        this.authService = authService;
        this.vupService = vupService;
        this.vupMapper = vupMapper;
        this.saveSlotMapper = saveSlotMapper;
        this.manualSaveSnapshotMapper = manualSaveSnapshotMapper;
        this.daySessionMapper = daySessionMapper;
        this.endingReviewMapper = endingReviewMapper;
        this.vupStateMapper = vupStateMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public GameBootstrapDTO bootstrap(Long sessionUserId) {
        UserDTO user = sessionUserId == null ? authService.localPlayerIfExists() : authService.getUser(sessionUserId);
        if (user == null) {
            return new GameBootstrapDTO(false, null, null, null, emptySlots());
        }

        Vup active = vupMapper.findActiveByUserId(user.id());
        DaySession session = active == null ? null : requireCurrentSession(active);
        VupStateDTO activeRun = active == null ? null : vupStateMapper.toStateDto(active, session);
        DaySessionDTO daySession = session == null ? null : vupStateMapper.toDaySessionDto(session);
        List<SaveSlotDTO> slots = saveSlots(user.id());
        return new GameBootstrapDTO(slots.stream().anyMatch(SaveSlotDTO::occupied), user, activeRun, daySession, slots);
    }

    @Transactional
    public GameStartDTO quickStart(UserDTO user) {
        return startSlot(user, LOCAL_SLOT);
    }

    @Transactional
    public GameStartDTO startSlot(UserDTO user, int slotNumber) {
        validateSlotNumber(slotNumber);
        Vup existing = findSlotRun(user.id(), slotNumber);
        if (existing != null && canOpen(existing)) {
            return continueSlot(user, slotNumber);
        }

        vupMapper.suspendActiveByUserId(user.id());
        VupStateDTO activeRun = vupService.createVup(user.id(), DEFAULT_LOCAL_VUP, slotNumber);
        DaySessionDTO daySession = vupService.currentSession(user.id());
        return new GameStartDTO(user, activeRun, daySession, saveSlots(user.id()));
    }

    @Transactional
    public GameStartDTO restartSlot(UserDTO user, int slotNumber) {
        validateSlotNumber(slotNumber);
        if (user == null) {
            throw new GameException("NO_SAVE", "还没有本地存档，先开始第一局。");
        }
        Vup current = findSlotRun(user.id(), slotNumber);
        if (current == null) {
            throw new GameException("NO_SAVE", "还没有本地存档，先开始第一局。");
        }
        vupMapper.suspendActiveByUserId(user.id());
        vupMapper.updateStatus(current.getId(), VupStatus.ABANDONED.name());
        VupStateDTO activeRun = vupService.createVup(user.id(), DEFAULT_LOCAL_VUP, slotNumber);
        DaySessionDTO daySession = vupService.currentSession(user.id());
        return new GameStartDTO(user, activeRun, daySession, saveSlots(user.id()));
    }

    public GameStartDTO continueRun(UserDTO user) {
        return continueSlot(user, LOCAL_SLOT);
    }

    @Transactional
    public GameStartDTO continueSlot(UserDTO user, int slotNumber) {
        validateSlotNumber(slotNumber);
        if (user == null) {
            throw new GameException("NO_SAVE", "还没有本地存档，先开始第一局。");
        }
        Vup active = findSlotRun(user.id(), slotNumber);
        if (active == null || !canOpen(active)) {
            throw new GameException("NO_SAVE", "还没有可继续的本地存档，先开始第一局。");
        }
        if (!VupStatus.ACTIVE.name().equals(active.getStatus())) {
            vupMapper.suspendActiveByUserId(user.id());
            vupMapper.updateStatus(active.getId(), VupStatus.ACTIVE.name());
            active = vupMapper.findById(active.getId());
        }
        DaySession session = requireCurrentSession(active);
        return new GameStartDTO(
                user,
                vupStateMapper.toStateDto(active, session),
                vupStateMapper.toDaySessionDto(session),
                saveSlots(user.id())
        );
    }

    public List<SaveSlotDTO> saveSlots(Long userId) {
        if (userId == null) {
            return emptySlots();
        }
        return java.util.stream.IntStream.rangeClosed(1, MAX_LOCAL_SLOT)
                .mapToObj(slotNumber -> {
                    Vup run = findSlotRun(userId, slotNumber);
                    return run == null ? emptySlot(slotNumber) : toSlot(run);
                })
                .toList();
    }

    @Transactional
    public SaveSlotDTO saveSlot(Long userId, int slotNumber) {
        validateSlotNumber(slotNumber);
        Vup run = findSlotRun(userId, slotNumber);
        if (run == null) {
            throw new GameException("NO_SAVE", "还没有本地存档，先开始第一局。");
        }
        SaveSlotDTO slot = toSlot(run);
        ManualSaveSnapshot snapshot = toManualSaveSnapshot(run, slotNumber, slot);
        ManualSaveSnapshot existing = manualSaveSnapshotMapper.findByUserIdAndSlot(userId, slotNumber);
        if (existing == null) {
            manualSaveSnapshotMapper.insert(snapshot);
        } else {
            manualSaveSnapshotMapper.update(userId, slotNumber, snapshot);
        }
        return toSlot(run);
    }

    @Transactional
    public SaveSlotDTO renameSlot(UserDTO user, int slotNumber, String name) {
        validateSlotNumber(slotNumber);
        if (user == null) {
            throw new GameException("NO_SAVE", "还没有本地存档，先开始第一局。");
        }
        Vup run = findSlotRun(user.id(), slotNumber);
        if (run == null) {
            throw new GameException("NO_SAVE", "还没有本地存档，先开始第一局。");
        }

        String displayName = normalizeSlotName(name);
        SaveSlot slot = saveSlotMapper.findByUserIdAndSlot(user.id(), slotNumber);
        if (slot == null) {
            SaveSlot created = new SaveSlot();
            created.setUserId(user.id());
            created.setSlotNumber(slotNumber);
            created.setDisplayName(displayName);
            saveSlotMapper.insert(created);
        } else {
            saveSlotMapper.updateDisplayName(user.id(), slotNumber, displayName);
        }
        return toSlot(run);
    }

    public SaveArchiveDTO exportSlot(UserDTO user, int slotNumber) {
        validateSlotNumber(slotNumber);
        if (user == null) {
            throw new GameException("NO_SAVE", "No local save exists.");
        }
        Vup run = findSlotRun(user.id(), slotNumber);
        if (run == null) {
            throw new GameException("NO_SAVE", "No local save exists.");
        }
        Long vupId = run.getId();
        return new SaveArchiveDTO(
                SAVE_VERSION,
                OffsetDateTime.now().toString(),
                slotNumber,
                toSlot(run),
                firstRow("SELECT id, username, nickname, coin, restart_count, create_time, update_time FROM user_account WHERE id = ?", user.id()),
                firstRow("SELECT * FROM save_slot WHERE user_id = ? AND slot_number = ?", user.id(), slotNumber),
                firstRow("SELECT * FROM manual_save_snapshot WHERE user_id = ? AND slot_number = ? AND vup_id = ?", user.id(), slotNumber, vupId),
                requiredRow("SELECT * FROM vup WHERE id = ?", vupId),
                rows("SELECT * FROM day_session WHERE vup_id = ? ORDER BY day ASC, id ASC", vupId),
                rows("SELECT * FROM business_log WHERE vup_id = ? ORDER BY id ASC", vupId),
                rows("SELECT * FROM daily_report WHERE vup_id = ? ORDER BY day ASC, id ASC", vupId),
                rows("SELECT * FROM ending_review WHERE vup_id = ? ORDER BY id ASC", vupId),
                rows("SELECT * FROM risk_debt WHERE vup_id = ? ORDER BY id ASC", vupId),
                rows("SELECT * FROM game_unlock WHERE user_id = ? ORDER BY id ASC", user.id()),
                rows("SELECT * FROM game_stats_history WHERE user_id = ? AND vup_id = ? ORDER BY id ASC", user.id(), vupId)
        );
    }

    @Transactional
    public GameStartDTO importSlot(UserDTO user, int slotNumber, SaveArchiveDTO archive) {
        validateSlotNumber(slotNumber);
        if (user == null) {
            throw new GameException("UNAUTHORIZED", "Create a local player before importing a save.");
        }
        validateArchive(archive);

        Map<String, Object> sourceVup = archive.vup();
        Long sourceVupId = longValue(sourceVup, "id");
        if (sourceVupId == null) {
            throw new GameException("SAVE_IMPORT_INVALID", "Save archive is missing the source run id.");
        }
        List<Long> oldVupIds = jdbcTemplate.queryForList(
                "SELECT id FROM vup WHERE user_id = ? AND slot_number = ?",
                Long.class,
                user.id(),
                slotNumber
        );
        vupMapper.suspendActiveByUserId(user.id());
        deleteSlotData(user.id(), slotNumber, oldVupIds);

        Map<String, Long> daySessionIds = new HashMap<>();
        Map<String, Long> reportIds = new HashMap<>();
        Map<String, Long> endingIds = new HashMap<>();
        Map<String, Long> riskDebtIds = new HashMap<>();
        Map<String, Long> businessLogIds = new HashMap<>();

        Map<String, Object> vupRow = copyRow(sourceVup);
        vupRow.put("user_id", user.id());
        vupRow.put("slot_number", slotNumber);
        vupRow.put("status", VupStatus.ACTIVE.name());
        vupRow.put("previous_ending_id", null);
        Long newVupId = insertRow("vup", VUP_IMPORT_COLUMNS, vupRow);

        for (Map<String, Object> row : safeRows(archive.riskDebts())) {
            Map<String, Object> copy = copyRow(row);
            copy.put("vup_id", newVupId);
            copy.put("source_log_id", null);
            Long newId = insertRow("risk_debt", RISK_DEBT_IMPORT_COLUMNS, copy);
            rememberId(riskDebtIds, row, newId);
        }

        for (Map<String, Object> row : safeRows(archive.dailyReports())) {
            Map<String, Object> copy = copyRow(row);
            copy.put("vup_id", newVupId);
            Long newId = insertRow("daily_report", DAILY_REPORT_IMPORT_COLUMNS, copy);
            rememberId(reportIds, row, newId);
        }

        for (Map<String, Object> row : safeRows(archive.endingReviews())) {
            Map<String, Object> copy = copyRow(row);
            copy.put("vup_id", newVupId);
            copy.put("final_report_id", remapId(reportIds, row.get("final_report_id")));
            Long newId = insertRow("ending_review", ENDING_REVIEW_IMPORT_COLUMNS, copy);
            rememberId(endingIds, row, newId);
        }

        for (Map<String, Object> row : safeRows(archive.daySessions())) {
            Map<String, Object> copy = copyRow(row);
            copy.put("vup_id", newVupId);
            copy.put("report_id", remapId(reportIds, row.get("report_id")));
            copy.put("ending_review_id", remapId(endingIds, row.get("ending_review_id")));
            copy.put("pending_formal_event_id", remapId(riskDebtIds, row.get("pending_formal_event_id")));
            Long newId = insertRow("day_session", DAY_SESSION_IMPORT_COLUMNS, copy);
            rememberId(daySessionIds, row, newId);
        }

        for (Map<String, Object> row : safeRows(archive.businessLogs())) {
            Map<String, Object> copy = copyRow(row);
            copy.put("vup_id", newVupId);
            copy.put("day_session_id", remapId(daySessionIds, row.get("day_session_id")));
            copy.put("report_id", remapId(reportIds, row.get("report_id")));
            Long newId = insertRow("business_log", BUSINESS_LOG_IMPORT_COLUMNS, copy);
            rememberId(businessLogIds, row, newId);
        }

        for (Map<String, Object> row : safeRows(archive.riskDebts())) {
            Long newDebtId = remapId(riskDebtIds, row.get("id"));
            Long newSourceLogId = remapId(businessLogIds, row.get("source_log_id"));
            if (newDebtId != null && newSourceLogId != null) {
                jdbcTemplate.update("UPDATE risk_debt SET source_log_id = ? WHERE id = ?", newSourceLogId, newDebtId);
            }
        }

        Map<String, Object> saveSlot = copyRow(archive.saveSlot());
        if (saveSlot.isEmpty()) {
            saveSlot.put("display_name", "Imported Save " + slotNumber);
        }
        saveSlot.put("user_id", user.id());
        saveSlot.put("slot_number", slotNumber);
        insertRow("save_slot", SAVE_SLOT_IMPORT_COLUMNS, saveSlot);

        if (archive.manualSave() != null && !archive.manualSave().isEmpty()) {
            Map<String, Object> manualSave = copyRow(archive.manualSave());
            manualSave.put("user_id", user.id());
            manualSave.put("slot_number", slotNumber);
            manualSave.put("vup_id", newVupId);
            insertRow("manual_save_snapshot", MANUAL_SAVE_IMPORT_COLUMNS, manualSave);
        }

        for (Map<String, Object> row : safeRows(archive.unlocks())) {
            insertUnlockIfMissing(user.id(), row);
        }

        for (Map<String, Object> row : safeRows(archive.statsHistory())) {
            Map<String, Object> copy = copyRow(row);
            copy.put("user_id", user.id());
            copy.put("vup_id", newVupId);
            insertRow("game_stats_history", GAME_STATS_IMPORT_COLUMNS, copy);
        }

        return continueSlot(user, slotNumber);
    }

    private void validateArchive(SaveArchiveDTO archive) {
        if (archive == null) {
            throw new GameException("SAVE_IMPORT_INVALID", "Save archive is empty.");
        }
        if (!SAVE_VERSION.equals(archive.saveVersion())) {
            throw new GameException("SAVE_VERSION_UNSUPPORTED", "Save archive version is not supported.");
        }
        if (archive.vup() == null || archive.vup().isEmpty()) {
            throw new GameException("SAVE_IMPORT_INVALID", "Save archive is missing the run data.");
        }
        if (archive.daySessions() == null || archive.daySessions().isEmpty()) {
            throw new GameException("SAVE_IMPORT_INVALID", "Save archive is missing day sessions.");
        }
    }

    private Map<String, Object> firstRow(String sql, Object... args) {
        List<Map<String, Object>> result = rows(sql, args);
        return result.isEmpty() ? null : result.getFirst();
    }

    private Map<String, Object> requiredRow(String sql, Object... args) {
        Map<String, Object> row = firstRow(sql, args);
        if (row == null) {
            throw new GameException("SAVE_EXPORT_INVALID", "Save archive source data is incomplete.");
        }
        return row;
    }

    private List<Map<String, Object>> rows(String sql, Object... args) {
        return jdbcTemplate.queryForList(sql, args).stream()
                .map(this::normalizeRow)
                .toList();
    }

    private Map<String, Object> normalizeRow(Map<String, Object> source) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            normalized.put(normalizeKey(entry.getKey()), exportValue(entry.getValue()));
        }
        return normalized;
    }

    private Object exportValue(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toString();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toString();
        }
        return value;
    }

    private Map<String, Object> copyRow(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = normalizeKey(entry.getKey());
            if ("id".equals(key) || "create_time".equals(key) || "update_time".equals(key)) {
                continue;
            }
            copy.put(key, entry.getValue());
        }
        return copy;
    }

    private List<Map<String, Object>> safeRows(List<Map<String, Object>> rows) {
        return rows == null ? List.of() : rows;
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.toLowerCase(Locale.ROOT);
    }

    private void deleteSlotData(Long userId, int slotNumber, List<Long> vupIds) {
        jdbcTemplate.update("DELETE FROM save_slot WHERE user_id = ? AND slot_number = ?", userId, slotNumber);
        jdbcTemplate.update("DELETE FROM manual_save_snapshot WHERE user_id = ? AND slot_number = ?", userId, slotNumber);
        if (vupIds == null || vupIds.isEmpty()) {
            return;
        }
        deleteByVupIds("api_idempotency_record", vupIds);
        deleteByVupIds("game_stats_history", vupIds);
        deleteByVupIds("risk_debt", vupIds);
        deleteByVupIds("ending_review", vupIds);
        deleteByVupIds("daily_report", vupIds);
        deleteByVupIds("business_log", vupIds);
        deleteByVupIds("day_session", vupIds);
        deleteByIds("vup", vupIds);
    }

    private void deleteByVupIds(String tableName, List<Long> vupIds) {
        jdbcTemplate.update("DELETE FROM " + tableName + " WHERE vup_id IN (" + placeholders(vupIds.size()) + ")",
                vupIds.toArray());
    }

    private void deleteByIds(String tableName, List<Long> ids) {
        jdbcTemplate.update("DELETE FROM " + tableName + " WHERE id IN (" + placeholders(ids.size()) + ")",
                ids.toArray());
    }

    private String placeholders(int size) {
        return String.join(", ", java.util.Collections.nCopies(size, "?"));
    }

    private Long insertRow(String tableName, List<String> columns, Map<String, Object> row) {
        String columnSql = String.join(", ", columns);
        String placeholderSql = placeholders(columns.size());
        String sql = "INSERT INTO " + tableName + " (" + columnSql + ") VALUES (" + placeholderSql + ")";
        Object[] values = columns.stream()
                .map(column -> toJdbcValue(row.get(column)))
                .toArray();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[] {"id"});
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    private Object toJdbcValue(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return value;
    }

    private void insertUnlockIfMissing(Long userId, Map<String, Object> row) {
        String unlockType = stringValue(row, "unlock_type");
        String unlockKey = stringValue(row, "unlock_key");
        if (unlockType == null || unlockKey == null) {
            return;
        }
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM game_unlock WHERE user_id = ? AND unlock_type = ? AND unlock_key = ?",
                Integer.class,
                userId,
                unlockType,
                unlockKey
        );
        if (existing != null && existing > 0) {
            return;
        }
        Map<String, Object> copy = copyRow(row);
        copy.put("user_id", userId);
        insertRow("game_unlock", GAME_UNLOCK_IMPORT_COLUMNS, copy);
    }

    private void rememberId(Map<String, Long> idMap, Map<String, Object> row, Long newId) {
        Long oldId = longValue(row, "id");
        if (oldId != null && newId != null) {
            idMap.put(oldId.toString(), newId);
        }
    }

    private Long remapId(Map<String, Long> idMap, Object oldId) {
        Long normalized = longValue(oldId);
        return normalized == null ? null : idMap.get(normalized.toString());
    }

    private Long longValue(Map<String, Object> row, String key) {
        return row == null ? null : longValue(row.get(key));
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String stringValue(Map<String, Object> row, String key) {
        Object value = row == null ? null : row.get(key);
        return value == null ? null : value.toString();
    }

    private Vup findSlotRun(Long userId, int slotNumber) {
        if (userId == null) {
            return null;
        }
        Vup active = vupMapper.findActiveByUserIdAndSlot(userId, slotNumber);
        return active == null ? vupMapper.findLatestByUserIdAndSlot(userId, slotNumber) : active;
    }

    private void validateSlotNumber(int slotNumber) {
        if (slotNumber < 1 || slotNumber > MAX_LOCAL_SLOT) {
            throw new GameException("SAVE_SLOT_UNSUPPORTED", "当前版本开放 1-3 号本地存档槽。");
        }
    }

    private boolean canOpen(Vup vup) {
        String status = vup.getStatus();
        return VupStatus.ACTIVE.name().equals(status) || VupStatus.SUSPENDED.name().equals(status);
    }

    private String normalizeSlotName(String name) {
        if (name == null || name.isBlank()) {
            throw new GameException("SAVE_SLOT_NAME_INVALID", "存档名不能为空。");
        }
        String trimmed = name.trim();
        if (trimmed.length() > SAVE_SLOT_NAME_MAX_LENGTH) {
            throw new GameException("SAVE_SLOT_NAME_INVALID", "存档名不能超过64个字。");
        }
        return trimmed;
    }

    private DaySession requireCurrentSession(Vup vup) {
        DaySession session = daySessionMapper.findByVupIdAndDay(vup.getId(), vup.getDayCount());
        if (session == null) {
            throw new GameException("SYSTEM_ERROR", "本地存档缺少当前日排班，无法继续。");
        }
        return session;
    }

    private List<SaveSlotDTO> emptySlots() {
        return java.util.stream.IntStream.rangeClosed(1, MAX_LOCAL_SLOT)
                .mapToObj(this::emptySlot)
                .toList();
    }

    private SaveSlotDTO emptySlot(int slotNumber) {
        return new SaveSlotDTO(
                slotNumber,
                false,
                null,
                "空存档 " + slotNumber,
                0,
                "EMPTY",
                "EMPTY",
                "UNKNOWN",
                false,
                "还没有本地存档。",
                null,
                null,
                null,
                null
        );
    }

    private SaveSlotDTO toSlot(Vup vup) {
        DaySession session = daySessionMapper.findLatestByVupId(vup.getId());
        EndingReview ending = endingReviewMapper.findLatestByVupId(vup.getId());
        int slotNumber = Math.max(1, vup.getSlotNumber());
        SaveSlot slot = saveSlotMapper.findByUserIdAndSlot(vup.getUserId(), slotNumber);
        ManualSaveSnapshot manualSave = manualSaveSnapshotMapper.findByUserIdAndSlot(vup.getUserId(), slotNumber);
        if (manualSave != null && !vup.getId().equals(manualSave.getVupId())) {
            manualSave = null;
        }
        String status = vup.getStatus() == null ? "" : vup.getStatus();
        boolean canContinue = canOpen(vup);
        int day = session == null ? vup.getDayCount() : session.getDay();
        String phase = session == null ? "UNKNOWN" : session.getPhase();
        String displayName = slot == null || slot.getDisplayName() == null || slot.getDisplayName().isBlank()
                ? vup.getName()
                : slot.getDisplayName();
        boolean endingReady = "ENDING_READY".equals(phase) && ending != null;
        String summary = endingReady
                ? "已结局：" + ending.getFinalTitle()
                : canContinue
                ? "继续第" + day + "天：" + phase
                : ending == null
                        ? "已归档：" + status
                        : "已结局：" + ending.getFinalTitle();
        return new SaveSlotDTO(
                slotNumber,
                true,
                vup.getId(),
                displayName,
                day,
                phase,
                status,
                vup.getCurrentRoute(),
                canContinue,
                summary,
                ending == null ? null : ending.getEndingType(),
                ending == null ? null : ending.getFinalTitle(),
                manualSaveTime(manualSave),
                manualSave == null ? null : manualSave.getSummary()
        );
    }

    private ManualSaveSnapshot toManualSaveSnapshot(Vup vup, int slotNumber, SaveSlotDTO slot) {
        ManualSaveSnapshot snapshot = new ManualSaveSnapshot();
        snapshot.setUserId(vup.getUserId());
        snapshot.setSlotNumber(slotNumber);
        snapshot.setVupId(vup.getId());
        snapshot.setDay(slot.day());
        snapshot.setPhase(slot.phase());
        snapshot.setCurrentRoute(slot.currentRoute());
        snapshot.setSummary(slot.summary());
        return snapshot;
    }

    private String manualSaveTime(ManualSaveSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        LocalDateTime savedAt = snapshot.getUpdateTime() == null ? snapshot.getCreateTime() : snapshot.getUpdateTime();
        return savedAt == null ? null : savedAt.toString();
    }
}
