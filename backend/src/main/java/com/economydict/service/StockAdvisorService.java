package com.economydict.service;

import com.economydict.dto.StockAdvisorRequest;
import com.economydict.dto.StockAdvisorResponse;
import com.economydict.dto.StockAdvisorSourceResponse;
import com.economydict.dto.KrStockCandleResponse;
import com.economydict.dto.KrStockSnapshotResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StockAdvisorService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final WeekFields ISO_WEEK = WeekFields.ISO;

    private final OpenAiService openAiService;
    private final KiwoomKrStockService kiwoomKrStockService;

    public StockAdvisorService(OpenAiService openAiService, KiwoomKrStockService kiwoomKrStockService) {
        this.openAiService = openAiService;
        this.kiwoomKrStockService = kiwoomKrStockService;
    }

    public StockAdvisorResponse analyze(StockAdvisorRequest request) {
        String symbol = normalizeSymbol(request.getSymbol());
        String riskProfile = normalizeLabel(request.getRiskProfile());
        String tradeStyle = normalizeLabel(request.getTradeStyle());
        OpenAiService.WebSearchResult result = adviseConversation(
                symbol,
                riskProfile,
                tradeStyle,
                null,
                List.of(new OpenAiService.ChatTurn("user", buildInitialAnalysisPrompt(symbol, riskProfile, tradeStyle, null)))
        );

        if (result.getContent() == null || result.getContent().isBlank()) {
            throw new IllegalStateException("주식 분석 응답이 비어 있습니다. 잠시 후 다시 시도하세요.");
        }

        StockAdvisorResponse response = new StockAdvisorResponse();
        response.setSymbol(symbol);
        response.setRiskProfile(riskProfile);
        response.setTradeStyle(tradeStyle);
        response.setGeneratedAt(Instant.now());
        response.setContent(result.getContent());
        response.setSources(toSourceResponses(result.getSources()));
        return response;
    }

    public OpenAiService.WebSearchResult adviseConversation(
            String symbol,
            String riskProfile,
            String tradeStyle,
            String notes,
            List<OpenAiService.ChatTurn> conversation
    ) {
        OpenAiService.WebSearchResult result = openAiService.respondWithWebSearch(
                buildSystemPrompt(),
                buildConversation(symbol, riskProfile, tradeStyle, notes, conversation)
        );
        if (result.getContent() == null || result.getContent().isBlank()) {
            throw new IllegalStateException("주식 분석 응답이 비어 있습니다. 잠시 후 다시 시도하세요.");
        }
        return result;
    }

    private List<StockAdvisorSourceResponse> toSourceResponses(List<OpenAiService.WebSource> sources) {
        if (sources == null) {
            return List.of();
        }

        return sources.stream()
                .map(source -> {
                    StockAdvisorSourceResponse item = new StockAdvisorSourceResponse();
                    item.setTitle(source.getTitle());
                    item.setUrl(source.getUrl());
                    item.setDomain(source.getDomain());
                    return item;
                })
                .toList();
    }

    public List<StockAdvisorSourceResponse> toSourceResponsesFromResult(List<OpenAiService.WebSource> sources) {
        return toSourceResponses(sources);
    }

    public String normalizeSymbol(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("TradingView 심볼을 입력하세요.");
        }

        String normalized = raw.trim().replace(" ", "").toUpperCase();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("TradingView 심볼을 입력하세요.");
        }
        if (!normalized.matches("^[0-9A-Z:_./!\\-]+$")) {
            throw new IllegalArgumentException("TradingView 형식의 심볼만 입력할 수 있습니다. 예: NASDAQ:TSLA");
        }
        return normalized;
    }

    public String normalizeLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("투자 성향을 선택하세요.");
        }
        return raw.trim();
    }

    public String normalizeNotes(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.trim();
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }

    public String buildThreadTitle(String symbol, String tradeStyle) {
        String title = symbol + " · " + tradeStyle + " 전략";
        return title.length() <= 42 ? title : title.substring(0, 42) + "...";
    }

    public String buildInitialAnalysisPrompt(String symbol, String riskProfile, String tradeStyle, String notes) {
        String memoLine = notes == null || notes.isBlank() ? "없음" : notes;
        return """
                아래 설정으로 첫 투자 전략 브리핑을 시작해줘.

                - 종목: %s
                - 투자 성향: %s
                - 매매 스타일: %s
                - 투자 메모: %s

                현재 시점 기준으로 실전형 투자 전략을 정리하고, 이후 대화에서도 이 설정을 유지해줘.
                """.formatted(symbol, riskProfile, tradeStyle, memoLine).trim();
    }

    private String buildSystemPrompt() {
        return """
                You are a senior equity strategist, technical analyst, disclosure analyst, and market sentiment researcher.
                Your job is to analyze one stock-focused advisory session and produce practical Korean responses grounded in web search.

                Non-negotiable rules:
                - Answer in Korean.
                - Use web search results to ground recent disclosures, news, issues, current market context, and price references.
                - Never invent filings, prices, dates, corporate events, or source details.
                - When referring to recent disclosures, news, or price data, include concrete dates whenever available.
                - Separate facts from interpretation.
                - Treat buy pressure, sell pressure, volume profile, support, resistance, trendline, and supply-overhang analysis carefully.
                - Always state the stock's current price, recent 1-year low, and recent 1-year high first. If any item cannot be verified, explicitly say it could not be confirmed.
                - For short-term or swing-style discussions, also cover the recent 2-3 week window and about 1 month window with low, high, midpoint, support, resistance, trendline, and volume-overhang when verifiable.
                - For longer-term discussions, emphasize 52-week positioning, broader support/resistance zones, and the conditions needed for trend continuation or failure.
                - If exact chart, order-flow, or volume-profile data is not available from retrieved sources, explicitly say precision is limited and provide only approximate price zones inferred from public reporting or technical commentary.
                - If the symbol is not a common operating company stock, adapt the analysis and explain the limits.
                - Do not promise profit, certainty, or guaranteed upside.
                - Treat the conversation as one continuous advisory session for the same symbol unless the user explicitly changes it.
                - Do not ask follow-up questions. Deliver the best possible answer with explicit caveats.

                Output format:
                ## 한줄 결론
                - 2~3문장 요약.

                ## 가격 브리핑
                - 현재가, 기준 시점, 최근 1년 최저가/최고가를 bullet로 정리.
                - 단타 또는 스윙이면 최근 2~3주, 최근 1개월 가격 범위와 중간값도 추가.
                - 수치가 불확실하면 무엇을 확인하지 못했는지 명확히 적기.

                ## 종합 판단
                - 현재 강세/중립/약세인지와 그 근거.

                ## 매수세와 매도세
                - 수급/가격 흐름/거래대금/이슈 반응 관점에서 해석.

                ## 가격대 구조
                - 중요 지지선, 저항선, 추세선, 중간값, 돌파/이탈 가격대를 bullet로 정리.
                - 매물대와 공급 부담 구간이 보이면 함께 적기.
                - 정확한 수치 근거가 부족하면 "정밀 차트 데이터 부족"을 먼저 명시.

                ## 투자심리
                - 시장 분위기, 섹터 심리, 대중 관심도, 리스크 선호 변화.

                ## 최근 공시 및 뉴스 이슈
                - bullet list only.
                - each bullet starts with [YYYY-MM-DD] if available.
                - distinguish confirmed fact and likely market interpretation.

                ## 향후 전망
                - 낙관 시나리오 / 기본 시나리오 / 보수 시나리오.

                ## 맞춤 전략 제안
                - 반드시 사용자가 선택한 투자 성향과 매매 스타일을 먼저 반영.
                - buy / hold / partial sell / wait among realistic actions and explain why.
                - entry idea, additional buy condition, invalidation condition, profit-taking idea, and monitoring point를 포함.
                - 단타 / 스윙 / 장기 관점도 짧게 비교.

                ## 리스크 관리
                - position sizing, stop discipline, event risk, and what would invalidate the thesis.

                ## 최종 요약
                - 3 bullet points only.
                - bullet 1: 핵심 판단
                - bullet 2: 주목 가격대
                - bullet 3: 지금 행동 원칙

                End with one short disclaimer line that this is an information support tool, not personalized investment advice.
                """
                .trim();
    }

    private List<OpenAiService.ChatTurn> buildConversation(
            String symbol,
            String riskProfile,
            String tradeStyle,
            String notes,
            List<OpenAiService.ChatTurn> conversation
    ) {
        List<OpenAiService.ChatTurn> turns = new ArrayList<>();
        turns.add(new OpenAiService.ChatTurn("user", buildSessionContext(symbol, riskProfile, tradeStyle, notes)));
        if (conversation != null) {
            turns.addAll(conversation.stream()
                    .filter(item -> item != null && item.getContent() != null && !item.getContent().isBlank())
                    .toList());
        }
        return turns;
    }

    private String buildSessionContext(String symbol, String riskProfile, String tradeStyle, String notes) {
        String noteLine = notes == null || notes.isBlank() ? "없음" : notes;
        String chartContext = buildKrChartContext(symbol, tradeStyle);
        return """
                Advisory session context:
                - TradingView symbol: %s
                - Risk profile: %s
                - Trading style: %s
                - Investor notes: %s

                Use this setup across the whole conversation.
                Update prior analysis when the user asks follow-up questions.
                When discussing price levels, distinguish confirmed figures from approximate zones.
                %s
                """.formatted(symbol, riskProfile, tradeStyle, noteLine, chartContext).trim();
    }

    private String buildKrChartContext(String symbol, String tradeStyle) {
        String krCode = extractKrCode(symbol);
        if (krCode == null) {
            return "";
        }

        try {
            KrStockSnapshotResponse snapshot = kiwoomKrStockService.getSnapshot(krCode);
            List<KrStockCandleResponse> dailyCandles = snapshot.getDailyCandles() == null ? List.of() : snapshot.getDailyCandles();
            List<KrStockCandleResponse> intradayCandles = snapshot.getIntradayCandles() == null ? List.of() : snapshot.getIntradayCandles();
            List<KrStockCandleResponse> selectedCandles = selectCandlesForTradeStyle(tradeStyle, intradayCandles, dailyCandles);

            if (selectedCandles.isEmpty()) {
                return "";
            }

            List<KrStockCandleResponse> oneYearCandles = lastItems(dailyCandles, 252);
            double oneYearLow = oneYearCandles.stream().mapToDouble(KrStockCandleResponse::getLow).min().orElse(0D);
            double oneYearHigh = oneYearCandles.stream().mapToDouble(KrStockCandleResponse::getHigh).max().orElse(0D);
            double windowLow = selectedCandles.stream().mapToDouble(KrStockCandleResponse::getLow).min().orElse(0D);
            double windowHigh = selectedCandles.stream().mapToDouble(KrStockCandleResponse::getHigh).max().orElse(0D);
            double midpoint = (windowLow + windowHigh) / 2D;
            List<KrStockCandleResponse> anchorWindow = lastItems(selectedCandles, Math.min(selectedCandles.size(), "스윙".equals(tradeStyle) ? 12 : 18));
            double support = average(anchorWindow.stream().map(KrStockCandleResponse::getLow).sorted().limit(3).toList());
            double resistance = average(anchorWindow.stream()
                    .map(KrStockCandleResponse::getHigh)
                    .sorted(Comparator.reverseOrder())
                    .limit(3)
                    .toList());

            return """

                Internal KRX chart snapshot context:
                - Snapshot provider: 키움 REST API
                - Preferred timeframe: %s
                - Current price snapshot: %.2f
                - Recent 1-year low/high: %.2f / %.2f
                - Strategy window low/high: %.2f / %.2f
                - Midpoint: %.2f
                - Support anchor: %.2f
                - Resistance anchor: %.2f
                - Use these levels as chart snapshot anchors when you explain support, resistance, box range, and action plans.
                """.formatted(
                    timeframeLabel(tradeStyle),
                    snapshot.getQuote() != null && snapshot.getQuote().getLastPrice() != null
                            ? snapshot.getQuote().getLastPrice()
                            : selectedCandles.get(selectedCandles.size() - 1).getClose(),
                    oneYearLow,
                    oneYearHigh,
                    windowLow,
                    windowHigh,
                    midpoint,
                    support,
                    resistance
            );
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private String extractKrCode(String symbol) {
        String normalized = symbol == null ? "" : symbol.trim().toUpperCase();
        if (normalized.matches("^\\d{6}$")) {
            return normalized;
        }
        if (normalized.matches("^(KRX|KOSPI|KOSDAQ):\\d{6}$")) {
            return normalized.substring(normalized.indexOf(':') + 1);
        }
        return null;
    }

    private List<KrStockCandleResponse> selectCandlesForTradeStyle(
            String tradeStyle,
            List<KrStockCandleResponse> intradayCandles,
            List<KrStockCandleResponse> dailyCandles
    ) {
        String normalized = tradeStyle == null ? "" : tradeStyle.trim();
        if ("초단타".equals(normalized)) {
            return lastItems(intradayCandles, 90);
        }
        if ("단타".equals(normalized)) {
            return lastItems(aggregateIntradayCandles(intradayCandles, 3), 80);
        }
        if ("장투".equals(normalized) || "장기".equals(normalized)) {
            return lastItems(aggregateWeeklyCandles(dailyCandles), 52);
        }
        return lastItems(dailyCandles, 30);
    }

    private String timeframeLabel(String tradeStyle) {
        String normalized = tradeStyle == null ? "" : tradeStyle.trim();
        if ("초단타".equals(normalized)) {
            return "1분봉";
        }
        if ("단타".equals(normalized)) {
            return "3분봉";
        }
        if ("장투".equals(normalized) || "장기".equals(normalized)) {
            return "주봉";
        }
        return "일봉";
    }

    private List<KrStockCandleResponse> aggregateIntradayCandles(List<KrStockCandleResponse> candles, int minuteBucket) {
        List<KrStockCandleResponse> ordered = candles.stream()
                .sorted(Comparator.comparingLong(KrStockCandleResponse::getTime))
                .toList();
        List<KrStockCandleResponse> aggregated = new ArrayList<>();
        KrStockCandleResponse bucket = null;
        long bucketStart = -1L;
        long bucketSize = minuteBucket * 60L;

        for (KrStockCandleResponse candle : ordered) {
            long currentBucket = (candle.getTime() / bucketSize) * bucketSize;
            if (bucket == null || currentBucket != bucketStart) {
                if (bucket != null) {
                    aggregated.add(bucket);
                }
                bucket = new KrStockCandleResponse();
                bucket.setTime(currentBucket);
                bucket.setOpen(candle.getOpen());
                bucket.setHigh(candle.getHigh());
                bucket.setLow(candle.getLow());
                bucket.setClose(candle.getClose());
                bucket.setVolume(candle.getVolume());
                bucketStart = currentBucket;
                continue;
            }

            bucket.setHigh(Math.max(bucket.getHigh(), candle.getHigh()));
            bucket.setLow(Math.min(bucket.getLow(), candle.getLow()));
            bucket.setClose(candle.getClose());
            bucket.setVolume(bucket.getVolume() + candle.getVolume());
        }

        if (bucket != null) {
            aggregated.add(bucket);
        }
        return aggregated;
    }

    private List<KrStockCandleResponse> aggregateWeeklyCandles(List<KrStockCandleResponse> candles) {
        List<KrStockCandleResponse> ordered = candles.stream()
                .sorted(Comparator.comparingLong(KrStockCandleResponse::getTime))
                .toList();
        Map<String, KrStockCandleResponse> aggregated = new LinkedHashMap<>();

        for (KrStockCandleResponse candle : ordered) {
            LocalDate date = Instant.ofEpochSecond(candle.getTime()).atZone(KST).toLocalDate();
            String key = date.get(ISO_WEEK.weekBasedYear()) + "-" + date.get(ISO_WEEK.weekOfWeekBasedYear());
            KrStockCandleResponse bucket = aggregated.get(key);
            if (bucket == null) {
                aggregated.put(key, copyCandle(candle));
                continue;
            }

            bucket.setHigh(Math.max(bucket.getHigh(), candle.getHigh()));
            bucket.setLow(Math.min(bucket.getLow(), candle.getLow()));
            bucket.setClose(candle.getClose());
            bucket.setVolume(bucket.getVolume() + candle.getVolume());
        }

        return new ArrayList<>(aggregated.values());
    }

    private KrStockCandleResponse copyCandle(KrStockCandleResponse source) {
        KrStockCandleResponse target = new KrStockCandleResponse();
        target.setTime(source.getTime());
        target.setOpen(source.getOpen());
        target.setHigh(source.getHigh());
        target.setLow(source.getLow());
        target.setClose(source.getClose());
        target.setVolume(source.getVolume());
        return target;
    }

    private List<KrStockCandleResponse> lastItems(List<KrStockCandleResponse> candles, int count) {
        if (candles == null || candles.size() <= count) {
            return candles == null ? List.of() : candles;
        }
        return candles.subList(candles.size() - count, candles.size());
    }

    private double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0D;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
    }
}
