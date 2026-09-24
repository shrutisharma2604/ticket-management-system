package com.tickets.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TicketStatusMachineTest {

    private final TicketStatusMachine machine = new TicketStatusMachine();

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void allowsConfiguredTransitions(TicketStatus current, TicketStatus requested) {
        assertDoesNotThrow(() -> machine.validate(current, requested));
    }

    @Test
    void allowsSameStatusForEveryStatus() {
        for (TicketStatus status : TicketStatus.values()) {
            assertDoesNotThrow(() -> machine.validate(status, status));
        }
    }

    @ParameterizedTest
    @MethodSource("forbiddenTransitions")
    void rejectsForbiddenTransitions(TicketStatus current, TicketStatus requested) {
        assertThrows(IllegalTicketTransitionException.class, () -> machine.validate(current, requested));
    }

    private static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.CLOSED),
                Arguments.of(TicketStatus.OPEN, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED));
    }

    private static Stream<Arguments> forbiddenTransitions() {
        return Arrays.stream(TicketStatus.values())
                .flatMap(current -> Arrays.stream(TicketStatus.values())
                        .filter(requested -> current != requested)
                        .filter(requested -> !isAllowed(current, requested))
                        .map(requested -> Arguments.of(current, requested)));
    }

    private static boolean isAllowed(TicketStatus current, TicketStatus requested) {
        return (current == TicketStatus.OPEN
                && (requested == TicketStatus.IN_PROGRESS || requested == TicketStatus.CANCELLED))
                || (current == TicketStatus.IN_PROGRESS
                && (requested == TicketStatus.RESOLVED || requested == TicketStatus.CANCELLED))
                || (current == TicketStatus.RESOLVED && requested == TicketStatus.CLOSED);
    }
}
