# [stocknote] StockNoteDashboardRepository 포트-어댑터 분리 (옵션 A1)

> 분석: [dashboard-repository-port](../../../analyzes/stocknote/dashboard-repository-port/dashboard-repository-port.md). plan task: Phase 10 P1 #8.

## 의도

application 의 `StockNoteDashboardService` 에서 EntityManager 직접 사용을 제거하고, domain 포트 + infrastructure 어댑터로 분리. JPQL/Native SQL 5건을 어댑터로 이전. `DashboardResult.HitRate / TagComboEntry` 는 application DTO 그대로 어댑터 반환 타입에 사용 (외부 contract 가 아니라 누수 없음).

## 변경 사항

### 1. 신규 도메인 포트 — `StockNoteDashboardRepository`

위치: `stocknote/domain/repository/StockNoteDashboardRepository.java`

```java
package com.thlee.stock.market.stockmarket.stocknote.domain.repository;

import com.thlee.stock.market.stockmarket.stocknote.application.dto.DashboardResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 KPI 집계 전용 Repository 포트. 일반 CRUD 가 아닌 화면 단위 집계 쿼리만 보유.
 */
public interface StockNoteDashboardRepository {

    /** 사용자의 [from, to] 범위 noteDate 기록 수. */
    long countByUserAndNoteDateBetween(Long userId, LocalDate from, LocalDate to);

    /** 검증 결과 분포 — CORRECT/WRONG/PARTIAL/total. */
    DashboardResult.HitRate aggregateHitRate(Long userId);

    /** 검증되지 않은 기록 수. */
    long countPendingVerification(Long userId);

    /** CHARACTER 태그 분포 — value → count, count DESC. */
    Map<String, Long> aggregateCharacterDistribution(Long userId);

    /** 상위 N 개 태그 조합 — combo (List<source::value>) + count DESC. */
    List<DashboardResult.TagComboEntry> aggregateTopTagCombos(Long userId, int limit);
}
```

> **레이어 의존 검토**: domain → application DTO 참조. 일반적으론 domain 이 application 을 의존하면 안 되지만, `DashboardResult` 가 외부 contract 가 아닌 화면별 집계 결과라 실용적 타협. 변경 비용 < 도메인 record 새로 만드는 비용.

### 2. 신규 어댑터 — `StockNoteDashboardRepositoryImpl`

위치: `stocknote/infrastructure/persistence/StockNoteDashboardRepositoryImpl.java`

```java
@Repository
public class StockNoteDashboardRepositoryImpl implements StockNoteDashboardRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public long countByUserAndNoteDateBetween(Long userId, LocalDate from, LocalDate to) {
        Query q = em.createQuery(
                "SELECT COUNT(n) FROM StockNoteEntity n WHERE n.userId = :u AND n.noteDate BETWEEN :f AND :t");
        q.setParameter("u", userId);
        q.setParameter("f", from);
        q.setParameter("t", to);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public DashboardResult.HitRate aggregateHitRate(Long userId) {
        Query q = em.createQuery(
                "SELECT v.judgmentResult, COUNT(v) FROM StockNoteVerificationEntity v "
                        + "JOIN StockNoteEntity n ON n.id = v.noteId "
                        + "WHERE n.userId = :u GROUP BY v.judgmentResult");
        q.setParameter("u", userId);
        long correct = 0, wrong = 0, partial = 0;
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            String r = row[0] == null ? null : row[0].toString();
            long count = ((Number) row[1]).longValue();
            switch (r) {
                case "CORRECT" -> correct = count;
                case "WRONG" -> wrong = count;
                case "PARTIAL" -> partial = count;
                default -> { /* ignore */ }
            }
        }
        return new DashboardResult.HitRate(correct, wrong, partial, correct + wrong + partial);
    }

    @Override
    public long countPendingVerification(Long userId) {
        Query q = em.createQuery(
                "SELECT COUNT(n) FROM StockNoteEntity n "
                        + "WHERE n.userId = :u AND NOT EXISTS ("
                        + "  SELECT 1 FROM StockNoteVerificationEntity v WHERE v.noteId = n.id)");
        q.setParameter("u", userId);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Long> aggregateCharacterDistribution(Long userId) {
        Query q = em.createQuery(
                "SELECT t.tagValue, COUNT(t) FROM StockNoteTagEntity t "
                        + "WHERE t.userId = :u AND t.tagSource = 'CHARACTER' "
                        + "GROUP BY t.tagValue ORDER BY COUNT(t) DESC");
        q.setParameter("u", userId);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            result.put((String) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DashboardResult.TagComboEntry> aggregateTopTagCombos(Long userId, int limit) {
        Query q = em.createNativeQuery("""
                SELECT combo_key, COUNT(*) AS cnt
                FROM (
                    SELECT t.note_id,
                           STRING_AGG(t.tag_source || '::' || t.tag_value, '|'
                                      ORDER BY t.tag_source, t.tag_value) AS combo_key
                    FROM stock_note_tag t
                    WHERE t.user_id = :userId
                    GROUP BY t.note_id
                ) grouped
                GROUP BY combo_key
                ORDER BY cnt DESC
                LIMIT :limit
                """);
        q.setParameter("userId", userId);
        q.setParameter("limit", limit);
        List<DashboardResult.TagComboEntry> entries = new ArrayList<>();
        for (Object row : q.getResultList()) {
            Object[] r = (Object[]) row;
            String comboKey = (String) r[0];
            long count = ((Number) r[1]).longValue();
            entries.add(new DashboardResult.TagComboEntry(splitCombo(comboKey), count));
        }
        return entries;
    }

    private List<String> splitCombo(String comboKey) {
        if (comboKey == null) {
            return List.of();
        }
        return List.of(comboKey.split("\\|"));
    }
}
```

