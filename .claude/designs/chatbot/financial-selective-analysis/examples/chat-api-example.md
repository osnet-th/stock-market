# API·DTO 변경 예시

사용자는 지표를 선택하지 않고 분석 버튼만 클릭한다. 요청 본문에는 `analysisTask` 한 필드만 추가된다.

## 1. `FinancialCategory` (신규, 백엔드 내부용)

```java
// chatbot/application/dto/FinancialCategory.java
package com.thlee.stock.market.stockmarket.chatbot.application.dto;

public enum FinancialCategory {
    ACCOUNT,        // 재무계정 (매출/영업이익/자산 등)
    PROFITABILITY,  // 수익성 지표 (M210000)
    STABILITY,      // 안정성 지표 (M220000)
    GROWTH,         // 성장성 지표 (M230000)
    ACTIVITY,       // 활동성 지표 (M240000)
    VALUATION,      // 가치평가 지표 (EPS/BPS/PER/PBR)
}
```

## 2. `AnalysisTask` (신규, 카테고리 매핑 포함)

```java
// chatbot/application/dto/AnalysisTask.java
package com.thlee.stock.market.stockmarket.chatbot.application.dto;

import java.util.List;

public enum AnalysisTask {
    UNDERVALUATION(List.of(
            FinancialCategory.ACCOUNT,
            FinancialCategory.PROFITABILITY,
            FinancialCategory.VALUATION
    )),
    TREND_SUMMARY(List.of(
            FinancialCategory.ACCOUNT,
            FinancialCategory.PROFITABILITY,
            FinancialCategory.GROWTH
    )),
    RISK_DIAGNOSIS(List.of(
            FinancialCategory.ACCOUNT,
            FinancialCategory.STABILITY,
            FinancialCategory.ACTIVITY
    )),
    INVESTMENT_OPINION(List.of(
            FinancialCategory.ACCOUNT,
            FinancialCategory.PROFITABILITY,
            FinancialCategory.STABILITY,
            FinancialCategory.GROWTH,
            FinancialCategory.ACTIVITY,
            FinancialCategory.VALUATION
    ));

    private final List<FinancialCategory> categories;

    AnalysisTask(List<FinancialCategory> categories) {
        this.categories = categories;
    }

    public List<FinancialCategory> categories() {
        return categories;
    }
}
```

## 3. `ChatRequest` 필드 추가

```java
// chatbot/application/dto/ChatRequest.java
public record ChatRequest(
        Long userId,
        String message,
        ChatMode chatMode,
        String stockCode,
        String indicatorCategory,
        AnalysisTask analysisTask,      // 신규, FINANCIAL 모드에서 사용
        List<ChatMessage> messages
) {}
```

## 4. `ChatController.ChatMessageRequest` 확장

```java
// chatbot/presentation/ChatController.java
public record ChatMessageRequest(
        String message,
        ChatMode chatMode,
        String stockCode,
        String indicatorCategory,
        AnalysisTask analysisTask,
        List<ChatMessage> messages
) {}

@PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chat(
        @RequestParam Long userId,
        @RequestBody ChatMessageRequest request
) {
    return chatService.chat(new ChatRequest(
            userId,
            request.message(),
            request.chatMode(),
            request.stockCode(),
            request.indicatorCategory(),
            request.analysisTask(),
            request.messages()
    ));
}
```

## 5. 프론트 요청 바디 (`api.js` `streamChat`)

```javascript
body: JSON.stringify({
  message,                // 분석 버튼 호출 시 ''
  chatMode,
  stockCode,
  indicatorCategory,
  analysisTask,           // 'UNDERVALUATION' | 'TREND_SUMMARY' | 'RISK_DIAGNOSIS' | 'INVESTMENT_OPINION' | null
  messages
})
```