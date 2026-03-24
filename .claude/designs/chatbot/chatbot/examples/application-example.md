# Application 예시

```java
// chatbot/application/dto/ChatRequest.java
package com.thlee.stock.market.stockmarket.chatbot.application.dto;

public record ChatRequest(Long userId, String message) {}
```

```java
// chatbot/application/ChatContextBuilder.java
package com.thlee.stock.market.stockmarket.chatbot.application;

import com.thlee.stock.market.stockmarket.economics.application.EcosIndicatorService;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import com.thlee.stock.market.stockmarket.economics.domain.model.KeyStatIndicator;
import com.thlee.stock.market.stockmarket.news.application.KeywordService;
import com.thlee.stock.market.stockmarket.news.application.NewsQueryService;
import com.thlee.stock.market.stockmarket.news.application.dto.KeywordResponse;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioService;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatContextBuilder {

    private final PortfolioService portfolioService;
    private final KeywordService keywordService;
    private final NewsQueryService newsQueryService;
    private final EcosIndicatorService ecosIndicatorService;

    public String build(Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 주식/투자 전문 금융 어시스턴트입니다. 아래 사용자 데이터를 참고하여 답변하세요.\n\n");

        // 포트폴리오
        List<PortfolioItemResponse> items = portfolioService.getItems(userId);
        if (!items.isEmpty()) {
            sb.append("## 보유 포트폴리오\n");
            items.forEach(item ->
                sb.append("- ").append(item.getItemName())
                  .append(" (").append(item.getAssetType()).append(", ").append(item.getRegion()).append(")\n")
            );
            sb.append("\n");
        }

        // DB 저장 뉴스 조회 (외부 API 호출 없음)
        List<KeywordResponse> keywords = keywordService.getActiveKeywordsByUser(userId);
        if (!keywords.isEmpty()) {
            sb.append("## 관련 최신 뉴스\n");
            keywords.stream().limit(3).forEach(kw -> {
                List<NewsDto> newsList = newsQueryService.getNewsByKeywordId(kw.getId(), 0, 3).getContent();
                newsList.forEach(news ->
                    sb.append("- [").append(kw.getKeyword()).append("] ").append(news.getTitle()).append("\n")
                );
            });
            sb.append("\n");
        }

        // 주요 경제지표
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
        String systemPrompt = contextBuilder.build(request.userId());
        return llmPort.stream(systemPrompt, request.message());
    }
}
```
