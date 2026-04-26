# 주식 기록 기능 (stocknote) 설계 문서

**생성일**: 2026-04-23
**관련 이슈**: #31
**상태**: Entity/Enum 승인 대기
**Origin brainstorm**: [docs/brainstorms/2026-04-23-stock-note-brainstorm.md](../../../../docs/brainstorms/2026-04-23-stock-note-brainstorm.md)
**Full plan (deepened)**: [docs/plans/2026-04-23-001-feat-stock-note-plan.md](../../../../docs/plans/2026-04-23-001-feat-stock-note-plan.md)

---

## 목적

특정 주식의 **등락 시점별 판단 로그**를 구조화 저장 → 자동 가격 스냅샷(AT_NOTE/D+7/D+30) + 수동 사후 검증 → 태그 조합 기반 유사 패턴 매칭으로 투자 판단 적중률 복기.

## 핵심 결정 요약 (태형님 승인 반영)

- 신규 독립 도메인 `stocknote`, 기존 `salary` 와 동일한 4-레이어 헥사고날 구조
- **Entity 5개 + Enum 10개** (심화 A/B 반영, Valuation·Fundamental 을 `StockNote` 본체로 흡수, `TagCategory` 제거)
- ID 참조 원칙 준수 (`stockCode/marketType/exchangeCode` 는 stock 도메인 enum 재사용)
- SecurityContextHolder 기반 userId 주입 (favorite 패턴)
- 비동기: `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)` + `@Async("stocknoteSnapshotExecutor")` → `StockNoteSnapshotAsyncDispatcher` (application 계층)
- `stock.application.StockPriceService` 경유 호출 (port 직접 호출 금지)
- Caffeine 대시보드 캐시 TTL 30분 (심화 E 반영)
- 완전 잠금 정책 — `StockNoteVerification` 존재 시 본문 수정 차단
- Native SQL 로 태그 조합 매칭 (QueryDSL 미도입)

---

## Entity 목록 (5개, 승인 대기)

### 1. StockNote (본체)
| 컬럼 | 타입 | 비고 |
|------|------|------|
| id | Long | PK, auto |
| userId | Long | 필수 |
| stockCode | String(20) | 필수 |
| marketType | Enum MarketType | `stock` 도메인 재사용 |
| exchangeCode | Enum ExchangeCode | `stock` 도메인 재사용 |
| direction | Enum NoteDirection | UP/DOWN |
| changePercent | BigDecimal(6,2) | 당일 등락률 |
| noteDate | LocalDate | 미래 금지, 백데이트 허용(경고) |
| triggerText | TEXT | `@Size(max=4000)` |
| interpretationText | TEXT | `@Size(max=4000)` |
| riskText | TEXT | `@Size(max=4000)` |
| preReflected | boolean | |
| initialJudgment | Enum UserJudgment | 작성 시점 예측 |
| **per** | BigDecimal(10,2) nullable | **Valuation 흡수** |
| **pbr** | BigDecimal(10,2) nullable | Valuation 흡수 |
| **evEbitda** | BigDecimal(10,2) nullable | Valuation 흡수 |
| **vsAverage** | Enum VsAverageLevel nullable | Valuation 흡수 |
| **revenueImpact** | Enum ImpactLevel nullable | **Fundamental 흡수** |
| **profitImpact** | Enum ImpactLevel nullable | Fundamental 흡수 |
| **cashflowImpact** | Enum ImpactLevel nullable | Fundamental 흡수 |
| **isOneTime** | boolean | Fundamental 흡수 |
| **isStructural** | boolean | Fundamental 흡수 |
| createdAt | Instant | `@CreationTimestamp` |
| updatedAt | Instant | `@UpdateTimestamp` |

**인덱스**
- `(user_id, note_date DESC)` — 리스트 정렬
- `(user_id, stock_code, note_date DESC)` — 종목별 차트

### 2. StockNoteTag
| 컬럼 | 타입 | 비고 |
|------|------|------|
| id | Long | PK |
| noteId | Long | FK |
| **userId** | Long | **denormalize (performance H)** |
| tagSource | String(20) | `TRIGGER/CHARACTER/SUPPLY/CUSTOM` (enum 제거, 문자열 프리픽스) |
| tagValue | String(32) | `@Size(max=32)` + `@Pattern` |

