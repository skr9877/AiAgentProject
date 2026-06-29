package com.example.chatbot.tools.restaurant.service;

import com.example.chatbot.common.MasterMap;
import com.example.chatbot.tools.restaurant.mapper.RestaurantMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RestaurantRepositoryTool {

    private final RestaurantMapper restaurantMapper;

    public RestaurantRepositoryTool(RestaurantMapper restaurantMapper) {
        this.restaurantMapper = restaurantMapper;
    }

    @Tool(name = "getRestaurantById", description = "DB에서 식당 ID로 식당 정보를 조회합니다.")
    public MasterMap getRestaurantById(@ToolParam(description = "식당 ID") int restaurantId) {
        return restaurantMapper.findById(restaurantId);
    }

    @Tool(name = "searchRestaurantsByName", description = "DB에서 식당 이름으로 식당 목록을 검색합니다.")
    public List<MasterMap> searchRestaurantsByName(@ToolParam(description = "검색할 식당 이름 (부분 일치)") String name) {
        return restaurantMapper.findByName(name);
    }

    @Tool(name = "getRestaurantsByCategory", description = "DB에서 카테고리(한식, 중식, 양식 등)로 식당 목록을 조회합니다.")
    public List<MasterMap> getRestaurantsByCategory(@ToolParam(description = "식당 카테고리") String category) {
        return restaurantMapper.findByCategory(category);
    }
}
