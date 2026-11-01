package com.expensetracker;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the full Spring MVC stack against the real
 * Flyway-migrated H2 schema. Each test registers its own user from its own
 * fake client IP so the per-IP auth rate limit never couples tests together.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final AtomicInteger IP_SEQ = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    /** Tokens + client IP for a freshly registered user. */
    private record Session(String accessToken, String refreshToken, String ip) {}

    private static String nextIp() {
        int n = IP_SEQ.getAndIncrement();
        return "10.0." + (n / 250) + "." + (n % 250 + 1);
    }

    private static MockHttpServletRequestBuilder from(MockHttpServletRequestBuilder req, String ip) {
        return req.with(r -> { r.setRemoteAddr(ip); return r; });
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String body) {
        return req.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private Session register() throws Exception {
        String ip = nextIp();
        String email = "user-" + UUID.randomUUID() + "@example.com";
        MvcResult res = mockMvc.perform(from(json(post("/api/auth/register"), """
                        {"email":"%s","password":"correcthorse","firstName":"Ada","lastName":"Lovelace"}
                        """.formatted(email)), ip))
                .andExpect(status().isCreated())
                .andReturn();
        String body = res.getResponse().getContentAsString();
        return new Session(JsonPath.read(body, "$.data.accessToken"), JsonPath.read(body, "$.data.refreshToken"), ip);
    }

    private static String bearer(Session s) {
        return "Bearer " + s.accessToken();
    }

    @Test
    @DisplayName("the removed legacy /api/expenses endpoint is gone (404, not a 500 or open data)")
    void legacyExpensesEndpoint_isGone() throws Exception {
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("protected endpoints reject requests without a token")
    void protectedEndpoint_requiresToken() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Missing or invalid Authorization header"));
    }

    @Test
    @DisplayName("registration seeds the default categories")
    void register_seedsDefaultCategories() throws Exception {
        Session s = register();
        mockMvc.perform(get("/api/categories").header("Authorization", bearer(s)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].name").value("Income"));
    }

    @Test
    @DisplayName("a wrong password returns 401 with the backend's message")
    void login_wrongPassword_returnsMessage() throws Exception {
        mockMvc.perform(from(json(post("/api/auth/login"), """
                        {"email":"nobody-%s@example.com","password":"whatever1"}
                        """.formatted(UUID.randomUUID())), nextIp()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    @DisplayName("repeated failed logins are rate limited with 429")
    void login_bruteForce_rateLimited() throws Exception {
        String ip = nextIp();
        String body = """
                {"email":"victim@example.com","password":"guess-guess"}
                """;
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(from(json(post("/api/auth/login"), body), ip))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(from(json(post("/api/auth/login"), body), ip))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("logout revokes the refresh token")
    void logout_revokesRefreshToken() throws Exception {
        Session s = register();
        String refreshBody = "{\"refreshToken\":\"" + s.refreshToken() + "\"}";

        mockMvc.perform(json(post("/api/auth/refresh"), refreshBody)).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").header("Authorization", bearer(s)))
                .andExpect(status().isOk());

        mockMvc.perform(json(post("/api/auth/refresh"), refreshBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Refresh token has been revoked"));
    }

    @Test
    @DisplayName("client errors map to 4xx instead of 500")
    void clientErrors_are4xx() throws Exception {
        Session s = register();
        mockMvc.perform(get("/api/transactions/not-a-uuid").header("Authorization", bearer(s)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for 'id'"));
        mockMvc.perform(get("/api/transactions").param("type", "BOGUS").header("Authorization", bearer(s)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/transactions").header("Authorization", bearer(s)))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get("/api/does-not-exist").header("Authorization", bearer(s)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/transactions").header("Authorization", bearer(s))
                        .contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("transactions: type must match category kind; get by id and pagination work")
    void transactions_endToEnd() throws Exception {
        Session s = register();
        String categories = mockMvc.perform(get("/api/categories").header("Authorization", bearer(s)))
                .andReturn().getResponse().getContentAsString();
        String otherId = JsonPath.<List<String>>read(categories, "$.data[?(@.name=='Other')].id").get(0);
        String incomeId = JsonPath.<List<String>>read(categories, "$.data[?(@.name=='Income')].id").get(0);
        String incomeSubs = mockMvc.perform(get("/api/subcategories").param("categoryId", incomeId)
                        .header("Authorization", bearer(s)))
                .andReturn().getResponse().getContentAsString();
        String salaryId = JsonPath.<List<String>>read(incomeSubs, "$.data[?(@.name=='Salary')].id").get(0);

        // EXPENSE under the INCOME category is rejected
        mockMvc.perform(json(post("/api/transactions"), txBody("Coffee", "EXPENSE", incomeId, salaryId))
                        .header("Authorization", bearer(s)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("A EXPENSE transaction cannot use the INCOME category 'Income'"));

        // Valid transactions ("Other" has no sub-categories)
        String created = null;
        for (int i = 0; i < 3; i++) {
            created = mockMvc.perform(json(post("/api/transactions"), txBody("Misc " + i, "EXPENSE", otherId, null))
                            .header("Authorization", bearer(s)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
        }
        String txId = JsonPath.read(created, "$.data.id");

        mockMvc.perform(get("/api/transactions/" + txId).header("Authorization", bearer(s)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Misc 2"));

        mockMvc.perform(get("/api/transactions/paginated").param("page", "2").param("limit", "2")
                        .header("Authorization", bearer(s)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.pagination.total").value(3))
                .andExpect(jsonPath("$.pagination.page").value(2))
                .andExpect(jsonPath("$.pagination.limit").value(2))
                .andExpect(jsonPath("$.pagination.totalPages").value(2));

        // Another user cannot see it
        Session other = register();
        mockMvc.perform(get("/api/transactions/" + txId).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
    }

    private static String txBody(String name, String type, String categoryId, String subCategoryId) {
        Map<String, String> fields = new java.util.LinkedHashMap<>();
        fields.put("name", name);
        fields.put("amount", "12.50");
        fields.put("type", type);
        fields.put("categoryId", categoryId);
        if (subCategoryId != null) fields.put("subCategoryId", subCategoryId);
        fields.put("transactionDate", "2026-10-01");
        StringBuilder sb = new StringBuilder("{");
        fields.forEach((k, v) -> sb.append(sb.length() > 1 ? "," : "").append('"').append(k).append("\":\"").append(v).append('"'));
        return sb.append('}').toString();
    }

    @Test
    @DisplayName("Flyway applied both migrations")
    void flyway_appliedMigrations(@Autowired org.flywaydb.core.Flyway flyway) {
        assertThat(flyway.info().applied()).extracting(i -> i.getVersion().getVersion())
                .containsExactly("1", "2");
    }
}
