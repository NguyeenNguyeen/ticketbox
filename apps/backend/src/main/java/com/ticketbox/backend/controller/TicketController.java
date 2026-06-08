package com.ticketbox.backend.controller;

import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.User;
import com.ticketbox.backend.entity.Ticket;
import com.ticketbox.backend.repository.UserRepository;
import com.ticketbox.backend.repository.TicketRepository;
import com.ticketbox.backend.service.TicketPurchaseService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    @Autowired
    private TicketPurchaseService purchaseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    /**
     * Purchase a ticket category.
     * idempotencyKey is required in the request body to prevent double-charges.
     * Returns the order details and a mock paymentUrl for the demo.
     */
    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseTicket(@RequestBody PurchaseRequest request) {
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "idempotencyKey is required"));
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Order order = purchaseService.purchaseTicket(
                user, 
                request.getCategoryId(), 
                request.getQuantity(), 
                request.getIdempotencyKey()
        );

        // Build response with paymentUrl for frontend redirect
        Map<String, Object> response = new HashMap<>();
        response.put("id", order.getId().toString());
        response.put("status", order.getStatus().name());
        response.put("totalAmount", order.getTotalAmount());
        // Mock payment gateway redirect URL (simulates VNPAY/MoMo redirect)
        response.put("paymentUrl",
                "/payment/callback?vnp_ResponseCode=00&vnp_TxnRef=" + order.getId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<ETicketDto>> getTicketsByOrderId(@PathVariable Long orderId) {
        List<Ticket> tickets = ticketRepository.findByOrderId(orderId);
        List<ETicketDto> dtos = tickets.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ETicketDto> getTicketById(@PathVariable Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        return ResponseEntity.ok(mapToDto(ticket));
    }

    private ETicketDto mapToDto(Ticket ticket) {
        ETicketDto dto = new ETicketDto();
        dto.setId(ticket.getId().toString());
        dto.setOrderId(ticket.getOrder().getId().toString());
        dto.setConcertId(ticket.getCategory().getConcert().getId().toString());
        dto.setConcertTitle(ticket.getCategory().getConcert().getName());
        dto.setConcertDate(ticket.getCategory().getConcert().getStartTime().toString());
        dto.setVenue(ticket.getCategory().getConcert().getLocation());
        dto.setZone(ticket.getCategory().getName());
        
        // Deterministic row/seat assignment from ticket ID
        char rowChar = (char) ('A' + (ticket.getId() % 8));
        dto.setRow(String.valueOf(rowChar));
        dto.setSeatNumber((int) (ticket.getId() % 15 + 1));
        
        dto.setQrCode(ticket.getQrCode());
        dto.setHolderName(ticket.getOwner().getUsername());
        // Use actual email if available, else derive from username
        String email = ticket.getOwner().getUsername();
        dto.setHolderEmail(email.contains("@") ? email : email + "@ticketbox.vn");
        return dto;
    }

    @Data
    static class PurchaseRequest {
        /** Category ID to purchase */
        private Long categoryId;
        /** Quantity to purchase */
        private Integer quantity;
        /** 
         * Client-generated idempotency key (UUID recommended).
         * Prevents double-charges if client retries the same request.
         */
        private String idempotencyKey;
    }

    @Data
    static class ETicketDto {
        private String id;
        private String orderId;
        private String concertId;
        private String concertTitle;
        private String concertDate;
        private String venue;
        private String zone;
        private String row;
        private Integer seatNumber;
        private String qrCode;
        private String holderName;
        private String holderEmail;
    }
}