**인덱스**
- `(user_id, tag_source, tag_value, note_id)` covering index — 패턴 매칭
- `(note_id)` — 기록별 태그 조회

### 3. StockNotePriceSnapshot
| 컬럼 | 타입 | 비고 |
|------|------|------|
| id | Long | PK |
| noteId | Long | FK |
| snapshotType | Enum SnapshotType | AT_NOTE/D_PLUS_7/D_PLUS_30 |
| priceDate | LocalDate nullable | |
| closePrice | BigDecimal(18,4) nullable | |
| changePercent | BigDecimal(8,2) nullable | AT_NOTE 대비 |
| status | Enum SnapshotStatus | PENDING/SUCCESS/FAILED (3값, 심화 C) |
| failureReason | String(255) nullable | |
| retryCount | int | default 0 |
| capturedAt | Instant nullable | |

**인덱스**
- `(note_id, snapshot_type)` unique
- `(status, snapshot_type)` — PENDING 재시도 스캔

### 4. StockNoteVerification
| 컬럼 | 타입 | 비고 |
|------|------|------|
| id | Long | PK |
| noteId | Long | unique |
| judgmentResult | Enum JudgmentResult | CORRECT/WRONG/PARTIAL |
| verificationNote | TEXT | `@Size(max=4000)` |
| verifiedAt | Instant | |

### 5. StockNoteCustomTag
| 컬럼 | 타입 | 비고 |
|------|------|------|
| id | Long | PK |
| userId | Long | |
| tagValue | String(32) | lowercased trim |
| usageCount | Long | atomic increment |

**제약**: `(user_id, tag_value)` unique, 사용자당 최대 500개

---

## Enum 목록 (10개, 승인 대기)

| # | Enum | 값 | 용도 |
|---|------|-----|------|
| 1 | NoteDirection | UP, DOWN | 등락 방향 |
| 2 | RiseCharacter | FUNDAMENTAL, EXPECTATION, SUPPLY_DEMAND, THEME, REVALUATION | 등락 성격 |
| 3 | TriggerType | DISCLOSURE, EARNINGS, NEWS, POLICY, INDUSTRY, SUPPLY, THEME, ETC | 직접 트리거 |
| 4 | SupplyActor | FOREIGN, INSTITUTION, RETAIL, SHORT_COVERING, ETF_FLOW | 수급 주체 |
| 5 | ImpactLevel | HIGH, MEDIUM, LOW | 실적 영향도 (StockNote 본체 컬럼 3개) |
| 6 | UserJudgment | MORE_UPSIDE, NEUTRAL, OVERHEATED, CATALYST_EXHAUSTED | 작성 시점 예측 |
| 7 | JudgmentResult | CORRECT, WRONG, PARTIAL | 사후 검증 결과 |
| 8 | SnapshotType | AT_NOTE, D_PLUS_7, D_PLUS_30 | 스냅샷 종류 |
| 9 | SnapshotStatus | PENDING, SUCCESS, FAILED | 스냅샷 상태 (심화 C: DELISTED/SUSPENDED 제거) |
| 10 | VsAverageLevel | ABOVE, AVERAGE, BELOW | 밸류에이션 과거 평균 대비 |

**제거된 Enum** (심화 B): `TagCategory` → `StockNoteTag.tagSource` 문자열 컬럼 (`TRIGGER/CHARACTER/SUPPLY/CUSTOM`) 로 대체

---

## 패키지 구조

