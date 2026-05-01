# 관심지표 표시 모드 토글 (그래프 ↔ 지표) 설계

> 관련 이슈: [#38 관심지표 그래프 보기](https://github.com/osnet-th/stock-market/issues/38)

## 작업 리스트

### Domain 계층 (favorite)
- [x] `FavoriteDisplayMode` enum 신규 작성 (INDICATOR, GRAPH)
- [x] `FavoriteIndicator` 도메인 모델에 `displayMode` 필드 추가
- [x] `FavoriteIndicator.changeDisplayMode(FavoriteDisplayMode)` 도메인 메서드 추가
- [x] `FavoriteIndicatorRepository` 포트에 `updateDisplayMode(...)` 메서드 추가

### Infrastructure 계층 (favorite)
- [x] `UserFavoriteIndicatorEntity`에 `display_mode` 컬럼 추가 — `@Enumerated(EnumType.STRING)` + `columnDefinition = "VARCHAR(10) NOT NULL DEFAULT 'INDICATOR'"` **— Entity 변경 사전 승인 완료**
- [x] `FavoriteIndicatorMapper`: Entity ↔ Domain displayMode 매핑 추가
- [x] `FavoriteIndicatorRepositoryImpl.updateDisplayMode(...)` 구현
- [x] `UserFavoriteIndicatorJpaRepository.updateDisplayMode(...)` `@Modifying` 쿼리 추가

### Economics 모듈 신규 history 조회 메서드 추가 (favorite 가 활용)
- [x] `EcosIndicatorRepository.findHistory(className, keystatName, limit)` 포트 추가
- [x] `EcosIndicatorJpaRepository`에 native ROW_NUMBER 쿼리로 `findHistory(...)` 추가
- [x] `EcosIndicatorRepositoryImpl.findHistory(...)` 구현 (Entity → Domain 매핑)
- [x] `EcosIndicatorService.findHistory(className, keystatName, limit)` 위임 메서드 추가
- [x] `GlobalIndicatorRepository.findHistory(countryName, indicatorType, limit)` 포트 추가
- [x] `GlobalIndicatorJpaRepository.findHistory(...)` native ROW_NUMBER 쿼리 추가
- [x] `GlobalIndicatorRepositoryImpl.findHistory(...)` 구현
- [x] `GlobalIndicatorQueryService.findHistory(countryName, indicatorType, limit)` 위임 메서드 추가

### Application 계층 (favorite)
- [x] `FavoriteIndicatorService.changeDisplayMode(userId, sourceType, indicatorCode, mode)` 메서드 추가
- [x] `findEnrichedByUserId(userId)` 확장: GRAPH 모드 항목에 한해 시계열을 함께 모아 응답에 포함
- [x] EnrichedEcosFavorite/EnrichedGlobalFavorite 레코드에 `history: List<HistoryPoint>` 추가
- [x] indicatorCode 파싱하여 economics 모듈 신규 `findHistory(...)` 호출 (GRAPH 항목별 단건 조회)

### Presentation 계층
- [x] `PUT /api/favorites/display-mode` API 신규 추가
- [x] `FavoriteDisplayModeRequest` DTO 신규 작성
- [x] `EnrichedFavoriteResponse`의 EcosItem/GlobalItem에 `displayMode`, `history` 필드 추가
- [x] `EnrichedHistoryPoint` DTO 신규 작성 (snapshotDate, dataValue)

### Frontend 계층
- [x] `index.html` 대시보드: "그래프 영역"과 "단순 지표 영역" 카드 컨테이너 분리
- [x] `favorite.js`: 항목별 displayMode에 따라 두 영역에 분기 렌더
- [x] 카드별 "지표/그래프" 토글 버튼 UI 추가
- [x] 토글 시 `PUT /api/favorites/display-mode` 호출 후 화면 재배치
- [x] Chart.js 라인 차트 렌더링 함수 작성 (`renderFavoriteChart(canvasId, points)`)
- [x] 차트 라이프사이클 관리 (Chart 인스턴스 destroy → re-render)

---

## 배경

이슈 #38 요구사항:
1. 관심지표 항목별로 **그래프 / 단순 지표** 표시 방식 선택
2. 마지막 선택값은 **DB 저장**으로 영속화
3. 대시보드에서 **그래프 영역과 단순 지표 영역을 분리**

현재 `/api/favorites/enriched`는 최신값만 반환, 프론트는 단순 카드로만 표시.

**[설계 변경]** 초기 설계에서 활용하려던 `EcosIndicatorService.getHistoryByCategory()` / `GlobalIndicatorQueryService.getHistoryByIndicatorType()` 두 메서드는 **`(className, keystatName, cycle)` 또는 `(country, cycle)` 조합당 최신 1행만 반환**하는 것으로 확인됨 (cycle별 deduplicate). 시계열 그래프용 데이터가 아니므로 economics 모듈에 **신규 history 조회 메서드**를 추가한다.

---

## 핵심 결정

- **표시 모드 식별 단위**: `(userId, sourceType, indicatorCode)` 단위로 displayMode 저장 (기존 unique 키와 동일).
- **enum 값**: `INDICATOR`(기본), `GRAPH` 두 가지.
- **DB default 처리**: `@Column(columnDefinition = "VARCHAR(10) NOT NULL DEFAULT 'INDICATOR'")` + `ddl-auto: update`로 ALTER TABLE 자동 처리. 별도 SQL 마이그레이션 불필요.
- **시계열 데이터 전달 방식**: `/api/favorites/enriched` 응답에 history 배열을 함께 담음. **GRAPH 모드 항목에만** 포함 → 트래픽 절감.
- **시계열 길이**: 우선 최근 30포인트.
- **시계열 조회 방식 (변경)**: GRAPH 항목별로 economics 모듈의 신규 `findHistory(...)`를 단건 호출. 사용자 1인의 GRAPH 항목 수가 수 개 ~ 수십 개 수준이고 limit가 작아 부하 미미. 추후 N+1 영향 발생 시 batch IN 쿼리로 전환.
- **economics history 쿼리**: PostgreSQL native + ROW_NUMBER로 `(className, keystatName)` 또는 `(countryName, indicatorType)` 조합당 최근 N개 snapshotDate desc 반환.
- **프론트 영역 분리**: 한 화면에 두 섹션(`#favoriteGraphSection`, `#favoriteIndicatorSection`)을 두고, 카드 단위 분배.
- **토글 UX**: 카드 우상단 토글 버튼. 즉시 PUT 호출 → 카드를 반대편 섹션으로 이동.

---

## 구현

### Domain 계층 (favorite)

**위치**: `favorite/domain/model/`, `favorite/domain/repository/`

- `FavoriteDisplayMode.java` — enum (INDICATOR, GRAPH)
- `FavoriteIndicator.java` — `displayMode` 필드 + `changeDisplayMode()` 메서드
- `FavoriteIndicatorRepository.java` — `updateDisplayMode(...)` 포트 메서드

### Infrastructure 계층 (favorite)

**Entity**: `UserFavoriteIndicatorEntity` — `display_mode` 컬럼 추가, `columnDefinition`으로 default 처리, `@PrePersist` null 안전 처리.

**JPA**: `UserFavoriteIndicatorJpaRepository.updateDisplayMode` `@Modifying` 쿼리.

### Economics 모듈 신규 history 메서드

**ECOS**:
- 포트: `EcosIndicatorRepository.findHistory(String className, String keystatName, int limit)` → `List<EcosIndicator>`
- JPA Native:
  ```sql
  SELECT t.* FROM (
      SELECT e.*, ROW_NUMBER() OVER (
          PARTITION BY e.class_name, e.keystat_name
          ORDER BY e.snapshot_date DESC
      ) AS rn
      FROM ecos_indicator e
      WHERE e.class_name = :className AND e.keystat_name = :keystatName
  ) t
  WHERE t.rn <= :limit
  ORDER BY t.snapshot_date ASC
  ```
- Service: `EcosIndicatorService.findHistory(...)` 위임만.

**GLOBAL**:
- 포트: `GlobalIndicatorRepository.findHistory(String countryName, GlobalEconomicIndicatorType indicatorType, int limit)` → `List<GlobalIndicator>`
- JPA Native: 동일 ROW_NUMBER 패턴, PARTITION BY country_name, indicator_type
- Service: `GlobalIndicatorQueryService.findHistory(...)` 위임만.

### Application 계층 (favorite)

**위치**: `favorite/application/FavoriteIndicatorService.java`

신규/변경:
- `changeDisplayMode(userId, sourceType, indicatorCode, mode)` — `repository.updateDisplayMode(...)` 위임.
- `findEnrichedByUserId(userId)` 확장:
  - 기존 enriched 결과 빌드
  - GRAPH 모드 항목들에 한해 `EcosIndicatorService.findHistory(...)` / `GlobalIndicatorQueryService.findHistory(...)` 단건 호출
  - 시계열을 `EnrichedEcosFavorite` / `EnrichedGlobalFavorite` 레코드에 `history: List<HistoryPoint>`로 첨부
- `HistoryPoint(LocalDate snapshotDate, String dataValue)` record 신규 (Service 내부 record)

indicatorCode 파싱:
- ECOS: `className::keystatName`
- GLOBAL: `countryName::indicatorType`
- 파싱 실패 항목은 history 없이 통과 (실패 격리)

### Presentation 계층

**Controller** (`FavoriteIndicatorController`):
- `PUT /api/favorites/display-mode`
  - Request: `FavoriteDisplayModeRequest { sourceType, indicatorCode, mode }`
  - Response: 204 No Content

**응답 DTO**:
- `EnrichedFavoriteResponse.EcosItem` / `GlobalItem`에 `displayMode: String`, `history: List<EnrichedHistoryPoint>` 필드 추가
- `EnrichedHistoryPoint(LocalDate snapshotDate, String dataValue)` record 신규

### Frontend 계층

**HTML** (`index.html`):
- 기존 관심지표 단일 영역(L404-550)을 두 컨테이너로 분리:
  - `#favoriteGraphSection`
  - `#favoriteIndicatorSection`

**JS** (`favorite.js`):
- `render()` 시 displayMode 기준 분배
- 그래프 카드 내부 `<canvas>` → Chart.js 라인 차트
- 토글 버튼 핸들러: PUT 호출 성공 시 카드 DOM 재배치 (전체 새로고침 X)
- Chart 인스턴스 캐싱 + 토글/언마운트 시 destroy

---

## 주의사항

- **Entity 컬럼 추가 사전 승인 완료** (CLAUDE.md 규칙 7번).
- 기존 `/api/favorites/enriched` 응답 스키마에 신규 필드(displayMode, history) 추가하지만 nullable/optional → 기존 프론트와 호환.
- **history 조회 부하**: 항목별 단건 호출이라 GRAPH 항목 N개 시 N개 쿼리. limit가 작아(30포인트) 부담은 작지만, 사용자별 GRAPH 항목 수가 수십 개를 넘으면 batch IN 쿼리로 전환 검토.
- Chart.js는 이미 `index.html` L8에 로드됨 (추가 의존성 없음).
- 동일 사용자가 빠르게 토글 연타 시 race condition 방지: 클라이언트에서 토글 버튼 disable + 디바운스.
- displayMode 컬럼은 `@Enumerated(EnumType.STRING)`로 저장 — 추후 enum 추가 안전.
- 시계열이 없는 지표는 GRAPH 모드라도 "데이터 없음" placeholder 표시.
- 컬럼 추가는 `ddl-auto: update`로 자동 처리.
- economics history 메서드는 favorite에서만 사용. 추후 다른 모듈/대시보드에서 시계열이 필요하면 재사용.
