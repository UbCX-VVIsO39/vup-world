package com.example.vupworld.mapper;

import com.example.vupworld.model.EndingReview;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EndingReviewMapper {
    @Insert("""
            INSERT INTO ending_review (
                vup_id, final_report_id, ending_type, final_title, subtitle,
                ending_tags_json, ending_reason, ending_reason_json, summary, fan_profile_json,
                key_events_json, debt_refs_json, route_review_json, restart_hint
            ) VALUES (
                #{vupId}, #{finalReportId}, #{endingType}, #{finalTitle}, #{subtitle},
                #{endingTagsJson}, #{endingReason}, #{endingReasonJson}, #{summary}, #{fanProfileJson},
                #{keyEventsJson}, #{debtRefsJson}, #{routeReviewJson}, #{restartHint}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(EndingReview endingReview);

    @Select("""
            SELECT *
            FROM ending_review
            WHERE vup_id = #{vupId}
            ORDER BY id DESC
            LIMIT 1
            """)
    EndingReview findLatestByVupId(Long vupId);

    @Select("""
            SELECT *
            FROM ending_review
            WHERE id = #{id}
            """)
    EndingReview findById(Long id);

    @Select("""
            SELECT er.*
            FROM ending_review er
            JOIN vup v ON v.id = er.vup_id
            WHERE v.user_id = #{userId}
            ORDER BY er.id DESC
            """)
    List<EndingReview> findByUserId(Long userId);

    @Select("""
            SELECT DISTINCT er.ending_type
            FROM ending_review er
            JOIN vup v ON v.id = er.vup_id
            WHERE v.user_id = #{userId}
            ORDER BY er.ending_type
            """)
    List<String> findUnlockedEndingTypesByUserId(Long userId);
}
