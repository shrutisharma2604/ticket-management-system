package com.tickets.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
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
class GetTicketsContractTest {

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
    void listsTicketsWithPageMetadataDefaultSizeAndNewestFirst() throws Exception {
        String token = loginToken();
        String olderTitle = "Older ticket " + UUID.randomUUID();
        String newerTitle = "Newer ticket " + UUID.randomUUID();
        createTicket(token, olderTitle, "First created", "LOW", "Agent A");
        Thread.sleep(15);
        createTicket(token, newerTitle, "Second created", "HIGH", null);

        String body = mockMvc.perform(get("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode content = objectMapper.readTree(body).get("content");
        JsonNode newer = findByTitle(content, newerTitle);
        JsonNode older = findByTitle(content, olderTitle);
        assertEquals("HIGH", newer.get("priority").asText());
        assertTrue(newer.get("assignee").isNull());
        assertEquals("OPEN", newer.get("status").asText());
        assertTrue(newer.get("description") == null || newer.get("description").isMissingNode());
        assertEquals("Agent A", older.get("assignee").asText());
        assertTrue(indexOfTitle(content, newerTitle) < indexOfTitle(content, olderTitle));
    }

    @Test
    void paginatesTicketsAndRejectsSizeAboveMaximum() throws Exception {
        String token = loginToken();
        String firstTitle = "Page ticket " + UUID.randomUUID();
        String secondTitle = "Page ticket " + UUID.randomUUID();
        createTicket(token, firstTitle, "First page item", "MEDIUM", null);
        Thread.sleep(15);
        createTicket(token, secondTitle, "Second page item", "MEDIUM", null);

        mockMvc.perform(get("/api/tickets")
                        .param("page", "0")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1));

        JsonNode allTickets = allTitles(token);
        int secondIndex = indexOfTitle(allTickets, secondTitle);
        int firstIndex = indexOfTitle(allTickets, firstTitle);
        assertTrue(secondIndex < firstIndex);

        JsonNode laterPage = objectMapper.readTree(mockMvc.perform(get("/api/tickets")
                        .param("page", String.valueOf(firstIndex))
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertEquals(firstTitle, laterPage.get("content").get(0).get("title").asText());

        mockMvc.perform(get("/api/tickets")
                        .param("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void returnsTicketDetailWithEmptyComments() throws Exception {
        String token = loginToken();
        String title = "Detail ticket " + UUID.randomUUID();
        JsonNode created = createTicket(token, title, "Full description", "LOW", "Agent B");

        mockMvc.perform(get("/api/tickets/" + created.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(created.get("id").asText()))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.description").value("Full description"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.assignee").value("Agent B"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments").isEmpty());
    }

    @Test
    void returnsNotFoundProblemForUnknownTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void rejectsNonUuidTicketIdWithProblemDetails() throws Exception {
        mockMvc.perform(get("/api/tickets/not-a-uuid")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginToken()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.ticketId").exists());
    }

    @Test
    void rejectsUnauthenticatedListAndDetail() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/tickets/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private JsonNode allTitles(String token) throws Exception {
        String body = mockMvc.perform(get("/api/tickets")
                        .param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("content");
    }

    private static JsonNode findByTitle(JsonNode content, String title) {
        for (JsonNode ticket : content) {
            if (title.equals(ticket.get("title").asText())) {
                return ticket;
            }
        }
        throw new AssertionError("Missing ticket titled " + title);
    }

    private static int indexOfTitle(JsonNode content, String title) {
        int index = 0;
        for (JsonNode ticket : content) {
            if (title.equals(ticket.get("title").asText())) {
                return index;
            }
            index++;
        }
        throw new AssertionError("Missing ticket titled " + title);
    }

    private JsonNode createTicket(
            String token,
            String title,
            String description,
            String priority,
            String assignee) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("title", title);
        body.put("description", description);
        body.put("priority", priority);
        if (assignee != null) {
            body.put("assignee", assignee);
        }
        String response = mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
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
