package com.tickets.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class CreateTicketContractTest {

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
        registry.add("JWT_SECRET", () -> TEST_JWT_SECRET);
        registry.add("OPERATOR_USERNAME", () -> TEST_USERNAME);
        registry.add("OPERATOR_PASSWORD", () -> TEST_PASSWORD_HASH);
        registry.add("app.security.operator-username", () -> TEST_USERNAME);
        registry.add("app.security.operator-password", () -> TEST_PASSWORD_HASH);
        registry.add("app.security.jwt-secret", () -> TEST_JWT_SECRET);
    }

    @Test
    void createsOpenTicketAndTrimsFields() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "  Printer issue  ",
                                "description", "  Printer cannot connect  ",
                                "priority", "HIGH",
                                "assignee", "  Agent One  "))))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Printer issue"))
                .andExpect(jsonPath("$.description").value("Printer cannot connect"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assignee").value("Agent One"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void createsUnassignedTicketWhenAssigneeIsOmitted() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "Network outage",
                                "description", "Office network is unavailable",
                                "priority", "MEDIUM"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignee").isEmpty())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void treatsWhitespaceOnlyAssigneeAsUnassigned() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "Network outage",
                                "description", "Office network is unavailable",
                                "priority", "MEDIUM",
                                "assignee", "   "))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignee").isEmpty());
    }

    @Test
    void rejectsWhitespaceOnlyTitleAndDescription() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "   ",
                                "description", "\t ",
                                "priority", "LOW"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.description").exists());
    }

    @Test
    void rejectsUnknownPriority() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Printer issue",
                                  "description": "Printer cannot connect",
                                  "priority": "URGENT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.priority").exists());
    }

    @Test
    void rejectsFieldsBeyondMaximumLengths() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "x".repeat(201),
                                "description", "x".repeat(10001),
                                "priority", "HIGH",
                                "assignee", "x".repeat(101)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.assignee").exists());
    }

    @Test
    void rejectsUnauthenticatedCreate() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private String loginToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", TEST_USERNAME,
                                "password", TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }
}
