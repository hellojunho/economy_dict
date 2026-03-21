package com.economydict.service;

import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ErrorLogService {
    private static final Logger log = LoggerFactory.getLogger(ErrorLogService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final Path basePath;
    private final ZoneId zoneId = ZoneId.systemDefault();

    public ErrorLogService(@Value("${app.error-log.storage-path:${LOG_PATH:backend/logs}}") String basePath) {
        this.basePath = Path.of(basePath);
    }

    public String writeRequestError(String traceId,
                                    String code,
                                    String message,
                                    List<String> details,
                                    HttpServletRequest request,
                                    Throwable throwable) {
        String userId = resolveUserId(request == null ? null : request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName());
        String requestPath = request == null ? "-" : request.getRequestURI();
        String query = request == null ? null : request.getQueryString();
        String method = request == null ? "-" : request.getMethod();
        String body = buildContent(
                traceId,
                code,
                message,
                details,
                userId,
                method,
                requestPath,
                query,
                throwable == null ? null : stackTrace(throwable)
        );
        return writeLogFile(userId, body);
    }

    public String writeBackgroundError(String traceId,
                                       String code,
                                       String message,
                                       List<String> details,
                                       String userId,
                                       String stackTrace) {
        String body = buildContent(
                traceId,
                code,
                message,
                details,
                resolveUserId(userId),
                "ASYNC",
                "-",
                null,
                stackTrace
        );
        return writeLogFile(resolveUserId(userId), body);
    }

    private String writeLogFile(String userId, String content) {
        Instant now = Instant.now();
        String dateFolder = DATE_FORMAT.format(now.atZone(zoneId));
        String timestamp = FILE_TIMESTAMP_FORMAT.format(now.atZone(zoneId));
        String safeUserId = sanitizeUserId(userId);
        String fileName = timestamp + "_" + safeUserId + ".log";
        Path directory = basePath.resolve(dateFolder);
        Path filePath = directory.resolve(fileName);

        try {
            Files.createDirectories(directory);
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            return "logs/" + dateFolder + "/" + fileName;
        } catch (Exception ex) {
            log.error("Failed to write structured error log file: {}", filePath, ex);
            return null;
        }
    }

    private String buildContent(String traceId,
                                String code,
                                String message,
                                List<String> details,
                                String userId,
                                String method,
                                String path,
                                String query,
                                String stackTrace) {
        StringBuilder builder = new StringBuilder();
        builder.append("timestamp: ").append(Instant.now()).append('\n');
        builder.append("traceId: ").append(traceId).append('\n');
        builder.append("userId: ").append(resolveUserId(userId)).append('\n');
        builder.append("code: ").append(code).append('\n');
        builder.append("message: ").append(message).append('\n');
        builder.append("method: ").append(method).append('\n');
        builder.append("path: ").append(path).append('\n');
        if (query != null && !query.isBlank()) {
            builder.append("query: ").append(query).append('\n');
        }
        if (details != null && !details.isEmpty()) {
            builder.append("details:").append('\n');
            for (String detail : details) {
                builder.append("- ").append(detail).append('\n');
            }
        }
        builder.append('\n');
        builder.append("stackTrace:").append('\n');
        builder.append(stackTrace == null || stackTrace.isBlank() ? "N/A" : stackTrace);
        if (builder.charAt(builder.length() - 1) != '\n') {
            builder.append('\n');
        }
        return builder.toString();
    }

    private String sanitizeUserId(String userId) {
        return resolveUserId(userId).replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String resolveUserId(String userId) {
        if (userId == null || userId.isBlank() || "anonymousUser".equalsIgnoreCase(userId)) {
            return "anonymous";
        }
        return userId;
    }

    private String stackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
