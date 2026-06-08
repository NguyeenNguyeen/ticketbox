package com.ticketbox.backend.pattern.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TicketFactoryProvider {

    @Autowired
    private Map<String, TicketFactory> factories;

    /**
     * Maps category names (from DB) to Spring bean names of TicketFactory implementations.
     * - "STANDARD" → standardTicketFactory
     * - "VIP"      → vIPTicketFactory
     * - "SVIP"     → vIPTicketFactory  (SVIP uses VIP factory, same ticketing logic)
     * Any unknown category falls back to standardTicketFactory.
     */
    private static final Map<String, String> CATEGORY_TO_BEAN = Map.of(
            "STANDARD", "standardTicketFactory",
            "VIP",      "VIPTicketFactory",
            "SVIP",     "VIPTicketFactory"
    );

    public TicketFactory getFactory(String categoryName) {
        String beanName = CATEGORY_TO_BEAN.getOrDefault(
                categoryName.toUpperCase(), "standardTicketFactory");
        TicketFactory factory = factories.get(beanName);
        if (factory == null) {
            factory = factories.get("standardTicketFactory");
        }
        return factory;
    }
}
