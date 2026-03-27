package com.economydict.config;

import com.economydict.websocket.KrStockRealtimeWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class KrStockWebSocketConfig implements WebSocketConfigurer {
    private final KrStockRealtimeWebSocketHandler krStockRealtimeWebSocketHandler;

    public KrStockWebSocketConfig(KrStockRealtimeWebSocketHandler krStockRealtimeWebSocketHandler) {
        this.krStockRealtimeWebSocketHandler = krStockRealtimeWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(krStockRealtimeWebSocketHandler, "/api/ws/kr-stocks/*")
                .setAllowedOriginPatterns("*");
    }
}
