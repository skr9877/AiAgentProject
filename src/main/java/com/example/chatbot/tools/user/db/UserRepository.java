package com.example.chatbot.tools.user.db;

import com.example.chatbot.tools.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class UserRepository {

    @Autowired(required = false)
    private NamedParameterJdbcTemplate jdbc;

    public User findById(int userId) {
        if (jdbc == null) return null;
        String sql = """
                SELECT user_id, name, email, phone, address
                FROM users
                WHERE user_id = :userId
                """;
        List<User> result = jdbc.query(sql, new MapSqlParameterSource("userId", userId),
                (rs, i) -> {
                    User u = new User();
                    u.setUserId(rs.getInt("user_id"));
                    u.setName(rs.getString("name"));
                    u.setEmail(rs.getString("email"));
                    u.setPhone(rs.getString("phone"));
                    u.setAddress(rs.getString("address"));
                    return u;
                });
        return result.isEmpty() ? null : result.get(0);
    }

    public List<User> findByName(String name) {
        if (jdbc == null) return Collections.emptyList();
        String sql = """
                SELECT user_id, name, email, phone, address
                FROM users
                WHERE name LIKE :name
                ORDER BY name
                FETCH FIRST 20 ROWS ONLY
                """;
        return jdbc.query(sql, new MapSqlParameterSource("name", "%" + name + "%"),
                (rs, i) -> {
                    User u = new User();
                    u.setUserId(rs.getInt("user_id"));
                    u.setName(rs.getString("name"));
                    u.setEmail(rs.getString("email"));
                    u.setPhone(rs.getString("phone"));
                    u.setAddress(rs.getString("address"));
                    return u;
                });
    }
}
