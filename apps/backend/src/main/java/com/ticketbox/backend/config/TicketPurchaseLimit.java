package com.ticketbox.backend.config;

import java.util.Map;

/**
 * Hardcoded per-account ticket purchase limits per ticket category name.
 *
 * Rules:
 *   - SVIP     : max 2 tickets per account
 *   - STANDARD : max 4 tickets per account
 *
 * These limits apply across ALL successfully paid orders —
 * users cannot bypass them by splitting into multiple small orders.
 *
 * To add or change limits, update the LIMITS map below.
 */
public final class TicketPurchaseLimit {

    private TicketPurchaseLimit() {}

    /** Default limit applied when category name is not explicitly listed. */
    public static final int DEFAULT_LIMIT = 1;

    private static final Map<String, Integer> LIMITS = Map.of(
            "SVIP",     2,
            "STANDARD", 4
    );

    /**
     * Returns the maximum number of tickets a single account may purchase
     * for the given category (across all completed orders).
     *
     * @param categoryName the category name, e.g. "SVIP", "STANDARD"
     * @return the per-account limit for that category
     */
    public static int getLimit(String categoryName) {
        return LIMITS.getOrDefault(categoryName.toUpperCase(), DEFAULT_LIMIT);
    }
}
