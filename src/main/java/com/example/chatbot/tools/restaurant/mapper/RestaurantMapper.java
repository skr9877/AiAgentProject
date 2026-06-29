package com.example.chatbot.tools.restaurant.mapper;

import com.example.chatbot.common.MasterMap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RestaurantMapper {

    MasterMap findById(@Param("restaurantId") int restaurantId);

    List<MasterMap> findByName(@Param("name") String name);

    List<MasterMap> findByCategory(@Param("category") String category);
}
