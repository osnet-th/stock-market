# 관심지표 표시 모드 토글 (그래프 ↔ 지표) 설계

> 관련 이슈: [#38 관심지표 그래프 보기](https://github.com/osnet-th/stock-market/issues/38)

## 작업 리스트

### Domain 계층
- [ ] `FavoriteDisplayMode` enum 신규 작성 (INDICATOR, GRAPH)
- [ ] `FavoriteIndicator` 도메인 모델에 `displayMode` 필드 추가
- [ ] `FavoriteIndicator.changeDisplayMode(FavoriteDisplayMode)` 도메인 메서드 추가
- [ ] `FavoriteIndicatorRepository` 포트에 `updateDisplayMode(...)` 메서드 추가

### Infrastructure 계층
- [ ] `UserFavoriteIndicatorEntity`에 `display_mode` 컬럼 추가 (VARCHAR(10), NOT NULL, default 'INDICATOR') **— Entity 변경 사전 승인 필요**
- [ ] DB 마이그레이션 SQL 작성 (`ALTER TABLE user_favorite_indicator ADD COLUMN display_mode VARCHAR(10) NOT NULL DEFAULT 'INDICATOR'`)
- [ ] `FavoriteIndicatorMapper`: Entity ↔ Domain displayMode 매핑 추가
- [ ] `FavoriteIndicatorRepositoryImpl.updateDisplayMode(...)` 구현
- [ ] `UserFavoriteIndicatorJpaRepository`: `findByUserIdAndSourceTypeAndIndicatorCode(...)` 추가 (단건 조회)

### Application 계층
- [ ] `FavoriteIndicatorService.changeDisplayMode(userId, sourceType, indicatorCode, mode)` 메서드 추가
- [ ] `FavoriteIndicatorService.findEnrichedWithHistory(userId)` — 그래프 모드 항목에 한해 시계열 포함
- [ ] EcosIndicatorService.getHistoryByCategory()/GlobalIndicatorQueryService.getHistoryByIndicatorType() 활용
- [ ] 시계열 조회 N+1 방지 — 한 번에 batch 조회 후 Map 매핑

### Presentation 계층
- [ ] `PUT /api/favorites/display-mode` API 신규 추가 (요청: sourceType, indicatorCode, mode)
- [ ] `FavoriteDisplayModeRequest` DTO 신규 작성
- [ ] `EnrichedFavoriteResponse`의 EcosItem/GlobalItem에 `displayMode`, `history` 필드 추가
- [ ] `EnrichedHistoryPoint` DTO 신규 작성 (snapshotDate, dataValue)

### Frontend 계층
- [ ] `index.html` 대시보드: "그래프 영역"과 "단순 지표 영역" 카드 컨테이너 분리
- [ ] `favorite.js`: 항목별 displayMode에 따라 두 영역에 분기 렌더
- [ ] 카드별 "지표/그래프" 토글 버튼 UI 추가
- [ ] 토글 시 `PUT /api/favorites/display-mode` 호출 후 화면 재배치
- [ ] Chart.js 라인 차트 렌더링 함수 작성 (`renderFavoriteChart(canvasId, points)`)
- [ ] 차트 라이프사이클 관리 (Chart 인스턴스 destroy → re-render)

---

## 배경

이슈 #38 요구사항:
1. 관심지표 항목별로 **그래프 / 단순 지표** 표시 방식 선택
2. 마지막 선택값은 **DB 저장**으로 영속화
3. 대시보드에서 **그래프 영역과 단순 지표 영역을 분리**

현재 `/api/favorites/enriched`는 최신값만 반환, 프론트는 단순 카드로만 표시. 시계열 데이터는 economics 모듈에 이미 조회 API 존재 (`EcosIndicatorService.getHistoryByCategory`, `GlobalIndicatorQueryService.getHistoryByIndicatorType`).

---

## 핵심 결정

- **표시 모드 식별 단위**: `(userId, sourceType, indicatorCode)` 단위로 displayMode 저장 (기존 unique 키와 동일). 사용자별 개별 지표마다 모드 다름.
- **enum 값**: `INDICATOR`(기본), `GRAPH` 두 가지. 추후 확장 여지 둠.
- **DB default**: 기존 row에는 'INDICATOR'를 default로 지정 → 마이그레이션 시 기존 사용자 영향 없음.
- **시계열 데이터 전달 방식**: 단순 enriched 응답에 history 배열을 함께 담음 (별도 endpoint 분리하지 않음). 단, **GRAPH 모드 항목에만** history 포함 → 트래픽 절감.
- **시계열 길이**: 우선 최근 30포인트 한도. 차트 가독성과 응답 크기 균형.
- **프론트 영역 분리**: 한 화면에 두 섹션(`#favoriteGraphSection`, `#favoriteIndicatorSection`)을 두고, 카드 단위로 항목을 분배. 같은 카드를 토글하면 다른 섹션으로 이동.
- **토글 UX**: 카드 우상단 작은 토글 버튼. 즉시 PUT 호출 → 성공 시 카드를 반대편 섹션으로 이동(전체 새로고침 X).

---

## 구현

### Domain 계층

**위치**: `favorite/domain/model/`

- `FavoriteDisplayMode.java` — enum (INDICATOR, GRAPH)
- `FavoriteIndicator.java` — `displayMode` 필드 + `changeDisplayMode()` 도메인 메서드 추가

**Repository 포트**: `favorite/domain/repository/FavoriteIndicatorRepository.java`
- `void updateDisplayMode(Long userId, FavoriteIndicatorSourceType sourceType, String indicatorCode, FavoriteDisplayMode mode)`

[구현 예시](./examples/domain-model-example.md)

### Infrastructure 계층

**Entity**: `favorite/infrastructure/persistence/UserFavoriteIndicatorEntity.java`
- `@Column(name = "display_mode", nullable = false, length = 10) @Enumerated(EnumType.STRING)` 추가

**Mapper**: `favorite/infrastructure/persistence/FavoriteIndicatorMapper.java`
- toDomain/toEntity에 displayMode 매핑 추가

**Repository 구현체**: `favorite/infrastructure/persistence/FavoriteIndicatorRepositoryImpl.java`
- `updateDisplayMode(...)` 구현 (JpaRepository 통한 update)

**JPA Repository**: `favorite/infrastructure/persistence/UserFavoriteIndicatorJpaRepository.java`
- `Optional<UserFavoriteIndicatorEntity> findByUserIdAndSourceTypeAndIndicatorCode(...)` 시그니처 추가

**DB 마이그레이션**: `src/main/resources/db/migration/Vxxxx__add_display_mode_to_favorite.sql`
```sql
ALTER TABLE user_favorite_indicator
  ADD COLUMN display_mode VARCHAR(10) NOT NULL DEFAULT 'INDICATOR';
```

[구현 예시](./examples/infrastructure-example.md)

### Application 계층

**위치**: `favorite/application/FavoriteIndicatorService.java`

신규/변경 메서드:
- `changeDisplayMode(userId, sourceType, indicatorCode, mode)` — 단순 update 위임
- `getEnriched(userId)` 기존 메서드를 확장: GRAPH 모드 항목들에 한해 시계열을 함께 모아 응답 빌드

시계열 조회 흐름:
1. 사용자 관심지표 전체 조회 → displayMode가 GRAPH인 항목만 필터
2. ECOS는 className+keystatName 조합으로, GLOBAL은 indicatorType 단위로 묶어 일괄 조회
3. 결과를 indicatorCode → List<HistoryPoint> 형태 Map으로 매핑
4. 응답 DTO 빌드 시 항목별 history 주입

[구현 예시](./examples/application-service-example.md)

### Presentation 계층

**Controller**: `favorite/presentation/FavoriteIndicatorController.java`

신규 API:
- `PUT /api/favorites/display-mode`
  - Request: `FavoriteDisplayModeRequest { sourceType, indicatorCode, mode }`
  - Response: 204 No Content (또는 변경된 항목 반환)

응답 DTO 변경:
- `EnrichedFavoriteResponse.EcosItem` / `GlobalItem`에 `displayMode: String`, `history: List<EnrichedHistoryPoint>` 필드 추가
- `EnrichedHistoryPoint { snapshotDate: LocalDate, dataValue: BigDecimal }` 신규

[구현 예시](./examples/presentation-example.md)

### Frontend 계층

**HTML**: `src/main/resources/static/index.html`
- 기존 관심지표 단일 영역(L404-550)을 두 컨테이너로 분리:
  - `#favoriteGraphSection` (그래프 카드)
  - `#favoriteIndicatorSection` (단순 지표 카드)
- 두 영역 모두 수평 스크롤 카드 레이아웃 유지, 섹션 헤더 구분

**JS**: `src/main/resources/static/js/components/favorite.js`
- `render()` 시 displayMode 기준으로 두 영역에 분배
- 그래프 카드 내부 `<canvas>` 추가 → Chart.js로 라인 차트
- 토글 버튼 핸들러: PUT 호출 성공 시 카드를 반대 섹션으로 이동 (DOM 재배치)
- Chart 인스턴스 캐싱 + 토글/언마운트 시 destroy

[구현 예시](./examples/frontend-example.md)

---

## 주의사항

- **Entity 컬럼 추가는 사전 승인 필요** (CLAUDE.md 규칙 7번). 본 설계가 그 승인 요청을 겸함.
- 기존 `/api/favorites/enriched` 응답 스키마에 신규 필드(displayMode, history)를 추가하지만, 신규 필드는 nullable/optional 취급 → 기존 프론트와 호환 유지.
- 시계열 batch 조회 시 N+1 발생 주의: GRAPH 항목이 많아도 ECOS/GLOBAL 각 1회 호출로 끝나도록 모아서 조회.
- Chart.js는 이미 `index.html` L8에 로드됨 (추가 의존성 없음).
- 동일 사용자가 빠르게 토글 연타 시 race condition 방지를 위해 클라이언트에서 토글 버튼 disable + 디바운스.
- displayMode 컬럼은 `@Enumerated(EnumType.STRING)`로 저장 — 추후 enum 추가 안전.
- 시계열이 없는 지표(`hasData=false`)는 GRAPH 모드라도 빈 차트 placeholder 또는 "데이터 없음" 메시지 표시.
- 마이그레이션 파일 버전 번호는 기존 마지막 마이그레이션 다음 번호로 부여 (작성 시점에 확인).
