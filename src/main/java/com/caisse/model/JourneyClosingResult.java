package com.caisse.model;

import java.math.BigDecimal;

/** Carries a just-closed journey plus the cashier's confirmed counted amounts, for the Ticket Z screen. */
public class JourneyClosingResult {

    private final Journey journey;
    private final BigDecimal cashCounted;
    private final BigDecimal cardCounted;

    public JourneyClosingResult(Journey journey, BigDecimal cashCounted, BigDecimal cardCounted) {
        this.journey = journey;
        this.cashCounted = cashCounted;
        this.cardCounted = cardCounted;
    }

    public Journey getJourney() { return journey; }
    public BigDecimal getCashCounted() { return cashCounted; }
    public BigDecimal getCardCounted() { return cardCounted; }
}