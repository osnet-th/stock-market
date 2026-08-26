package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb.dto.SttsApiTblDataRow;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb.dto.SttsApiTblItmRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * R-ONE 통계표 항목(ITM) 메타 캐시.
 * <p>
 * key={@code STATBL_ID}, value={@code List<SttsApiTblItmRow>}. entry 수 ≈ 11 (STATBL 11개).
 * 발표 주기 메타는 통계표 deprecated 또는 항목 추가가 드물므로 TTL 24h.
 * <p>
 * 부분 실패 허용 (학습 #16): STATBL N개 중 일부 호출 실패 시 negative cache 60s로 storm 차단,
 * 나머지 STATBL은 정상 사용 가능.
 * <p>
 * 명세: {@code .claude/analyzes/realestate/reb-r-one-openapi-spec.md} (2번 엔드포인트).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RebMetadataCache {

    private static final List<SttsApiTblItmRow> NEGATIVE = List.of();

    private final RebClient client;

    /** STATBL_ID → ITM rows. Caffeine TTL 24h, 음수 캐시 60s 별도. */
    private final Cache<String, List<SttsApiTblItmRow>> cache = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(50)
            .build();

    /**
     * STATBL_ID + (start,end) → DATA rows 캐시. 한 batch 내에서 동일 STATBL의 17 SIDO 호출이
     * 반복되는 비용을 차단 (P0 #5 — redundant 호출 폭증 해소). TTL 30분 — batch 윈도우 안에서 공유.
     */
    private final Cache<DataCacheKey, List<SttsApiTblDataRow>> dataCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    /** STATBL_ID → failure 발생 시점 negative cache (ITM 메타). 60s 후 자동 만료되어 재시도 허용. */
    private final Cache<String, Boolean> negativeCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(50)
            .build();

    /**
     * DATA fetch 실패용 negative cache. itmsFor의 패턴과 동일 — 17 SIDO 동시 호출 시
     * intra-window retry storm 차단. TTL 60s.
     */
    private final Cache<DataCacheKey, Boolean> dataNegativeCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(100)
            .build();

    /**
     * STATBL_ID의 ITM 메타 조회 (lazy load).
     * 캐시 hit 시 즉시 반환. miss & negative cache 활성 시 빈 리스트 반환 (storm 차단).
     * miss & negative inactive 시 Caffeine atomic {@code get(key, loader)} 사용 — 동일 키 동시 호출은
     * Caffeine 내부에서 자동 직렬화되지만, 서로 다른 STATBL_ID는 병렬로 로드된다.
     *
     * @return ITM rows. 호출 실패 시 빈 리스트 (어댑터 fail-soft).
     */
    public List<SttsApiTblItmRow> itmsFor(String statblId) {
        List<SttsApiTblItmRow> cached = cache.getIfPresent(statblId);
        if (cached != null) {
            return cached;
        }
        if (Boolean.TRUE.equals(negativeCache.getIfPresent(statblId))) {
            return NEGATIVE;
        }
        // Caffeine atomic loader: 동일 key 동시 호출은 1회만 loader 실행 (per-key lock).
        // 다른 key의 호출은 영향 없음 — 이전 synchronized 메소드의 전역 직렬화 제거.
        List<SttsApiTblItmRow> result = cache.get(statblId, this::loadOrNegative);
        return result == null ? NEGATIVE : result;
    }

    /**
     * 특정 STATBL_ID 캐시 무효화. R-ONE이 ITM_ID를 추가하는 등 운영 사고 대응용.
     */
    public void invalidate(String statblId) {
        cache.invalidate(statblId);
        negativeCache.invalidate(statblId);
    }

    /**
     * 전체 캐시 무효화.
     */
    public void invalidateAll() {
        cache.invalidateAll();
        dataCache.invalidateAll();
        negativeCache.invalidateAll();
        dataNegativeCache.invalidateAll();
    }

    /**
     * STATBL_ID 데이터 row 조회 (시점 윈도우 단위 캐시). 동일 (statblId, start, end) 호출은
     * 한 번만 fetch, 이후 region 17개는 동일 cache hit. 실패 시 빈 리스트 반환 (어댑터 fail-soft).
     */
    public List<SttsApiTblDataRow> dataFor(String statblId,
                                           String dtacycleCd,
                                           String startWrttime,
                                           String endWrttime) {
        DataCacheKey key = new DataCacheKey(statblId, dtacycleCd, startWrttime, endWrttime);
        if (Boolean.TRUE.equals(dataNegativeCache.getIfPresent(key))) {
            // intra-window retry storm 차단 — itmsFor 패턴과 동일
            return List.of();
        }
        List<SttsApiTblDataRow> result = dataCache.get(key, this::loadDataOrEmpty);
        return result == null ? List.of() : result;
    }

    private List<SttsApiTblDataRow> loadDataOrEmpty(DataCacheKey key) {
        try {
            List<SttsApiTblDataRow> rows = client.fetchData(
                    key.statblId(), key.dtacycleCd(), key.startWrttime(), key.endWrttime());
            log.info("[realestate.reb.cache.data] loaded statbl={}, window={}~{}, rows={}",
                    key.statblId(), key.startWrttime(), key.endWrttime(), rows.size());
            return rows;
        } catch (Exception e) {
            log.warn("[realestate.reb.cache.data] load failed statbl={} window={}~{} ({}) — negative-cache 60s",
                    key.statblId(), key.startWrttime(), key.endWrttime(), e.getClass().getSimpleName());
            dataNegativeCache.put(key, Boolean.TRUE);
            return null;
        }
    }

    /**
     * Caffeine cache loader. 실패 시 negative cache에 표시하고 null을 반환하여
     * Caffeine이 entry를 저장하지 않도록 한다 (negativeCache가 60s storm 차단 담당).
     */
    private List<SttsApiTblItmRow> loadOrNegative(String statblId) {
        try {
            List<SttsApiTblItmRow> rows = client.fetchItm(statblId);
            log.info("[realestate.reb.cache] loaded statbl={}, rows={}", statblId, rows.size());
            return rows;
        } catch (Exception e) {
            log.warn("[realestate.reb.cache] load failed statbl={} ({}), enter negative-cache 60s",
                    statblId, e.getClass().getSimpleName());
            negativeCache.put(statblId, Boolean.TRUE);
            return null; // Caffeine은 null 반환 시 entry 저장 안 함
        }
    }

    /**
     * 검색 헬퍼: STATBL_ID의 ITM rows 중 {@code itmNm}/{@code itmFullnm}이 주어진 region 키워드를
     * 포함하는 첫 매치 반환.
     */
    public Optional<SttsApiTblItmRow> findBySidoKeyword(String statblId, String keyword) {
        return itmsFor(statblId).stream()
                .filter(row -> matches(row, keyword))
                .findFirst();
    }

    private boolean matches(SttsApiTblItmRow row, String keyword) {
        if (row == null || keyword == null) return false;
        return (row.itmNm() != null && row.itmNm().contains(keyword))
                || (row.itmFullnm() != null && row.itmFullnm().contains(keyword));
    }

    private record DataCacheKey(String statblId, String dtacycleCd,
                                 String startWrttime, String endWrttime) {
        DataCacheKey {
            Objects.requireNonNull(statblId, "statblId");
        }
    }
}
