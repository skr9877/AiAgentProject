package com.example.chatbot.tools.order.service;

import com.example.chatbot.tools.order.api.OrderApiClient;
import com.example.chatbot.tools.order.entity.Order;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderApiTool {

    private final OrderApiClient orderApiClient;

    public OrderApiTool(OrderApiClient orderApiClient) {
        this.orderApiClient = orderApiClient;
    }

    @Tool(name = "getOrdersByCustomerFromApi", description = "외부 API에서 고객 ID로 주문 목록을 조회합니다.")
    public List<Order> getOrdersByCustomer(@ToolParam(description = "고객 ID") int customerId) {
        return orderApiClient.findByCustomer(customerId);
    }

    @Tool(name = "getOrderDetailFromApi", description = "외부 API에서 주문 ID로 주문 상세 정보를 조회합니다.")
    public Order getOrderDetail(@ToolParam(description = "주문 ID") int orderId) {
        return orderApiClient.findById(orderId);
    }
}
