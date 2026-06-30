package com.notetaking.notes.controller;

import com.notetaking.notes.NotesServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = NotesServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void loginReturnsToken() {
        Map<String, String> body = Map.of("userId", "11111111-1111-1111-1111-111111111111");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .value(tokenResponse -> {
                    assertThat(tokenResponse.get("token")).isNotNull();
                    assertThat(tokenResponse.get("userId")).isEqualTo("11111111-1111-1111-1111-111111111111");
                });
    }

    @Test
    void loginWithInvalidUserIdReturnsBadRequest() {
        Map<String, String> body = Map.of("userId", "invalid-uuid");

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
