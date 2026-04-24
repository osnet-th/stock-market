# 주식 기록 기능 (Stock Note) — 브레인스토밍

**날짜**: 2026-04-23
**관련 이슈**: #31
**상태**: 초안 (승인 대기)

---

## What We're Building

특정 주식의 **등락 시점별 판단 로그**를 구조화해서 남기고, 누적된 로그를 통해 **투자 판단 패턴/적중률/인사이트**를 복기하는 신규 도메인. 단순 메모장이 아닌 **"구조화된 투자 판단 로그 + 패턴 분석 대시보드"** 를 지향한다.

### 핵심 사용자 흐름
1. 관심/보유 종목에서 등락 이벤트 발생 → **기록 작성** (고정 태그 + 자유서술 혼합 폼)
2. 기록 시점의 종가를 **자동 스냅샷**으로 저장
3. 1주/1개월 후 스냅샷을 **스케줄러가 자동 수집**하여 실제 등락률 계산
4. 사용자가 "내 판단이 맞았는지" 를 **수동 평가** (맞음/틀림/부분적중 + 사유)
5. **조회 화면**: 종목 가격 차트 위 기록점 핀 + 아래 타임라인 카드
6. **분석 대시보드**: 판단 적중률, 상승 성격 분포, 자주 맞/틀리는 태그 조합

### 기록할 10가지 항목 (이슈 본문 기반)
1. 직접 트리거 (뉴스/공시/실적/정책 등) + 시장 해석
2. 상승/하락의 성격 (실적형/기대형/수급형/테마형/리레이팅형)
3. 실적 연결성 (매출·영업이익·현금흐름 영향도)
4. 일회성 vs 구조적 지속성
5. 선반영 여부
6. 수급 주체 (외국인/기관/개인/숏커버링/ETF)
7. 업종 vs 개별 (베타 vs 알파)
8. 반대 논리 / 반증 조건
9. 당시 밸류에이션 (PER/PBR/EV·EBITDA + 평균 대비 위치)
10. 내 판단 + 사후 검증 결과

---

## Why This Approach

### 선택한 아키텍처 개요

**신규 독립 도메인**: `stocknote` (기존 `portfolio`, `favorite`, `salary` 와 동일한 4-레이어 헥사고날 구조)

**Entity 설계 (완전 정규화, Entity 연관관계 금지 원칙 준수)**
- `StockNote` — 기록 본체 (**userId**, stockCode, marketType, exchangeCode, direction, changePercent, noteDate, triggerText, interpretationText, riskText, initialJudgment 등)
  - `initialJudgment`: 작성 시점의 예측(`UserJudgment` enum). 사후 실측 결과와 분리됨.
- `StockNoteTag` — 기록별 태그 (noteId, tagCategory, tagValue) — 복수 태그 저장. 고정 enum 태그와 자유 태그 모두 이 테이블에 저장 (tagCategory 로 구분).
- `StockNoteValuation` — 당시 밸류에이션 스냅샷 (noteId, per, pbr, evEbitda, vsAverage)
- `StockNoteFundamentalLink` — 실적/현금흐름 영향도 (noteId, revenueImpact, profitImpact, cashflowImpact — 각각 `ImpactLevel` enum, isOneTime, isStructural)
- `StockNotePriceSnapshot` — 가격 스냅샷 (noteId, snapshotType: AT_NOTE/D+7/D+30, priceDate, closePrice, changePercent)
- `StockNoteVerification` — 사후 검증 실측 (noteId, verifiedAt, judgmentResult: CORRECT/WRONG/PARTIAL, verificationNote)
- `StockNoteCustomTag` — 사용자별 자유 보조 태그 마스터 (userId, tagValue, usageCount) — 자동완성/중복 병합용

모든 Entity는 **ID 기반 참조**만 가지며 JPA 연관관계는 두지 않는다 (`ARCHITECTURE.md` 규칙). 모든 루트 Entity (`StockNote`, `StockNoteCustomTag`) 는 **userId** 를 보유한다.

**Enum (고정 분류)**
- `NoteDirection` — UP / DOWN
- `RiseCharacter` — FUNDAMENTAL / EXPECTATION / SUPPLY_DEMAND / THEME / REVALUATION
- `TriggerType` — DISCLOSURE / EARNINGS / NEWS / POLICY / INDUSTRY / SUPPLY / THEME / ETC
- `SupplyActor` — FOREIGN / INSTITUTION / RETAIL / SHORT_COVERING / ETF_FLOW
- `ImpactLevel` — HIGH / MEDIUM / LOW (`StockNoteFundamentalLink` 의 매출/영업이익/현금흐름 영향도)
- `UserJudgment` — MORE_UPSIDE / NEUTRAL / OVERHEATED / CATALYST_EXHAUSTED (작성 시점 예측)
- `JudgmentResult` — CORRECT / WRONG / PARTIAL
- `PriceSnapshotType` — AT_NOTE / D_PLUS_7 / D_PLUS_30

