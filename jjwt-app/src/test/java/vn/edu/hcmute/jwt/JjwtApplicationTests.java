package vn.edu.hcmute.jwt;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.jwt.secret=test-only-secret-key-with-at-least-32-bytes",
        "spring.datasource.url=jdbc:h2:mem:jjwt;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.admin.email=admin@example.com",
        "app.admin.password=Admin123!",
        "app.admin.full-name=Test Administrator"
})
@AutoConfigureMockMvc
class JjwtApplicationTests {
    private static final String SECRET = "test-only-secret-key-with-at-least-32-bytes";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void completeAuthenticationAndAuthorizationFlow() throws Exception {
        String signup = """
                {"email":"student@example.com","password":"Student123!","fullName":"Student Test"}
                """;
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signup))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        String userToken = login("student@example.com", "Student123!");
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Student Test"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        String adminToken = login("admin@example.com", "Admin123!");
        mockMvc.perform(get("/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void rejectsMissingMalformedExpiredAndWrongCredentials() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        String expired = Jwts.builder()
                .issuer("bt10-jjwt")
                .subject("admin@example.com")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void servesBothAjaxPages() throws Exception {
        mockMvc.perform(get("/login.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("login-form")))
                .andExpect(content().string(containsString("/app.js")));
        mockMvc.perform(get("/profile.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("profile-card")))
                .andExpect(content().string(not(containsString("signup-password"))));
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginPayload(email, password)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.get("token").asText();
    }

    private record LoginPayload(String email, String password) {}
}
