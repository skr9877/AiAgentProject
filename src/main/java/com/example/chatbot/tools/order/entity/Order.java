package com.example.chatbot.tools.order.entity;

import lombok.Data;

@Data
public class Order {
    private int orderId;
    private int customerId;
    private int productId;
    private String productName;
    private int quantity;
    private double totalPrice;
    private String status;
    private String orderDate;
    private String deliveryAddress;
}
