package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.entities.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

@Mapper
public interface UserMapper {
    @Insert("INSERT INTO users (id, phone, password, join_time) VALUES (#{id}, #{phone}, #{password}, #{joinTime})")
    int insert(User user);

    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(Long id);

    @Select("SELECT * FROM users WHERE phone = #{phone}")
    Optional<User> findByPhone(String phone);

    @Select("SELECT * FROM users WHERE email = #{email}")
    Optional<User> findByEmail(String email);

    @Update("UPDATE users SET password = #{password} WHERE id = #{id}")
    int updatePassword(User user);

    @Update("UPDATE users SET avatar = #{avatar} WHERE id = #{id}")
    int updateAvatar(User user);

    @Update("UPDATE users SET nickname = #{nickname} WHERE id = #{id}")
    int updateNickname(User user);
}
