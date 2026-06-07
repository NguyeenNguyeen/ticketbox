package com.ticketbox.backend.pattern.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TicketFactoryProvider {

    @Autowired
    private Map<String, TicketFactory> factories;

    public TicketFactory getFactory(String ticketType) {
        // Assume ticketType is "STANDARD" or "VIP"
        // Spring bean names are typically camelCase, e.g., "standardTicketFactory"
        String beanName = ticketType.toLowerCase() + "TicketFactory";
        TicketFactory factory = factories.get(beanName);
        if (factory == null) {
            // Fallback to standard
            return factories.get("standardTicketFactory");
        }
        return factory;
    }
}
