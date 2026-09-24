package com.tickets.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tickets.TicketApplication;
import com.tickets.api.AddCommentRequest;
import com.tickets.api.CreateTicketRequest;
import com.tickets.api.TicketDetailResponse;
import com.tickets.api.TicketResponse;
import com.tickets.api.UpdateTicketRequest;
import com.tickets.domain.TicketPriority;
import com.tickets.domain.TicketStatus;
import com.tickets.service.TicketService;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class TicketPersistenceIT {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void ticketSurvivesApplicationContextRestart() {
        Map<String, Object> properties = applicationProperties();
        UUID ticketId;

        try (ConfigurableApplicationContext context = startApplication(properties)) {
            TicketService service = context.getBean(TicketService.class);
            TicketResponse created = service.create(new CreateTicketRequest(
                    "  Persistent ticket  ",
                    "  Must survive restart  ",
                    TicketPriority.HIGH,
                    null));
            ticketId = created.id();
            assertNotNull(ticketId);

            UpdateTicketRequest update = new UpdateTicketRequest();
            update.setTitle("Updated persistent ticket");
            service.updateDetails(ticketId, update);
            service.addComment(ticketId, new AddCommentRequest("Persistent comment"));
        }

        try (ConfigurableApplicationContext context = startApplication(properties)) {
            TicketDetailResponse reloaded = context.getBean(TicketService.class).getById(ticketId);

            assertEquals("Updated persistent ticket", reloaded.title());
            assertEquals("Must survive restart", reloaded.description());
            assertEquals(TicketPriority.HIGH, reloaded.priority());
            assertEquals(TicketStatus.OPEN, reloaded.status());
            assertNotNull(reloaded.createdAt());
            assertNotNull(reloaded.updatedAt());
            assertEquals(1, reloaded.comments().size());
            assertEquals("Persistent comment", reloaded.comments().getFirst().body());
        }
    }

    private ConfigurableApplicationContext startApplication(Map<String, Object> properties) {
        String[] arguments = properties.entrySet()
                .stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
        return new SpringApplicationBuilder(TicketApplication.class)
                .profiles("local")
                .web(WebApplicationType.SERVLET)
                .run(arguments);
    }

    private Map<String, Object> applicationProperties() {
        String databasePath = temporaryDirectory.resolve("tickets").toAbsolutePath().toString();
        String password = new BCryptPasswordEncoder().encode(UUID.randomUUID().toString());
        String jwtSecret = UUID.randomUUID() + UUID.randomUUID().toString();

        return Map.ofEntries(
                Map.entry(
                        "spring.datasource.url",
                        "jdbc:h2:file:" + databasePath
                                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"),
                Map.entry("server.port", "0"),
                Map.entry("JWT_SECRET", jwtSecret),
                Map.entry("OPERATOR_USERNAME", "integration-operator"),
                Map.entry("OPERATOR_PASSWORD", password),
                Map.entry("app.security.jwt-secret", jwtSecret),
                Map.entry("app.security.operator-username", "integration-operator"),
                Map.entry("app.security.operator-password", password));
    }
}
