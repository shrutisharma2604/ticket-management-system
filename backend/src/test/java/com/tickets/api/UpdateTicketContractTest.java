package com.tickets.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class UpdateTicketContractTest {

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
    void updatesFieldsTrimsValuesAndKeepsStatus() throws Exception {
        String token = loginToken();
        String ticketId = createTicket(token);
        transition(token, ticketId, "IN_PROGRESS");

        mockMvc.perform(patch("/api/tickets/" + ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "  Updated title  ",
                                  "description": "  Updated description  ",
                                  "priority": "LOW",
                                  "assignee": "  Agent Two  ",
                                  "status": "CLOSED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.assignee").value("Agent Two"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void clearsAssigneeWithNullAndLeavesOmittedFieldsUnchanged() throws Exception {
        String token = loginToken();
        String ticketId = createTicket(token);

        mockMvc.perform(patch("/api/tickets/" + ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignee\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Original title"))
                .andExpect(jsonPath("$.assignee").isEmpty())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void rejectsInvalidFieldsAndUnknownPriority() throws Exception {
        String token = loginToken();
        String ticketId = createTicket(token);

        mockMvc.perform(patch("/api/tickets/" + ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"description\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.description").exists());

        mockMvc.perform(patch("/api/tickets/" + ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"URGENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.priority").exists());
    }

    @Test
    void leavesUpdatedAtUnchangedWhenPatchIsNoOp() throws Exception {
        String token = loginToken();
        String ticketId = createTicket(token);
        String before = mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String updatedAt = objectMapper.readTree(before).get("updatedAt").asText();

        Thread.sleep(15);
        mockMvc.perform(patch("/api/tickets/" + ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value(updatedAt));
    }

    @Test
    void returnsNotFoundForMissingTicket() throws Exception {
        mockMvc.perform(patch("/api/tickets/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private String createTicket(String token) throws Exception {
        String response = mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "Original title",
                                "description", "Original description",
                                "priority", "HIGH",
                                "assignee", "Agent One"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void transition(String token, String ticketId, String requestedStatus) throws Exception {
        mockMvc.perform(post("/api/tickets/" + ticketId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("status", requestedStatus))))
                .andExpect(status().isOk());
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
