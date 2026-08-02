package com.thlee.stock.market.stockmarket.economics.application;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.repository.GlobalIndicatorLatestRepository;
import com.thlee.stock.market.stockmarket.economics.domain.repository.GlobalIndicatorRepository;
import com.thlee.stock.market.stockmarket.economics.domain.service.GlobalIndicatorPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 글로벌 경제지표 배치 저장 서비스
 * - 41개 지표를 순차 수집 → (국가, 지표)별 cycle 변경 감지 → 변경분만 히스토리 INSERT
 * - 지표 사이에 1.5초 딜레이로 TradingEconomics 차단 위험 최소화
 * - 지표 단위 독립 트랜잭션(#51, {@link GlobalIndicatorSnapshotWriter}):
 *   한 지표의 실패가 다른 지표의 저장을 롤백시키지 않는다 (부분 성공 실제 보장).
 *   HTTP 수집은 트랜잭션 밖에서 수행해 커넥션 점유를 최소화한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalIndicatorSaveService {

    private static final long REQUEST_DELAY_MS = 1500L;

    private final GlobalIndicatorPort globalIndicatorPort;
    private final GlobalIndicatorRepository globalIndicatorRepository;
    private final GlobalIndicatorLatestRepository globalIndicatorLatestRepository;
    private final GlobalIndicatorSnapshotWriter snapshotWriter;

    /**
     * 전체 지표 순차 수집 → cycle 변경분만 히스토리 + latest 전건 갱신
     *
     * @return 저장된 히스토리 레코드 수
     */
    public int fetchAndSave() {
        LocalDate today = LocalDate.now();

        // 루프 진입 전 공통 상태 확인 (1회)
        boolean historyExists = globalIndicatorRepository.existsAny();

        // latest 전체 조회 → Map 변환 (compareKey 기준)
        Map<String, GlobalIndicatorLatest> latestMap = new HashMap<>();
        for (GlobalIndicatorLatest latest : globalIndicatorLatestRepository.findAll()) {
            latestMap.put(latest.toCompareKey(), latest);
        }

        int totalSaved = 0;
        int successCount = 0;
        int failCount = 0;
        int indicatorIndex = 0;

        for (GlobalEconomicIndicatorType type : GlobalEconomicIndicatorType.values()) {
            // 첫 지표 이후에는 요청 간 딜레이
            if (indicatorIndex > 0 && !sleepBetweenRequests()) {
                break;
            }
            indicatorIndex++;

            try {
                totalSaved += fetchAndPersistOne(type, today, historyExists, latestMap);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("글로벌 지표 수집 실패: type={}", type, e);
                // 다음 지표로 계속 진행 (지표 단위 트랜잭션이라 이전 저장분은 유지됨)
            }
        }

        log.info("글로벌 경제지표 저장 완료: 히스토리 {}건 (지표 성공 {} / 실패 {} / 전체 {})",
                totalSaved, successCount, failCount, GlobalEconomicIndicatorType.values().length);
        return totalSaved;
    }

    /**
     * 단일 지표 처리: 수집(트랜잭션 밖) → 유효성 필터 → 지표 단위 트랜잭션 저장
     */
    private int fetchAndPersistOne(GlobalEconomicIndicatorType type,
                                    LocalDate today,
                                    boolean historyExists,
                                    Map<String, GlobalIndicatorLatest> latestMap) {
        List<CountryIndicatorSnapshot> snapshots = globalIndicatorPort.fetchByIndicator(type);

        // referenceText(=cycle) 및 lastValue 유효성 필터
        List<CountryIndicatorSnapshot> valid = snapshots.stream()
            .filter(s -> s.getReferenceText() != null && !s.getReferenceText().isBlank())
            .filter(s -> s.getLastValue() != null)
            .toList();

        if (valid.isEmpty()) {
            log.info("글로벌 지표 유효 데이터 없음: type={}", type);
            return 0;
        }

        return snapshotWriter.persistIndicator(type, valid, today, historyExists, latestMap);
    }

    private boolean sleepBetweenRequests() {
        try {
            Thread.sleep(REQUEST_DELAY_MS);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("글로벌 지표 배치 인터럽트, 루프 중단");
            return false;
        }
    }
}
