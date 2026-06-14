package com.example.chatbot.tools.registry;

import com.example.chatbot.tools.order.service.OrderApiTool;
import com.example.chatbot.tools.order.service.OrderRepositoryTool;
import com.example.chatbot.tools.user.service.UserRepositoryTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

@Component
public class ToolRegistry {

    private final OrderRepositoryTool orderRepositoryTool;
    private final OrderApiTool orderApiTool;
    private final UserRepositoryTool userRepositoryTool;
    private final ObjectMapper objectMapper;

    private static final List<ToolDefinition> ORDER_TOOLS = Stream.concat(
            OrderRepositoryTool.TOOL_SPECS.stream(),
            OrderApiTool.TOOL_SPECS.stream()
    ).toList();

    private static final List<ToolDefinition> ALL_TOOLS = Stream.of(
            ORDER_TOOLS, UserRepositoryTool.TOOL_SPECS
    ).flatMap(List::stream).toList();

    public ToolRegistry(OrderRepositoryTool orderRepositoryTool,
                        OrderApiTool orderApiTool,
                        UserRepositoryTool userRepositoryTool,
                        ObjectMapper objectMapper) {
        this.orderRepositoryTool = orderRepositoryTool;
        this.orderApiTool = orderApiTool;
        this.userRepositoryTool = userRepositoryTool;
        this.objectMapper = objectMapper;
    }

    public List<ToolDefinition> getToolsForCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) return ALL_TOOLS;

        Set<String> seen = new LinkedHashSet<>();
        List<ToolDefinition> result = new ArrayList<>();
        for (String cat : categories) {
            List<ToolDefinition> specs = switch (cat) {
                case "주문조회" -> ORDER_TOOLS;
                case "유저조회" -> UserRepositoryTool.TOOL_SPECS;
                default -> Collections.emptyList();
            };
            for (ToolDefinition spec : specs) {
                if (seen.add(spec.getName())) result.add(spec);
            }
        }
        return result.isEmpty() ? ALL_TOOLS : result;
    }

    public Object dispatch(String toolName, Map<String, Object> args) {
        return switch (toolName) {
            case "get_orders_by_customer_db"  -> orderRepositoryTool.getOrdersByCustomer(toInt(args.get("customer_id")));
            case "get_order_detail_db"        -> orderRepositoryTool.getOrderDetail(toInt(args.get("order_id")));
            case "get_orders_by_customer_api" -> orderApiTool.getOrdersByCustomer(toInt(args.get("customer_id")));
            case "get_order_detail_api"       -> orderApiTool.getOrderDetail(toInt(args.get("order_id")));
            case "get_user"                   -> userRepositoryTool.getUser(toInt(args.get("user_id")));
            case "search_users"               -> userRepositoryTool.searchUsers((String) args.get("name"));
            default -> throw new IllegalArgumentException("알 수 없는 툴: " + toolName);
        };
    }

    private int toInt(Object value) {
        if (value instanceof Integer i) return i;
        if (value instanceof Long l) return l.intValue();
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(value));
    }
}
