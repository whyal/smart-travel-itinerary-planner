package com.yonglun.itineraryassistant.dto;

import java.time.Instant;

/**
 * Standardized error envelope returned by {@link com.yonglun.itineraryassistant.controller.GlobalExceptionHandler}
 * for all unhandled exceptions across every controller.
 *
 * <pre>
 * {
 *   "timestamp": "2026-08-30T15:00:00Z",
 *   "status":    500,
 *   "error":     "Internal Server Error",
 *   "message":   "Unexpected error occurred"
 * }
 * </pre>
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message
) {}
