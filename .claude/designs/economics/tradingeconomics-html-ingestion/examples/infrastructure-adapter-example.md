# Infrastructure Adapter 구현 예시

## TradingEconomicsProperties (infrastructure/global/tradingeconomics/config)

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "economics.api.global.tradingeconomics")
public class TradingEconomicsProperties {
    private String baseUrl;
    private int timeout = 5000;
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
}
```

## TradingEconomicsIndicatorRegistry (infrastructure/global/tradingeconomics)

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.config.TradingEconomicsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradingEconomicsIndicatorRegistry {

    private final TradingEconomicsProperties properties;

    /**
     * 지표 타입 → 전체 URL 생성
     * 예: CORE_CPI → https://ko.tradingeconomics.com/country-list/core-consumer-prices?continent=g20
     */
    public String getUrl(GlobalEconomicIndicatorType type) {
        return properties.getBaseUrl()
            + "/country-list/" + type.getPathSegment()
            + "?continent=g20";
    }
}
```

## TradingEconomicsHtmlClient (infrastructure/global/tradingeconomics)

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics;

import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.config.TradingEconomicsProperties;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.exception.TradingEconomicsFetchException;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TradingEconomicsHtmlClient {

    private final TradingEconomicsProperties properties;

    public Document fetch(String url) {
        try {
            return Jsoup.connect(url)
                .userAgent(properties.getUserAgent())
                .timeout(properties.getTimeout())
                .get();
        } catch (IOException e) {
            throw new TradingEconomicsFetchException(
                "HTML 수집 실패: url=" + url + ", cause=" + e.getMessage(), e);
        }
    }
}
```

## TradingEconomicsIndicatorAdapter (infrastructure/global/tradingeconomics)

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.service.GlobalIndicatorPort;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.dto.ParsedTable;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TradingEconomicsIndicatorAdapter implements GlobalIndicatorPort {

    private static final BigxLogger log = BigxLogger.create(TradingEconomicsIndicatorAdapter.class);

    private final TradingEconomicsIndicatorRegistry registry;
    private final TradingEconomicsHtmlClient htmlClient;
    private final TradingEconomicsTableParser tableParser;
    private final TradingEconomicsValueNormalizer normalizer;

    @Override
    public List<CountryIndicatorSnapshot> fetchByIndicator(GlobalEconomicIndicatorType indicatorType) {
        String url = registry.getUrl(indicatorType);

        long start = System.currentTimeMillis();

        // 1. HTML 수집
        Document document = htmlClient.fetch(url);

        // 2. 테이블 파싱 (헤더 기반 동적 매핑)
        ParsedTable parsedTable = tableParser.parse(document);

        // 3. 값 정규화 → 도메인 모델 변환
        List<CountryIndicatorSnapshot> result = normalizer.normalize(indicatorType, parsedTable.rows());

        log.info("TradingEconomics 지표 수집 완료. type={}, rows={}, elapsedMs={}",
            indicatorType, result.size(), System.currentTimeMillis() - start);

        return result;
    }
}
```

## 예외 클래스

### TradingEconomicsFetchException

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.exception;

public class TradingEconomicsFetchException extends RuntimeException {
    public TradingEconomicsFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### TradingEconomicsParseException

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.exception;

public class TradingEconomicsParseException extends RuntimeException {
    public TradingEconomicsParseException(String message) {
        super(message);
    }
}
```