```
src/main/java/com/thlee/stock/market/stockmarket/stocknote/
├── domain/
│   ├── model/
│   │   ├── StockNote.java
│   │   ├── StockNoteTag.java
│   │   ├── StockNotePriceSnapshot.java
│   │   ├── StockNoteVerification.java
│   │   ├── StockNoteCustomTag.java
│   │   └── enums/ (10개 enum)
│   └── repository/ (5개 포트)
├── application/
│   ├── StockNoteWriteService.java
│   ├── StockNoteReadService.java
│   ├── StockNoteVerificationService.java
│   ├── StockNoteSnapshotService.java
│   ├── StockNoteSnapshotAsyncDispatcher.java       # 심화 6
│   ├── StockNotePatternMatchService.java
│   ├── StockNoteDashboardService.java
│   ├── StockNoteChartService.java
│   ├── event/StockNoteCreatedEvent.java
│   └── dto/ (Result, Command)
├── infrastructure/
│   ├── persistence/ (5개 Entity/JpaRepository/RepositoryImpl + mapper)
│   ├── scheduler/
│   │   ├── StockNoteSnapshotScheduler.java         # 국내/해외/재시도
│   │   └── BusinessDayCalculator.java              # 간이 한국 영업일
│   ├── cache/StockNoteCacheConfig.java              # Caffeine 30분
│   └── async/StocknoteAsyncConfig.java              # 전용 ThreadPoolTaskExecutor
└── presentation/
    ├── StockNoteController.java                     # /api/stock-notes
    ├── StockNoteVerificationController.java
    ├── StockNoteAnalyticsController.java            # 대시보드/패턴/차트
    ├── StockNoteCustomTagController.java
    └── dto/ (Request, Response)
```

---

## API 엔드포인트 (11개, 승인 대기)

```
# CRUD
POST   /api/stock-notes
GET    /api/stock-notes                          # 리스트 (필터/페이징)
GET    /api/stock-notes/{id}
PUT    /api/stock-notes/{id}                     # 409 if verification exists
DELETE /api/stock-notes/{id}                     # cascade

# 검증
PUT    /api/stock-notes/{id}/verification
DELETE /api/stock-notes/{id}/verification

# 분석
GET    /api/stock-notes/{id}/similar-patterns
GET    /api/stock-notes/dashboard                # pendingVerificationCount 포함 (심화 D)
GET    /api/stock-notes/by-stock/{stockCode}/chart

# 자유 태그
GET    /api/stock-notes/custom-tags?prefix=&limit=10

# 수동 재시도
POST   /api/stock-notes/{id}/snapshots/{type}/retry
```

**제거된 엔드포인트** (심화 D): `/pending-verifications/count` — dashboard 응답으로 통합

---

## 스케줄러 설계

| 스케줄러 | cron | 용도 |
|---|---|---|
| `captureDomesticSnapshots` | `0 0 16 * * MON-FRI` (KST) | 국내 D+7/D+30 |
| `captureOverseasSnapshots` | `0 0 7 * * TUE-SAT` (KST) | 해외 D+7/D+30 (미국 장 마감 후 다음날) |
| `retryPendingAtNoteSnapshots` | `0 */10 * * * *` | AT_NOTE PENDING 재시도 (retryCount < 3) |

공통: `LoggingContext.forScheduler(jobName)` MDC + 종목별 try-catch 격리 + Micrometer 메트릭 (`snapshot.capture.success/failure/retry.exhausted`)

**AT_NOTE 비동기 플로우**:
```
POST /api/stock-notes (HTTP)
  └─ StockNoteWriteService.create @Transactional
       ├─ repo.save(note + tags + AT_NOTE PENDING)
       ├─ eventPublisher.publishEvent(StockNoteCreatedEvent(noteId))
       └─ [commit]

[AFTER_COMMIT TransactionalEventListener]
  └─ StockNoteSnapshotAsyncDispatcher.captureAtNote(noteId) @Async("stocknoteSnapshotExecutor")
       └─ StockPriceService.getPrice(...) — 심화 5 (port 직접 호출 금지)
       └─ @Retryable(maxAttempts=2, backoff=exp+jitter) — 심화 12
       └─ conditional UPDATE: WHERE id=? AND status='PENDING' — 심화 10
```

---

## 보안 스펙 (심화 14~18 반영)

1. **IDOR 방어**: 모든 Repository `findByIdAndUserId` 패턴. 미일치 시 `StockNoteNotFoundException` → 404 (403 이면 존재 여부 노출)
2. **Native SQL 바인딩**: `(cat, val) IN ((?,?),(?,?)...)` 동적 placeholder + 개별 `setString` 바인딩
3. **입력 검증**:
   - TEXT 필드: `@Size(max=4000)`
   - Tag: `@Size(max=32)` + `@Pattern("^[\\p{IsHangul}\\p{Alnum}._\\- ]+$")` + lowercased trim
   - 사용자당 custom tag 상한 500
