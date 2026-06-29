package com.example.chatbot.tools.restaurant.mapper;

import com.example.chatbot.common.MasterMap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RestaurantMapper {

    MasterMap findById(@Param("id") int id);

    List<MasterMap> findByName(@Param("bizName") String bizName);

    List<MasterMap> findByFoodType(@Param("foodType") String foodType);

    List<MasterMap> findByBizStatus(@Param("bizStatusNm") String bizStatusNm);
}