### 프론트엔드 구성

**신규 메뉴**: `내 투자 노트` (사이드바 단일 메뉴, 기존 `app.js` menus 배열에 추가)

**페이지 내부 3-탭 구성 (동일 화면 + 액션 구분 패턴)**
1. **대시보드 탭** — 이번 달 기록 수, 판단 적중률, 상승/하락 원인 분포(도넛), 태그 조합 TOP5
2. **종목 상세 탭** — 좌측 종목 리스트 / 우측 상단 가격 차트 (Chart.js, 기록점 핀 표시) / 우측 하단 타임라인 카드
3. **기록 리스트 탭** — 전체 기록 테이블 + 필터 (종목/기간/방향/성격/적중여부)

**기록 작성 UX**: 우측 슬라이드 드로워 (포트폴리오의 `showAddModal` 패턴 차용)
- Step 1: 기본(종목 선택, 날짜, 방향, 당일 등락률)
- Step 2: 트리거 & 해석 (태그 + 자유서술)
- Step 3: 성격 & 실적 연결 (태그 선택)
- Step 4: 수급 & 밸류에이션 (태그 + 수치)
- Step 5: 내 판단 & 반대 논리

### 사후 검증 자동화
- `StockNotePriceSnapshotScheduler` (`@Scheduled`, 매 영업일 오후 KIS 호출)
- 기록일(D) 기준 D+7, D+30 도달 시 종가 스냅샷 생성
- `changePercent` = (스냅샷가 - 기록일 종가) / 기록일 종가
- 판정 자체는 수동 평가 (`StockNoteVerification` 별도)

### 패턴 매칭 (태그 조합 기반)
- 현재 기록의 `tagCategory+tagValue` 조합 집합을 키로 과거 기록 조회
- 동일 조합의 `JudgmentResult` 분포 + D+7/D+30 평균 등락률 반환
- 예: "상승성격=EXPECTATION × 수급주체=FOREIGN 패턴 — 과거 7회 중 1주 후 상승 4회 / 1개월 후 하락 5회"

### 근거 요약
1. **YAGNI**: 섹터 분석은 Stock 도메인에 섹터 정보가 없어 제외 (종목별/태그별만)
2. **아키텍처 준수**: 연관관계 없이 ID 참조만, application 계층 트랜잭션, 4-레이어 분리
3. **기존 패턴 차용**: 프론트는 `portfolio.js` 의 모달 + 리스트 하이브리드, 차트는 `favorite`/`economics` 의 Chart.js 패턴
4. **분석 성능 우선**: JSONB 대신 완전 정규화로 QueryDSL 친화적 통계 쿼리 확보

---

## Key Decisions

| 결정 | 내용 | 이유 |
|------|------|------|
| **MVP 범위** | 풀(기록 + 조회 + 분석 + 패턴 매칭 전체) | 이슈의 핵심 가치가 분석/패턴 복기에 있어 기록만으로는 의미 약함 |
| **기록 대상** | 상승/하락 모두 (`NoteDirection` enum) | 대칭성 있는 인사이트 확보, 이슈 원문의 "등락" 표현 충실 |
| **종목 범위** | 보유 + 관심 종목 (favorite 포함, 검색 추가 가능) | 미보유 관찰 종목도 커버 |
| **사후 검증** | 자동 가격 스냅샷 + 수동 판정 | 객관 데이터는 자동, 해석은 사람이 담당 |
| **조회 표현** | 가격 차트 + 기록점 핀 (상단) / 타임라인 카드 (하단) | 이슈 요구 "그래프로 기록점 확인" 직접 충족 |
| **기록/조회 분리** | 동일 화면 내 탭 + 드로워 | 기존 portfolio UX 일관성, 새 메뉴 폭증 방지 |
| **패턴 매칭** | 태그 조합 기반 | 태그만 정확하면 성능/정확도 균형 우수 |
| **태그 체계** | 고정 enum + 소량 자유 보조 태그 | 통계 정확성 + 유연성 절충 |
| **섹터 분석** | 제외 (종목별만) | Stock 도메인에 섹터 없음, 범위 축소로 완성도 확보 |
| **데이터 모델** | 완전 정규화 | 분석 쿼리 성능/타입안전, `batch_size=1000` 활용 |
| **모듈명** | `stocknote` | 기존 도메인 명명 패턴(짧은 영어 단일 단어)과 정합 |