4. **프론트 XSS 방어**: `x-text` 강제, `x-html` 금지
5. **localStorage draft**: key = `stocknote_draft_${userId}`, 로그아웃 시 removeItem
6. **@CacheEvict**: `@TransactionalEventListener(AFTER_COMMIT)` 내부에서 호출

---

## 작업 리스트 (Phase별)

### Phase 1: 도메인 모델 + 인프라 스켈레톤
- [x] Entity 5개 + Enum 10개 승인 (본 문서) — 2026-04-23 승인
- [x] **Phase 1a**: Enum 10개 + Domain 모델 5개 + Repository 포트 5개 + StockNoteListFilter — 2026-04-24 완료, compileJava 성공
- [x] 테이블 마이그레이션 (ddl-auto: update 로 자동 생성 예정) — Phase 1b 배포 시 자동 적용
- [x] **Phase 1b**: Entity 5개 + JpaRepository + RepositoryImpl 어댑터 + StockNoteMapper — 2026-04-24 완료, compileJava 성공
- [x] **Phase 1c**: `BusinessDayCalculator` 간이 구현 + `StocknoteAsyncConfig` (전용 ThreadPoolTaskExecutor + MdcTaskDecorator) — 2026-04-24 완료, compileJava 성공

### Phase 2: 응용 계층 + REST API (기록 CRUD)
- [x] `StockNoteWriteService.create` — 본체/태그/AT_NOTE PENDING 원자 + 이벤트 발행 — 2026-04-24
- [x] `StockNoteWriteService.update/delete` — 잠금 검증 + cascade — 2026-04-24
- [x] `StockNoteReadService.findById/findList` — IN 배치 fetch (심화 9) — 2026-04-24
- [x] `StockNoteController` + Request/Response DTO + ExceptionHandler — 2026-04-24
- [x] 인증: `SecurityContextHolder` 기반 userId — `currentUserId()` 헬퍼 — 2026-04-24
- [x] Bean Validation 스펙 적용 (심화 16) — @Size/@Pattern/@NotBlank — 2026-04-24

### Phase 3: 가격 스냅샷 + 스케줄러
- [x] `StockNoteSnapshotAsyncDispatcher` + `@TransactionalEventListener(AFTER_COMMIT)` — 2026-04-24
- [x] `StockNoteSnapshotService.captureAtNote` (stock.application.StockPriceService 경유) — 2026-04-24
- [x] `StockNoteSnapshotScheduler` 3개 cron (국내/해외/재시도) — 2026-04-24
- [x] conditional UPDATE (`WHERE status='PENDING'`) — 2026-04-24
- [ ] Micrometer 메트릭 등록 — Phase 9 (폴리싱) 으로 이월
- [x] **결정 변경**: `@Retryable` 생략 (spring-retry 의존성 추가 회피), 10분 배치 재시도만 사용 — 2026-04-24

### 추가 메서드
- `StockNoteRepository.findById(Long)` — 내부 async/배치 전용 (userId 스코프 없음, 주석 명시)

### Phase 4: 사후 검증 + 본문 잠금
- [x] `StockNoteVerificationService.upsert/delete` — 2026-04-24 완료
- [x] 본문 잠금 409 Conflict 반환 — `StockNoteExceptionHandler` 확장 (Verification Controller 포함)
- [ ] `@CacheEvict` — Phase 5 (대시보드 캐시 도입 시 함께 적용)

### Phase 5: 분석 API (패턴 매칭 + 대시보드 + 차트)
- [x] `StockNotePatternMatchService` — native SQL 동적 placeholder (`src0..N`, `val0..N`) + 집계 — 2026-04-24
- [x] `StockNoteDashboardService` — Caffeine TTL 30분, `@Cacheable(sync=true)`, 전용 CacheManager — 2026-04-24
- [x] `StockNoteChartService` — 일봉 + 기록점 (`priceAtNote` 조인, 심화 22) — 2026-04-24
- [x] **선행(옵션 A 채택)**: `StockPricePort.getDailyHistory` default 메서드 빈 리스트 반환, `DailyPrice` record 추가 — 실제 KIS 연동은 후속 — 2026-04-24
- [x] `StockNoteCustomTagService` + `StockNoteCustomTagController` — 자동완성 — 2026-04-24
- [x] `StocknoteCacheConfig` + WriteService/VerificationService 에 `@CacheEvict` 적용 — 2026-04-24
- [x] `StockNoteAnalyticsController` + 3 Response DTO + ExceptionHandler 확장 — 2026-04-24

