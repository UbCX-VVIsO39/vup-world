package com.example.vupworld.mapper;

import com.example.vupworld.model.RiskDebt;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RiskDebtMapper {
    @Insert("""
            INSERT INTO risk_debt (
                vup_id, source_log_id, debt_type, status, severity, create_day,
                due_day, source_action, source_title, summary
            ) VALUES (
                #{vupId}, #{sourceLogId}, #{debtType}, #{status}, #{severity}, #{createDay},
                #{dueDay}, #{sourceAction}, #{sourceTitle}, #{summary}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RiskDebt riskDebt);

    @Select("""
            SELECT *
            FROM risk_debt
            WHERE vup_id = #{vupId} AND status = 'OPEN'
            ORDER BY severity DESC, due_day ASC, id ASC
            """)
    List<RiskDebt> findOpenByVupId(Long vupId);

    @Select("""
            SELECT *
            FROM risk_debt
            WHERE vup_id = #{vupId}
              AND status IN ('OPEN', 'CLEARED')
            ORDER BY
              CASE WHEN status = 'OPEN' THEN 0 ELSE 1 END,
              severity DESC, due_day ASC, id ASC
            """)
    List<RiskDebt> findEndingRefsByVupId(Long vupId);

    @Select("""
            SELECT *
            FROM risk_debt
            WHERE id = #{id}
            """)
    RiskDebt findById(Long id);

    @Select("""
            SELECT *
            FROM risk_debt
            WHERE vup_id = #{vupId}
              AND status = 'OPEN'
              AND due_day <= #{day}
              AND create_day < #{day}
            ORDER BY severity DESC, due_day ASC, id ASC
            LIMIT 1
            """)
    RiskDebt findDueOpenByVupIdAndDay(Long vupId, int day);

    @Select("""
            SELECT *
            FROM risk_debt
            WHERE vup_id = #{vupId}
              AND status = 'OPEN'
              AND create_day < #{day}
            ORDER BY severity DESC, due_day ASC, id ASC
            LIMIT 1
            """)
    RiskDebt findEscalatableOpenByVupIdAndDay(Long vupId, int day);

    @Update("""
            UPDATE risk_debt
            SET status = #{status},
                severity = #{severity},
                due_day = #{dueDay},
                summary = #{summary}
            WHERE id = #{id}
            """)
    void updateMitigation(RiskDebt riskDebt);

    @Select("""
            SELECT *
            FROM risk_debt
            WHERE vup_id = #{vupId}
            ORDER BY create_time DESC
            """)
    List<RiskDebt> findByVupId(Long vupId);

    @Select("""
            SELECT COUNT(*)
            FROM risk_debt
            WHERE vup_id = #{vupId}
              AND debt_type = #{debtType}
            """)
    int countByVupIdAndDebtType(@Param("vupId") Long vupId, @Param("debtType") String debtType);
}
