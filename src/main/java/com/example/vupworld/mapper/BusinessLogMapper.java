package com.example.vupworld.mapper;

import com.example.vupworld.model.BusinessLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BusinessLogMapper {
    @Insert("""
            INSERT INTO business_log (
                vup_id, day_session_id, day, phase, idempotency_key, action, meme_subtype, plan_id, title_template_id, interaction_event_id, event_id, risk_tool_id, fan_group_topic_id, result,
                raw_fan_gain, final_fan_gain, multiplier_detail, cap_detail, clamp_detail,
                weight_detail, rng_detail, fan_change, true_fan_change, fun_fan_change,
                unicorn_fan_change, dd_fan_change, popularity_change, watch_heat_change,
                reputation_change, meme_change, commercial_change, coin_change, inspiration_change,
                expectation_change, route_score_change, debt_ids, accident_material_ids,
                report_id, ending_ref_flag
            ) VALUES (
                #{vupId}, #{daySessionId}, #{day}, #{phase}, #{idempotencyKey}, #{action}, #{memeSubtype}, #{planId}, #{titleTemplateId}, #{interactionEventId}, #{eventId}, #{riskToolId}, #{fanGroupTopicId}, #{result},
                #{rawFanGain}, #{finalFanGain}, #{multiplierDetail}, #{capDetail}, #{clampDetail},
                #{weightDetail}, #{rngDetail}, #{fanChange}, #{trueFanChange}, #{funFanChange},
                #{unicornFanChange}, #{ddFanChange}, #{popularityChange}, #{watchHeatChange},
                #{reputationChange}, #{memeChange}, #{commercialChange}, #{coinChange}, #{inspirationChange},
                #{expectationChange}, #{routeScoreChange}, #{debtIds}, #{accidentMaterialIds},
                #{reportId}, #{endingRefFlag}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(BusinessLog businessLog);

    @Update("""
            UPDATE business_log
            SET report_id = #{reportId}
            WHERE id = #{id}
            """)
    void attachReport(BusinessLog businessLog);

    @Update("""
            UPDATE business_log
            SET debt_ids = #{debtIds}
            WHERE id = #{id}
            """)
    void updateDebtIds(BusinessLog businessLog);

    @Select("""
            SELECT *
            FROM business_log
            WHERE vup_id = #{vupId}
              AND ending_ref_flag = TRUE
            ORDER BY day ASC, id ASC
            """)
    List<BusinessLog> findEndingReferenceLogs(Long vupId);

    @Select("""
            SELECT COUNT(*)
            FROM business_log
            WHERE vup_id = #{vupId}
            """)
    int countByVupId(Long vupId);

    @Select("""
            SELECT *
            FROM business_log
            WHERE vup_id = #{vupId}
              AND day = #{day}
            ORDER BY id DESC
            LIMIT 1
            """)
    BusinessLog findLatestByVupIdAndDay(Long vupId, int day);

    @Select("""
            SELECT *
            FROM business_log
            WHERE vup_id = #{vupId}
              AND day = #{day}
              AND action = 'FAN_TOPIC'
            ORDER BY id DESC
            LIMIT 1
            """)
    BusinessLog findLatestFanTopicByVupIdAndDay(@Param("vupId") Long vupId, @Param("day") int day);

    @Select("""
            SELECT *
            FROM business_log
            WHERE vup_id = #{vupId}
            ORDER BY day DESC, id DESC
            LIMIT #{limit}
            """)
    List<BusinessLog> findRecentByVupId(@Param("vupId") Long vupId, @Param("limit") int limit);

    @Select("""
            SELECT bl.*
            FROM business_log bl
            JOIN vup v ON v.id = bl.vup_id
            WHERE v.user_id = #{userId}
            ORDER BY bl.id DESC
            LIMIT #{limit}
            """)
    List<BusinessLog> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM business_log
            WHERE vup_id = #{vupId}
              AND day BETWEEN #{startDay} AND #{endDay}
            ORDER BY day ASC, id ASC
            """)
    List<BusinessLog> findByVupIdBetweenDays(
            @Param("vupId") Long vupId,
            @Param("startDay") int startDay,
            @Param("endDay") int endDay
    );

    @Select("""
            SELECT *
            FROM business_log
            WHERE vup_id = #{vupId}
              AND day = #{day}
            ORDER BY id ASC
            """)
    List<BusinessLog> findByVupIdAndDay(@Param("vupId") Long vupId, @Param("day") int day);

    @Select("""
            SELECT GREATEST(0,
                COALESCE(SUM(CASE
                    WHEN action = 'PUBLISH_VIDEO' THEN 2
                    WHEN action = 'FAN_GROUP_MAINTAIN' THEN 1
                    WHEN action = 'FAN_TOPIC' AND meme_subtype = 'CLIP_SUBMISSION_STOCK' THEN 1
                    WHEN action = 'PUBLISH_CLIP' THEN -1
                    ELSE 0
                END), 0)
            )
            FROM business_log
            WHERE vup_id = #{vupId}
            """)
    int countMaterialStockByVupId(Long vupId);

    @Select("""
            SELECT * FROM business_log
            WHERE vup_id = #{vupId} AND action = 'PUBLISH_CLIP'
            ORDER BY day DESC
            LIMIT 10
            """)
    List<BusinessLog> findRecentClipLogs(Long vupId);

    @Select("""
            SELECT * FROM business_log
            WHERE vup_id = #{vupId} AND action = 'PUBLISH_CLIP' AND meme_subtype = #{memeSubtype}
            ORDER BY day DESC
            LIMIT 10
            """)
    List<BusinessLog> findRecentClipLogsBySubtype(@Param("vupId") Long vupId, @Param("memeSubtype") String memeSubtype);

    @Select("""
            SELECT COUNT(DISTINCT day) FROM business_log
            WHERE vup_id = #{vupId}
              AND ending_ref_flag = TRUE
              AND action = #{actionType}
              AND day >= (
                  SELECT COALESCE(MAX(day), 0) FROM business_log
                  WHERE vup_id = #{vupId} AND ending_ref_flag = TRUE AND action != #{actionType}
              )
            """)
    int countConsecutiveSameAction(@Param("vupId") Long vupId, @Param("actionType") String actionType);

    @Select("""
            SELECT day, action, fan_change, route_score_change, result
            FROM business_log
            WHERE vup_id = #{vupId}
            ORDER BY day ASC, id ASC
            """)
    List<BusinessLog> findTimelineByVupId(@Param("vupId") Long vupId);
}
