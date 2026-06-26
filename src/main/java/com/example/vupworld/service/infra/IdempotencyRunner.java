package com.example.vupworld.service.infra;


import com.example.vupworld.service.infra.RequestHashService;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.common.GameException;
import com.example.vupworld.mapper.ApiIdempotencyMapper;
import com.example.vupworld.model.ApiIdempotencyRecord;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 统一幂等执行器，从各 Service 中抽取重复的幂等逻辑。
 * <p>
 * 所有写接口遵循相同流程：
 * <ol>
 *   <li>校验幂等键非空</li>
 *   <li>计算请求哈希</li>
 *   <li>查找已有记录</li>
 *   <li>若存在：比对哈希 → 冲突或回放</li>
 *   <li>若不存在：执行业务逻辑，记录结果</li>
 * </ol>
 */
@Component
public class IdempotencyRunner {
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_PROCESSING = "PROCESSING";

    private final ApiIdempotencyMapper apiIdempotencyMapper;
    private final JsonService jsonService;

    public IdempotencyRunner(
            ApiIdempotencyMapper apiIdempotencyMapper,
            JsonService jsonService
    ) {
        this.apiIdempotencyMapper = apiIdempotencyMapper;
        this.jsonService = jsonService;
    }

    /**
     * 执行带幂等保护的操作。
     *
     * @param userId         当前用户 ID
     * @param apiPath        规范 API 路径（如 "/api/day/action"）
     * @param idempotencyKey 客户端提供的幂等键（必须非空）
     * @param requestHash    请求哈希（由调用方通过 RequestHashService 计算）
     * @param action         无幂等记录时执行的业务逻辑
     * @param resultType     返回值的 Class（用于回放反序列化）
     * @param recordBuilder  根据结果构建 ApiIdempotencyRecord 的回调
     * @param <T>            返回类型
     * @return 业务结果或回放的历史响应
     */
    public <T> T execute(
            Long userId,
            String apiPath,
            String idempotencyKey,
            String requestHash,
            Supplier<T> action,
            Class<T> resultType,
            BiFunction<String, T, ApiIdempotencyRecord> recordBuilder
    ) {
        ApiIdempotencyRecord existing = apiIdempotencyMapper.find(userId, apiPath, idempotencyKey);
        if (existing != null) {
            return replay(existing, requestHash, resultType);
        }

        try {
            apiIdempotencyMapper.insertProcessing(processingRecord(userId, apiPath, idempotencyKey, requestHash));
        } catch (DuplicateKeyException exception) {
            return replayAfterInsertConflict(userId, apiPath, idempotencyKey, requestHash, resultType);
        }

        T result = action.get();
        ApiIdempotencyRecord record = recordBuilder.apply(requestHash, result);
        int updated = apiIdempotencyMapper.complete(record);
        if (updated != 1) {
            throw new GameException("IDEMPOTENCY_RECORD_MISSING", "幂等记录写入异常，请重试。");
        }
        return result;
    }

    private ApiIdempotencyRecord processingRecord(
            Long userId,
            String apiPath,
            String idempotencyKey,
            String requestHash
    ) {
        ApiIdempotencyRecord record = new ApiIdempotencyRecord();
        record.setUserId(userId);
        record.setApiPath(apiPath);
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setStatus(STATUS_PROCESSING);
        return record;
    }

    private <T> T replayAfterInsertConflict(
            Long userId,
            String apiPath,
            String idempotencyKey,
            String requestHash,
            Class<T> resultType
    ) {
        ApiIdempotencyRecord existing = apiIdempotencyMapper.find(userId, apiPath, idempotencyKey);
        if (existing == null) {
            throw new GameException("IDEMPOTENCY_IN_PROGRESS", "相同幂等键的请求正在处理，请稍后重试。");
        }
        return replay(existing, requestHash, resultType);
    }

    private <T> T replay(ApiIdempotencyRecord existing, String requestHash, Class<T> resultType) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new GameException("IDEMPOTENCY_CONFLICT", "同一个幂等键对应了不同请求。");
        }
        if (!STATUS_SUCCESS.equals(existing.getStatus())) {
            throw new GameException("IDEMPOTENCY_IN_PROGRESS", "相同幂等键的请求正在处理，请稍后重试。");
        }
        return jsonService.read(existing.getResponseJson(), resultType);
    }

    /**
     * 校验幂等键非空并 trim。
     */
    public String requireKey(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new GameException("CONFIG_FIELD_INVALID", "写接口必须提供 idempotencyKey。");
        }
        return raw.trim();
    }

}
