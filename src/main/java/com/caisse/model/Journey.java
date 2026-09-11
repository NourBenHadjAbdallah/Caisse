package com.caisse.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Journey {
    private String id;             // e.g. J-2026-0015
    private String userId;
    private String cashierCode;
    private OffsetDateTime openedAt;
    private OffsetDateTime closedAt;
    private String status;         // OPEN / CLOSED
    private BigDecimal totalSales = BigDecimal.ZERO;
    private BigDecimal totalCash = BigDecimal.ZERO;
    private BigDecimal totalCard = BigDecimal.ZERO;
    private BigDecimal totalReturns = BigDecimal.ZERO;
    private BigDecimal netTotal = BigDecimal.ZERO;
    private int ticketCount = 0;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCashierCode() { return cashierCode; }
    public void setCashierCode(String cashierCode) { this.cashierCode = cashierCode; }

    public OffsetDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(OffsetDateTime openedAt) { this.openedAt = openedAt; }

    public OffsetDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(OffsetDateTime closedAt) { this.closedAt = closedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal totalSales) { this.totalSales = totalSales; }

    public BigDecimal getTotalCash() { return totalCash; }
    public void setTotalCash(BigDecimal totalCash) { this.totalCash = totalCash; }

    public BigDecimal getTotalCard() { return totalCard; }
    public void setTotalCard(BigDecimal totalCard) { this.totalCard = totalCard; }

    public BigDecimal getTotalReturns() { return totalReturns; }
    public void setTotalReturns(BigDecimal totalReturns) { this.totalReturns = totalReturns; }

    public BigDecimal getNetTotal() { return netTotal; }
    public void setNetTotal(BigDecimal netTotal) { this.netTotal = netTotal; }

    public int getTicketCount() { return ticketCount; }
    public void setTicketCount(int ticketCount) { this.ticketCount = ticketCount; }

    public boolean isOpen() { return "OPEN".equalsIgnoreCase(status); }
}