### Phase 6: 프론트엔드 — 기본 골격
- [x] **프로젝트 관행 조정**: partials 디렉토리 미사용 → `index.html` 인라인 블록. 2026-04-24
- [x] `static/js/components/stocknote.js` (모듈 스코프 chartRegistry + 초기상태 + 탭전환 + API 호출 스텁)
- [x] `static/js/api.js` 에 stocknote 11개 API 메서드 추가
- [x] `app.js` 5 수정 포인트 (validPages/menus/spread/cleanup/navigateTo)
- [x] `index.html` stocknote 블록 (헤더/탭/대시보드 KPI/리스트 테이블/상세 패널/드로워 플레이스홀더)

### Phase 7: 프론트엔드 — 기록 작성 드로워 (3-section 폼, 심화 23)
- [x] 3-section 폼 (기본 / 분석 / 판단 & 밸류에이션) — 2026-04-24
- [x] 필드별 validation + 전체 검증 (미래 날짜 금지, 필수 필드) — 2026-04-24
- [x] localStorage draft 자동 저장 (@change, key scope `stocknote_draft_${userId}`) — 2026-04-24
- [x] 자유 태그 자동완성 (API.getStockNoteCustomTags) — 2026-04-24
- [x] 종목 검색 autocomplete (portfolio.js 패턴, 300ms debounce) — 2026-04-24
- [x] 고정 enum 태그 멀티 체크 버튼 UI — 2026-04-24

### Phase 8: 프론트엔드 — 종목 차트 + 검증 패널
- [x] Chart.js mixed chart (line + scatter, 모듈 스코프 `_stocknoteChartRegistry`, 심화 19) — 2026-04-24
- [x] `animation:false` + scatter `pointStyle: triangle` + rotation:180 (심화 20/21) — 2026-04-24
- [x] line `pointRadius:0, pointHitRadius:0` (심화 20) — scatter 만 클릭되도록
- [x] 기록점 클릭 → 상세 슬라이드 패널 오픈 (onClick 핸들러) — 2026-04-24
- [x] 검증 패널 내 인라인 폼 (radio + textarea + 저장/삭제) — 본문 잠금 상태 시각화 — 2026-04-24
- [x] 유사 패턴 미니 카드 (집계 + TOP5 매치 + judgmentResult 뱃지) — 2026-04-24
- [x] 종목 코드 직접 입력으로 차트 조회 (일봉 공급되면 자동 렌더) — 2026-04-24
- [ ] 일봉 KIS 연동 (별도 작업): Phase 8 UI 는 완성, 데이터 공급만 남음

### Phase 9: 폴리싱
- [ ] 사이드바 배지 (dashboard 응답 재활용)
- [ ] 스냅샷 실패 상태 UI + 수동 재시도 버튼
- [ ] 에러 메시지 / 빈 상태 / 로딩
- [ ] 반응형 (모바일 드로워 fullscreen)

---

## 구현 시 참고 파일

- 도메인 4-레이어 레퍼런스: `src/main/java/com/thlee/stock/market/stockmarket/salary/**`
- 주가 조회 경유: `stock.application.StockPriceService`
- 스케줄러 패턴: `economics/infrastructure/scheduler/EcosIndicatorBatchScheduler.java:20-32`
- `@Async` + MDC: `logging/infrastructure/async/LogAsyncConfig.java:71-106`
- SecurityContextHolder userId: `favorite/presentation/FavoriteIndicatorController.java:89-91`
- 네이티브 쿼리 레퍼런스: `salary/infrastructure/persistence/SpendingConfigJpaRepository.java:21-31`
- Chart.js lifecycle: `static/js/components/salary.js:9` 주석
- Alpine proxy 회피 선례: `docs/solutions/architecture-patterns/ecos-timeseries-chart-visualization.md`