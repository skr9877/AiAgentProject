package com.example.chatbot.tools.user.mapper;

import com.example.chatbot.tools.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User findById(@Param("userId") int userId);

    List<User> findByName(@Param("name") String name);
}
