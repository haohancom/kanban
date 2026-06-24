package com.example.kanban.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthHttpSessionTest {
    private static final Path DATABASE_PATH = createDatabasePath();

    @LocalServerPort
    int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath());
    }

    @Test
    void sessionCookieAuthenticatesRepeatedRequests() {
        ResponseEntity<String> login = rest.postForEntity(
                url("/api/auth/login"),
                new HttpEntity<Map<String, String>>(loginBody(), jsonHeaders()),
                String.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String sessionCookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(sessionCookie).contains("JSESSIONID");

        HttpEntity<Void> authenticatedRequest = new HttpEntity<Void>(cookieHeaders(sessionCookie));

        ResponseEntity<String> first = rest.exchange(
                url("/api/auth/me"),
                HttpMethod.GET,
                authenticatedRequest,
                String.class);
        ResponseEntity<String> second = rest.exchange(
                url("/api/auth/me"),
                HttpMethod.GET,
                authenticatedRequest,
                String.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private Map<String, String> loginBody() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "admin123");
        return body;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders cookieHeaders(String setCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, setCookie.split(";", 2)[0]);
        return headers;
    }

    private static Path createDatabasePath() {
        try {
            Path directory = Files.createTempDirectory("kanban-auth-http-");
            return directory.resolve("auth-http-session.sqlite3");
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
