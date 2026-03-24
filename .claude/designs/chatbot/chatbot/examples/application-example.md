# Application 예시

```java
// chatbot/application/dto/FinancialData.java
package com.thlee.stock.market.stockmarket.chatbot.application.dto;

import java.math.BigDecimal;

public record FinancialData(
        String ticker,           // 종목명 또는 코드
        BigDecimal currentPrice, // 현재 주가
        BigDecimal per,          // PER (nullable)
        BigDecimal pbr,          // PBR (nullable)
        BigDecimal eps,          // EPS (nullable)
        BigDecimal roe,          // ROE (nullable)
        String memo              // 추가 설명 (nullable)
) {}
```

```java
// chatbot/application/dto/ChatRequest.java
package com.thlee.stock.market.stockmarket.chatbot.application.dto;

public record ChatRequest(
        Long userId,
        String message,
        FinancialData financialData  // nullable — 재무 분석 질문 시에만 포함
) {}
```

```java
// chatbot/application/ChatContextBuilder.java
package com.thlee.stock.market.stockmarket.chatbot.application;

import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatRequest;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.FinancialData;
import com.thlee.stock.market.stockmarket.economics.application.EcosIndicatorService;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import com.thlee.stock.market.stockmarket.economics.domain.model.KeyStatIndicator;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioAllocationService;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioService;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.AllocationResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatContextBuilder {

    private final PortfolioService portfolioService;
    private final PortfolioAllocationService portfolioAllocationService;
    private final EcosIndicatorService ecosIndicatorService;

    public String build(ChatRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 주식/투자 전문 금융 어시스턴트입니다. 아래 사용자 데이터를 참고하여 답변하세요.\n\n");

        // 자산 유형별 비중
        List<AllocationResponse> allocations = portfolioAllocationService.getAllocation(request.userId());
        if (!allocations.isEmpty()) {
            sb.append("## 포트폴리오 자산 비중\n");
            allocations.forEach(a ->
                sb.append("- ").append(a.getAssetTypeName())
                  .append(": ").append(a.getPercentage()).append("%")
                  .append(" (").append(a.getTotalAmount()).append("원)\n")
            );
            sb.append("\n");
        }

        // 보유 종목 목록
        List<PortfolioItemResponse> items = portfolioService.getItems(request.userId());
        if (!items.isEmpty()) {
            sb.append("## 보유 종목\n");
            items.forEach(item ->
                sb.append("- ").append(item.getItemName())
                  .append(" (").append(item.getAssetType()).append(", ").append(item.getRegion()).append(")\n")
            );
            sb.append("\n");
        }

        // 재무 데이터 (질문에 포함된 경우만)
        if (request.financialData() != null) {
            FinancialData fd = request.financialData();
            sb.append("## 분석 대상 종목 재무 데이터\n");
            sb.append("- 종목: ").append(fd.ticker()).append("\n");
            sb.append("- 현재 주가: ").append(fd.currentPrice()).append("원\n");
            if (fd.per() != null) sb.append("- PER: ").append(fd.per()).append("\n");
            if (fd.pbr() != null) sb.append("- PBR: ").append(fd.pbr()).append("\n");
            if (fd.eps() != null) sb.append("- EPS: ").append(fd.eps()).append("원\n");
            if (fd.roe() != null) sb.append("- ROE: ").append(fd.roe()).append("%\n");
            if (fd.memo() != null) sb.append("- 추가 정보: ").append(fd.memo()).append("\n");
            sb.append("\n");
        }

        // 주요 경제지표 (금리)
        List<KeyStatIndicator> indicators = ecosIndicatorService.getIndicatorsByCategory(EcosIndicatorCategory.INTEREST_RATE);
        if (!indicators.isEmpty()) {
            sb.append("## 주요 경제지표\n");
            indicators.stream().limit(3).forEach(ind ->
                sb.append("- ").append(ind.keystatName()).append(": ").append(ind.dataValue()).append("\n")
            );
        }

        return sb.toString();
    }
}
```

```java
// chatbot/application/ChatService.java
package com.thlee.stock.market.stockmarket.chatbot.application;

import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatRequest;
import com.thlee.stock.market.stockmarket.chatbot.application.port.LlmPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final LlmPort llmPort;
    private final ChatContextBuilder contextBuilder;

    public Flux<String> chat(ChatRequest request) {
        String systemPrompt = contextBuilder.build(request);
        return llmPort.stream(systemPrompt, request.message());
    }
}
```