---

## 주요 API 윤곽 (Phase 2에서 확정)

```
POST   /api/stock-notes                                    # 기록 생성 (자동 AT_NOTE 스냅샷 생성)
GET    /api/stock-notes                                    # 기록 리스트 (필터: stockCode/기간/direction/character)
GET    /api/stock-notes/{id}                               # 기록 단건 + 밸류에이션/실적/스냅샷/검증 포함
PUT    /api/stock-notes/{id}                               # 기록 수정
DELETE /api/stock-notes/{id}                               # 기록 삭제 (관련 스냅샷/검증 cascade)
PUT    /api/stock-notes/{id}/verification                  # 사후 판정 upsert
GET    /api/stock-notes/{id}/similar-patterns              # 태그 조합 기반 과거 유사 패턴
GET    /api/stock-notes/dashboard                          # 대시보드 KPI (적중률, 원인 분포, 태그 TOP5)
GET    /api/stock-notes/by-stock/{stockCode}/chart         # 종목 가격 차트 + 기록점 핀 데이터
```

---

## Resolved Questions

브레인스토밍 과정에서 모두 해결됨.

1. **가격 스냅샷 데이터 소스**: 국내=KIS 일봉, 해외=KIS+Finnhub 혼합 (기존 Stock 도메인 소스 재사용)
2. **영업일 판정**: 간이 처리 — 주말 + 한국 고정 공휴일만 스킵. D+7/D+30 이 영업일 아니면 차기 영업일로 이월
3. **기록 수정 정책**: 사후 검증(`StockNoteVerification`) 존재 여부로 **본문 잠금** 판정. 잠긴 기록 수정 필요 시 검증을 먼저 삭제 후 재작성 (MVP는 단순화, 별도 플래그 없음)
4. **삭제 정책**: **Hard delete + cascade** (스냅샷/검증/태그 모두 함께 삭제). 개인 투자 노트 특성상 사용자 의도 존중
5. **알림 연동**: 별도 이메일/푸시 알림 없음. **사이트 내 메뉴 바 배지**(검증 대기 N건)만 제공 → `notification` 모듈 의존성 제거
6. **차트 핀 인터랙션**: **우측 슬라이드 상세 패널** (차트 유지, Chart.js 리렌더 최소화)
7. **대시보드 집계**: **온디맨드 QueryDSL + Caffeine 캐싱** (TTL 짧게, 예: 5분). 집계 테이블 불필요
8. **자유 보조 태그 관리**: **자동완성 + 고유 테이블** (`StockNoteCustomTag`: 사용자별 태그 마스터, 입력 시 자동완성 / 중복 병합)

### 결정 반영 추가 사항
- **Entity 추가**: `StockNoteCustomTag` (id, userId, tagValue, usageCount) — 자유 보조 태그 자동완성용 마스터
- **추가 유틸**: `BusinessDayCalculator` (간이 한국 영업일 계산, `stocknote/domain/util/` 혹은 `stocknote/infrastructure/util/` 배치는 ce:plan에서 확정)
- **잠금 처리**: `StockNoteVerification` 존재 여부로 판정 (추가 플래그 불필요)

---

## 작업 리스트 (Phase 2에서 상세 분해 예정)

- [ ] stocknote 도메인 Entity/Enum 승인 요청 (7개 Entity, 8개 Enum)
- [ ] 백엔드 헥사고날 계층 설계 (domain → application → infrastructure → presentation)
- [ ] 가격 스냅샷 스케줄러 설계 (KIS 연동 + 영업일 처리)
- [ ] 패턴 매칭 쿼리 설계 (QueryDSL, 태그 집합 매칭)
- [ ] 프론트엔드 신규 메뉴 + 3탭 + 드로워 컴포넌트 (`static/js/components/stocknote.js`)
- [ ] 차트 렌더링 (Chart.js scatter overlay on line chart)
- [ ] 대시보드 집계 API 설계

---

## 참고 파일 (기존 패턴)

- 도메인 계층 참조: `src/main/java/com/thlee/stock/market/stockmarket/salary/**`
- 기록-스냅샷 이력 패턴: `portfolio/infrastructure/persistence/StockPurchaseHistoryEntity.java`
- 프론트 컴포넌트 조립: `src/main/resources/static/js/app.js:29-42`
- Chart.js lifecycle 패턴: `src/main/resources/static/js/components/salary.js:9`
- 유사 브레인스토밍: `docs/brainstorms/2026-04-16-salary-usage-ratio-brainstorm.md`