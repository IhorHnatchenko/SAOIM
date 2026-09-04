package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiClient {

    private static final String BASE_URL =
            "http://127.0.0.1:8080";

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    public static RegisterResponse register(
            String username,
            String email,
            String password,
            String country,
            String city
    ) throws IOException, InterruptedException {

        RegisterRequest registerRequest =
                new RegisterRequest(
                        username,
                        email,
                        password,
                        country,
                        city
                );

        String json =
                OBJECT_MAPPER.writeValueAsString(registerRequest);

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(
                                BASE_URL + "/api/register"
                        ))
                        .timeout(Duration.ofSeconds(10))
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(json)
                        )
                        .build();

        HttpResponse<String> response =
                HTTP_CLIENT.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {

            throw new IOException(
                    "Server returned HTTP "
                            + response.statusCode()
            );
        }

        return OBJECT_MAPPER.readValue(
                response.body(),
                RegisterResponse.class
        );
    }
}