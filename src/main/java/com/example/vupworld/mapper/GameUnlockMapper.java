package com.example.vupworld.mapper;

import com.example.vupworld.model.GameUnlock;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GameUnlockMapper {

    @Insert("""
            INSERT INTO game_unlock (user_id, unlock_type, unlock_key, source_ending_type, source_run_score)
            VALUES (#{userId}, #{unlockType}, #{unlockKey}, #{sourceEndingType}, #{sourceRunScore})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(GameUnlock unlock);

    @Select("SELECT * FROM game_unlock WHERE user_id = #{userId}")
    List<GameUnlock> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM game_unlock WHERE user_id = #{userId} AND unlock_type = #{unlockType}")
    List<GameUnlock> findByUserIdAndType(@Param("userId") Long userId, @Param("unlockType") String unlockType);

    @Select("SELECT COUNT(*) > 0 FROM game_unlock WHERE user_id = #{userId} AND unlock_type = #{unlockType} AND unlock_key = #{unlockKey}")
    boolean existsByUserAndKey(@Param("userId") Long userId, @Param("unlockType") String unlockType, @Param("unlockKey") String unlockKey);

    @Select("SELECT unlock_key FROM game_unlock WHERE user_id = #{userId} AND unlock_type = #{unlockType}")
    List<String> findKeysByUserAndType(@Param("userId") Long userId, @Param("unlockType") String unlockType);
}
