package com.example.chatbot.tools.order.db;

import com.example.chatbot.tools.order.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class OrderRepository {

    @Autowired(required = false)
    private NamedParameterJdbcTemplate jdbc;

    public List<Order> findByCustomer(int customerId) {
        if (jdbc == null) return Collections.emptyList();
        String sql = """
                SELECT order_id, product_id, product_name, quantity, total_price, status,
                       TO_CHAR(order_date) AS order_date
                FROM orders
                WHERE customer_id = :customerId
                ORDER BY order_date DESC
                FETCH FIRST 20 ROWS ONLY
                """;
        return jdbc.query(sql, new MapSqlParameterSource("customerId", customerId),
                (rs, i) -> {
                    Order o = new Order();
                    o.setOrderId(rs.getInt("order_id"));
                    o.setProductId(rs.getInt("product_id"));
                    o.setProductName(rs.getString("product_name"));
                    o.setQuantity(rs.getInt("quantity"));
                    o.setTotalPrice(rs.getDouble("total_price"));
                    o.setStatus(rs.getString("status"));
                    o.setOrderDate(rs.getString("order_date"));
                    return o;
                });
    }

    public Order findById(int orderId) {
        if (jdbc == null) return null;
        String sql = """
                SELECT o.order_id, o.customer_id, o.product_id, p.product_name,
                       o.quantity, o.total_price, o.status,
                       TO_CHAR(o.order_date) AS order_date, o.delivery_address
                FROM orders o
                JOIN products p ON o.product_id = p.product_id
                WHERE o.order_id = :orderId
                """;
        List<Order> result = jdbc.query(sql, new MapSqlParameterSource("orderId", orderId),
                (rs, i) -> {
                    Order o = new Order();
                    o.setOrderId(rs.getInt("order_id"));
                    o.setCustomerId(rs.getInt("customer_id"));
                    o.setProductId(rs.getInt("product_id"));
                    o.setProductName(rs.getString("product_name"));
                    o.setQuantity(rs.getInt("quantity"));
                    o.setTotalPrice(rs.getDouble("total_price"));
                    o.setStatus(rs.getString("status"));
                    o.setOrderDate(rs.getString("order_date"));
                    o.setDeliveryAddress(rs.getString("delivery_address"));
                    return o;
                });
        return result.isEmpty() ? null : result.get(0);
    }
}
