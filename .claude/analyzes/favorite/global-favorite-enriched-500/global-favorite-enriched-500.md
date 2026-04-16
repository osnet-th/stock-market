# 글로벌 경제지표 관심 조회 시 500 INTERNAL_ERROR

## 현상

글로벌(GLOBAL) 경제지표를 관심(★) 등록한 사용자가 홈 대시보드에 진입하면 `GET /api/favorites/enriched` 가 HTTP 500 을 반환한다.

- 응답 본문:
  ```json
  {
    "timestamp": "2026-04-16T06:03:25.109955899",
    "error": "INTERNAL_ERROR",
    "message": "서버 내부 오류가 발생했습니다."
  }
  ```
- 국내(ECOS) 관심만 등록된 사용자는 동일 엔드포인트가 정상 동작.
- 프런트는 `Promise.allSettled` 덕에 대시보드 자체가 깨지지는 않으나 관심 지표 카드 영역이 비어 표시됨.

## 진단을 가로막는 메타 문제

`src/main/java/.../infrastructure/web/GlobalExceptionHandler.java:99-102` 의 `handleGeneral(Exception e)` 이 예외를 **어떤 logger 로도 남기지 않고** 삼켜 일반 메시지만 반환한다.

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");
}
```

→ 서버 콘솔/파일 로그 어느 쪽에도 stacktrace 가 남지 않아 원인 확정이 불가능하며, 이로 인해 아래 원인 분석은 모두 가설 수준에 머문다.

## 원인 확정 (2026-04-16 추가)

**가설 B 적중**. 서버 로그 stacktrace 로 원인 확정:

```
org.springframework.dao.InvalidDataAccessApiUsageException:
  No enum constant com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType.MILITARY_EXPENDITURE
  at java.base/java.lang.Enum.valueOf(Unknown Source)
  at org.hibernate.type.descriptor.java.EnumJavaType.fromName(EnumJavaType.java:262)
  at GlobalIndicatorLatestRepositoryImpl.findAll(GlobalIndicatorLatestRepositoryImpl.java:22)
  at GlobalIndicatorSaveService.fetchAndSave(GlobalIndicatorSaveService.java:51)   // 배치 경로
  // — 동일 findAll() 이 FavoriteIndicatorService.findEnrichedByUserId():83 에서도 호출됨 —
