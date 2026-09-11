package com.caisse.state;

/** Mirrors the lifecycle described in the spec: NO_OPEN_JOURNEY -> OPENING -> OPEN -> CLOSING -> CLOSED (-> NO_OPEN_JOURNEY). */
public enum JourneyState {
    NO_OPEN_JOURNEY,
    OPENING,
    OPEN,
    CLOSING,
    CLOSED;

    public boolean ticketsEnabled() { return this == OPEN; }
    public boolean returnsEnabled() { return this == OPEN; }
    public boolean openJourneyEnabled() { return this == NO_OPEN_JOURNEY || this == CLOSED; }
    public boolean closeJourneyEnabled() { return this == OPEN; }
}
