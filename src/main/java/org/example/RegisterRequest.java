package org.example;

public record RegisterRequest(
        String username,
        String email,
        String password,
        String country,
        String city
) {
}