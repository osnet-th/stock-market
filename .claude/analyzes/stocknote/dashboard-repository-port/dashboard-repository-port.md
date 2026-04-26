# [stocknote] DashboardService EntityManager 직접 사용 — 포트-어댑터 위반

> ce-review 2026-04-25 P1 #8 (project-standards). plan task: Phase 10 P1.

## 현재 상태

`StockNoteDashboardService.java:31-33` (application 계층) 가 `@PersistenceContext EntityManager em` 직접 주입.

```java
@PersistenceContext
private final EntityManager em;
```

5개 메서드가 모두 `em.createQuery(JPQL)` / `em.createNativeQuery(SQL)` 직접 호출:
- `countThisMonth` — JPQL `SELECT COUNT(n) FROM StockNoteEntity ...`
- `aggregateHitRate` — JPQL JOIN `StockNoteVerificationEntity` × `StockNoteEntity`
- `countPendingVerification` — JPQL NOT EXISTS subquery
- `aggregateCharacterDistribution` — JPQL `StockNoteTagEntity` GROUP BY
- `aggregateTopTagCombos` — Native SQL (PostgreSQL `STRING_AGG`)

## ARCHITECTURE.md 위반

**Section 4 — application**: "유스케이스 구현, @Transactional 경계 / 여러 도메인 조합, domain 서비스 호출"
**Section 4 — infrastructure**: "DB, 외부 API, 인프라 연동 구현 (어댑터) / domain.repository 인터페이스 구현"
**Section 5 — 포트-어댑터 패턴**: "domain에 인터페이스(포트), infrastructure에 구현체(어댑터)"

application 계층의 클래스가 **`StockNoteEntity`, `StockNoteVerificationEntity`, `StockNoteTagEntity`** (모두 infrastructure/persistence) 를 직접 참조 + native SQL 작성 → 명백한 레이어 경계 위반.

## 부수 위반

### `@PersistenceContext + final + @RequiredArgsConstructor` 모순

```java
@Service @RequiredArgsConstructor
public class StockNoteDashboardService {
    @PersistenceContext
    private final EntityManager em;   // ← @PersistenceContext 가 무력화됨
}
```

final 필드는 Lombok 생성자로만 주입 — `@PersistenceContext` 어노테이션은 실제로 동작 안 함. 동일 도메인 RepositoryImpl 두 곳은 `@PersistenceContext private EntityManager em;` (final 미부착, 필드 주입) 패턴이라 일관성도 깨짐.

## 영향 범위

| 항목 | 현재 | 위반 |
|---|---|---|
| application → infrastructure Entity 참조 | StockNoteEntity 등 직접 import | 레이어 경계 |
| application 에서 SQL/JPQL 작성 | em.createQuery + em.createNativeQuery | 단일 책임 |
| application 에서 PostgreSQL 종속 함수(STRING_AGG) | aggregateTopTagCombos | DB 결합도 application 으로 누수 |
| `@PersistenceContext` annotation 무력화 | final + 생성자 주입 | Lombok 패턴 일관성 |

## 해결 옵션

### 옵션 A — domain 포트 + infrastructure 어댑터 (권장)

`domain/repository/StockNoteDashboardRepository` 신설 (메서드 5개) + `infrastructure/persistence/StockNoteDashboardRepositoryImpl` (JPQL/Native SQL 보유).

| 장점 | 단점 |
|---|---|
| ARCHITECTURE.md 정합 | 신규 파일 2개 + DTO projection (DashboardResult.HitRate / TagComboEntry 가 인프라에서도 import 필요) |
| application 단순화 | |
| 기존 RepositoryImpl 패턴과 일관 | |

### 옵션 B — 기존 Repository 4개에 분산

countThisMonth → StockNoteRepository, aggregateHitRate → StockNoteVerificationRepository, etc.

| 장점 | 단점 |
|---|---|
| 신규 빈 추가 없음 | 단일 화면(대시보드) 의 쿼리가 4개 빈에 분산 → 응집도 저하 |
| | 기존 Repository 가 KPI 집계 책임까지 떠안음 |

### 옵션 C — 현재 구조 유지 + javadoc 으로 예외 사유 명시

| 장점 | 단점 |
|---|---|
| 변경 범위 0 | ARCHITECTURE.md 위반 그대로 |

## 추천: 옵션 A

근거:
- ARCHITECTURE.md 의 포트-어댑터 패턴 명시적 준수
- 단일 화면(대시보드) 쿼리들이 한 어댑터에 응집
- 기존 RepositoryImpl 패턴과 일관

## 코드 위치

| 파일 | 변경 |
|---|---|
| `domain/repository/StockNoteDashboardRepository.java` | 신규 — 포트 인터페이스 (메서드 5개) |
| `infrastructure/persistence/StockNoteDashboardRepositoryImpl.java` | 신규 — JPQL/Native SQL 어댑터 |
| `application/StockNoteDashboardService.java` | EntityManager 의존 제거 → DashboardRepository 주입. 5 메서드 호출만 위임 + 결과 조립 |
| `application/dto/DashboardResult.java` | 변경 없음 (HitRate/TagComboEntry 는 그대로 사용) |

## 도메인 객체 vs DTO 결정

집계 결과(HitRate, TagComboEntry, characterDistribution Map) 는 **도메인 모델이 아닌 application DTO 의 일부**. 어댑터에서 JPQL/Native 결과를 application DTO 에 직접 채워서 반환 → application 은 조립만.

선택:
- **A1**: 어댑터가 `DashboardResult.HitRate` 등 application DTO 를 직접 반환. application/dto 가 인프라에서 import 됨 — DDD 정통은 아니지만 실용적.
- **A2**: 어댑터가 도메인 model 의 새 record 반환 (예: `HitRateAggregate`) 후 application 이 DashboardResult.HitRate 로 변환. 변환 한 단계 추가, 도메인 모델 수 증가.

추천: **A1** — application DTO 는 외부 노출 contract 가 아니므로 인프라가 의존해도 누수 없음. (presentation DTO 가 외부 contract 책임.)

## 후속 task 와의 관계

| Task | 정합 |
|---|---|
| #13 P1 DashboardResponse contract drift | 본 task 와 독립 (presentation DTO 변경) |
| #48 P2 dashboard Caffeine cacheManager mismatch | 본 task 와 독립 (캐시 매니저 일관성 검증) |

## 설계 문서

[dashboard-repository-port](../../../designs/stocknote/dashboard-repository-port/dashboard-repository-port.md)