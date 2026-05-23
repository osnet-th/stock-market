package com.thlee.stock.market.stockmarket.favorite.application;

import com.thlee.stock.market.stockmarket.economics.application.EcosIndicatorService;

import com.thlee.stock.market.stockmarket.economics.application.GlobalIndicatorCacheService;
import com.thlee.stock.market.stockmarket.economics.application.GlobalIndicatorQueryService;
import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorCategory;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.exception.TradingEconomicsFetchException;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.exception.TradingEconomicsParseException;
import com.thlee.stock.market.stockmarket.favorite.application.exception.FavoriteRefreshForbiddenException;
import com.thlee.stock.market.stockmarket.favorite.application.exception.RefreshRateLimitExceededException;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteDisplayMode;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicator;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicatorSourceType;
import com.thlee.stock.market.stockmarket.favorite.domain.repository.FavoriteIndicatorRepository;
import com.thlee.stock.market.stockmarket.favorite.infrastructure.config.GlobalFavoriteExecutorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FavoriteIndicatorService {

    private static final int HISTORY_LIMIT = 30;

    private final FavoriteIndicatorRepository favoriteIndicatorRepository;
    private final FavoritePriorityInserter favoritePriorityInserter;
    private final EcosIndicatorService ecosIndicatorService;
    private final GlobalIndicatorQueryService globalIndicatorQueryService;
    private final GlobalIndicatorCacheService globalIndicatorCacheService;
    private final RefreshRateLimiter refreshRateLimiter;
    private final SingleFlightCoordinator singleFlightCoordinator;
    private final ExecutorService globalFavoriteFetchExecutor;

    public FavoriteIndicatorService(
            FavoriteIndicatorRepository favoriteIndicatorRepository,
            FavoritePriorityInserter favoritePriorityInserter,
            EcosIndicatorService ecosIndicatorService,
            GlobalIndicatorQueryService globalIndicatorQueryService,
            GlobalIndicatorCacheService globalIndicatorCacheService,
            RefreshRateLimiter refreshRateLimiter,
            SingleFlightCoordinator singleFlightCoordinator,
            @Qualifier(GlobalFavoriteExecutorConfig.BEAN_NAME) ExecutorService globalFavoriteFetchExecutor) {
        this.favoriteIndicatorRepository = favoriteIndicatorRepository;
        this.favoritePriorityInserter = favoritePriorityInserter;
        this.ecosIndicatorService = ecosIndicatorService;
        this.globalIndicatorQueryService = globalIndicatorQueryService;
        this.globalIndicatorCacheService = globalIndicatorCacheService;
        this.refreshRateLimiter = refreshRateLimiter;
        this.singleFlightCoordinator = singleFlightCoordinator;
        this.globalFavoriteFetchExecutor = globalFavoriteFetchExecutor;
    }

    private static final int INSERT_MAX_RETRIES = 3;
    private static final long INSERT_RETRY_BACKOFF_BASE_MS = 50L;

    /**
     * 관심 지표 토글 (등록/해제).
     * INSERT 분기는 단일 SQL `INSERT ... SELECT MAX(priority)+1` + DEFERRABLE UNIQUE로 race를 처리한다.
     * SQLState 23505(UNIQUE 위반)는 transient race로 간주해 최대 INSERT_MAX_RETRIES만큼 randomized backoff 후 재시도.
     * 그 외 무결성 위반은 즉시 상위로 propagate.
     * @return true: 등록됨, false: 해제됨
     */
    @Transactional
    public boolean toggle(Long userId, FavoriteIndicatorSourceType sourceType, String indicatorCode) {
        int deleted = favoriteIndicatorRepository.deleteByUserIdAndSourceTypeAndIndicatorCode(
            userId, sourceType, indicatorCode);
        if (deleted > 0) {
            return false;
        }
        insertWithRetry(userId, sourceType, indicatorCode);
        return true;
    }

    private void insertWithRetry(Long userId, FavoriteIndicatorSourceType sourceType, String indicatorCode) {
        for (int attempt = 1; attempt <= INSERT_MAX_RETRIES; attempt++) {
            try {
                // REQUIRES_NEW로 격리된 트랜잭션을 호출 — 매 attempt가 독립 commit/rollback되어
                // 23505 발생 시 후속 attempt가 'transaction aborted (25P02)' 상태에 빠지지 않는다.
                favoritePriorityInserter.insert(userId, sourceType, indicatorCode);
                return;
            } catch (DataIntegrityViolationException e) {
                if (!isUniqueViolation(e) || attempt == INSERT_MAX_RETRIES) {
                    log.error("FAVORITE_INSERT_UNIQUE_RETRY_EXHAUSTED userId={} sourceType={} indicator={} attempts={}",
                        userId, sourceType, indicatorCode, attempt, e);
                    throw e;
                }
                sleepBackoff(attempt);
            }
        }
    }

    private boolean isUniqueViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof java.sql.SQLException sqlEx
                && "23505".equals(sqlEx.getSQLState())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void sleepBackoff(int attempt) {
        try {
            long base = INSERT_RETRY_BACKOFF_BASE_MS * attempt;
            long jitter = (long) (Math.random() * base);
            Thread.sleep(base + jitter);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 관심 지표 표시 모드 변경 (INDICATOR ↔ GRAPH)
     */
    @Transactional
    public void changeDisplayMode(Long userId,
                                  FavoriteIndicatorSourceType sourceType,
                                  String indicatorCode,
                                  FavoriteDisplayMode displayMode) {
        favoriteIndicatorRepository.updateDisplayMode(userId, sourceType, indicatorCode, displayMode);
    }

    /**
     * 컨테이너 (sourceType × displayMode) 내 항목들의 새 순서를 일괄 반영한다.
     * 알고리즘 (Option B — group-wide dense reassignment):
     *  1) (user_id, sourceType) 그룹을 PESSIMISTIC_WRITE로 잠그고 priority ASC NULLS LAST + id ASC로 조회
     *  2) edited(현재 컨테이너 항목)와 others(다른 displayMode 항목)로 분리
     *  3) 페이로드 indicatorCodes에서 dedup 후 매칭되는 edited 항목을 그 순서로, 매칭 안 된 edited 항목(다른 탭에서 추가됨)은 기존 priority 순으로 뒤에 append
     *  4) others의 기존 *상대 순서*를 유지하면서 ordered_edited와 인터리브해 group dense 0..N-1 할당
     *  5) 변경된 행만 bulk update (no-op 단축)
     *  6) Post-write invariant assertion: 같은 그룹의 priority 목록이 0..N-1 contiguous인지 검증, 위반 시 IllegalStateException
     *
     * R7 정책:
     *  (a) 페이로드의 cross-container 코드는 silent ignore
     *  (b) 다른 displayMode 항목의 *상대 순서* 보존 (priority 정수 자체는 dense 재할당 — 코드 주석 참조)
     *  (c) 다른 탭에서 추가된 항목은 컨테이너 맨 뒤로 부여
     *  (d) 동시 저장 시 ordering intent는 last-writer-wins
     */
    @Transactional
    public void reorder(Long userId,
                        FavoriteIndicatorSourceType sourceType,
                        FavoriteDisplayMode displayMode,
                        List<String> indicatorCodes) {
        try {
            List<FavoriteIndicator> rows = favoriteIndicatorRepository.findForReorderUpdate(userId, sourceType);
            List<FavoriteIndicator> combined = computeNewOrder(rows, displayMode, indicatorCodes);
            Map<Long, Integer> assignments = denseAssign(combined);
            Map<Long, Integer> diff = filterChanged(rows, assignments);
            if (!diff.isEmpty()) {
                favoriteIndicatorRepository.bulkUpdatePriority(diff);
            }
            assertGroupInvariant(userId, sourceType, combined.size());
            log.info("FAVORITE_REORDER_OK userId={} sourceType={} displayMode={} payloadSize={} rowsUpdated={}",
                userId, sourceType, displayMode, indicatorCodes.size(), diff.size());
        } catch (IllegalStateException e) {
            log.error("FAVORITE_REORDER_INVARIANT_VIOLATION userId={} sourceType={} displayMode={}",
                userId, sourceType, displayMode, e);
            throw e;
        }
        // DEFERRABLE INITIALLY DEFERRED UNIQUE 제약 위반은 commit 시점에만 검증되므로
        // 본 메서드 내부에서 catch 불가능 — GlobalExceptionHandler.handleDataIntegrityViolation 이
        // 409 CONFLICT 응답으로 매핑한다(공식 race 시그널). 운영 모니터링은 그 핸들러의
        // publishError 이벤트로 잡힌다.
    }

    /**
     * 페이로드 + 서버 상태에서 그룹 전체의 새 순서를 산출한다 (순수 함수).
     */
    private List<FavoriteIndicator> computeNewOrder(List<FavoriteIndicator> rows,
                                                   FavoriteDisplayMode mode,
                                                   List<String> codes) {
        List<FavoriteIndicator> edited = filterByMode(rows, mode, true);
        List<FavoriteIndicator> others = filterByMode(rows, mode, false);
        Map<String, FavoriteIndicator> byCode = indexFirstOccurrenceByCode(edited);
        List<FavoriteIndicator> fromPayload = pickByCodes(codes, byCode);
        List<FavoriteIndicator> appended = appendMissing(edited, fromPayload);
        List<FavoriteIndicator> orderedEdited = concat(fromPayload, appended);
        return interleavePreservingOthersOrder(rows, others, orderedEdited);
    }

    private List<FavoriteIndicator> filterByMode(List<FavoriteIndicator> rows,
                                                FavoriteDisplayMode mode,
                                                boolean equals) {
        return rows.stream()
            .filter(r -> equals == (r.getDisplayMode() == mode))
            .toList();
    }

    private Map<String, FavoriteIndicator> indexFirstOccurrenceByCode(List<FavoriteIndicator> edited) {
        Map<String, FavoriteIndicator> byCode = new HashMap<>();
        for (FavoriteIndicator e : edited) {
            byCode.putIfAbsent(e.getIndicatorCode(), e);
        }
        return byCode;
    }

    private List<FavoriteIndicator> pickByCodes(List<String> codes, Map<String, FavoriteIndicator> byCode) {
        java.util.LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<FavoriteIndicator> picked = new ArrayList<>();
        for (String code : codes) {
            if (!seen.add(code)) {
                continue;
            }
            FavoriteIndicator match = byCode.get(code);
            if (match != null) {
                picked.add(match);
            }
        }
        return picked;
    }

    private List<FavoriteIndicator> appendMissing(List<FavoriteIndicator> edited,
                                                 List<FavoriteIndicator> fromPayload) {
        Set<Long> picked = fromPayload.stream()
            .map(FavoriteIndicator::getId)
            .collect(Collectors.toSet());
        return edited.stream()
            .filter(e -> !picked.contains(e.getId()))
            .toList();
    }

    private List<FavoriteIndicator> concat(List<FavoriteIndicator> a, List<FavoriteIndicator> b) {
        List<FavoriteIndicator> out = new ArrayList<>(a.size() + b.size());
        out.addAll(a);
        out.addAll(b);
        return out;
    }

    /**
     * others(다른 displayMode 항목)는 기존 등장 순서 그대로, edited는 orderedEdited 순서로,
     * 두 시퀀스가 원본 rows의 displayMode 패턴을 따라 인터리브된 새 List를 반환한다.
     * 결과는 group dense 0..N-1로 할당될 수 있다.
     */
    private List<FavoriteIndicator> interleavePreservingOthersOrder(List<FavoriteIndicator> rows,
                                                                    List<FavoriteIndicator> others,
                                                                    List<FavoriteIndicator> orderedEdited) {
        List<FavoriteIndicator> result = new ArrayList<>(others.size() + orderedEdited.size());
        Iterator<FavoriteIndicator> othersIt = others.iterator();
        Iterator<FavoriteIndicator> editedIt = orderedEdited.iterator();
        FavoriteDisplayMode editedMode = resolveEditedMode(orderedEdited);
        fillByPattern(rows, editedMode, othersIt, editedIt, result);
        drain(editedIt, result);
        drain(othersIt, result);
        return result;
    }

    private FavoriteDisplayMode resolveEditedMode(List<FavoriteIndicator> orderedEdited) {
        return orderedEdited.isEmpty() ? null : orderedEdited.get(0).getDisplayMode();
    }

    private void fillByPattern(List<FavoriteIndicator> rows,
                              FavoriteDisplayMode editedMode,
                              Iterator<FavoriteIndicator> othersIt,
                              Iterator<FavoriteIndicator> editedIt,
                              List<FavoriteIndicator> result) {
        for (FavoriteIndicator r : rows) {
            takeNextSlot(r, editedMode, othersIt, editedIt, result);
        }
    }

    private void takeNextSlot(FavoriteIndicator r,
                             FavoriteDisplayMode editedMode,
                             Iterator<FavoriteIndicator> othersIt,
                             Iterator<FavoriteIndicator> editedIt,
                             List<FavoriteIndicator> result) {
        boolean isEditedSlot = editedMode != null && r.getDisplayMode() == editedMode;
        Iterator<FavoriteIndicator> source = isEditedSlot ? editedIt : othersIt;
        if (source.hasNext()) {
            result.add(source.next());
        }
    }

    private void drain(Iterator<FavoriteIndicator> it, List<FavoriteIndicator> result) {
        while (it.hasNext()) {
            result.add(it.next());
        }
    }

    private Map<Long, Integer> denseAssign(List<FavoriteIndicator> combined) {
        Map<Long, Integer> assignments = new LinkedHashMap<>();
        for (int i = 0; i < combined.size(); i++) {
            assignments.put(combined.get(i).getId(), i);
        }
        return assignments;
    }

    private Map<Long, Integer> filterChanged(List<FavoriteIndicator> rows, Map<Long, Integer> assignments) {
        Map<Long, Integer> diff = new LinkedHashMap<>();
        Map<Long, Integer> currentById = rows.stream()
            .collect(Collectors.toMap(FavoriteIndicator::getId, FavoriteIndicator::getPriority, (a, b) -> a));
        for (Map.Entry<Long, Integer> e : assignments.entrySet()) {
            Integer current = currentById.get(e.getKey());
            if (current == null || !current.equals(e.getValue())) {
                diff.put(e.getKey(), e.getValue());
            }
        }
        return diff;
    }

    /**
     * Post-write invariant: 같은 그룹의 priority가 dense 0..N-1 contiguous인지 검증.
     *
     * <p>Transition window 가드: 3-phase 마이그레이션 phase 1~2 사이(NOT NULL 격상 전)에
     * backfill 실패/일부 진행으로 NULL priority 행이 잔존할 수 있다. 이때 dense 검증을 강행하면
     * 정상 reorder가 false positive로 차단된다. NULL이 발견되면 invariant 검증을 skip하고
     * WARN 로그만 남겨 운영자가 backfill 완료/재시도를 결정하도록 한다.
     */
    private void assertGroupInvariant(Long userId, FavoriteIndicatorSourceType sourceType, int expectedSize) {
        List<Integer> ps = favoriteIndicatorRepository.findPriorities(userId, sourceType);
        if (containsNull(ps)) {
            log.warn("FAVORITE_REORDER_INVARIANT_SKIPPED reason=null-priority-in-transition userId={} sourceType={}",
                userId, sourceType);
            return;
        }
        if (ps.size() != expectedSize) {
            throw new IllegalStateException(
                "priority count mismatch: expected=" + expectedSize + " actual=" + ps.size());
        }
        for (int i = 0; i < ps.size(); i++) {
            if (ps.get(i) != i) {
                throw new IllegalStateException(
                    "priority not dense 0..N-1: index=" + i + " value=" + ps.get(i));
            }
        }
    }

    private boolean containsNull(List<Integer> ps) {
        for (Integer p : ps) {
            if (p == null) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<FavoriteIndicator> findByUserId(Long userId) {
        return favoriteIndicatorRepository.findByUserId(userId);
    }

    /**
     * 관심 지표 + Latest 데이터 통합 조회 (대시보드용)
     * GRAPH 모드 항목들에는 시계열 history 포함.
     */
    @Transactional(readOnly = true)
    public EnrichedFavorites findEnrichedByUserId(Long userId) {
        List<FavoriteIndicator> favorites = favoriteIndicatorRepository.findByUserId(userId);

        List<FavoriteIndicator> ecosFavorites = favorites.stream()
            .filter(f -> f.getSourceType() == FavoriteIndicatorSourceType.ECOS)
            .toList();

        List<FavoriteIndicator> globalFavorites = favorites.stream()
            .filter(f -> f.getSourceType() == FavoriteIndicatorSourceType.GLOBAL)
            .toList();

        List<EnrichedEcosFavorite> enrichedEcos = enrichEcosFavorites(ecosFavorites);
        List<EnrichedGlobalFavorite> enrichedGlobal = enrichGlobalFavorites(globalFavorites);

        List<EnrichedEcosFavorite> withEcosHistory = attachHistoryToEcos(enrichedEcos);
        List<EnrichedGlobalFavorite> withGlobalHistory = attachHistoryToGlobal(enrichedGlobal);

        return new EnrichedFavorites(withEcosHistory, withGlobalHistory);
    }

    /**
     * 단일 indicatorType 에 대한 사용자 관심 지표 재조회.
     * 1) 입력 파싱 (실패 시 IllegalArgumentException → 400)
     * 2) 권한 체크: 해당 indicatorType 관심 지표를 1개 이상 소유해야 함 (없으면 403)
     * 3) 레이트리밋 (user+indicatorType 60s/1회, 초과 시 429)
     * 4) SingleFlight 로 type 단위 락 → 캐시 강제 갱신 (성공 시 put, 실패 시 예외 전파)
     * 5) fresh 스냅샷을 해당 사용자의 관심 카드들에 매핑해 반환
     */
    @Transactional(readOnly = true)
    public List<EnrichedGlobalFavorite> refreshGlobalIndicator(Long userId, GlobalEconomicIndicatorType indicatorType) {
        List<FavoriteIndicator> userGlobalFavorites = favoriteIndicatorRepository
            .findByUserIdAndSourceType(userId, FavoriteIndicatorSourceType.GLOBAL);

        String suffix = "::" + indicatorType.name();
        List<FavoriteIndicator> targeted = userGlobalFavorites.stream()
            .filter(f -> f.getIndicatorCode() != null && f.getIndicatorCode().endsWith(suffix))
            .toList();

        if (targeted.isEmpty()) {
            throw new FavoriteRefreshForbiddenException(
                "해당 지표는 본인의 관심 지표가 아닙니다: " + indicatorType.name());
        }

        if (!refreshRateLimiter.tryAcquire(userId, indicatorType)) {
            throw new RefreshRateLimitExceededException(
                "재조회는 " + indicatorType.name() + " 기준 60초에 한 번만 허용됩니다.");
        }

        List<CountryIndicatorSnapshot> fresh = singleFlightCoordinator.run(
            indicatorType,
            () -> globalIndicatorCacheService.forceRefresh(indicatorType)
        );

        Map<String, CountryIndicatorSnapshot> snapshotMap = new HashMap<>();
        if (fresh != null) {
            for (CountryIndicatorSnapshot snap : fresh) {
                snapshotMap.put(snapshotKey(snap), snap);
            }
        }

        List<EnrichedGlobalFavorite> enriched = new ArrayList<>(targeted.size());
        for (FavoriteIndicator fav : targeted) {
            ParsedGlobalFavorite parsed = ParsedGlobalFavorite.of(fav);
            if (parsed.indicatorType() == null) {
                enriched.add(EnrichedGlobalFavorite.failed(fav, FailureReason.INVALID_CODE, false));
                continue;
            }
            CountryIndicatorSnapshot snap = snapshotMap.get(parsed.countryName() + "::" + indicatorType.name());
            if (snap == null) {
                enriched.add(EnrichedGlobalFavorite.noData(fav));
            } else {
                enriched.add(EnrichedGlobalFavorite.success(fav, snap));
            }
        }

        return enriched;
    }

    private List<EnrichedEcosFavorite> enrichEcosFavorites(List<FavoriteIndicator> ecosFavorites) {
        if (ecosFavorites.isEmpty()) {
            return List.of();
        }
        Map<String, EcosIndicatorLatest> latestMap = ecosIndicatorService.findAllLatest().stream()
            .collect(Collectors.toMap(EcosIndicatorLatest::toCompareKey, l -> l, (a, b) -> a));

        return ecosFavorites.stream()
            .map(fav -> new EnrichedEcosFavorite(fav, latestMap.get(fav.getIndicatorCode()), List.of()))
            .toList();
    }

    /**
     * GLOBAL 관심 지표를 실시간 스크래핑(캐시 경유) 기반으로 enrich.
     * 관심 지표가 속한 카테고리 단위로 조회 후 사용자 favorite에 매칭한다.
     * 카테고리 단위 try/catch 로 부분 실패를 격리한다.
     */
    private List<EnrichedGlobalFavorite> enrichGlobalFavorites(List<FavoriteIndicator> globalFavorites) {
        if (globalFavorites.isEmpty()) {
            return List.of();
        }

        List<ParsedGlobalFavorite> parsed = globalFavorites.stream()
            .map(ParsedGlobalFavorite::of)
            .toList();

        Set<IndicatorCategory> categories = parsed.stream()
            .filter(p -> p.indicatorType() != null)
            .map(p -> p.indicatorType().getCategory())
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(IndicatorCategory.class)));

        // Rev.2: 카테고리 단위 병렬 조회 (bounded executor). thread-safe 구조 사용.
        // Rev.3: 벽시계 timeout 적용 — 초과 시 미완료 카테고리는 FETCH 실패로 강등.
        Map<String, CountryIndicatorSnapshot> snapshotMap = new ConcurrentHashMap<>();
        Map<IndicatorCategory, String> categoryFailure = new ConcurrentHashMap<>();

        Map<IndicatorCategory, CompletableFuture<Void>> categoryFutures = new EnumMap<>(IndicatorCategory.class);
        for (IndicatorCategory category : categories) {
            categoryFutures.put(category, CompletableFuture.runAsync(
                () -> fetchCategoryInto(category, snapshotMap, categoryFailure),
                globalFavoriteFetchExecutor
            ));
        }
        try {
            CompletableFuture.allOf(categoryFutures.values().toArray(CompletableFuture[]::new))
                .get(GlobalFavoriteExecutorConfig.WALL_CLOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            categoryFutures.forEach((category, future) -> {
                if (!future.isDone()) {
                    future.cancel(true);
                    categoryFailure.putIfAbsent(category, FailureReason.FETCH);
                    log.warn("글로벌 카테고리 조회 벽시계 타임아웃: category={}", category);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            categoryFutures.forEach((category, future) -> {
                if (!future.isDone()) {
                    future.cancel(true);
                    categoryFailure.putIfAbsent(category, FailureReason.FETCH);
                }
            });
        } catch (ExecutionException e) {
            // 개별 future 는 자체 try/catch 로 swallow 하므로 여기 도달하려면 RejectedExecutionException 등 비정상 케이스.
            // 미완료 카테고리는 FETCH 로 강등.
            log.error("글로벌 카테고리 병렬 조회 예외", e);
            categoryFutures.forEach((category, future) -> {
                if (!future.isDone()) {
                    future.cancel(true);
                    categoryFailure.putIfAbsent(category, FailureReason.FETCH);
                }
            });
        }

        List<EnrichedGlobalFavorite> result = new ArrayList<>(parsed.size());
        for (ParsedGlobalFavorite p : parsed) {
            if (p.indicatorType() == null) {
                result.add(EnrichedGlobalFavorite.failed(p.favorite(), FailureReason.INVALID_CODE, false));
                continue;
            }
            String failure = categoryFailure.get(p.indicatorType().getCategory());
            if (failure != null) {
                boolean refreshable = !FailureReason.PARSE.equals(failure);
                result.add(EnrichedGlobalFavorite.failed(p.favorite(), failure, refreshable));
                continue;
            }
            CountryIndicatorSnapshot snap = snapshotMap.get(p.countryName() + "::" + p.indicatorType().name());
            if (snap == null) {
                result.add(EnrichedGlobalFavorite.noData(p.favorite()));
            } else {
                result.add(EnrichedGlobalFavorite.success(p.favorite(), snap));
            }
        }
        return result;
    }

    /**
     * GRAPH 모드 ECOS 항목에 한해 시계열을 조회해 history 를 채운 새 리스트 반환.
     * INDICATOR 모드 항목은 history 비어있는 상태로 통과.
     */
    private List<EnrichedEcosFavorite> attachHistoryToEcos(List<EnrichedEcosFavorite> enriched) {
        if (enriched.isEmpty()) {
            return enriched;
        }
        return enriched.stream()
            .map(this::attachEcosHistoryIfGraph)
            .toList();
    }

    private EnrichedEcosFavorite attachEcosHistoryIfGraph(EnrichedEcosFavorite item) {
        if (item.favorite().getDisplayMode() != FavoriteDisplayMode.GRAPH) {
            return item;
        }
        String[] parts = item.favorite().getIndicatorCode().split("::", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return item;
        }
        List<EcosIndicator> rows = ecosIndicatorService.findHistory(parts[0], parts[1], HISTORY_LIMIT);
        List<HistoryPoint> points = rows.stream()
            .map(r -> new HistoryPoint(r.getSnapshotDate(), r.getDataValue()))
            .toList();
        return item.withHistory(points);
    }

    /**
     * GRAPH 모드 GLOBAL 항목에 한해 시계열을 조회해 history 를 채운 새 리스트 반환.
     */
    private List<EnrichedGlobalFavorite> attachHistoryToGlobal(List<EnrichedGlobalFavorite> enriched) {
        if (enriched.isEmpty()) {
            return enriched;
        }
        return enriched.stream()
            .map(this::attachGlobalHistoryIfGraph)
            .toList();
    }

    private EnrichedGlobalFavorite attachGlobalHistoryIfGraph(EnrichedGlobalFavorite item) {
        if (item.favorite().getDisplayMode() != FavoriteDisplayMode.GRAPH) {
            return item;
        }
        ParsedGlobalFavorite parsed = ParsedGlobalFavorite.of(item.favorite());
        if (parsed.indicatorType() == null) {
            return item;
        }
        List<GlobalIndicator> rows = globalIndicatorQueryService.findHistory(
            parsed.countryName(), parsed.indicatorType(), HISTORY_LIMIT);
        List<HistoryPoint> points = rows.stream()
            .map(r -> new HistoryPoint(r.getSnapshotDate(), r.getDataValue()))
            .toList();
        return item.withHistory(points);
    }

    private static String snapshotKey(CountryIndicatorSnapshot snap) {
        return snap.getCountryName() + "::" + snap.getIndicatorType().name();
    }

    /**
     * 카테고리 단위 조회 결과를 공유 맵에 적재. 실패는 {@code categoryFailure} 에 사유 코드로 기록한다.
     * {@link CompletableFuture#runAsync} 에서 호출되므로 예외는 자체 catch 해 swallow 한다.
     */
    private void fetchCategoryInto(IndicatorCategory category,
                                   Map<String, CountryIndicatorSnapshot> snapshotMap,
                                   Map<IndicatorCategory, String> categoryFailure) {
        try {
            globalIndicatorQueryService.getIndicatorsByCategory(category)
                .values().stream()
                .flatMap(List::stream)
                .forEach(snap -> snapshotMap.put(snapshotKey(snap), snap));
        } catch (TradingEconomicsFetchException e) {
            log.error("글로벌 카테고리 조회 실패(FETCH): category={}", category, e);
            categoryFailure.put(category, FailureReason.FETCH);
        } catch (TradingEconomicsParseException e) {
            log.error("글로벌 카테고리 조회 실패(PARSE): category={}", category, e);
            categoryFailure.put(category, FailureReason.PARSE);
        }
    }

    /**
     * indicatorCode "countryName::IndicatorType" 파싱. stale/잘못된 enum 은 indicatorType=null 로 격리.
     */
    private record ParsedGlobalFavorite(FavoriteIndicator favorite, String countryName, GlobalEconomicIndicatorType indicatorType) {
        static ParsedGlobalFavorite of(FavoriteIndicator favorite) {
            String[] parts = favorite.getIndicatorCode().split("::", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                return new ParsedGlobalFavorite(favorite, parts.length > 0 ? parts[0] : "", null);
            }
            try {
                return new ParsedGlobalFavorite(favorite, parts[0], GlobalEconomicIndicatorType.valueOf(parts[1]));
            } catch (IllegalArgumentException e) {
                return new ParsedGlobalFavorite(favorite, parts[0], null);
            }
        }
    }

    public static final class FailureReason {
        public static final String FETCH = "FETCH";
        public static final String PARSE = "PARSE";
        public static final String INVALID_CODE = "INVALID_CODE";
        private FailureReason() {}
    }

    public record HistoryPoint(LocalDate snapshotDate, String dataValue) {}

    public record EnrichedEcosFavorite(FavoriteIndicator favorite,
                                       EcosIndicatorLatest latest,
                                       List<HistoryPoint> history) {
        public EnrichedEcosFavorite withHistory(List<HistoryPoint> newHistory) {
            return new EnrichedEcosFavorite(favorite, latest, newHistory);
        }
    }

    public record EnrichedGlobalFavorite(
        FavoriteIndicator favorite,
        CountryIndicatorSnapshot snapshot,
        String failureReason,
        boolean refreshable,
        List<HistoryPoint> history
    ) {
        public static EnrichedGlobalFavorite success(FavoriteIndicator favorite, CountryIndicatorSnapshot snapshot) {
            return new EnrichedGlobalFavorite(favorite, snapshot, null, true, List.of());
        }
        public static EnrichedGlobalFavorite noData(FavoriteIndicator favorite) {
            return new EnrichedGlobalFavorite(favorite, null, null, true, List.of());
        }
        public static EnrichedGlobalFavorite failed(FavoriteIndicator favorite, String failureReason, boolean refreshable) {
            return new EnrichedGlobalFavorite(favorite, null, failureReason, refreshable, List.of());
        }
        public EnrichedGlobalFavorite withHistory(List<HistoryPoint> newHistory) {
            return new EnrichedGlobalFavorite(favorite, snapshot, failureReason, refreshable, newHistory);
        }
        public boolean isFailed() {
            return failureReason != null;
        }
    }

    public record EnrichedFavorites(List<EnrichedEcosFavorite> ecos, List<EnrichedGlobalFavorite> global) {}
}
