package com.example.vupworld.mapper;

import com.example.vupworld.model.GameStatsHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GameStatsHistoryMapper {

    @Insert("""
            INSERT INTO game_stats_history (user_id, vup_id, ending_type, final_score, final_grade,
                final_fans, final_reputation, run_days, route_key, ng_plus_level)
            VALUES (#{userId}, #{vupId}, #{endingType}, #{finalScore}, #{finalGrade},
                #{finalFans}, #{finalReputation}, #{runDays}, #{routeKey}, #{ngPlusLevel})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(GameStatsHistory history);

    @Select("SELECT * FROM game_stats_history WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<GameStatsHistory> findByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM game_stats_history WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    @Select("SELECT MAX(final_score) FROM game_stats_history WHERE user_id = #{userId}")
    Integer findBestScoreByUserId(@Param("userId") Long userId);
}
