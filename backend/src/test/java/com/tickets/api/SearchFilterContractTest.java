package com.tickets.api;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class SearchFilterContractTest {

    private static final String TEST_USERNAME = "operator-" + UUID.randomUUID();
    private static final String TEST_PASSWORD = UUID.randomUUID().toString();
    private static final String TEST_PASSWORD_HASH = new BCryptPasswordEncoder().encode(TEST_PASSWORD);
    private static final String TEST_JWT_SECRET = UUID.randomUUID() + UUID.randomUUID().toString();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private String openTitle;
    private String inProgressTitle;

    @DynamicPropertySource
    static void authenticationProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.operator-username", () -> TEST_USERNAME);
        registry.add("app.security.operator-password", () -> TEST_PASSWORD_HASH);
        registry.add("app.security.jwt-secret", () -> TEST_JWT_SECRET);
    }

    @BeforeEach
    void createSearchFixtures() throws Exception {
        token = loginToken();
        String marker = UUID.randomUUID().toString();
        openTitle = "Printer " + marker;
        inProgressTitle = "Network " + marker;
        createTicket(openTitle, "Paper jam alpha-" + marker);
        String inProgressId = createTicket(inProgressTitle, "Gateway beta-" + marker);
        transition(inProgressId, "IN_PROGRESS");
    }

    @Test
    void keywordMatchesTitleOrDescriptionCaseInsensitively() throws Exception {
        list("q", openTitle.toUpperCase())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem(openTitle)))
                .andExpect(jsonPath("$.content[*].title", not(hasItem(inProgressTitle))));

        list("q", "BETA-" + inProgressTitle.substring("Network ".length()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem(inProgressTitle)));
    }

    @Test
    void blankKeywordMeansNoKeywordAndStatusFiltersExactly() throws Exception {
        list("q", "   ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem(openTitle)))
                .andExpect(jsonPath("$.content[*].title", hasItem(inProgressTitle)));

        list("status", "IN_PROGRESS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(org.hamcrest.Matchers.is("IN_PROGRESS"))))
                .andExpect(jsonPath("$.content[*].title", hasItem(inProgressTitle)));
    }

    @Test
    void treatsPercentAndUnderscoreAsLiteralCharacters() throws Exception {
        String marker = UUID.randomUUID().toString();
        String literalTitle = "100% ready_" + marker;
        createTicket(literalTitle, "Contains wildcards " + marker);
        createTicket("Other ticket " + marker, "No special characters");

        list("q", "100% ready_" + marker)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem(literalTitle)))
                .andExpect(jsonPath("$.content.length()").value(1));

        list("q", "ready_" + marker)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem(literalTitle)))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void rejectsUnknownStatusFilterWithFieldError() throws Exception {
        mockMvc.perform(get("/api/tickets")
                        .param("status", "NOT_A_STATUS")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists());
    }

    @Test
    void combinesKeywordAndStatusAndReturnsEmptyPageForNoMatches() throws Exception {
        mockMvc.perform(get("/api/tickets")
                        .param("q", openTitle)
                        .param("status", "IN_PROGRESS")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        list("q", "no-match-" + UUID.randomUUID())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    private org.springframework.test.web.servlet.ResultActions list(String name, String value) throws Exception {
        return mockMvc.perform(get("/api/tickets")
                .param(name, value)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String createTicket(String title, String description) throws Exception {
        String response = mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", title,
                                "description", description,
                                "priority", "MEDIUM"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void transition(String ticketId, String requestedStatus) throws Exception {
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
