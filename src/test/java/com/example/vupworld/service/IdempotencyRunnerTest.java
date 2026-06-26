package com.example.vupworld.service;

import com.example.vupworld.common.GameException;
import com.example.vupworld.mapper.ApiIdempotencyMapper;
import com.example.vupworld.service.infra.IdempotencyRunner;
import com.example.vupworld.service.infra.JsonService;
import com.example.vupworld.model.ApiIdempotencyRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotencyRunnerTest {

    private static final Long USER_ID = 1L;
    private static final String API_PATH = "/api/test/write";
    private static final String KEY = "same-click-key";
    private static final String HASH = "request-hash";

    private final ApiIdempotencyMapper mapper = mock(ApiIdempotencyMapper.class);
    private final JsonService jsonService = new JsonService(new ObjectMapper());
    private final IdempotencyRunner runner = new IdempotencyRunner(mapper, jsonService);

    @Test
    void claimsIdempotencyKeyBeforeRunningActionAndCompletesRecord() {
        AtomicBoolean claimed = new AtomicBoolean(false);
        doAnswer(invocation -> {
            ApiIdempotencyRecord record = invocation.getArgument(0);
            assertEquals(USER_ID, record.getUserId());
            assertEquals(API_PATH, record.getApiPath());
            assertEquals(KEY, record.getIdempotencyKey());
            assertEquals(HASH, record.getRequestHash());
            assertEquals("PROCESSING", record.getStatus());
            claimed.set(true);
            return null;
        }).when(mapper).insertProcessing(any(ApiIdempotencyRecord.class));
        when(mapper.complete(any(ApiIdempotencyRecord.class))).thenReturn(1);

        SampleResponse result = runner.execute(USER_ID, API_PATH, KEY, HASH,
                () -> {
                    assertTrue(claimed.get());
                    return new SampleResponse("fresh");
                },
                SampleResponse.class,
                this::record);

        assertEquals("fresh", result.value());
        verify(mapper).complete(any(ApiIdempotencyRecord.class));
    }

    @Test
    void duplicateClaimReplaysCommittedResponseWithoutRunningAction() {
        ApiIdempotencyRecord existing = record(HASH, new SampleResponse("cached"));
        when(mapper.find(USER_ID, API_PATH, KEY)).thenReturn(null, existing);
        doThrow(new DuplicateKeyException("duplicate")).when(mapper)
                .insertProcessing(any(ApiIdempotencyRecord.class));
        AtomicBoolean actionRan = new AtomicBoolean(false);

        SampleResponse result = runner.execute(USER_ID, API_PATH, KEY, HASH,
                () -> {
                    actionRan.set(true);
                    return new SampleResponse("fresh");
                },
                SampleResponse.class,
                this::record);

        assertEquals("cached", result.value());
        assertFalse(actionRan.get());
        verify(mapper, never()).complete(any(ApiIdempotencyRecord.class));
    }

    @Test
    void duplicateClaimWithDifferentRequestHashKeepsConflictSemantics() {
        ApiIdempotencyRecord existing = record("other-hash", new SampleResponse("cached"));
        when(mapper.find(USER_ID, API_PATH, KEY)).thenReturn(null, existing);
        doThrow(new DuplicateKeyException("duplicate")).when(mapper)
                .insertProcessing(any(ApiIdempotencyRecord.class));

        GameException exception = assertThrows(GameException.class, () ->
                runner.execute(USER_ID, API_PATH, KEY, HASH,
                        () -> new SampleResponse("fresh"),
                        SampleResponse.class,
                        this::record));

        assertEquals("IDEMPOTENCY_CONFLICT", exception.code());
        verify(mapper, never()).complete(any(ApiIdempotencyRecord.class));
    }

    @Test
    void existingProcessingRecordReturnsExplicitRetryableError() {
        ApiIdempotencyRecord existing = processingRecord();
        when(mapper.find(USER_ID, API_PATH, KEY)).thenReturn(existing);

        GameException exception = assertThrows(GameException.class, () ->
                runner.execute(USER_ID, API_PATH, KEY, HASH,
                        () -> new SampleResponse("fresh"),
                        SampleResponse.class,
                        this::record));

        assertEquals("IDEMPOTENCY_IN_PROGRESS", exception.code());
        verify(mapper, never()).insertProcessing(any(ApiIdempotencyRecord.class));
        verify(mapper, never()).complete(any(ApiIdempotencyRecord.class));
    }

    @Test
    void missingClaimAfterDuplicateKeyReturnsExplicitRetryableError() {
        when(mapper.find(USER_ID, API_PATH, KEY)).thenReturn(null);
        doThrow(new DuplicateKeyException("duplicate")).when(mapper)
                .insertProcessing(any(ApiIdempotencyRecord.class));

        GameException exception = assertThrows(GameException.class, () ->
                runner.execute(USER_ID, API_PATH, KEY, HASH,
                        () -> new SampleResponse("fresh"),
                        SampleResponse.class,
                        this::record));

        assertEquals("IDEMPOTENCY_IN_PROGRESS", exception.code());
        verify(mapper, never()).complete(any(ApiIdempotencyRecord.class));
    }

    private ApiIdempotencyRecord record(String hash, SampleResponse response) {
        ApiIdempotencyRecord record = new ApiIdempotencyRecord();
        record.setUserId(USER_ID);
        record.setApiPath(API_PATH);
        record.setIdempotencyKey(KEY);
        record.setRequestHash(hash);
        record.setResponseJson(jsonService.write(response));
        record.setStatus("SUCCESS");
        return record;
    }

    private ApiIdempotencyRecord processingRecord() {
        ApiIdempotencyRecord record = new ApiIdempotencyRecord();
        record.setUserId(USER_ID);
        record.setApiPath(API_PATH);
        record.setIdempotencyKey(KEY);
        record.setRequestHash(HASH);
        record.setResponseJson("{}");
        record.setStatus("PROCESSING");
        return record;
    }

    private record SampleResponse(String value) {
    }
}
