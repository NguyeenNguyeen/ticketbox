package com.ticketbox.backend.pattern.strategy;

import java.math.BigDecimal;

public class StandardPricingStrategy implements PricingStrategy {
    @Override
    public BigDecimal calculatePrice(BigDecimal basePrice) {
        return basePrice;
    }
}
