package com.caisse.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReturnRecord {
    private String id;
    private String originalTicketId;
    private String journeyId;
    private OffsetDateTime createdAt;
    private final List<ReturnItem> items = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOriginalTicketId() { return originalTicketId; }
    public void setOriginalTicketId(String originalTicketId) { this.originalTicketId = originalTicketId; }

    public String getJourneyId() { return journeyId; }
    public void setJourneyId(String journeyId) { this.journeyId = journeyId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public List<ReturnItem> getItems() { return items; }

    public BigDecimal total() {
        BigDecimal sum = BigDecimal.ZERO;
        for (ReturnItem it : items) sum = sum.add(it.lineTotal());
        return sum.setScale(3, java.math.RoundingMode.HALF_UP);
    }
}
