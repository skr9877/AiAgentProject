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

    @Tool(name = "getRestaurantById", description = "ID로 식당 정보를 조회합니다. BIZ_NAME(상호명), ROAD_ADDR(도로명주소), PHONE_NO(전화번호), MAIN_FOOD_TYPE(음식종류), BIZ_STATUS_NM(영업상태) 등을 반환합니다.")
    public MasterMap getRestaurantById(@ToolParam(description = "식당 ID") int id) {
        return restaurantMapper.findById(id);
    }

    @Tool(name = "searchRestaurantsByName", description = "상호명(BIZ_NAME)으로 식당을 검색합니다. 부분 일치 검색이며 최대 20건 반환합니다.")
    public List<MasterMap> searchRestaurantsByName(@ToolParam(description = "검색할 상호명 (부분 일치)") String bizName) {
        return restaurantMapper.findByName(bizName);
    }

    @Tool(name = "getRestaurantsByFoodType", description = "음식 종류(MAIN_FOOD_TYPE)로 식당 목록을 조회합니다. 예: 한식, 중식, 양식, 일식 등. 최대 20건 반환합니다.")
    public List<MasterMap> getRestaurantsByFoodType(@ToolParam(description = "음식 종류 (예: 한식, 중식, 양식)") String foodType) {
        return restaurantMapper.findByFoodType(foodType);
    }

    @Tool(name = "getRestaurantsByBizStatus", description = "영업상태(BIZ_STATUS_NM)로 식당 목록을 조회합니다. 폐업 여부 확인 시 사용합니다. 상태값 예: 영업, 폐업, 휴업. 최대 20건 반환합니다.")
    public List<MasterMap> getRestaurantsByBizStatus(@ToolParam(description = "영업상태명 (예: 영업, 폐업, 휴업)") String bizStatusNm) {
        return restaurantMapper.findByBizStatus(bizStatusNm);
    }
}
