package com.thlee.stock.market.stockmarket.economics.application;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.repository.GlobalIndicatorLatestRepository;
import com.thlee.stock.market.stockmarket.economics.domain.repository.GlobalIndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 글로벌 경제지표 지표 단위 저장 (#51)
 *
 * 지표별 독립 트랜잭션으로 저장해 한 지표의 DB 실패가 같은 배치의 다른 지표를
 * 롤백시키지 않도록 한다 — PostgreSQL 은 트랜잭션 내 SQL 에러 1건이면 트랜잭션
 * 전체가 abort 되므로, 배치 전체 단일 트랜잭션 구조에서는 "부분 성공 허용"이
 * 실제로 동작하지 않았다 (히스토리가 2026-04-11 시딩 이후 동결된 원인 계열).
 * HTTP 수집은 트랜잭션 밖({@link GlobalIndicatorSaveService})에서 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalIndicatorSnapshotWriter {

    private final GlobalIndicatorRepository globalIndicatorRepository;
    private final GlobalIndicatorLatestRepository globalIndicatorLatestRepository;

    /**
     * 단일 지표 저장: 초기 시딩 또는 cycle 변경분 INSERT + latest 전건 갱신
     */
    @Transactional
    public int persistIndicator(GlobalEconomicIndicatorType type,
                                List<CountryIndicatorSnapshot> snapshots,
                                LocalDate today,
                                boolean historyExists,
                                Map<String, GlobalIndicatorLatest> latestMap) {
        if (!historyExists) {
            return initialSeedIndicator(type, snapshots, today, latestMap);
        }
        return saveChangedIndicator(type, snapshots, today, latestMap);
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

        upsertLatest(snapshots, latestMap);

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

        upsertLatest(snapshots, latestMap);

        return changed.size();
    }

    /**
     * latest 전건 갱신 (previous_data_value 는 LatestRepositoryImpl 에서 보존)
     * + 배치 내 일관성을 위해 latestMap 즉시 갱신
     */
    private void upsertLatest(List<CountryIndicatorSnapshot> snapshots,
                              Map<String, GlobalIndicatorLatest> latestMap) {
        List<GlobalIndicatorLatest> latestList = snapshots.stream()
            .map(GlobalIndicatorLatest::fromSnapshot)
            .toList();
        globalIndicatorLatestRepository.saveAll(latestList);

        for (GlobalIndicatorLatest latest : latestList) {
            latestMap.put(latest.toCompareKey(), latest);
        }
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
