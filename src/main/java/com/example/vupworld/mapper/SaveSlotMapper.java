package com.example.vupworld.mapper;

import com.example.vupworld.model.SaveSlot;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SaveSlotMapper {
    @Select("""
            SELECT *
            FROM save_slot
            WHERE user_id = #{userId} AND slot_number = #{slotNumber}
            LIMIT 1
            """)
    SaveSlot findByUserIdAndSlot(@Param("userId") Long userId, @Param("slotNumber") int slotNumber);

    @Insert("""
            INSERT INTO save_slot (user_id, slot_number, display_name)
            VALUES (#{userId}, #{slotNumber}, #{displayName})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SaveSlot saveSlot);

    @Update("""
            UPDATE save_slot
            SET display_name = #{displayName},
                update_time = CURRENT_TIMESTAMP
            WHERE user_id = #{userId} AND slot_number = #{slotNumber}
            """)
    void updateDisplayName(
            @Param("userId") Long userId,
            @Param("slotNumber") int slotNumber,
            @Param("displayName") String displayName
    );
}
