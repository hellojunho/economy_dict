package com.economydict.config;

import com.economydict.dto.ApiErrorResponse;
import com.economydict.service.ErrorLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final Logger errorLogger = LoggerFactory.getLogger("com.economydict.error");
    private final ObjectMapper objectMapper;
    private final ErrorLogService errorLogService;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper, ErrorLogService errorLogService) {
        this.objectMapper = objectMapper;
        this.errorLogService = errorLogService;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        String traceId = UUID.randomUUID().toString();
        String code = attribute(request, "auth.error.code", "UNAUTHORIZED");
        String message = attribute(request, "auth.error.message", "Authentication is required to access this resource.");
        List<String> details = List.of(attribute(request, "auth.error.detail", "Provide a valid access token."));

        ApiErrorResponse body = new ApiErrorResponse();
        body.setTimestamp(Instant.now());
        body.setStatus(HttpStatus.UNAUTHORIZED.value());
        body.setError(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        body.setCode(code);
        body.setMessage(message);
        body.setPath(request.getRequestURI());
        body.setTraceId(traceId);
        body.setDetails(details);

        String logFile = errorLogService.writeRequestError(traceId, code, message, details, request, authException);
        errorLogger.error(
                "traceId={} status={} code={} path={} message={} details={} logFile={}",
                traceId,
                HttpStatus.UNAUTHORIZED.value(),
                code,
                request.getRequestURI(),
                message,
                details,
                logFile,
                authException
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private String attribute(HttpServletRequest request, String name, String fallback) {
        Object value = request.getAttribute(name);
        return value == null ? fallback : value.toString();
    }
}
