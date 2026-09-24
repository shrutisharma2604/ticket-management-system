package com.tickets.domain;

public class IllegalTicketTransitionException extends RuntimeException {

    private final TicketStatus currentStatus;
    private final TicketStatus requestedStatus;

    public IllegalTicketTransitionException(TicketStatus currentStatus, TicketStatus requestedStatus) {
        super("Transition from " + currentStatus + " to " + requestedStatus + " is not allowed");
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    public TicketStatus getCurrentStatus() {
        return currentStatus;
    }

    public TicketStatus getRequestedStatus() {
        return requestedStatus;
    }
}
