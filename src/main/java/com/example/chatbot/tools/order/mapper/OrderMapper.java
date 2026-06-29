package com.example.chatbot.tools.order.mapper;

import com.example.chatbot.tools.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    List<Order> findByCustomer(@Param("customerId") int customerId);

    Order findById(@Param("orderId") int orderId);
}
