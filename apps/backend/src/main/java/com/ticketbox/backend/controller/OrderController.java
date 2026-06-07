package com.ticketbox.backend.controller;

import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.User;
import com.ticketbox.backend.repository.OrderRepository;
import com.ticketbox.backend.repository.UserRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/orders/history")
    public ResponseEntity<List<OrderHistoryDto>> getOrderHistory() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        return ResponseEntity.ok(orders.stream().map(this::mapToDto).collect(Collectors.toList()));
    }

    @GetMapping("/admin/orders")
    public ResponseEntity<List<OrderHistoryDto>> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(orders.stream().map(this::mapToDto).collect(Collectors.toList()));
    }

    private OrderHistoryDto mapToDto(Order order) {
        OrderHistoryDto dto = new OrderHistoryDto();
        dto.setId(order.getId().toString());
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUsername(order.getUser().getUsername());
        return dto;
    }

    @Data
    static class OrderHistoryDto {
        private String id;
        private String status;
        private BigDecimal totalAmount;
        private LocalDateTime createdAt;
        private String username;
    }
}
