package com.thlee.stock.market.stockmarket.economics.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.economics.application.GlobalIndicatorSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalIndicatorBatchScheduler {

    private final GlobalIndicatorSaveService globalIndicatorSaveService;

    /**
     * 매일 오전 7시 30분 글로벌 경제지표(TradingEconomics) 스냅샷 저장
     * — ECOS 배치(07:00) 직후에 실행되도록 30분 뒤로 분리
     */
    @Scheduled(cron = "${global.batch.cron:0 30 7 * * *}")
    public void saveIndicatorSnapshot() {
        log.info("글로벌 경제지표 배치 저장 시작");
        try {
            int savedCount = globalIndicatorSaveService.fetchAndSave();
            log.info("글로벌 경제지표 배치 저장 완료: {}건", savedCount);
        } catch (Exception e) {
            log.error("글로벌 경제지표 배치 저장 실패", e);
        }
    }
}