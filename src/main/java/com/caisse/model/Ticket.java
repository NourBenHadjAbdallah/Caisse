package com.caisse.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class Ticket {
    private String id;
    private String journeyId;
    private String cashierId;
    private OffsetDateTime createdAt;
    private String paymentMethod; // CASH / CARD
    private final List<TicketItem> items = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getJourneyId() { return journeyId; }
    public void setJourneyId(String journeyId) { this.journeyId = journeyId; }

    public String getCashierId() { return cashierId; }
    public void setCashierId(String cashierId) { this.cashierId = cashierId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public List<TicketItem> getItems() { return items; }

    public BigDecimal total() {
        BigDecimal sum = BigDecimal.ZERO;
        for (TicketItem it : items) sum = sum.add(it.lineTotal());
        return sum.setScale(3, java.math.RoundingMode.HALF_UP);
    }
}
