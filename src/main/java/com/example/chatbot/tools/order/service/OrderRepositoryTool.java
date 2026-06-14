package com.example.chatbot.tools.order.service;

import com.example.chatbot.tools.order.entity.Order;
import com.example.chatbot.tools.order.db.OrderRepository;
import com.example.chatbot.tools.registry.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OrderRepositoryTool {

    private final OrderRepository orderRepository;

    public OrderRepositoryTool(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> getOrdersByCustomer(int customerId) {
        return orderRepository.findByCustomer(customerId);
    }

    public Order getOrderDetail(int orderId) {
        return orderRepository.findById(orderId);
    }

    public static final List<ToolDefinition> TOOL_SPECS = List.of(
            new ToolDefinition(
                    "get_orders_by_customer_db",
                    "DB에서 고객 ID로 주문 목록을 조회합니다.",
                    Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "customer_id", Map.of("type", "integer", "description", "고객 ID")
                            ),
                            "required", List.of("customer_id")
                    )
            ),
            new ToolDefinition(
                    "get_order_detail_db",
                    "DB에서 주문 ID로 주문 상세 정보를 조회합니다.",
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
