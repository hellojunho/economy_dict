package com.economydict.service;

import com.economydict.dto.StockAdvisorRequest;
import com.economydict.dto.StockAdvisorResponse;
import com.economydict.dto.StockAdvisorSourceResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockAdvisorService {
    private final OpenAiService openAiService;

    public StockAdvisorService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public StockAdvisorResponse analyze(StockAdvisorRequest request) {
        String symbol = normalizeSymbol(request.getSymbol());
        String riskProfile = normalizeLabel(request.getRiskProfile());
        String tradeStyle = normalizeLabel(request.getTradeStyle());

        OpenAiService.WebSearchResult result = openAiService.respondWithWebSearch(
                buildSystemPrompt(),
                buildUserPrompt(symbol, riskProfile, tradeStyle)
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

    private String normalizeSymbol(String raw) {
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

    private String normalizeLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("투자 성향을 선택하세요.");
        }
        return raw.trim();
    }

    private String buildSystemPrompt() {
        return """
                You are a senior equity strategist, technical analyst, disclosure analyst, and market sentiment researcher.
                Your job is to analyze a TradingView symbol and produce a practical Korean investment strategy memo.

                Non-negotiable rules:
                - Answer in Korean.
                - Use web search results to ground recent disclosures, news, issues, and current market context.
                - Never invent filings, prices, dates, corporate events, or source details.
                - When referring to recent disclosures or news, include concrete dates whenever available.
                - Separate facts from interpretation.
                - Treat buy pressure, sell pressure, volume profile, support, resistance, and supply-overhang analysis carefully.
                - If exact chart, order-flow, or volume-profile data is not available from retrieved sources, explicitly say precision is limited and provide only approximate price zones inferred from public reporting or technical commentary.
                - If the symbol is not a common operating company stock, adapt the analysis and explain limits.
                - Do not promise profit, certainty, or guaranteed upside.
                - Do not ask follow-up questions. Deliver the best possible answer with explicit caveats.

                Output format:
                ## 한줄 결론
                - 2~3문장 요약.

                ## 종합 판단
                - 현재 강세/중립/약세인지와 그 근거.

                ## 매수세와 매도세
                - 수급/가격 흐름/거래대금/이슈 반응 관점에서 해석.

                ## 매물대와 가격대 체크
                - 중요 지지/저항/돌파/이탈 가격대를 bullet로 정리.
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
                - entry idea, additional buy condition, invalidation condition, profit-taking idea, and monitoring point를 포함.
                - 단타 / 스윙 / 장기 관점도 짧게 비교.

                ## 리스크 관리
                - position sizing, stop discipline, event risk, and what would invalidate the thesis.

                End with one short disclaimer line that this is an information support tool, not personalized investment advice.
                """
                .trim();
    }

    private String buildUserPrompt(String symbol, String riskProfile, String tradeStyle) {
        return """
                Analyze the TradingView symbol below and recommend a forward-looking strategy.

                Symbol: %s
                Selected risk profile: %s
                Selected trading style: %s

                Focus areas:
                1. Recent disclosures, earnings, guidance, regulatory filings, and notable company announcements.
                2. Recent news, controversies, partnerships, product launches, macro/social trends, and sector mood affecting outlook.
                3. Buy-side vs sell-side pressure, likely supply-overhang zones, support/resistance, and investor sentiment.
                4. Company outlook over the next several weeks to months.
                5. A practical strategy recommendation tailored first to the selected risk profile and trading style, then briefly compared with short-term trading, swing trading, and long-term holding viewpoints.

                Be explicit about uncertainty. If precise chart or order-flow data is unavailable, say so clearly instead of pretending.
                """.formatted(symbol, riskProfile, tradeStyle).trim();
    }
}
