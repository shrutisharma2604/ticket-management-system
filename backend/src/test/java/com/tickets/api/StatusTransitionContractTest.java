package com.tickets.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatusTransitionContractTest {

    private static final String TEST_USERNAME = "operator-" + UUID.randomUUID();
    private static final String TEST_PASSWORD = UUID.randomUUID().toString();
    private static final String TEST_PASSWORD_HASH = new BCryptPasswordEncoder().encode(TEST_PASSWORD);
    private static final String TEST_JWT_SECRET = UUID.randomUUID() + UUID.randomUUID().toString();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void authenticationProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.operator-username", () -> TEST_USERNAME);
        registry.add("app.security.operator-password", () -> TEST_PASSWORD_HASH);
        registry.add("app.security.jwt-secret", () -> TEST_JWT_SECRET);
    }

    @Test
    void appliesAllowedTransitionsAndSameStatusNoOp() throws Exception {
        String token = loginToken();
        String ticketId = createTicket(token);

        transition(token, ticketId, "OPEN").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
        transition(token, ticketId, "IN_PROGRESS").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        transition(token, ticketId, "RESOLVED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        transition(token, ticketId, "CLOSED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void rejectsIllegalTransitionWithoutChangingStoredStatus() throws Exception {
        String token = loginToken();
        String ticketId = createTicket(token);

        transition(token, ticketId, "RESOLVED")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("OPEN"),
                                org.hamcrest.Matchers.containsString("RESOLVED"))));

        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void rejectsUnknownStatusAndMissingTicket() throws Exception {
        String token = loginToken();
        transition(token, createTicket(token), "UNKNOWN")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.status").exists());

        transition(token, UUID.randomUUID().toString(), "IN_PROGRESS")
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void rejectsReopeningResolvedClosedAndCancelledTickets() throws Exception {
        String token = loginToken();

        String resolvedId = createTicket(token);
        transition(token, resolvedId, "IN_PROGRESS").andExpect(status().isOk());
        transition(token, resolvedId, "RESOLVED").andExpect(status().isOk());
        assertRejectedAndUnchanged(token, resolvedId, "RESOLVED", "OPEN");

        String closedId = createTicket(token);
        transition(token, closedId, "IN_PROGRESS").andExpect(status().isOk());
        transition(token, closedId, "RESOLVED").andExpect(status().isOk());
        transition(token, closedId, "CLOSED").andExpect(status().isOk());
        assertRejectedAndUnchanged(token, closedId, "CLOSED", "OPEN");

        String cancelledId = createTicket(token);
        transition(token, cancelledId, "CANCELLED").andExpect(status().isOk());
        assertRejectedAndUnchanged(token, cancelledId, "CANCELLED", "OPEN");
    }

    private org.springframework.test.web.servlet.ResultActions transition(
            String token, String ticketId, String requestedStatus) throws Exception {
        return mockMvc.perform(post("/api/tickets/" + ticketId + "/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(Map.of("status", requestedStatus))));
    }

    private void assertRejectedAndUnchanged(
            String token,
            String ticketId,
            String currentStatus,
            String requestedStatus) throws Exception {
        transition(token, ticketId, requestedStatus)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString(currentStatus),
                                org.hamcrest.Matchers.containsString(requestedStatus))));
        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(currentStatus));
    }

    private String createTicket(String token) throws Exception {
        String response = mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "Workflow ticket " + UUID.randomUUID(),
                                "description", "Workflow description",
                                "priority", "HIGH"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.get("id").asText();
    }

    private String loginToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", TEST_USERNAME,
                                "password", TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }
}
