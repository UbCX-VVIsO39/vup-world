package com.example.vupworld.mapper;

import com.example.vupworld.model.Vup;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VupMapper {
    @Insert("""
            INSERT INTO vup (
                user_id, slot_number, name, persona, status, day_count, run_seed,
                song_power, dance_power, talk_power, meme_power, plan_power, stress_power,
                stamina, max_stamina, coin, inspiration,
                fans, true_fans, fun_fans, unicorn_fans, dd_fans,
                popularity, watch_heat, reputation, meme_level, commercial_level,
                current_route, expectation_json, route_score_json, tutorial_flags_json, previous_ending_id
            ) VALUES (
                #{userId}, #{slotNumber}, #{name}, #{persona}, #{status}, #{dayCount}, #{runSeed},
                #{songPower}, #{dancePower}, #{talkPower}, #{memePower}, #{planPower}, #{stressPower},
                #{stamina}, #{maxStamina}, #{coin}, #{inspiration},
                #{fans}, #{trueFans}, #{funFans}, #{unicornFans}, #{ddFans},
                #{popularity}, #{watchHeat}, #{reputation}, #{memeLevel}, #{commercialLevel},
                #{currentRoute}, #{expectationJson}, #{routeScoreJson}, #{tutorialFlagsJson}, #{previousEndingId}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Vup vup);

    @Select("""
            SELECT *
            FROM vup
            WHERE user_id = #{userId} AND status = 'ACTIVE'
            ORDER BY id DESC
            LIMIT 1
            """)
    Vup findActiveByUserId(Long userId);

    @Select("""
            SELECT *
            FROM vup
            WHERE user_id = #{userId} AND slot_number = #{slotNumber} AND status = 'ACTIVE'
            ORDER BY id DESC
            LIMIT 1
            """)
    Vup findActiveByUserIdAndSlot(@Param("userId") Long userId, @Param("slotNumber") int slotNumber);

    @Select("""
            SELECT *
            FROM vup
            WHERE user_id = #{userId}
            ORDER BY id DESC
            LIMIT 1
            """)
    Vup findLatestByUserId(Long userId);

    @Select("""
            SELECT *
            FROM vup
            WHERE user_id = #{userId} AND slot_number = #{slotNumber}
            ORDER BY id DESC
            LIMIT 1
            """)
    Vup findLatestByUserIdAndSlot(@Param("userId") Long userId, @Param("slotNumber") int slotNumber);

    @Select("""
            SELECT *
            FROM vup
            WHERE id = #{id}
            """)
    Vup findById(Long id);

    @Update("""
            UPDATE vup
            SET day_count = #{dayCount},
                song_power = #{songPower},
                dance_power = #{dancePower},
                talk_power = #{talkPower},
                meme_power = #{memePower},
                plan_power = #{planPower},
                stress_power = #{stressPower},
                stamina = #{stamina},
                coin = #{coin},
                inspiration = #{inspiration},
                fans = #{fans},
                true_fans = #{trueFans},
                fun_fans = #{funFans},
                unicorn_fans = #{unicornFans},
                dd_fans = #{ddFans},
                popularity = #{popularity},
                watch_heat = #{watchHeat},
                reputation = #{reputation},
                meme_level = #{memeLevel},
                commercial_level = #{commercialLevel},
                current_route = #{currentRoute},
                expectation_json = #{expectationJson},
                route_score_json = #{routeScoreJson},
                tutorial_flags_json = #{tutorialFlagsJson},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    void updateState(Vup vup);

    @Update("""
            UPDATE vup
            SET status = #{status},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("""
            UPDATE vup
            SET status = 'SUSPENDED',
                update_time = CURRENT_TIMESTAMP
            WHERE user_id = #{userId} AND status = 'ACTIVE'
            """)
    void suspendActiveByUserId(Long userId);
}
