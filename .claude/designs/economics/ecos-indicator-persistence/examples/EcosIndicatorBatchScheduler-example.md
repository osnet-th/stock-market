# EcosIndicatorBatchScheduler 구현 예시

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.economics.application.EcosIndicatorSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EcosIndicatorBatchScheduler {

    private final EcosIndicatorSaveService ecosIndicatorSaveService;

    /**
     * 매일 오전 7시 ECOS 경제지표 스냅샷 저장
     */
    @Scheduled(cron = "${ecos.batch.cron:0 0 7 * * *}")
    public void saveIndicatorSnapshot() {
        log.info("ECOS 경제지표 배치 저장 시작");
        try {
            int savedCount = ecosIndicatorSaveService.fetchAndSave();
            log.info("ECOS 경제지표 배치 저장 완료: {}건", savedCount);
        } catch (Exception e) {
            log.error("ECOS 경제지표 배치 저장 실패", e);
        }
    }
}
```