```

### 확정 근거

- `global_indicator_latest.indicator_type` 컬럼에 **현재 enum 에 없는 `MILITARY_EXPENDITURE` 문자열이 저장됨** (과거에 존재했다가 제거/rename 된 것으로 추정).
- `@Enumerated(EnumType.STRING)` 로 매핑된 복합 PK 필드를 Hibernate 가 하이드레이션할 때 `Enum.valueOf(..., "MILITARY_EXPENDITURE")` → `IllegalArgumentException` → Spring `InvalidDataAccessApiUsageException` 으로 wrapping 되어 `handleGeneral` 로 떨어짐 → HTTP 500.
- `GlobalIndicatorBatchScheduler` 의 배치도 동일한 `findAll()` 을 호출하므로 배치 전체가 **매번 실패** → `global_indicator_latest` 에 새 데이터가 시딩되지 않음.

### 두 증상이 동일 원인에서 파생

| 증상 | 경로 | 결과 |
|---|---|---|
| `/api/favorites/enriched` 500 | `FavoriteIndicatorService.findEnrichedByUserId` → `GlobalIndicatorQueryService.findAllLatest` → `findAll()` | hydration IAE → 500 |
| `hasData: false` 빈 카드 | (500 이 일시 회피된 상황에서) 배치 미동작으로 PRODUCER_PRICES row 부재 | latestMap.get() miss → `latest == null` 브랜치 |

→ 근본 원인은 **stale enum row 1건**. 이를 제거하면 500 과 hasData=false 가 동시에 해결된다.

### 기각된 가설

- **가설 A (toCompareKey NPE)**: `indicator_type` 컬럼이 `NOT NULL` 이므로 null 은 아니었다.
- **가설 C (스키마 미동기)**: 스키마 자체는 정상, 데이터 내용만 문제였다.
- **가설 D (직렬화 실패)**: 응답 생성 전 hydration 단계에서 실패했다.
- **Jackson 바인딩 실패 해석**: 일시적으로 앱 기동을 막은 별개 이슈였으며, 500 의 직접 원인은 아니었다. Jackson 블록 제거 후에도 500 이 지속되다가 cleanup 으로 해결되는 것을 관측할 것으로 예상.

---

## 원인 후보 (초기 가설, 참고용)

### 가설 A — `GlobalIndicatorLatest.toCompareKey()` NPE ⭐ 최유력

- 위치: `src/main/java/.../economics/domain/model/GlobalIndicatorLatest.java:56-58`
  ```java
  public String toCompareKey() {
      return countryName + "::" + indicatorType.name();
  }
  ```
- 호출부: `src/main/java/.../favorite/application/FavoriteIndicatorService.java:83-84`
  ```java
  Map<String, GlobalIndicatorLatest> latestMap = globalIndicatorQueryService.findAllLatest().stream()
      .collect(Collectors.toMap(GlobalIndicatorLatest::toCompareKey, l -> l, (a, b) -> a));
  ```
- `global_indicator_latest` 에 `indicator_type = NULL` 인 row 가 단 한 건이라도 존재하면 `null.name()` 으로 NPE → `@RestControllerAdvice` → `handleGeneral` → 500.
- ECOS 는 `className`, `keystatName` 모두 String 이라 동일 NPE 가능성 없음 → **ECOS ↔ GLOBAL 비대칭 증상과 정확히 부합**.

### 가설 B — JPA Entity 하이드레이션 시 enum 매핑 실패

- `src/main/java/.../economics/infrastructure/persistence/GlobalIndicatorLatestEntity.java:24-27`
  ```java
  @Enumerated(EnumType.STRING)
  @Column(name = "indicator_type", nullable = false, length = 100)
  private GlobalEconomicIndicatorType indicatorType;
  ```
- DB 의 `indicator_type` 값이 현재 `GlobalEconomicIndicatorType` 에 존재하지 않는 상수 문자열이면 하이드레이션 시 `IllegalArgumentException` 발생.
- 보통 `IllegalArgumentException` 은 `handleIllegalArgument` 가 400 으로 변환하나, Hibernate 가 `PersistenceException` 등으로 **wrapping** 하면 400 경로를 우회하여 일반 Exception 핸들러로 떨어져 500 이 된다.
- **DDL 모드가 `update`** 이므로(아래 참조) 과거 enum rename/삭제 이후에도 DB 행이 잔존할 수 있어 가능성이 낮지 않다.

참고 설정:
- `src/main/resources/application-dev.yml:22` → `ddl-auto: update`
- `src/main/resources/application-prod.yml:34` → `ddl-auto: update`

### 가설 C — 테이블/스키마 미동기

- 자동 DDL update 특성상 컬럼 추가는 되어도 컬럼 타입/길이/제약 변경은 누락되기 쉽다.
- `SELECT` 자체가 실패하면 `JdbcSQLSyntaxErrorException` 계열 → 500.
- 다만 같은 테이블을 쓰는 `GlobalIndicatorSaveService.fetchAndSave()` 배치가 현재 정상 가동 중이라면 가능성 낮음.

### 가설 D — 응답 직렬화 실패 (낮음)

- `EnrichedFavoriteResponse` 는 record + primitive/String 만 사용해 실패 가능성 낮음.
- 단, `src/main/java/.../favorite/presentation/dto/EnrichedFavoriteResponse.java:84` 에서 두 번째 `latest.getIndicatorType().name()` 호출이 있어, **가설 A 를 데이터 정리만으로 봉합할 경우에도 잠재 위험이 남는다**.

## 영향 범위

- `/api/favorites/enriched` 엔드포인트를 호출하는 홈 대시보드 진입 사용자 중 **글로벌 관심을 1건이라도 등록한 사용자 전체**.
- `FavoriteIndicatorService.findEnrichedByUserId` 의 map 빌드 실패이므로, ECOS + GLOBAL 관심을 함께 가진 사용자도 ECOS 부분마저 표시되지 않음 (전체 응답 실패).
- 관심 등록(POST) 및 해제(DELETE) 는 같은 예외 경로를 타지 않아 영향 없음.

## 코드 위치 요약

| 목적 | 경로 |
|---|---|
| 문제 엔드포인트 | `src/main/java/.../favorite/presentation/FavoriteIndicatorController.java:67-72` |
| enriched 빌드 | `src/main/java/.../favorite/application/FavoriteIndicatorService.java:54-95` |
| 글로벌 최신값 조회 | `src/main/java/.../economics/application/GlobalIndicatorQueryService.java:66-69` |
| JPA 저장소 | `src/main/java/.../economics/infrastructure/persistence/GlobalIndicatorLatestRepositoryImpl.java:20-25` |
| 일차 NPE 후보 | `src/main/java/.../economics/domain/model/GlobalIndicatorLatest.java:56-58` |
| 이차 NPE 후보 | `src/main/java/.../favorite/presentation/dto/EnrichedFavoriteResponse.java:84` |
| JPA Entity enum 컬럼 | `src/main/java/.../economics/infrastructure/persistence/GlobalIndicatorLatestEntity.java:24-27` |
| 로깅 부재 핸들러 | `src/main/java/.../infrastructure/web/GlobalExceptionHandler.java:99-102` |
| 유사 enum.name() 패턴 | `src/main/java/.../economics/application/GlobalIndicatorSaveService.java:180` |

## 기타 컨텍스트

- 코드베이스 실사용 로거 컨벤션은 **Lombok `@Slf4j`** (예: `GlobalIndicatorSaveService.java:11`). CLAUDE.md 의 `BigxLogger` 표기와 불일치 — 별도 이슈.
- DB 는 **PostgreSQL** (`application.yml:19` `org.hibernate.dialect.PostgreSQLDialect`). CLAUDE.md 의 MariaDB 표기와 불일치 — 별도 이슈.

## 관련 문서

- 브레인스토밍: `docs/brainstorms/2026-04-16-global-favorite-enriched-500-brainstorm.md`
- 플랜: `docs/plans/2026-04-16-002-fix-global-favorite-enriched-500-plan.md`
- 원 기능 plan: `docs/plans/2026-04-15-002-feat-favorite-indicator-dashboard-plan.md`