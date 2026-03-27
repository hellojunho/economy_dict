// 지금 구현은 UI/전달 계층은 websocket이지만, backend 내부 소스는 기존 Kiwoom REST 기반 realtime 조회를 websocket으로 내보내는 구조입니다. 키움의 공식 /api/dostk/websocket를 서버가 직접 구독하는 형태로 완전히 바꾸려면, 계좌 환경에서 실제 사용하는 authorization 전달 방식과 0B/0C 구독 payload 필드명을 계정 문서 기준으로 확정해서 KrStockRealtimeWebSocketHandler.java (line 1) 뒤쪽 소스를 교체하면 됩니다.

package com.economydict.websocket;

import com.economydict.dto.KrStockRealtimeResponse;
import com.economydict.service.KiwoomKrStockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class KrStockRealtimeWebSocketHandler extends TextWebSocketHandler {
    private final KiwoomKrStockService kiwoomKrStockService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public KrStockRealtimeWebSocketHandler(KiwoomKrStockService kiwoomKrStockService, ObjectMapper objectMapper) {
        this.kiwoomKrStockService = kiwoomKrStockService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String symbol = extractSymbol(session.getUri());
        sendUpdate(session, symbol);

        long refreshSeconds = Math.max(2, kiwoomKrStockService.getLiveRefreshIntervalSeconds());
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(() -> {
            if (!session.isOpen()) {
                cancelTask(session.getId());
                return;
            }
            try {
                sendUpdate(session, symbol);
            } catch (Exception exception) {
                try {
                    session.close(CloseStatus.SERVER_ERROR.withReason("KR stock realtime stream failed"));
                } catch (IOException ignored) {
                    // ignore close failure
                }
                cancelTask(session.getId());
            }
        }, refreshSeconds, refreshSeconds, TimeUnit.SECONDS);
        tasks.put(session.getId(), future);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cancelTask(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        cancelTask(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR.withReason("KR stock realtime transport error"));
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void sendUpdate(WebSocketSession session, String symbol) throws Exception {
        KrStockRealtimeResponse response = kiwoomKrStockService.getRealtime(symbol);
        synchronized (session) {
            if (!session.isOpen()) {
                return;
            }
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }

    private String extractSymbol(URI uri) {
        if (uri == null || uri.getPath() == null || uri.getPath().isBlank()) {
            throw new IllegalArgumentException("A domestic stock symbol is required.");
        }
        String path = uri.getPath();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == path.length() - 1) {
            throw new IllegalArgumentException("A domestic stock symbol is required.");
        }
        return path.substring(lastSlash + 1);
    }

    private void cancelTask(String sessionId) {
        ScheduledFuture<?> future = tasks.remove(sessionId);
        if (future != null) {
            future.cancel(true);
        }
    }
}
