package com.economydict.service;

import com.economydict.dto.StockSymbolSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class TradingViewSymbolService {
    private static final String DIRECT_VIEW_ONLY_MESSAGE =
            "이 심볼은 TradingView 임베드보다 TradingView 웹사이트에서 직접 조회해야 할 수 있습니다.";

    private static final List<StockSymbolSearchResponse> FALLBACK_SYMBOLS = List.of(
            fallback("NASDAQ:TSLA", "Tesla, Inc.", "NASDAQ", "stock", "US", false),
            fallback("NASDAQ:AAPL", "Apple Inc.", "NASDAQ", "stock", "US", false),
            fallback("NASDAQ:NVDA", "NVIDIA Corporation", "NASDAQ", "stock", "US", false),
            fallback("NASDAQ:MSFT", "Microsoft Corporation", "NASDAQ", "stock", "US", false),
            fallback("NASDAQ:AMZN", "Amazon.com, Inc.", "NASDAQ", "stock", "US", false),
            fallback("NASDAQ:GOOGL", "Alphabet Inc.", "NASDAQ", "stock", "US", false),
            fallback("NASDAQ:META", "Meta Platforms, Inc.", "NASDAQ", "stock", "US", false),
            fallback("NYSE:PLTR", "Palantir Technologies Inc.", "NYSE", "stock", "US", false),
            fallback("NYSE:BRK.B", "Berkshire Hathaway Inc.", "NYSE", "stock", "US", false),
            fallback("KRX:005930", "Samsung Electronics Co., Ltd.", "KRX", "stock", "KR", true),
            fallback("KRX:000660", "SK hynix, Inc.", "KRX", "stock", "KR", true),
            fallback("KRX:035420", "NAVER Corporation", "KRX", "stock", "KR", true),
            fallback("KRX:051910", "LG Chem, Ltd.", "KRX", "stock", "KR", true),
            fallback("KRX:005380", "Hyundai Motor Company", "KRX", "stock", "KR", true),
            fallback("KRX:035720", "Kakao Corp.", "KRX", "stock", "KR", true),
            fallback("KRX:207940", "Samsung Biologics Co., Ltd.", "KRX", "stock", "KR", true),
            fallback("KRX:000270", "Kia Corporation", "KRX", "stock", "KR", true),
            fallback("KRX:068270", "Celltrion, Inc.", "KRX", "stock", "KR", true),
            fallback("KRX:005490", "POSCO Holdings Inc.", "KRX", "stock", "KR", true),
            fallback("KRX:105560", "KB Financial Group Inc.", "KRX", "stock", "KR", true),
            fallback("KRX:012450", "Hanwha Aerospace Co., Ltd.", "KRX", "stock", "KR", true)
    );

    private final WebClient webClient;

    public TradingViewSymbolService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://symbol-search.tradingview.com")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                .defaultHeader(HttpHeaders.REFERER, "https://www.tradingview.com/")
                .defaultHeader(HttpHeaders.ORIGIN, "https://www.tradingview.com")
                .build();
    }

    public List<StockSymbolSearchResponse> search(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isBlank()) {
            return FALLBACK_SYMBOLS;
        }

        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/symbol_search/")
                            .queryParam("text", query)
                            .queryParam("hl", "1")
                            .queryParam("exchange", "")
                            .queryParam("lang", "en")
                            .queryParam("type", "")
                            .queryParam("domain", "production")
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.isArray()) {
                return filterFallback(query);
            }

            return toResponses(response);
        } catch (WebClientResponseException ex) {
            return filterFallback(query);
        } catch (Exception ex) {
            return filterFallback(query);
        }
    }

    private List<StockSymbolSearchResponse> toResponses(JsonNode response) {
        return java.util.stream.StreamSupport.stream(response.spliterator(), false)
                .map(this::toResponse)
                .filter(item -> item.getSymbol() != null && !item.getSymbol().isBlank())
                .limit(12)
                .toList();
    }

    private StockSymbolSearchResponse toResponse(JsonNode node) {
        String symbolText = stripHtml(node.path("symbol").asText(""));
        String prefix = node.path("prefix").asText("");
        String exchange = node.path("exchange").asText("");
        String country = node.path("country").asText("");
        String symbol = toTradingViewSymbol(prefix, exchange, symbolText);
        boolean directViewOnly = isDirectViewOnly(symbol, exchange, country);

        StockSymbolSearchResponse response = new StockSymbolSearchResponse();
        response.setSymbol(symbol);
        response.setDescription(node.path("description").asText(symbolText));
        response.setExchange(exchange);
        response.setType(node.path("type").asText(""));
        response.setCountry(country);
        response.setDirectViewOnly(directViewOnly);
        response.setDirectViewOnlyMessage(directViewOnly ? DIRECT_VIEW_ONLY_MESSAGE : "");
        return response;
    }

    private List<StockSymbolSearchResponse> filterFallback(String query) {
        String normalized = query.toLowerCase(Locale.ROOT);
        return FALLBACK_SYMBOLS.stream()
                .filter(item -> item.getSymbol().toLowerCase(Locale.ROOT).contains(normalized)
                        || item.getDescription().toLowerCase(Locale.ROOT).contains(normalized)
                        || item.getExchange().toLowerCase(Locale.ROOT).contains(normalized))
                .limit(12)
                .toList();
    }

    private String toTradingViewSymbol(String prefix, String exchange, String symbolText) {
        if (symbolText == null || symbolText.isBlank()) {
            return "";
        }
        if (symbolText.contains(":")) {
            return symbolText.toUpperCase(Locale.ROOT);
        }
        String namespace = prefix == null || prefix.isBlank() ? exchange : prefix;
        if (namespace == null || namespace.isBlank()) {
            return symbolText.toUpperCase(Locale.ROOT);
        }
        return (namespace + ":" + symbolText).toUpperCase(Locale.ROOT);
    }

    private boolean isDirectViewOnly(String symbol, String exchange, String country) {
        String upperSymbol = symbol == null ? "" : symbol.toUpperCase(Locale.ROOT);
        String upperExchange = exchange == null ? "" : exchange.toUpperCase(Locale.ROOT);
        String upperCountry = country == null ? "" : country.toUpperCase(Locale.ROOT);
        return upperSymbol.startsWith("KRX:") || "KRX".equals(upperExchange) || "KR".equals(upperCountry);
    }

    private String stripHtml(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("<[^>]+>", "");
    }

    private static StockSymbolSearchResponse fallback(
            String symbol,
            String description,
            String exchange,
            String type,
            String country,
            boolean directViewOnly
    ) {
        StockSymbolSearchResponse response = new StockSymbolSearchResponse();
        response.setSymbol(symbol);
        response.setDescription(description);
        response.setExchange(exchange);
        response.setType(type);
        response.setCountry(country);
        response.setDirectViewOnly(directViewOnly);
        response.setDirectViewOnlyMessage(directViewOnly ? DIRECT_VIEW_ONLY_MESSAGE : "");
        return response;
    }
}
