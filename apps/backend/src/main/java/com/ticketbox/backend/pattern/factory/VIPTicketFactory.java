package com.ticketbox.backend.pattern.factory;

import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.Ticket;
import com.ticketbox.backend.entity.TicketCategory;
import com.ticketbox.backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class VIPTicketFactory implements TicketFactory {
    @Override
    public Ticket createTicket(TicketCategory category, User owner, Order order) {
        Ticket ticket = buildBaseTicket(category, owner, order, "VIP");
        // Additional VIP specific logic could be added here
        return ticket;
    }
}
