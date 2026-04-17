# ChatContextBuilder + PromptTemplate 예시

사용자는 카테고리를 선택하지 않는다. `AnalysisTask.categories()`가 백엔드 자동 매핑을 보유한다.

## 1. `FinancialAnalysisPromptTemplate` (신규)

```java
// chatbot/application/prompt/FinancialAnalysisPromptTemplate.java
package com.thlee.stock.market.stockmarket.chatbot.application.prompt;

import com.thlee.stock.market.stockmarket.chatbot.application.dto.AnalysisTask;
import org.springframework.stereotype.Component;

@Component
public class FinancialAnalysisPromptTemplate {

    private static final String COMMON_HEADER = """
            당신은 한국 주식 종목 분석 전문가입니다.
            아래 '제공된 팩트' 만을 1순위 근거로 사용하세요.
            업종 평균이 필요하면 일반지식으로 추정하되, 추정치임을 반드시 명시하세요.

            """;

    public String render(AnalysisTask task, String financialContext) {
        String instruction = switch (task) {
            case UNDERVALUATION -> """
                    ## 요청
                    제공된 3개년 재무 데이터와 최근 가치평가 지표를 기반으로 이 종목이 저평가인지 고평가인지 판단하세요.
                    - 자기 자신의 과거 3년 추세를 1순위 근거로 사용
                    - 업종 평균 대비는 일반지식으로 보조 판단 (추정치 명시)
                    - 결론(저평가/고평가/중립)을 명확히 제시
                    """;
            case TREND_SUMMARY -> """
                    ## 요청
                    제공된 재무 데이터의 3개년 추세를 요약하세요.
                    - 매출/이익/자산 흐름의 방향성
                    - 수익성·성장성의 개선/악화 항목을 구분
                    """;
            case RISK_DIAGNOSIS -> """
                    ## 요청
                    제공된 재무 데이터에서 투자 리스크 요인을 진단하세요.
                    - 재무안정성·유동성·자본효율(활동성) 관점
                    - 시급한 리스크 vs 관찰 필요 리스크 구분
                    """;
            case INVESTMENT_OPINION -> """
                    ## 요청
                    제공된 재무 데이터 기반으로 투자 적정성 의견을 제시하세요.
                    - 매수/보유/매도 관점의 근거
                    - 이 데이터만으로 판단하기 어려운 항목 명시
                    """;
        };
        return COMMON_HEADER + instruction + "\n## 제공된 팩트\n" + financialContext;
    }
}
```

## 2. `ChatContextBuilder` FINANCIAL 분기 재작성 (자동 매핑)

```java
// chatbot/application/ChatContextBuilder.java — 핵심 부분만
private final FinancialAnalysisPromptTemplate promptTemplate;
private final ValuationMetricService valuationMetricService;

public String build(ChatRequest request) {
    return switch (request.chatMode()) {
        case PORTFOLIO -> buildPortfolioContext(request.userId());
        case FINANCIAL -> buildFinancialAnalysis(request);
        case ECONOMIC  -> buildEconomicContext(request.indicatorCategory());
    };
}

private String buildFinancialAnalysis(ChatRequest request) {
    if (request.analysisTask() == null || request.stockCode() == null) {
        // 방어 로직: 프론트에서 이미 차단됐지만 직접 호출 대비
        return """
                당신은 한국 주식 종목 분석 전문가입니다.
                분석 작업이 지정되지 않았습니다. 분석 버튼을 눌러달라고 정중히 안내하세요.
                """;
    }
    String facts = assembleFacts(request.stockCode(), request.analysisTask());
    return promptTemplate.render(request.analysisTask(), facts);
}

private String assembleFacts(String stockCode, AnalysisTask task) {
    StringBuilder sb = new StringBuilder();
    String year = String.valueOf(LocalDate.now().getYear());

    for (FinancialCategory category : task.categories()) {
        switch (category) {
            case ACCOUNT       -> appendAccounts(sb, stockCode, year);
            case PROFITABILITY -> appendIndicesAcrossYears(sb, "수익성 지표", stockCode, year, "M210000");
            case STABILITY     -> appendIndicesAcrossYears(sb, "안정성 지표", stockCode, year, "M220000");
            case GROWTH        -> appendIndicesAcrossYears(sb, "성장성 지표", stockCode, year, "M230000");
            case ACTIVITY      -> appendIndicesAcrossYears(sb, "활동성 지표", stockCode, year, "M240000");
            case VALUATION     -> appendValuation(sb, stockCode);
        }
    }
    return sb.toString();
}
```

**세부 함수 설계 포인트**
- `appendAccounts` — `getFinancialAccounts` 1회 호출. `statementDiv` 그룹(IS/BS)으로 서브 섹션. 3개년(currentTerm/previousTerm/beforePreviousTerm) 그대로 테이블화.
- `appendIndicesAcrossYears` — 3개년 병렬 호출(`CompletableFuture.allOf`). `StockFinancialService.getFinancialIndices` 자체에 Caffeine 캐싱. 한 섹션 안에 3개년 값을 지표명별로 정렬.
- `appendValuation` — `valuationMetricService.calculate(stockCode)` 1회 호출. 최근 1년 EPS/BPS/PER/PBR을 마크다운 섹션으로.

## 3. `StockFinancialService.getFinancialIndices` 캐싱

```java
// stock/application/StockFinancialService.java — 메서드에 적용
@Cacheable(
    value = "financial-indices",
    key = "T(java.util.Objects).hash(#stockCode, #year, #indexClassCode)"
)
public List<FinancialIndexResponse> getFinancialIndices(
        String stockCode, String year, String reportCode, String indexClassCode) {
    // 기존 구현 그대로
}
```

`application.yml`에 `financial-indices` 캐시 이름 등록 + Caffeine spec(`maximumSize=500,expireAfterWrite=1d`).

## 4. 프롬프트 출력 예시 (UNDERVALUATION)

```
당신은 한국 주식 종목 분석 전문가입니다.
아래 '제공된 팩트' 만을 1순위 근거로 사용하세요.
업종 평균이 필요하면 일반지식으로 추정하되, 추정치임을 반드시 명시하세요.

## 요청
제공된 3개년 재무 데이터와 최근 가치평가 지표를 기반으로 이 종목이 저평가인지 고평가인지 판단하세요.
- 자기 자신의 과거 3년 추세를 1순위 근거로 사용
- 업종 평균 대비는 일반지식으로 보조 판단 (추정치 명시)
- 결론(저평가/고평가/중립)을 명확히 제시

## 제공된 팩트
### 재무계정 (IS)
- 매출액: 2023=2,589,000억 / 2024=2,800,000억 / 2025=3,000,000억
- 영업이익: 2023=66,000억 / 2024=32,700억 / 2025=50,200억

### 수익성 지표
- ROE: 2023=4.5%, 2024=2.1%, 2025=3.8%
- 영업이익률: 2023=2.5%, 2024=1.2%, 2025=1.7%

### 가치평가 지표 (기준 회계기: 제55기, 기준 주가일: 2026-04-17)
- EPS: 2,610원
- BPS: 55,800원
- PER: 26.4배
- PBR: 1.23배
- 참고: 역사 주가 API 부재로 과거 PER/PBR은 제공되지 않음. 추세 판단 시 재무계정·수익성 지표 3개년을 활용하세요.
```