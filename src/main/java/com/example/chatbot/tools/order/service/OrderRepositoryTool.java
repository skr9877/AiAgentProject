package com.example.chatbot.tools.order.service;

import com.example.chatbot.tools.order.entity.Order;
import com.example.chatbot.tools.order.mapper.OrderMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderRepositoryTool {

    private final OrderMapper orderMapper;

    public OrderRepositoryTool(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Tool(name = "getOrdersByCustomerFromDb", description = "DB에서 고객 ID로 주문 목록을 조회합니다.")
    public List<Order> getOrdersByCustomer(@ToolParam(description = "고객 ID") int customerId) {
        return orderMapper.findByCustomer(customerId);
    }

    @Tool(name = "getOrderDetailFromDb", description = "DB에서 주문 ID로 주문 상세 정보를 조회합니다.")
    public Order getOrderDetail(@ToolParam(description = "주문 ID") int orderId) {
        return orderMapper.findById(orderId);
    }
}
