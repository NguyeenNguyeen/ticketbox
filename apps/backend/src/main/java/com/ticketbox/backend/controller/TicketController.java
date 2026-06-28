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

import java.util.List;
import java.util.UUID;
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

    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseTicket(
            @RequestBody PurchaseRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKeyHeader
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String idempotencyKey = idempotencyKeyHeader;
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = request.getIdempotencyKey();
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        Order order = purchaseService.purchaseTicket(
                user,
                request.getCategoryId(),
                request.getQuantity(),
                idempotencyKey
        );

        return ResponseEntity.ok(order);
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserveTicket(
            @RequestBody PurchaseRequest request,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKeyHeader
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String idempotencyKey = idempotencyKeyHeader;
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = request.getIdempotencyKey();
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }

        Order order = purchaseService.reserveTickets(
                user,
                request.getCategoryId(),
                request.getQuantity(),
                idempotencyKey
        );

        return ResponseEntity.ok(order);
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
        
        // Mock row and seat number based on ticket ID
        char rowChar = (char) ('A' + (ticket.getId() % 8));
        dto.setRow(String.valueOf(rowChar));
        dto.setSeatNumber((int) (ticket.getId() % 15 + 1));
        
        dto.setQrCode(ticket.getQrCode());
        dto.setHolderName(ticket.getOwner().getUsername());
        dto.setHolderEmail(ticket.getOwner().getUsername() + "@ticketbox.vn");
        return dto;
    }

    @Data
    static class PurchaseRequest {
        private Long categoryId;
        private Integer quantity;
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
