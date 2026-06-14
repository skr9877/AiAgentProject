package com.example.chatbot.tools.order.api;

import com.example.chatbot.tools.order.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
public class OrderApiClient {

    @Value("${java.server.url:http://localhost:8080}")
    private String serverUrl;

    private final RestTemplate restTemplate;

    public OrderApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Order> findByCustomer(int customerId) {
        try {
            String url = serverUrl + "/api/orders?customerId=" + customerId;
            ResponseEntity<List<Order>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public Order findById(int orderId) {
        try {
            String url = serverUrl + "/api/orders/" + orderId;
            return restTemplate.getForObject(url, Order.class);
        } catch (Exception e) {
            return null;
        }
    }
}
