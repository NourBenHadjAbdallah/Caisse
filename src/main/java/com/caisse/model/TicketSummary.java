package com.caisse.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Lightweight row used by the ticket history screen. */
public class TicketSummary {
    private final String id;
    private final OffsetDateTime createdAt;
    private final BigDecimal total;
    private final String paymentMethod;

    public TicketSummary(String id, OffsetDateTime createdAt, BigDecimal total, String paymentMethod) {
        this.id = id;
        this.createdAt = createdAt;
        this.total = total;
        this.paymentMethod = paymentMethod;
    }

    public String getId() { return id; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public BigDecimal getTotal() { return total; }
    public String getPaymentMethod() { return paymentMethod; }
}