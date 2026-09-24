package com.tickets.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public final class TicketStatusMachine {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = createAllowedTransitions();

    public void validate(TicketStatus current, TicketStatus requested) {
        if (current == requested) {
            return;
        }
        if (!ALLOWED_TRANSITIONS.get(current).contains(requested)) {
            throw new IllegalTicketTransitionException(current, requested);
        }
    }

    private static Map<TicketStatus, Set<TicketStatus>> createAllowedTransitions() {
        Map<TicketStatus, Set<TicketStatus>> transitions = new EnumMap<>(TicketStatus.class);
        transitions.put(TicketStatus.OPEN, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED));
        transitions.put(TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED));
        transitions.put(TicketStatus.RESOLVED, EnumSet.of(TicketStatus.CLOSED));
        transitions.put(TicketStatus.CLOSED, EnumSet.noneOf(TicketStatus.class));
        transitions.put(TicketStatus.CANCELLED, EnumSet.noneOf(TicketStatus.class));
        return Map.copyOf(transitions);
    }
}
