package com.tinasheGomo.EventManagementSystem.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinasheGomo.EventManagementSystem.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Answers authenticated-but-forbidden requests with the same JSON error shape the
 * {@code GlobalExceptionHandler} uses, keeping 403 responses consistent for the SPA.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String message = accessDeniedException.getMessage() != null
                ? accessDeniedException.getMessage() : "Access denied";
        ErrorResponse body = new ErrorResponse(message, request.getRequestURI(), "FORBIDDEN");
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
