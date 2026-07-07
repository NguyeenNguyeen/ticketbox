package com.ticketbox.backend.controller;

import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.OrderItem;
import com.ticketbox.backend.entity.User;
import com.ticketbox.backend.repository.OrderRepository;
import com.ticketbox.backend.repository.OrderItemRepository;
import com.ticketbox.backend.service.TicketPurchaseService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments/sandbox")
@Slf4j
public class PaymentSandboxController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TicketPurchaseService purchaseService;

    @PostMapping("/complete")
    public ResponseEntity<?> completeSandboxPayment(@RequestBody SandboxCompleteRequest request) {
        log.info("Sandbox payment complete request: orderId={}, status={}, provider={}",
                request.getOrderId(), request.getStatus(), request.getProvider());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        List<OrderItem> items = orderItemRepository.findByOrderId(request.getOrderId());
        if (items.isEmpty()) {
            return ResponseEntity.badRequest().body("No items found for order ID " + request.getOrderId());
        }

        User user = order.getUser();

        Order updatedOrder;
        if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
            updatedOrder = purchaseService.finalizeOrderSuccess(order.getId(), user);
            log.info("Sandbox payment success. Order {} finalized.", order.getId());
        } else {
            updatedOrder = purchaseService.finalizeOrderFailure(order.getId());
            log.info("Sandbox payment failed/cancelled. Order {} cancelled and inventory restored.", order.getId());
        }

        // Return a simplified map to avoid Jackson serialization issues with Hibernate lazy loading of the User entity
        return ResponseEntity.ok(java.util.Map.of(
            "orderId", updatedOrder.getId(),
            "status", updatedOrder.getStatus().name(),
            "totalAmount", updatedOrder.getTotalAmount(),
            "createdAt", updatedOrder.getCreatedAt().toString()
        ));
    }

    @Data
    static class SandboxCompleteRequest {
        private Long orderId;
        private String status;   // "SUCCESS" or "FAILED"
        private String provider; // "VNPAY" or "MOMO"
    }
}