### 3. `StockNoteDashboardService` 슬림화

```java
@Service
@RequiredArgsConstructor
public class StockNoteDashboardService {

    private static final int TOP_TAG_COMBO_LIMIT = 5;

    private final StockNoteDashboardRepository dashboardRepository;

    @Cacheable(
            cacheNames = StocknoteCacheConfig.DASHBOARD_CACHE_NAME,
            cacheManager = "stocknoteCacheManager",
            key = "#userId",
            sync = true
    )
    @Transactional(readOnly = true)
    public DashboardResult getDashboard(Long userId) {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate lastOfMonth = firstOfMonth.plusMonths(1).minusDays(1);

        long thisMonthCount = dashboardRepository.countByUserAndNoteDateBetween(userId, firstOfMonth, lastOfMonth);
        DashboardResult.HitRate hitRate = dashboardRepository.aggregateHitRate(userId);
        long pendingVerificationCount = dashboardRepository.countPendingVerification(userId);
        Map<String, Long> characterDistribution = dashboardRepository.aggregateCharacterDistribution(userId);
        List<DashboardResult.TagComboEntry> topCombos =
                dashboardRepository.aggregateTopTagCombos(userId, TOP_TAG_COMBO_LIMIT);

        return new DashboardResult(
                thisMonthCount,
                hitRate.total(),
                pendingVerificationCount,
                hitRate,
                characterDistribution,
                topCombos
        );
    }
}
```

EntityManager / JPQL / Native SQL 모두 제거. 의존: `StockNoteDashboardRepository` 1개.

## 변경 동작

기존과 동일. 단지 어댑터 분리만으로 SQL/JPQL 위치만 이동.

## 회귀 위험

| 위험 | 영향 | 완화 |
|---|---|---|
| Spring Data JPA `@Repository` + EntityManager 주입 동작 검증 | 타 RepositoryImpl 패턴과 동일 (StockNoteRepositoryImpl, StockNoteTagRepositoryImpl 검증된 패턴) | n/a |
| domain 패키지가 application/dto 참조 — 의존 방향 일부 위배 | 실용적 타협, javadoc 으로 사유 명시 | A2 옵션(도메인 record 추가) 으로 후속 정리 가능 |
| 캐시 동작 (`@Cacheable` + key=userId + sync=true) | 변경 없음 | n/a |

## 작업 리스트

- [ ] `domain/repository/StockNoteDashboardRepository.java` 신설
- [ ] `infrastructure/persistence/StockNoteDashboardRepositoryImpl.java` 신설 (JPQL/Native SQL 5건 이전)
- [ ] `application/StockNoteDashboardService.java` 슬림화 (EntityManager 제거 + 의존 변경)
- [ ] 컴파일 확인
- [ ] plan checkbox 갱신 (P1 #8)

## 승인 대기

태형님 승인 후 구현 진행.