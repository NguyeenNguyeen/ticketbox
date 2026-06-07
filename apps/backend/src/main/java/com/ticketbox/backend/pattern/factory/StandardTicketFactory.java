package com.ticketbox.backend.pattern.factory;

import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.Ticket;
import com.ticketbox.backend.entity.TicketCategory;
import com.ticketbox.backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class StandardTicketFactory implements TicketFactory {
    @Override
    public Ticket createTicket(TicketCategory category, User owner, Order order) {
        return buildBaseTicket(category, owner, order, "STANDARD");
    }
}
