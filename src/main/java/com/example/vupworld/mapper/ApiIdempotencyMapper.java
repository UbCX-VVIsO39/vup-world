package com.example.vupworld.mapper;

import com.example.vupworld.model.ApiIdempotencyRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ApiIdempotencyMapper {
    @Select("""
            SELECT *
            FROM api_idempotency_record
            WHERE user_id = #{userId}
              AND api_path = #{apiPath}
              AND idempotency_key = #{idempotencyKey}
            """)
    ApiIdempotencyRecord find(Long userId, String apiPath, String idempotencyKey);

    @Insert("""
            INSERT INTO api_idempotency_record (
                user_id, api_path, idempotency_key, request_hash, response_json, status
            ) VALUES (
                #{userId}, #{apiPath}, #{idempotencyKey}, #{requestHash}, '{}', 'PROCESSING'
            )
            """)
    void insertProcessing(ApiIdempotencyRecord record);

    @Update("""
            UPDATE api_idempotency_record
            SET vup_id = #{vupId},
                day = #{day},
                phase = #{phase},
                request_hash = #{requestHash},
                response_json = #{responseJson},
                status = #{status},
                error_code = #{errorCode}
            WHERE user_id = #{userId}
              AND api_path = #{apiPath}
              AND idempotency_key = #{idempotencyKey}
            """)
    int complete(ApiIdempotencyRecord record);
}
