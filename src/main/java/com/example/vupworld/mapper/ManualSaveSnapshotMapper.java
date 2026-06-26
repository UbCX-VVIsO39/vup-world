package com.example.vupworld.mapper;

import com.example.vupworld.model.ManualSaveSnapshot;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ManualSaveSnapshotMapper {
    @Select("""
            SELECT *
            FROM manual_save_snapshot
            WHERE user_id = #{userId} AND slot_number = #{slotNumber}
            LIMIT 1
            """)
    ManualSaveSnapshot findByUserIdAndSlot(@Param("userId") Long userId, @Param("slotNumber") int slotNumber);

    @Insert("""
            INSERT INTO manual_save_snapshot (
                user_id, slot_number, vup_id, day, phase, current_route, summary
            ) VALUES (
                #{userId}, #{slotNumber}, #{vupId}, #{day}, #{phase}, #{currentRoute}, #{summary}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ManualSaveSnapshot snapshot);

    @Update("""
            UPDATE manual_save_snapshot
            SET vup_id = #{snapshot.vupId},
                day = #{snapshot.day},
                phase = #{snapshot.phase},
                current_route = #{snapshot.currentRoute},
                summary = #{snapshot.summary},
                update_time = CURRENT_TIMESTAMP
            WHERE user_id = #{userId} AND slot_number = #{slotNumber}
            """)
    void update(
            @Param("userId") Long userId,
            @Param("slotNumber") int slotNumber,
            @Param("snapshot") ManualSaveSnapshot snapshot
    );
}
