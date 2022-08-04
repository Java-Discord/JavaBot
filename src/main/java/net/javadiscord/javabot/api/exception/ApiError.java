package net.javadiscord.javabot.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Represents a single API-Error which holds the {@link HttpStatus}, a message and
 * an array of errors.
 *
 * @param status The {@link HttpStatus}.
 * @param message The errors' message.
 * @param errors An array of additional error notices.
 */
public record ApiError(HttpStatus status, String message, String... errors) {}
