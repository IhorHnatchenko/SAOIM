package org.example;

public record RegisterResponse(
        boolean success,
        String message,
        String saoId
) {
}