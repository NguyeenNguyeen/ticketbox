package com.ticketbox.backend.pattern.strategy;

import java.math.BigDecimal;

public class DiscountPricingStrategy implements PricingStrategy {
    private final BigDecimal discountPercentage;

    public DiscountPricingStrategy(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    @Override
    public BigDecimal calculatePrice(BigDecimal basePrice) {
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discountPercentage);
        return basePrice.multiply(discountMultiplier);
    }
}
