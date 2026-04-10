package com.thlee.stock.market.stockmarket.economics.application;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.repository.GlobalIndicatorLatestRepository;
import com.thlee.stock.market.stockmarket.economics.domain.repository.GlobalIndicatorRepository;
import com.thlee.stock.market.stockmarket.economics.domain.service.GlobalIndicatorPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 글로벌 경제지표 배치 저장 서비스
 * - 55개 지표를 순차 수집 → (국가, 지표)별 cycle 변경 감지 → 변경분만 히스토리 INSERT
 * - 지표 사이에 1.5초 딜레이로 TradingEconomics 차단 위험 최소화
 * - 단일 트랜잭션 + 지표별 try-catch로 부분 성공 허용
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GlobalIndicatorSaveService {

    private static final long REQUEST_DELAY_MS = 1500L;

    private final GlobalIndicatorPort globalIndicatorPort;
    private final GlobalIndicatorRepository globalIndicatorRepository;
    private final GlobalIndicatorLatestRepository globalIndicatorLatestRepository;

    /**
     * 55개 지표 순차 수집 → cycle 변경분만 히스토리 + latest 전건 갱신
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
        int indicatorIndex = 0;

        for (GlobalEconomicIndicatorType type : GlobalEconomicIndicatorType.values()) {
            // 첫 지표 이후에는 요청 간 딜레이
            if (indicatorIndex > 0 && !sleepBetweenRequests()) {
                break;
            }
            indicatorIndex++;

            try {
                totalSaved += saveOneIndicator(type, today, historyExists, latestMap);
            } catch (Exception e) {
                log.error("글로벌 지표 수집 실패: type={}", type, e);
                // 다음 지표로 계속 진행 (부분 성공 허용)
            }
        }

        log.info("글로벌 경제지표 저장 완료: 총 {}건", totalSaved);
        return totalSaved;
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

    /**
     * 단일 지표 처리: 수집 → 필터링 → (초기 시딩 또는 변경분 저장)
     */
    private int saveOneIndicator(GlobalEconomicIndicatorType type,
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

        if (!historyExists) {
            return initialSeedIndicator(type, valid, today, latestMap);
        }

        return saveChangedIndicator(type, valid, today, latestMap);
    }

    /**
     * 초기 시딩 (지표 단위): 수집된 전체 snapshot → history + latest
     */
    private int initialSeedIndicator(GlobalEconomicIndicatorType type,
                                      List<CountryIndicatorSnapshot> snapshots,
                                      LocalDate today,
                                      Map<String, GlobalIndicatorLatest> latestMap) {
        List<GlobalIndicator> histories = snapshots.stream()
            .map(s -> GlobalIndicator.fromSnapshot(s, today))
            .toList();
        globalIndicatorRepository.saveAll(histories);

        List<GlobalIndicatorLatest> latestList = snapshots.stream()
            .map(GlobalIndicatorLatest::fromSnapshot)
            .toList();
        globalIndicatorLatestRepository.saveAll(latestList);

        // 배치 내 일관성: latestMap 즉시 갱신
        for (GlobalIndicatorLatest latest : latestList) {
            latestMap.put(latest.toCompareKey(), latest);
        }

        log.info("글로벌 지표 초기 시딩: type={}, count={}", type, histories.size());
        return histories.size();
    }

    /**
     * cycle 변경분만 history INSERT + latest 전건 갱신 (previous_data_value 보존)
     */
    private int saveChangedIndicator(GlobalEconomicIndicatorType type,
                                      List<CountryIndicatorSnapshot> snapshots,
                                      LocalDate today,
                                      Map<String, GlobalIndicatorLatest> latestMap) {
        List<GlobalIndicator> changed = snapshots.stream()
            .filter(s -> isCycleChanged(s, latestMap))
            .map(s -> GlobalIndicator.fromSnapshot(s, today))
            .toList();

        if (!changed.isEmpty()) {
            globalIndicatorRepository.saveAll(changed);
            log.info("글로벌 지표 히스토리 저장: type={}, count={}", type, changed.size());
        } else {
            log.info("글로벌 지표 변경 없음: type={}", type);
        }

        // latest 전건 갱신 (previous_data_value 는 LatestRepositoryImpl 에서 보존)
        List<GlobalIndicatorLatest> latestList = snapshots.stream()
            .map(GlobalIndicatorLatest::fromSnapshot)
            .toList();
        globalIndicatorLatestRepository.saveAll(latestList);

        // 배치 내 일관성: latestMap 즉시 갱신
        for (GlobalIndicatorLatest latest : latestList) {
            latestMap.put(latest.toCompareKey(), latest);
        }

        return changed.size();
    }

    /**
     * latestMap 과 비교하여 cycle 변경 여부 판단
     */
    private boolean isCycleChanged(CountryIndicatorSnapshot snapshot,
                                    Map<String, GlobalIndicatorLatest> latestMap) {
        String newCycle = snapshot.getReferenceText();
        String key = snapshot.getCountryName() + "::" + snapshot.getIndicatorType().name();
        GlobalIndicatorLatest existing = latestMap.get(key);

        // latestMap 에 없으면 신규 → 저장 대상
        // cycle 이 다르면 → 저장 대상
        return existing == null || !newCycle.equals(existing.getCycle());
    }
}