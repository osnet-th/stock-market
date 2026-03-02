package com.thlee.stock.market.stockmarket.economics.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.economics.application.EcosIndicatorSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EcosIndicatorWarmupListener {

    private final EcosIndicatorSaveService ecosIndicatorSaveService;

    /**
     * 앱 시작 완료 후 ECOS 경제지표 캐시 warm-up + DB 저장
     * 실패해도 앱 기동에 영향 없음 (캐시 miss 시 fallback 대응)
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("ECOS 경제지표 warm-up 시작");
        try {
            int savedCount = ecosIndicatorSaveService.fetchAndSave();
            log.info("ECOS 경제지표 warm-up 완료: {}건 저장", savedCount);
        } catch (Exception e) {
            log.error("ECOS 경제지표 warm-up 실패, 캐시 miss 시 API fallback 대응", e);
        }
    }
}
