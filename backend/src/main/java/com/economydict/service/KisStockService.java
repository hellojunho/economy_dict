package com.economydict.service;

import com.economydict.dto.StockCandleResponse;
import com.economydict.dto.StockMetricResponse;
import com.economydict.dto.StockOrderBookLevelResponse;
import com.economydict.dto.StockOrderBookResponse;
import com.economydict.dto.StockQuoteResponse;
import com.economydict.dto.StockRealtimeResponse;
import com.economydict.dto.StockSectionResponse;
import com.economydict.dto.StockSnapshotResponse;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class KisStockService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter BASIC_TIME = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter KIS_EXPIRY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PROVIDER = "KIS";

    private final WebClient webClient;
    private final String appKey;
    private final String appSecret;
    private final String marketCode;
    private final String productTypeCode;
    private final int liveRefreshIntervalSeconds;
    private final Map<String, String> labelMap;
    private volatile String accessToken;
    private volatile Instant accessTokenExpiresAt = Instant.EPOCH;

    public KisStockService(
            @Value("${stock.kis.base-url}") String baseUrl,
            @Value("${stock.kis.app-key}") String appKey,
            @Value("${stock.kis.app-secret}") String appSecret,
            @Value("${stock.kis.market-code}") String marketCode,
            @Value("${stock.kis.product-type-code}") String productTypeCode,
            @Value("${stock.kis.live-refresh-seconds}") int liveRefreshIntervalSeconds
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.marketCode = marketCode;
        this.productTypeCode = productTypeCode;
        this.liveRefreshIntervalSeconds = liveRefreshIntervalSeconds;
        this.labelMap = buildLabelMap();
    }

    public StockSnapshotResponse getSnapshot(String rawSymbol) {
        ensureConfigured();

        String symbol = normalizeSymbol(rawSymbol);
        Map<String, String> quoteMap = requestSingleOutput(
                "/uapi/domestic-stock/v1/quotations/inquire-price",
                "FHKST01010100",
                Map.of(
                        "FID_COND_MRKT_DIV_CODE", marketCode,
                        "FID_INPUT_ISCD", symbol
                ),
                "output"
        );
        Map<String, String> stockInfoMap = requestFlexibleOutput(
                "/uapi/domestic-stock/v1/quotations/search-stock-info",
                "CTPF1002R",
                Map.of(
                        "PRDT_TYPE_CD", productTypeCode,
                        "PDNO", symbol
                )
        );
        JsonNode orderBookNode = request(
                "/uapi/domestic-stock/v1/quotations/inquire-asking-price-exp-ccn",
                "FHKST01010200",
                Map.of(
                        "FID_COND_MRKT_DIV_CODE", marketCode,
                        "FID_INPUT_ISCD", symbol
                )
        );
        Map<String, String> orderBookMap = toFlatMap(orderBookNode.path("output1"));
        Map<String, String> expectedMap = firstRow(orderBookNode.path("output2"));

        JsonNode intradayNode = request(
                "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice",
                "FHKST03010200",
                Map.of(
                        "FID_COND_MRKT_DIV_CODE", marketCode,
                        "FID_INPUT_ISCD", symbol,
                        "FID_INPUT_HOUR_1", "235959",
                        "FID_PW_DATA_INCU_YN", "Y",
                        "FID_ETC_CLS_CODE", ""
                )
        );
        List<StockCandleResponse> intradayCandles = toIntradayCandles(
                toRows(intradayNode.path("output2")),
                text(intradayNode.path("output1"), "stck_bsop_date")
        );

        LocalDate today = LocalDate.now(KST);
        JsonNode dailyNode = request(
                "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice",
                "FHKST03010100",
                Map.of(
                        "FID_COND_MRKT_DIV_CODE", marketCode,
                        "FID_INPUT_ISCD", symbol,
                        "FID_INPUT_DATE_1", today.minusDays(180).format(BASIC_DATE),
                        "FID_INPUT_DATE_2", today.format(BASIC_DATE),
                        "FID_PERIOD_DIV_CODE", "D",
                        "FID_ORG_ADJ_PRC", "1"
                )
        );
        List<StockCandleResponse> dailyCandles = toDailyCandles(toRows(dailyNode.path("output2")));

        StockQuoteResponse quote = toQuote(symbol, quoteMap, stockInfoMap);

        StockSnapshotResponse response = new StockSnapshotResponse();
        response.setProvider(PROVIDER);
        response.setSymbol(symbol);
        response.setName(firstNonBlank(quote.getName(), text(stockInfoMap, "prdt_name"), symbol));
        response.setMarket(firstNonBlank(text(stockInfoMap, "prdt_clsf_name"), text(stockInfoMap, "scts_mket_lstg_dt"), marketCode));
        response.setLiveRefreshIntervalSeconds(liveRefreshIntervalSeconds);
        response.setFetchedAt(Instant.now());
        response.setQuote(quote);
        response.setOrderBook(toOrderBook(orderBookMap, expectedMap));
        response.setIntradayCandles(intradayCandles);
        response.setDailyCandles(dailyCandles);
        response.setSections(buildSections(quoteMap, stockInfoMap, orderBookMap, expectedMap, quote));
        return response;
    }

    public StockRealtimeResponse getRealtime(String rawSymbol) {
        ensureConfigured();

        String symbol = normalizeSymbol(rawSymbol);
        Map<String, String> quoteMap = requestSingleOutput(
                "/uapi/domestic-stock/v1/quotations/inquire-price",
                "FHKST01010100",
                Map.of(
                        "FID_COND_MRKT_DIV_CODE", marketCode,
                        "FID_INPUT_ISCD", symbol
                ),
                "output"
        );
        JsonNode orderBookNode = request(
                "/uapi/domestic-stock/v1/quotations/inquire-asking-price-exp-ccn",
                "FHKST01010200",
                Map.of(
                        "FID_COND_MRKT_DIV_CODE", marketCode,
                        "FID_INPUT_ISCD", symbol
                )
        );

        StockRealtimeResponse response = new StockRealtimeResponse();
        response.setSymbol(symbol);
        response.setFetchedAt(Instant.now());
        response.setQuote(toQuote(symbol, quoteMap, Map.of()));
        response.setOrderBook(toOrderBook(
                toFlatMap(orderBookNode.path("output1")),
                firstRow(orderBookNode.path("output2"))
        ));
        return response;
    }

    private void ensureConfigured() {
        if (isUnset(appKey) || isUnset(appSecret)) {
            throw new IllegalStateException("KIS Open API credentials are not configured. Set STOCK_KIS_APP_KEY and STOCK_KIS_APP_SECRET.");
        }
    }

    private boolean isUnset(String value) {
        return value == null || value.isBlank() || "REPLACE_ME".equals(value);
    }

    private String normalizeSymbol(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            throw new IllegalArgumentException("A KRX stock symbol is required.");
        }

        String normalized = rawSymbol.replaceAll("[^0-9A-Za-z]", "").toUpperCase(Locale.ROOT);
        if (normalized.length() < 6) {
            normalized = String.format("%6s", normalized).replace(' ', '0');
        }
        if (normalized.length() != 6 && !(normalized.startsWith("Q") && normalized.length() == 7)) {
            throw new IllegalArgumentException("Symbol must be a 6-digit domestic stock code.");
        }
        return normalized;
    }

    private JsonNode request(String path, String trId, Map<String, String> params) {
        JsonNode root = webClient.get()
                .uri((uriBuilder) -> {
                    uriBuilder.path(path);
                    params.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", trId)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (root == null) {
            throw new IllegalStateException("KIS API returned an empty response.");
        }
        if (!"0".equals(root.path("rt_cd").asText())) {
            throw new IllegalStateException("KIS API error: " + root.path("msg1").asText("Unknown error"));
        }
        return root;
    }

    private Map<String, String> requestSingleOutput(String path, String trId, Map<String, String> params, String outputField) {
        JsonNode root = request(path, trId, params);
        return toFlatMap(root.path(outputField));
    }

    private Map<String, String> requestFlexibleOutput(String path, String trId, Map<String, String> params) {
        JsonNode root = request(path, trId, params);
        JsonNode output = root.path("output");
        if (output.isArray()) {
            return firstRow(output);
        }
        return toFlatMap(output);
    }

    private synchronized String accessToken() {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpiresAt.minusSeconds(60))) {
            return accessToken;
        }

        JsonNode response = webClient.post()
                .uri("/oauth2/tokenP")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "grant_type", "client_credentials",
                        "appkey", appKey,
                        "appsecret", appSecret
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || response.path("access_token").isMissingNode()) {
            throw new IllegalStateException("Failed to obtain a KIS access token.");
        }

        accessToken = response.path("access_token").asText();
        String expiryText = response.path("access_token_token_expired").asText("");
        accessTokenExpiresAt = parseExpiry(expiryText);
        return accessToken;
    }

    private Instant parseExpiry(String expiryText) {
        if (expiryText == null || expiryText.isBlank()) {
            return Instant.now().plusSeconds(60L * 60L * 12L);
        }
        try {
            return LocalDateTime.parse(expiryText, KIS_EXPIRY).atZone(KST).toInstant();
        } catch (DateTimeParseException ignored) {
            return Instant.now().plusSeconds(60L * 60L * 12L);
        }
    }

    private StockQuoteResponse toQuote(String symbol, Map<String, String> quoteMap, Map<String, String> stockInfoMap) {
        StockQuoteResponse quote = new StockQuoteResponse();
        quote.setSymbol(symbol);
        quote.setName(firstNonBlank(text(quoteMap, "hts_kor_isnm"), text(stockInfoMap, "prdt_name"), symbol));
        quote.setLastPrice(doubleValue(quoteMap, "stck_prpr"));
        quote.setChange(doubleValue(quoteMap, "prdy_vrss"));
        quote.setChangeRate(doubleValue(quoteMap, "prdy_ctrt"));
        quote.setOpen(doubleValue(quoteMap, "stck_oprc"));
        quote.setHigh(doubleValue(quoteMap, "stck_hgpr"));
        quote.setLow(doubleValue(quoteMap, "stck_lwpr"));
        quote.setPreviousClose(doubleValue(quoteMap, "stck_sdpr"));
        quote.setVolume(longValue(quoteMap, "acml_vol"));
        quote.setTradeValue(longValue(quoteMap, "acml_tr_pbmn"));
        quote.setMarketCap(longValue(quoteMap, "hts_avls"));
        quote.setHigh52Week(doubleValue(quoteMap, "w52_hgpr"));
        quote.setLow52Week(doubleValue(quoteMap, "w52_lwpr"));
        quote.setPer(doubleValue(quoteMap, "per"));
        quote.setPbr(doubleValue(quoteMap, "pbr"));
        quote.setEps(doubleValue(quoteMap, "eps"));
        quote.setBps(doubleValue(quoteMap, "bps"));
        quote.setListedShares(longValue(quoteMap, "lstn_stcn"));
        quote.setTradeTimestamp(toTradeTimestamp(text(quoteMap, "stck_cntg_hour")));
        quote.setTradeTimeLabel(formatTradeTime(text(quoteMap, "stck_cntg_hour")));
        quote.setRiskLabel(firstNonBlank(
                marketWarning(text(quoteMap, "mrkt_warn_cls_code")),
                marketWarning(text(stockInfoMap, "mrkt_warn_cls_code"))
        ));
        return quote;
    }

    private StockOrderBookResponse toOrderBook(Map<String, String> orderBookMap, Map<String, String> expectedMap) {
        StockOrderBookResponse response = new StockOrderBookResponse();
        response.setAsks(buildOrderLevels(orderBookMap, "askp", "askp_rsqn"));
        response.setBids(buildOrderLevels(orderBookMap, "bidp", "bidp_rsqn"));
        response.setTotalAskQuantity(longValue(orderBookMap, "total_askp_rsqn", "total_ask_rsqn"));
        response.setTotalBidQuantity(longValue(orderBookMap, "total_bidp_rsqn", "total_bid_rsqn"));
        response.setExpectedPrice(optionalDouble(expectedMap, "antc_cnpr"));
        response.setExpectedChange(optionalDouble(expectedMap, "antc_cntg_vrss"));
        response.setExpectedChangeRate(optionalDouble(expectedMap, "antc_cntg_prdy_ctrt"));
        return response;
    }

    private List<StockOrderBookLevelResponse> buildOrderLevels(Map<String, String> source, String pricePrefix, String quantityPrefix) {
        List<StockOrderBookLevelResponse> levels = new ArrayList<>();
        for (int index = 10; index >= 1; index--) {
            Double price = optionalDouble(source, pricePrefix + index);
            Long quantity = optionalLong(source, quantityPrefix + index);
            if (price == null && quantity == null) {
                continue;
            }
            levels.add(new StockOrderBookLevelResponse(
                    price == null ? 0D : price,
                    quantity == null ? 0L : quantity
            ));
        }
        return levels;
    }

    private List<StockCandleResponse> toIntradayCandles(List<Map<String, String>> rows, String businessDate) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<StockCandleResponse> candles = new ArrayList<>();
        for (int index = rows.size() - 1; index >= 0; index--) {
            Map<String, String> row = rows.get(index);
            StockCandleResponse candle = new StockCandleResponse();
            candle.setTime(toEpochSecond(
                    firstNonBlank(text(row, "stck_bsop_date"), businessDate),
                    text(row, "stck_cntg_hour")
            ));
            candle.setOpen(doubleValue(row, "stck_oprc", "stck_prpr"));
            candle.setHigh(doubleValue(row, "stck_hgpr", "stck_prpr"));
            candle.setLow(doubleValue(row, "stck_lwpr", "stck_prpr"));
            candle.setClose(doubleValue(row, "stck_prpr", "stck_clpr"));
            candle.setVolume(longValue(row, "cntg_vol", "acml_vol"));
            candles.add(candle);
        }
        return candles;
    }

    private List<StockCandleResponse> toDailyCandles(List<Map<String, String>> rows) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<StockCandleResponse> candles = new ArrayList<>();
        for (int index = rows.size() - 1; index >= 0; index--) {
            Map<String, String> row = rows.get(index);
            String date = text(row, "stck_bsop_date");
            if (date == null || date.isBlank()) {
                continue;
            }
            StockCandleResponse candle = new StockCandleResponse();
            candle.setTime(LocalDate.parse(date, BASIC_DATE).atStartOfDay(KST).toEpochSecond());
            candle.setOpen(doubleValue(row, "stck_oprc"));
            candle.setHigh(doubleValue(row, "stck_hgpr"));
            candle.setLow(doubleValue(row, "stck_lwpr"));
            candle.setClose(doubleValue(row, "stck_clpr", "stck_prpr"));
            candle.setVolume(longValue(row, "acml_vol"));
            candles.add(candle);
        }
        return candles;
    }

    private List<StockSectionResponse> buildSections(
            Map<String, String> quoteMap,
            Map<String, String> stockInfoMap,
            Map<String, String> orderBookMap,
            Map<String, String> expectedMap,
            StockQuoteResponse quote
    ) {
        List<StockSectionResponse> sections = new ArrayList<>();
        sections.add(new StockSectionResponse("Overview", List.of(
                new StockMetricResponse("Current Price", formatNumber(quote.getLastPrice())),
                new StockMetricResponse("Day Change", formatSignedNumber(quote.getChange())),
                new StockMetricResponse("Day Change %", formatPercent(quote.getChangeRate())),
                new StockMetricResponse("Market Cap", formatWholeNumber(quote.getMarketCap())),
                new StockMetricResponse("PER", formatNumber(quote.getPer())),
                new StockMetricResponse("PBR", formatNumber(quote.getPbr())),
                new StockMetricResponse("EPS", formatNumber(quote.getEps())),
                new StockMetricResponse("BPS", formatNumber(quote.getBps())),
                new StockMetricResponse("52W High", formatNumber(quote.getHigh52Week())),
                new StockMetricResponse("52W Low", formatNumber(quote.getLow52Week())),
                new StockMetricResponse("Listed Shares", formatWholeNumber(quote.getListedShares())),
                new StockMetricResponse("Risk", firstNonBlank(quote.getRiskLabel(), "-"))
        )));
        sections.add(new StockSectionResponse("Broker Feed", buildRawRows(quoteMap, Set.of("stck_prpr", "prdy_vrss", "prdy_ctrt"))));
        sections.add(new StockSectionResponse("Order Book Feed", buildRawRows(orderBookMap, Set.of())));
        sections.add(new StockSectionResponse("Expected Match Feed", buildRawRows(expectedMap, Set.of())));
        sections.add(new StockSectionResponse("Profile Feed", buildRawRows(stockInfoMap, Set.of())));
        return sections;
    }

    private List<StockMetricResponse> buildRawRows(Map<String, String> source, Set<String> exclude) {
        List<StockMetricResponse> rows = new ArrayList<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (exclude.contains(entry.getKey())) {
                continue;
            }
            String value = entry.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }
            rows.add(new StockMetricResponse(labelMap.getOrDefault(entry.getKey(), entry.getKey()), value));
        }
        return rows;
    }

    private Map<String, String> buildLabelMap() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("hts_kor_isnm", "종목명");
        labels.put("stck_prpr", "현재가");
        labels.put("prdy_vrss", "전일 대비");
        labels.put("prdy_ctrt", "등락률");
        labels.put("stck_oprc", "시가");
        labels.put("stck_hgpr", "고가");
        labels.put("stck_lwpr", "저가");
        labels.put("stck_sdpr", "전일 종가");
        labels.put("acml_vol", "누적 거래량");
        labels.put("acml_tr_pbmn", "누적 거래대금");
        labels.put("w52_hgpr", "52주 최고가");
        labels.put("w52_lwpr", "52주 최저가");
        labels.put("per", "PER");
        labels.put("pbr", "PBR");
        labels.put("eps", "EPS");
        labels.put("bps", "BPS");
        labels.put("lstn_stcn", "상장 주식 수");
        labels.put("hts_avls", "시가총액");
        labels.put("stck_cntg_hour", "체결 시각");
        labels.put("mrkt_warn_cls_code", "시장 경고 구분");
        labels.put("prdt_name", "상품명");
        labels.put("prdt_eng_name", "영문 상품명");
        labels.put("prdt_abrv_name", "약식명");
        labels.put("std_pdno", "표준 종목번호");
        labels.put("scts_mket_lstg_dt", "상장일");
        labels.put("prdt_clsf_name", "상품 분류");
        labels.put("askp1", "매도 1호가");
        labels.put("askp2", "매도 2호가");
        labels.put("askp3", "매도 3호가");
        labels.put("askp4", "매도 4호가");
        labels.put("askp5", "매도 5호가");
        labels.put("askp6", "매도 6호가");
        labels.put("askp7", "매도 7호가");
        labels.put("askp8", "매도 8호가");
        labels.put("askp9", "매도 9호가");
        labels.put("askp10", "매도 10호가");
        labels.put("bidp1", "매수 1호가");
        labels.put("bidp2", "매수 2호가");
        labels.put("bidp3", "매수 3호가");
        labels.put("bidp4", "매수 4호가");
        labels.put("bidp5", "매수 5호가");
        labels.put("bidp6", "매수 6호가");
        labels.put("bidp7", "매수 7호가");
        labels.put("bidp8", "매수 8호가");
        labels.put("bidp9", "매수 9호가");
        labels.put("bidp10", "매수 10호가");
        labels.put("askp_rsqn1", "매도 1잔량");
        labels.put("bidp_rsqn1", "매수 1잔량");
        labels.put("total_askp_rsqn", "총 매도 잔량");
        labels.put("total_bidp_rsqn", "총 매수 잔량");
        labels.put("antc_cnpr", "예상 체결가");
        labels.put("antc_cntg_vrss", "예상 체결 대비");
        labels.put("antc_cntg_prdy_ctrt", "예상 체결 등락률");
        return labels;
    }

    private Map<String, String> toFlatMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Map.of();
        }
        Map<String, String> data = new LinkedHashMap<>();
        node.fields().forEachRemaining((entry) -> data.put(entry.getKey(), entry.getValue().asText("")));
        return data;
    }

    private List<Map<String, String>> toRows(JsonNode node) {
        if (node == null || node.isMissingNode() || !node.isArray()) {
            return Collections.emptyList();
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (JsonNode item : node) {
            rows.add(toFlatMap(item));
        }
        return rows;
    }

    private Map<String, String> firstRow(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return Map.of();
        }
        if (node.isArray() && !node.isEmpty()) {
            return toFlatMap(node.get(0));
        }
        if (node.isObject()) {
            return toFlatMap(node);
        }
        return Map.of();
    }

    private String text(JsonNode node, String key) {
        return text(toFlatMap(node), key);
    }

    private String text(Map<String, String> source, String key) {
        return source.getOrDefault(key, "");
    }

    private double doubleValue(Map<String, String> source, String... keys) {
        Double value = optionalDouble(source, keys);
        return value == null ? 0D : value;
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

    private long longValue(Map<String, String> source, String... keys) {
        Long value = optionalLong(source, keys);
        return value == null ? 0L : value;
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

    private String firstNonBlank(Map<String, String> source, String... keys) {
        for (String key : keys) {
            String value = source.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private long toEpochSecond(String date, String time) {
        if (date == null || date.isBlank()) {
            return Instant.now().getEpochSecond();
        }
        try {
            LocalDate parsedDate = LocalDate.parse(date, BASIC_DATE);
            LocalTime parsedTime = (time == null || time.isBlank())
                    ? LocalTime.MIDNIGHT
                    : LocalTime.parse(padTime(time), BASIC_TIME);
            return ZonedDateTime.of(parsedDate, parsedTime, KST).toEpochSecond();
        } catch (DateTimeParseException ex) {
            return Instant.now().getEpochSecond();
        }
    }

    private Long toTradeTimestamp(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        return toEpochSecond(LocalDate.now(KST).format(BASIC_DATE), time);
    }

    private String formatTradeTime(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        String padded = padTime(time);
        return padded.substring(0, 2) + ":" + padded.substring(2, 4) + ":" + padded.substring(4, 6);
    }

    private String padTime(String time) {
        return String.format("%6s", time).replace(' ', '0');
    }

    private String marketWarning(String code) {
        if (Objects.equals(code, "01")) {
            return "투자주의";
        }
        if (Objects.equals(code, "02")) {
            return "투자경고";
        }
        if (Objects.equals(code, "03")) {
            return "투자위험";
        }
        if (Objects.equals(code, "04")) {
            return "매매정지";
        }
        return null;
    }

    private String formatNumber(Double value) {
        if (value == null) {
            return "-";
        }
        if (Math.floor(value) == value) {
            return String.format(Locale.US, "%,.0f", value);
        }
        return String.format(Locale.US, "%,.2f", value);
    }

    private String formatSignedNumber(Double value) {
        if (value == null) {
            return "-";
        }
        return value > 0
                ? "+" + formatNumber(value)
                : formatNumber(value);
    }

    private String formatPercent(Double value) {
        if (value == null) {
            return "-";
        }
        return String.format(Locale.US, "%+.2f%%", value);
    }

    private String formatWholeNumber(Long value) {
        return value == null ? "-" : String.format(Locale.US, "%,d", value);
    }
}
