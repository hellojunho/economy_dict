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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    private static final Logger errorLogger = LoggerFactory.getLogger("com.economydict.error");
    private final ObjectMapper objectMapper;
    private final ErrorLogService errorLogService;

    public RestAccessDeniedHandler(ObjectMapper objectMapper, ErrorLogService errorLogService) {
        this.objectMapper = objectMapper;
        this.errorLogService = errorLogService;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        String traceId = UUID.randomUUID().toString();
        ApiErrorResponse body = new ApiErrorResponse();
        body.setTimestamp(Instant.now());
        body.setStatus(HttpStatus.FORBIDDEN.value());
        body.setError(HttpStatus.FORBIDDEN.getReasonPhrase());
        body.setCode("ACCESS_DENIED");
        body.setMessage("You do not have permission to access this resource.");
        body.setPath(request.getRequestURI());
        body.setTraceId(traceId);
        body.setDetails(List.of("Required authority is missing for this request."));

        String logFile = errorLogService.writeRequestError(
                traceId,
                "ACCESS_DENIED",
                body.getMessage(),
                body.getDetails(),
                request,
                accessDeniedException
        );
        errorLogger.error(
                "traceId={} status={} code={} path={} message={} details={} logFile={}",
                traceId,
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                request.getRequestURI(),
                body.getMessage(),
                body.getDetails(),
                logFile,
                accessDeniedException
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
