package com.example.vupworld.mapper;

import com.example.vupworld.model.UserAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserAccountMapper {
    @Insert("""
            INSERT INTO user_account (username, password_hash, nickname, coin, restart_count)
            VALUES (#{username}, #{passwordHash}, #{nickname}, #{coin}, #{restartCount})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(UserAccount userAccount);

    @Select("""
            SELECT id, username, password_hash, nickname, coin, restart_count
            FROM user_account
            WHERE username = #{username}
            """)
    UserAccount findByUsername(String username);

    @Select("""
            SELECT id, username, password_hash, nickname, coin, restart_count
            FROM user_account
            WHERE id = #{id}
            """)
    UserAccount findById(Long id);

    @Update("""
            UPDATE user_account
            SET restart_count = restart_count + 1,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    void incrementRestartCount(Long id);

    @Update("""
            UPDATE user_account
            SET nickname = #{nickname},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    void updateNickname(@Param("id") Long id, @Param("nickname") String nickname);
}
