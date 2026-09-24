package com.tickets.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class CommentContractTest {

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
    void addsTrimmedCommentsAndReturnsThemOldestFirst() throws Exception {
        String token = loginToken();
        String ticketId = createTicket(token);

        addComment(token, ticketId, "  First comment  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("First comment"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
        Thread.sleep(15);
        addComment(token, ticketId, "Second comment")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].body").value("First comment"))
                .andExpect(jsonPath("$.comments[1].body").value("Second comment"));
    }

    @Test
    void rejectsBlankCommentAndMissingTicket() throws Exception {
        String token = loginToken();
        addComment(token, createTicket(token), "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body").exists());

        addComment(token, UUID.randomUUID().toString(), "Missing ticket")
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void allowsCommentOnClosedTicketWithoutChangingStatus() throws Exception {
        String token = loginToken();
        String ticketId = createTicket(token);
        transition(token, ticketId, "IN_PROGRESS");
        transition(token, ticketId, "RESOLVED");
        transition(token, ticketId, "CLOSED");

        addComment(token, ticketId, "Closed-ticket note")
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.comments[0].body").value("Closed-ticket note"));

        String cancelledTicketId = createTicket(token);
        transition(token, cancelledTicketId, "CANCELLED");
        addComment(token, cancelledTicketId, "Cancelled-ticket note")
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/tickets/" + cancelledTicketId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.comments[0].body").value("Cancelled-ticket note"));
    }

    private org.springframework.test.web.servlet.ResultActions addComment(
            String token, String ticketId, String body) throws Exception {
        return mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(Map.of("body", body))));
    }

    private void transition(String token, String ticketId, String requestedStatus) throws Exception {
        mockMvc.perform(post("/api/tickets/" + ticketId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("status", requestedStatus))))
                .andExpect(status().isOk());
    }

    private String createTicket(String token) throws Exception {
        String response = mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "Comment ticket " + UUID.randomUUID(),
                                "description", "Comment description",
                                "priority", "MEDIUM"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
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
