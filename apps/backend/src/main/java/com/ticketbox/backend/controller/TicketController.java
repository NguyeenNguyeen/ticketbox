package com.ticketbox.backend.controller;

import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.User;
import com.ticketbox.backend.repository.UserRepository;
import com.ticketbox.backend.service.TicketPurchaseService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketPurchaseService purchaseService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseTicket(@RequestBody PurchaseRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Order order = purchaseService.purchaseTicket(
                user, 
                request.getCategoryId(), 
                request.getQuantity(), 
                request.getIdempotencyKey()
        );

        return ResponseEntity.ok(order);
    }

    @Data
    static class PurchaseRequest {
        private Long categoryId;
        private Integer quantity;
        private String idempotencyKey;
    }
}
