package io.github.diogohmcruz.marketlibrary.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of an order. Executed shares are final: an order that is cancelled or expires after a partial fill keeps
 * its {@code filledQuantity} and only the remaining quantity is withdrawn.
 */
public enum OrderStatus {
    /** Accepted, nothing executed yet. */
    OPEN,
    /** Some shares executed, the rest is still resting in the book. */
    PARTIALLY_FILLED,
    /** Fully executed. */
    FILLED,
    /** Withdrawn by the user; any previously executed shares stay executed. */
    CANCELLED,
    /** Reached its expiration time; any previously executed shares stay executed. */
    EXPIRED;

    /** Statuses of orders that may still be matched. */
    public static final Set<OrderStatus> ACTIVE = EnumSet.of(OPEN, PARTIALLY_FILLED);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
