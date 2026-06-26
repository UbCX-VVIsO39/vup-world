package com.example.vupworld.mapper;

import com.example.vupworld.model.GameContent;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Options;

import java.util.List;

@Mapper
public interface ContentCatalogMapper {
    @Select("""
            SELECT * FROM game_content
            WHERE enabled = TRUE
            ORDER BY category, content_key, sub_key, sort_order
            """)
    List<GameContent> selectAll();

    @Insert("""
            <script>
            INSERT INTO game_content (category, content_key, sub_key, content_text, content_json, sort_order, enabled)
            VALUES
            <foreach collection="list" item="item" separator=",">
                (#{item.category}, #{item.contentKey}, #{item.subKey}, #{item.contentText}, #{item.contentJson}, #{item.sortOrder}, #{item.enabled})
            </foreach>
            </script>
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertBatch(@Param("list") List<GameContent> list);
}
