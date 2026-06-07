package com.ticketbox.backend.pattern.factory;

import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.Ticket;
import com.ticketbox.backend.entity.TicketCategory;
import com.ticketbox.backend.entity.TicketStatus;
import com.ticketbox.backend.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TicketFactory {
    Ticket createTicket(TicketCategory category, User owner, Order order);
    
    default Ticket buildBaseTicket(TicketCategory category, User owner, Order order, String type) {
        return Ticket.builder()
                .category(category)
                .owner(owner)
                .order(order)
                .status(TicketStatus.ACTIVE)
                .qrCode(UUID.randomUUID().toString())
                .ticketType(type)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
