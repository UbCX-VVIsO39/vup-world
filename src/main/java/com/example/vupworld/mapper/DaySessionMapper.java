package com.example.vupworld.mapper;

import com.example.vupworld.model.DaySession;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DaySessionMapper {
    @Insert("""
            INSERT INTO day_session (
                vup_id, day, phase, selected_action, selected_plan_id, selected_title_template_id,
                title_candidates_json, title_reroll_count, stream_plan_cancelled, risk_tool_used,
                cancelled_plan_snapshot_json, pending_interaction_event_id, pending_formal_event_id,
                formal_event_slot_status, formal_event_source, formal_event_priority,
                formal_event_roll_detail_json, pending_action_result_json, pending_event_result_json,
                report_id, ending_review_id, random_seed, rng_cursor, locked, off_stream_action
            ) VALUES (
                #{vupId}, #{day}, #{phase}, #{selectedAction}, #{selectedPlanId}, #{selectedTitleTemplateId},
                #{titleCandidatesJson}, #{titleRerollCount}, #{streamPlanCancelled}, #{riskToolUsed},
                #{cancelledPlanSnapshotJson}, #{pendingInteractionEventId}, #{pendingFormalEventId},
                #{formalEventSlotStatus}, #{formalEventSource}, #{formalEventPriority},
                #{formalEventRollDetailJson}, #{pendingActionResultJson}, #{pendingEventResultJson},
                #{reportId}, #{endingReviewId}, #{randomSeed}, #{rngCursor}, #{locked}, #{offStreamAction}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(DaySession daySession);

    @Select("""
            SELECT *
            FROM day_session
            WHERE vup_id = #{vupId} AND day = #{day}
            """)
    DaySession findByVupIdAndDay(Long vupId, int day);

    @Select("""
            SELECT *
            FROM day_session
            WHERE vup_id = #{vupId}
            ORDER BY day DESC
            LIMIT 1
            """)
    DaySession findLatestByVupId(Long vupId);

    @Update("""
            UPDATE day_session
            SET phase = #{phase},
                selected_action = #{selectedAction},
                selected_plan_id = #{selectedPlanId},
                selected_title_template_id = #{selectedTitleTemplateId},
                title_candidates_json = #{titleCandidatesJson},
                title_reroll_count = #{titleRerollCount},
                stream_plan_cancelled = #{streamPlanCancelled},
                risk_tool_used = #{riskToolUsed},
                cancelled_plan_snapshot_json = #{cancelledPlanSnapshotJson},
                pending_interaction_event_id = #{pendingInteractionEventId},
                pending_formal_event_id = #{pendingFormalEventId},
                formal_event_slot_status = #{formalEventSlotStatus},
                formal_event_source = #{formalEventSource},
                formal_event_priority = #{formalEventPriority},
                formal_event_roll_detail_json = #{formalEventRollDetailJson},
                pending_action_result_json = #{pendingActionResultJson},
                pending_event_result_json = #{pendingEventResultJson},
                report_id = #{reportId},
                ending_review_id = #{endingReviewId},
                off_stream_action = #{offStreamAction},
                gift_count = #{giftCount},
                gift_coin_value = #{giftCoinValue},
                danmaku_count = #{danmakuCount},
                danmaku_heat = #{danmakuHeat},
                action_points = #{actionPoints},
                max_action_points = #{maxActionPoints},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    void updateAfterAction(DaySession daySession);

    @Update("UPDATE day_session SET rng_cursor = #{cursor} WHERE id = #{id}")
    void updateRngCursor(@Param("id") Long id, @Param("cursor") int cursor);

    @Update("""
            UPDATE day_session
            SET gift_count = gift_count + #{count},
                gift_coin_value = gift_coin_value + #{coinValue},
                update_time = CURRENT_TIMESTAMP
            WHERE vup_id = #{vupId} AND day = #{day}
            """)
    void incrementGift(@Param("vupId") Long vupId,
                       @Param("day") int day,
                       @Param("count") int count,
                       @Param("coinValue") int coinValue);

    @Update("""
            UPDATE day_session
            SET danmaku_heat = danmaku_heat + #{heatDelta},
                update_time = CURRENT_TIMESTAMP
            WHERE vup_id = #{vupId} AND day = #{day}
            """)
    void incrementDanmaku(@Param("vupId") Long vupId,
                          @Param("day") int day,
                          @Param("heatDelta") int heatDelta);

    @Update("""
            UPDATE day_session
            SET phase = #{phase},
                update_time = CURRENT_TIMESTAMP
            WHERE vup_id = #{vupId} AND day = #{day}
            """)
    void updatePhase(@Param("vupId") Long vupId,
                     @Param("day") int day,
                     @Param("phase") String phase);
}
