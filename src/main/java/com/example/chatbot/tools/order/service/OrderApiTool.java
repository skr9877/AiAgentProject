package com.example.chatbot.tools.order.service;

import com.example.chatbot.tools.order.client.OrderApiClient;
import com.example.chatbot.tools.order.model.Order;
import com.example.chatbot.tools.registry.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OrderApiTool {

    private final OrderApiClient orderApiClient;

    public OrderApiTool(OrderApiClient orderApiClient) {
        this.orderApiClient = orderApiClient;
    }

    public List<Order> getOrdersByCustomer(int customerId) {
        return orderApiClient.findByCustomer(customerId);
    }

    public Order getOrderDetail(int orderId) {
        return orderApiClient.findById(orderId);
    }

    public static final List<ToolDefinition> TOOL_SPECS = List.of(
            new ToolDefinition(
                    "get_orders_by_customer_api",
                    "외부 API에서 고객 ID로 주문 목록을 조회합니다.",
                    Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "customer_id", Map.of("type", "integer", "description", "고객 ID")
                            ),
                            "required", List.of("customer_id")
                    )
            ),
            new ToolDefinition(
                    "get_order_detail_api",
                    "외부 API에서 주문 ID로 주문 상세 정보를 조회합니다.",
                    Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "order_id", Map.of("type", "integer", "description", "주문 ID")
                            ),
                            "required", List.of("order_id")
                    )
            )
    );
}
