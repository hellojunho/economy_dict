package com.economydict.service;

import com.economydict.dto.KrStockCandleResponse;
import com.economydict.dto.KrStockMetricResponse;
import com.economydict.dto.KrStockOrderBookLevelResponse;
import com.economydict.dto.KrStockOrderBookResponse;
import com.economydict.dto.KrStockQuoteResponse;
import com.economydict.dto.KrStockRealtimeResponse;
import com.economydict.dto.KrStockSectionResponse;
import com.economydict.dto.KrStockSnapshotResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class KiwoomKrStockService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter KIWOOM_EXPIRES = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String PROVIDER = "Kiwoom REST API";

    private final WebClient webClient;
    private final String appKey;
    private final String secretKey;
    private final String exchange;
    private final int liveRefreshIntervalSeconds;
    private volatile String accessToken;
    private volatile Instant accessTokenExpiresAt = Instant.EPOCH;

    public KiwoomKrStockService(
            @Value("${stock.kiwoom.base-url}") String baseUrl,
            @Value("${stock.kiwoom.app-key}") String appKey,
            @Value("${stock.kiwoom.secret-key}") String secretKey,
            @Value("${stock.kiwoom.exchange}") String exchange,
            @Value("${stock.kiwoom.live-refresh-seconds}") int liveRefreshIntervalSeconds
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.appKey = appKey;
        this.secretKey = secretKey;
        this.exchange = exchange;
        this.liveRefreshIntervalSeconds = liveRefreshIntervalSeconds;
    }

    public KrStockSnapshotResponse getSnapshot(String rawSymbol) {
        ensureConfigured();

        String symbol = normalizeSymbol(rawSymbol);
        String brokerSymbol = toBrokerSymbol(symbol);

        JsonNode quoteNode = post("/api/dostk/mrkcond", "ka10006", Map.of("stk_cd", brokerSymbol));
        JsonNode orderBookNode = post("/api/dostk/mrkcond", "ka10004", Map.of("stk_cd", brokerSymbol));
        JsonNode intradayNode = post("/api/dostk/chart", "ka10080", Map.of(
                "stk_cd", brokerSymbol,
                "tic_scope", "1",
                "upd_stkpc_tp", "1"
        ));
        JsonNode dailyNode = post("/api/dostk/chart", "ka10081", Map.of(
                "stk_cd", brokerSymbol,
                "base_dt", LocalDate.now(KST).format(BASIC_DATE),
                "upd_stkpc_tp", "1"
        ));

        Map<String, String> quoteMap = toFlatMap(quoteNode);
        Map<String, String> orderMap = toFlatMap(orderBookNode);
        List<KrStockCandleResponse> intradayCandles = toCandles(extractObjectArray(intradayNode, "stk_min_pole_chart_qry"), true);
        List<KrStockCandleResponse> dailyCandles = toCandles(extractObjectArray(dailyNode, "stk_dt_pole_chart_qry"), false);
        ensureChartData(symbol, "intraday", intradayCandles, intradayNode);
        ensureChartData(symbol, "daily", dailyCandles, dailyNode);
        KrStockQuoteResponse quote = toQuote(symbol, quoteMap, intradayCandles);
        KrStockOrderBookResponse orderBook = toOrderBook(orderMap);

        KrStockSnapshotResponse response = new KrStockSnapshotResponse();
        response.setProvider(PROVIDER);
        response.setMarket(exchange);
        response.setSymbol(symbol);
        response.setName(quote.getName());
        response.setFetchedAt(Instant.now());
        response.setLiveRefreshIntervalSeconds(liveRefreshIntervalSeconds);
        response.setQuote(quote);
        response.setOrderBook(orderBook);
        response.setIntradayCandles(intradayCandles);
        response.setDailyCandles(dailyCandles);
        response.setSections(List.of(
                new KrStockSectionResponse("Quote Feed", toMetrics(quoteMap)),
                new KrStockSectionResponse("Order Book Feed", toMetrics(orderMap))
        ));
        return response;
    }

    public KrStockRealtimeResponse getRealtime(String rawSymbol) {
        ensureConfigured();

        String symbol = normalizeSymbol(rawSymbol);
        String brokerSymbol = toBrokerSymbol(symbol);
        JsonNode quoteNode = post("/api/dostk/mrkcond", "ka10006", Map.of("stk_cd", brokerSymbol));
        JsonNode orderBookNode = post("/api/dostk/mrkcond", "ka10004", Map.of("stk_cd", brokerSymbol));
        Map<String, String> quoteMap = toFlatMap(quoteNode);
        Map<String, String> orderMap = toFlatMap(orderBookNode);
        ensureRealtimeData(symbol, quoteMap, quoteNode);

        KrStockRealtimeResponse response = new KrStockRealtimeResponse();
        response.setSymbol(symbol);
        response.setFetchedAt(Instant.now());
        response.setQuote(toQuote(symbol, quoteMap, Collections.emptyList()));
        response.setOrderBook(toOrderBook(orderMap));
        return response;
    }

    public int getLiveRefreshIntervalSeconds() {
        return liveRefreshIntervalSeconds;
    }

    private void ensureConfigured() {
        if (isUnset(appKey) || isUnset(secretKey)) {
            throw new IllegalStateException("Kiwoom API credentials are not configured. Set STOCK_KIWOOM_APP_KEY and STOCK_KIWOOM_SECRET_KEY.");
        }
    }

    private boolean isUnset(String value) {
        return value == null || value.isBlank() || "REPLACE_ME".equals(value);
    }

    private String normalizeSymbol(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            throw new IllegalArgumentException("A domestic stock symbol is required.");
        }
        String normalized = rawSymbol.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains(":")) {
            normalized = normalized.substring(normalized.indexOf(':') + 1);
        }
        normalized = normalized.replaceAll("[^0-9]", "");
        if (normalized.length() < 6) {
            normalized = String.format("%6s", normalized).replace(' ', '0');
        }
        if (normalized.length() != 6) {
            throw new IllegalArgumentException("Symbol must be a 6-digit KRX code.");
        }
        return normalized;
    }

    private String toBrokerSymbol(String symbol) {
        return symbol;
    }

    private synchronized String accessToken() {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpiresAt.minusSeconds(60))) {
            return accessToken;
        }

        JsonNode response = webClient.post()
                .uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "grant_type", "client_credentials",
                        "appkey", appKey,
                        "secretkey", secretKey
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || response.path("token").isMissingNode()) {
            throw new IllegalStateException("Kiwoom token issuance failed.");
        }

        accessToken = response.path("token").asText();
        accessTokenExpiresAt = parseExpiry(response.path("expires_dt").asText(""));
        return accessToken;
    }

    private Instant parseExpiry(String expiresAt) {
        if (expiresAt == null || expiresAt.isBlank()) {
            return Instant.now().plusSeconds(60L * 60L * 12L);
        }
        try {
            return LocalDateTime.parse(expiresAt, KIWOOM_EXPIRES).atZone(KST).toInstant();
        } catch (DateTimeParseException ignored) {
            return Instant.now().plusSeconds(60L * 60L * 12L);
        }
    }

    private JsonNode post(String path, String apiId, Map<String, String> body) {
        try {
            JsonNode response = webClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                    .header("api-id", apiId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                throw new IllegalStateException("Kiwoom API returned an empty response.");
            }
            return response;
        } catch (WebClientResponseException ex) {
            String detail = ex.getResponseBodyAsString();
            throw new IllegalStateException("Kiwoom API error: " + (detail == null || detail.isBlank() ? ex.getMessage() : detail));
        }
    }

    private KrStockQuoteResponse toQuote(String symbol, Map<String, String> source, List<KrStockCandleResponse> intradayCandles) {
        KrStockQuoteResponse quote = new KrStockQuoteResponse();
        KrStockCandleResponse latestCandle = intradayCandles.isEmpty() ? null : intradayCandles.get(intradayCandles.size() - 1);
        double latestPrice = absDouble(source, "cur_prc", "close_pric", "cur_price");
        if (latestPrice == 0D && latestCandle != null) {
            latestPrice = latestCandle.getClose();
        }

        double open = absDouble(source, "open_pric", "start_pric");
        double high = absDouble(source, "high_pric", "high");
        double low = absDouble(source, "low_pric", "low");
        long volume = absLong(source, "trde_qty", "acc_trde_qty");
        long tradeValue = absLong(source, "acc_trde_prica", "trde_prica");
        double change = signedDouble(source, "pred_pre", "pre");
        Double previousClose = latestPrice > 0D ? latestPrice - change : null;

        if (open == 0D && !intradayCandles.isEmpty()) {
            open = intradayCandles.get(0).getOpen();
        }
        if (high == 0D && !intradayCandles.isEmpty()) {
            high = intradayCandles.stream().mapToDouble(KrStockCandleResponse::getHigh).max().orElse(0D);
        }
        if (low == 0D && !intradayCandles.isEmpty()) {
            low = intradayCandles.stream().mapToDouble(KrStockCandleResponse::getLow).min().orElse(0D);
        }
        if (volume == 0L && !intradayCandles.isEmpty()) {
            volume = intradayCandles.get(intradayCandles.size() - 1).getVolume();
        }

        quote.setSymbol(symbol);
        quote.setName(firstNonBlankValue(firstNonBlank(source, "stk_nm", "stk_name", "name"), symbol));
        quote.setLastPrice(latestPrice > 0D ? latestPrice : null);
        quote.setChange(change);
        quote.setChangeRate(percent(source, latestPrice, change));
        quote.setOpen(open > 0D ? open : null);
        quote.setHigh(high > 0D ? high : null);
        quote.setLow(low > 0D ? low : null);
        quote.setPreviousClose(previousClose);
        quote.setVolume(volume > 0L ? volume : null);
        quote.setTradeValue(tradeValue > 0L ? tradeValue : null);
        quote.setTradeTimeLabel(formatTradeTime(firstNonBlank(source, "tm", "trde_tm", "bid_req_base_tm", "cntr_tm")));
        return quote;
    }

    private Double percent(Map<String, String> source, double latestPrice, double change) {
        Double explicit = optionalDouble(source, "flu_rt", "pred_rt", "chg_rt");
        if (explicit != null) {
            return explicit;
        }
        double previousClose = latestPrice - change;
        if (latestPrice == 0D || previousClose == 0D) {
            return null;
        }
        return change * 100D / previousClose;
    }

    private KrStockOrderBookResponse toOrderBook(Map<String, String> source) {
        KrStockOrderBookResponse orderBook = new KrStockOrderBookResponse();
        orderBook.setAsks(buildAskLevels(source));
        orderBook.setBids(buildBidLevels(source));
        orderBook.setTotalAskQuantity(optionalLong(source, "tot_sel_req"));
        orderBook.setTotalBidQuantity(optionalLong(source, "tot_buy_req"));
        orderBook.setBaseTime(formatTradeTime(firstNonBlank(source, "bid_req_base_tm")));
        return orderBook;
    }

    private List<KrStockOrderBookLevelResponse> buildAskLevels(Map<String, String> source) {
        List<KrStockOrderBookLevelResponse> levels = new ArrayList<>();
        for (int index = 10; index >= 1; index--) {
            Double price = optionalDouble(source, askPriceKeys(index));
            Long quantity = optionalLong(source, askQuantityKeys(index));
            if (price == null && quantity == null) {
                continue;
            }
            levels.add(new KrStockOrderBookLevelResponse(price == null ? 0D : Math.abs(price), quantity == null ? 0L : Math.abs(quantity)));
        }
        return levels;
    }

    private List<KrStockOrderBookLevelResponse> buildBidLevels(Map<String, String> source) {
        List<KrStockOrderBookLevelResponse> levels = new ArrayList<>();
        for (int index = 1; index <= 10; index++) {
            Double price = optionalDouble(source, bidPriceKeys(index));
            Long quantity = optionalLong(source, bidQuantityKeys(index));
            if (price == null && quantity == null) {
                continue;
            }
            levels.add(new KrStockOrderBookLevelResponse(price == null ? 0D : Math.abs(price), quantity == null ? 0L : Math.abs(quantity)));
        }
        return levels;
    }

    private String[] askPriceKeys(int index) {
        if (index == 1) {
            return new String[]{"sel_fpr_bid", "sel_1th_pre_bid", "sel_1st_pre_bid"};
        }
        return new String[]{"sel_" + index + "th_pre_bid", "sel_" + ordinal(index) + "_pre_bid"};
    }

    private String[] askQuantityKeys(int index) {
        if (index == 1) {
            return new String[]{"sel_fpr_req", "sel_1th_pre_req", "sel_1st_pre_req"};
        }
        return new String[]{"sel_" + index + "th_pre_req", "sel_" + ordinal(index) + "_pre_req"};
    }

    private String[] bidPriceKeys(int index) {
        if (index == 1) {
            return new String[]{"buy_fpr_bid", "buy_1th_pre_bid", "buy_1st_pre_bid"};
        }
        return new String[]{"buy_" + index + "th_pre_bid", "buy_" + ordinal(index) + "_pre_bid"};
    }

    private String[] bidQuantityKeys(int index) {
        if (index == 1) {
            return new String[]{"buy_fpr_req", "buy_1th_pre_req", "buy_1st_pre_req"};
        }
        return new String[]{"buy_" + index + "th_pre_req", "buy_" + ordinal(index) + "_pre_req"};
    }

    private String ordinal(int value) {
        return switch (value) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> value + "th";
        };
    }

    private List<KrStockCandleResponse> toCandles(List<Map<String, String>> rows, boolean intraday) {
        List<KrStockCandleResponse> candles = new ArrayList<>();
        for (Map<String, String> row : rows) {
            long epoch = intraday ? parseDateTime(row) : parseDate(row);
            if (epoch <= 0L) {
                continue;
            }

            double close = absDouble(row, "cur_prc", "close_pric", "close");
            double open = absDouble(row, "open_pric", "start_pric", "open");
            double high = absDouble(row, "high_pric", "high");
            double low = absDouble(row, "low_pric", "low");
            long volume = absLong(row, "trde_qty", "acc_trde_qty", "volume");

            if (close == 0D) {
                continue;
            }

            KrStockCandleResponse candle = new KrStockCandleResponse();
            candle.setTime(epoch);
            candle.setOpen(open == 0D ? close : open);
            candle.setHigh(high == 0D ? Math.max(close, candle.getOpen()) : high);
            candle.setLow(low == 0D ? Math.min(close, candle.getOpen()) : low);
            candle.setClose(close);
            candle.setVolume(volume);
            candles.add(candle);
        }

        candles.sort(Comparator.comparingLong(KrStockCandleResponse::getTime));
        return candles;
    }

    private long parseDate(Map<String, String> row) {
        String raw = firstNonBlank(row, "dt", "date", "base_dt", "trde_dt");
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        String normalized = raw.replaceAll("[^0-9]", "");
        if (normalized.length() < 8) {
            return 0L;
        }
        try {
            LocalDate date = LocalDate.parse(normalized.substring(0, 8), BASIC_DATE);
            return date.atStartOfDay(KST).toEpochSecond();
        } catch (DateTimeParseException ex) {
            return 0L;
        }
    }

    private long parseDateTime(Map<String, String> row) {
        String raw = firstNonBlank(row, "dt", "time", "tm", "cntr_tm", "trde_tm");
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        String normalized = raw.replaceAll("[^0-9]", "");
        try {
            if (normalized.length() >= 14) {
                return LocalDateTime.of(
                        LocalDate.parse(normalized.substring(0, 8), BASIC_DATE),
                        LocalTime.of(
                                Integer.parseInt(normalized.substring(8, 10)),
                                Integer.parseInt(normalized.substring(10, 12)),
                                Integer.parseInt(normalized.substring(12, 14))
                        )
                ).atZone(KST).toEpochSecond();
            }
            if (normalized.length() >= 12) {
                return LocalDateTime.of(
                        LocalDate.parse(normalized.substring(0, 8), BASIC_DATE),
                        LocalTime.of(
                                Integer.parseInt(normalized.substring(8, 10)),
                                Integer.parseInt(normalized.substring(10, 12))
                        )
                ).atZone(KST).toEpochSecond();
            }
            if (normalized.length() == 6) {
                return ZonedDateTime.of(LocalDate.now(KST), LocalTime.of(
                        Integer.parseInt(normalized.substring(0, 2)),
                        Integer.parseInt(normalized.substring(2, 4)),
                        Integer.parseInt(normalized.substring(4, 6))
                ), KST).toEpochSecond();
            }
            if (normalized.length() == 4) {
                return ZonedDateTime.of(LocalDate.now(KST), LocalTime.of(
                        Integer.parseInt(normalized.substring(0, 2)),
                        Integer.parseInt(normalized.substring(2, 4))
                ), KST).toEpochSecond();
            }
        } catch (RuntimeException ignored) {
            return 0L;
        }
        return 0L;
    }

    private void ensureChartData(String symbol, String seriesType, List<KrStockCandleResponse> candles, JsonNode rawNode) {
        if (!candles.isEmpty()) {
            return;
        }
        throw new IllegalStateException(
                "Kiwoom returned an empty " + seriesType + " payload for symbol " + symbol
                        + ". The request succeeded but contained no usable chart rows. "
                        + "Check the symbol format, allowed IP registration, and Kiwoom market-data permissions. "
                        + "return_code=" + rawNode.path("return_code").asText("")
                        + ", return_msg=" + rawNode.path("return_msg").asText("")
        );
    }

    private void ensureRealtimeData(String symbol, Map<String, String> quoteMap, JsonNode rawNode) {
        if (hasAnyValue(quoteMap, "cur_prc", "close_pric", "open_pric", "trde_qty")) {
            return;
        }
        throw new IllegalStateException(
                "Kiwoom returned an empty realtime payload for symbol " + symbol
                        + ". The request succeeded but the quote fields were blank. "
                        + "Check the symbol format, allowed IP registration, and Kiwoom market-data permissions. "
                        + "return_code=" + rawNode.path("return_code").asText("")
                        + ", return_msg=" + rawNode.path("return_msg").asText("")
        );
    }

    private boolean hasAnyValue(Map<String, String> source, String... keys) {
        for (String key : keys) {
            String value = source.get(key);
            if (value != null && !value.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private List<Map<String, String>> extractObjectArray(JsonNode node, String preferredField) {
        JsonNode preferred = node.path(preferredField);
        if (preferred.isArray()) {
            List<Map<String, String>> mapped = new ArrayList<>();
            for (JsonNode child : preferred) {
                if (child.isObject()) {
                    mapped.add(toFlatMap(child));
                }
            }
            if (!mapped.isEmpty()) {
                return mapped;
            }
        }
        return findLargestObjectArray(node);
    }

    private List<Map<String, String>> findLargestObjectArray(JsonNode node) {
        List<List<Map<String, String>>> candidates = new ArrayList<>();
        collectArrays(node, candidates);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        return candidates.stream().max(Comparator.comparingInt(Collection::size)).orElse(Collections.emptyList());
    }

    private void collectArrays(JsonNode node, List<List<Map<String, String>>> candidates) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            List<Map<String, String>> mapped = new ArrayList<>();
            for (JsonNode child : node) {
                if (child.isObject()) {
                    mapped.add(toFlatMap(child));
                }
            }
            if (!mapped.isEmpty()) {
                candidates.add(mapped);
            }
            for (JsonNode child : node) {
                collectArrays(child, candidates);
            }
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining((entry) -> collectArrays(entry.getValue(), candidates));
        }
    }

    private Map<String, String> toFlatMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        if (node.isObject()) {
            node.fields().forEachRemaining((entry) -> {
                if (entry.getValue().isValueNode()) {
                    values.put(entry.getKey(), entry.getValue().asText(""));
                }
            });
        }
        return values;
    }

    private List<KrStockMetricResponse> toMetrics(Map<String, String> source) {
        List<KrStockMetricResponse> rows = new ArrayList<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            rows.add(new KrStockMetricResponse(entry.getKey(), entry.getValue()));
        }
        return rows;
    }

    private String firstNonBlank(Map<String, String> source, String... keys) {
        for (String key : keys) {
            String value = source.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlankValue(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Double optionalDouble(Map<String, String> source, String... keys) {
        String value = firstNonBlank(source, keys);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private double absDouble(Map<String, String> source, String... keys) {
        Double value = optionalDouble(source, keys);
        return value == null ? 0D : Math.abs(value);
    }

    private double signedDouble(Map<String, String> source, String... keys) {
        Double value = optionalDouble(source, keys);
        return value == null ? 0D : value;
    }

    private Long optionalLong(Map<String, String> source, String... keys) {
        String value = firstNonBlank(source, keys);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.replace(",", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private long absLong(Map<String, String> source, String... keys) {
        Long value = optionalLong(source, keys);
        return value == null ? 0L : Math.abs(value);
    }

    private String formatTradeTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.replaceAll("[^0-9]", "");
        if (normalized.length() < 4) {
            return raw;
        }
        if (normalized.length() == 4) {
            return normalized.substring(0, 2) + ":" + normalized.substring(2, 4);
        }
        if (normalized.length() >= 6) {
            return normalized.substring(0, 2) + ":" + normalized.substring(2, 4) + ":" + normalized.substring(4, 6);
        }
        return raw;
    }
}
