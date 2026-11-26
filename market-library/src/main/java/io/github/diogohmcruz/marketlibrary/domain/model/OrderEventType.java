package io.github.diogohmcruz.marketlibrary.domain.model;

/** What happened to an order. One event is recorded per change of the order's lifecycle. */
public enum OrderEventType {
    /** The order was accepted by the matching engine. */
    NEW,
    /** Part (or all) of the order was executed in a trade. */
    FILL,
    /** The unexecuted remainder was withdrawn by the user. */
    CANCELLED,
    /** The unexecuted remainder was withdrawn because the order expired. */
    EXPIRED
}
