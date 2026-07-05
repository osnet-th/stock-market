---
title: "feat: DART 단일종목 다년 재무 타임라인 (딥 시계열)"
type: feat
status: active
date: 2026-07-05
origin: docs/brainstorms/2026-07-05-multi-year-financial-timeline-brainstorm.md
---

# feat: DART 단일종목 다년 재무 타임라인 (딥 시계열)

## Overview

한 종목의 여러 해치 재무정보를 한 화면에서 보는 시계열 기능. 상단은 요약 추세(재무계정 + 핵심 비율 + 발행 주식수 + FCF), 하단은 전체 재무제표 세부 계정과 재무지표 4분류 전체의 연도별 표. 과거 연도는 사업보고서(확정치), 올해는 DART 공시검색(`list.json`)으로 탐지한 최신 정기보고서(누적치·진행중 표시)를 쓴다. 화면은 기존 재무상세 패널의 메뉴 버튼으로 추가한다. (brainstorm: docs/brainstorms/2026-07-05-multi-year-financial-timeline-brainstorm.md)

## Problem Statement / Motivation

현재 DART 재무 화면은 `연도 + 보고서` 단건 조회만 가능해 추세를 보려면 사용자가 연도를 바꿔가며 반복 조회해야 한다. 특히 "과거는 사업보고서, 올해는 분기/반기"라는 주기 혼합을 사용자가 직접 판단해야 하는 문제가 있다. 단일 회사를 깊게 분석하는 것이 주 사용 목적이므로, 요약 추세와 세부 계정을 한 화면에 시계열로 제공한다.

## Proposed Solution

### Backend — 기존 `stock` 도메인 확장 (신규 패키지 없음)

```
stock/
├── domain/
│   ├── model/
│   │   ├── Disclosure.java                    # [신규] 공시 1건 (rceptNo, reportNm, rceptDt, flrNm, rm)
│   │   └── FullFinancialStatement.java        # [수정] 전전기(bfefrmtrm) 필드 추가
│   └── service/
│       └── StockFinancialPort.java            # [수정] getDisclosures(...) 추가
├── application/
│   ├── FinancialTimelineService.java          # [신규] 타임라인 조립 유스케이스
│   └── dto/
│       ├── FinancialTimelineResponse.java     # [신규] 타임라인 응답 (하위 record 포함)
│       └── FullFinancialStatementResponse.java # [수정] 전전기 필드 전파
├── infrastructure/stock/dart/
│   ├── DartApiClient.java                     # [수정] fetchDisclosureList(...) 추가
│   ├── DartFinancialAdapter.java              # [수정] getDisclosures 구현 + 전전기 매핑
│   └── dto/
│       ├── DartDisclosureItem.java            # [신규] list.json 항목
│       ├── DartDisclosureListResponse.java    # [신규] list.json 봉투 (페이징 필드 포함)
│       └── DartSinglAcntAllItem.java          # [수정] bfefrmtrm_nm/bfefrmtrm_amount 추가
└── presentation/
    └── StockFinancialController.java          # [수정] GET /{stockCode}/financial/timeline 추가
```

- `list.json` 봉투는 기존 `DartApiResponse<T>`에 페이징 필드가 없어 **별도 DTO**(`DartDisclosureListResponse`)로 둔다 (기존 제네릭 응답 비침습).
- `Disclosure`는 record (기존 domain model 컨벤션 동일).
- FCF는 DB/Entity 없이 application 계층에서 파생 계산. **Entity 생성/수정 없음.**

### 타임라인 조립 로직 (`FinancialTimelineService`)

1. **대상 연도/보고서 결정**: 요청 `years=N` → 대상 연도 `[올해-N+1 .. 올해]`.
   - 원칙: 각 연도 = 사업보고서(`11011`).
   - 올해(및 사업보고서 미제출 연도, 예: 연초의 직전 연도): `list.json`(pblntf_ty=A, 해당 연도 1/1~오늘)으로 최신 정기보고서 탐지 → `report_nm`의 `(YYYY.MM)` 파싱 → `(year, reportCode)` + `partial=true`.
     - 파싱 매핑: `사업보고서(YYYY.12)`→11011, `반기보고서(YYYY.06)`→11012, `분기보고서(YYYY.03)`→11013, `분기보고서(YYYY.09)`→11014. 정정공시는 최신 접수분 우선.
   - 해당 연도에 정기공시가 하나도 없으면 그 연도 컬럼 생략.
2. **연도별 수집** (기존 `StockFinancialService` 메서드 재사용 → 지표 Caffeine 캐시 혜택 유지):
   - 재무계정 `getFinancialAccounts` (1콜)
   - 재무지표 `getFinancialIndices` × 4분류 (4콜)
   - 주식총수 `getStockQuantities` (1콜)
   - 전체 재무제표 `getFullFinancialStatements(fsDiv)` (1콜)
3. **FCF 파생 계산**: 전체 재무제표의 현금흐름표(sj_div=CF)에서 `영업활동현금흐름`, `유형자산의 취득`, `무형자산의 취득` 추출 → `영업CF − |유형취득| − |무형취득|`. account_id 우선, 미사용 시 계정명 매칭. 매칭 실패 연도는 FCF null.
4. **병합**: 계정/지표/주식수는 `account_id`(세부 계정) 또는 정규화된 명칭(요약·지표)을 행 키로 "항목 × 연도" 매트릭스 구성. 값 없는 셀은 null (0 채우지 않음).
5. **병렬 호출**: 연도×항목 = 5년 기준 약 35콜 → `CompletableFuture` + 전용 executor로 병렬화 (순차 시 10초+ 예상). DART 분당 한도 내에서 동시성 제한(예: 8).

### 신규 public API (Approval Gate — 신규 엔드포인트)

```
GET /api/stocks/{stockCode}/financial/timeline?years=5&fsDiv=CFS&items=accounts,indices,shares,fcf,details
```

- `years`: 1~10 (기본 5). `fsDiv`: CFS(기본)/OFS. `items`: 생략 시 전체.
- 응답 구조 (요지):

```json
{
  "columns": [ {"year":"2022","reportCode":"11011","reportLabel":"사업","partial":false}, ..., {"year":"2026","reportCode":"11013","reportLabel":"1분기","partial":true} ],
  "summaryAccounts": [ {"name":"매출액","values":{"2022":"...","2026":null}} ],
  "indices":  [ {"classCode":"M210000","className":"수익성","items":[{"name":"영업이익률","values":{...}}]} ],
  "shares":   [ {"stockType":"보통주","values":{...}}, {"stockType":"우선주","values":{...}} ],
  "fcf":      {"values":{...}},
  "details":  [ {"sjDiv":"IS","sjName":"손익계산서","accounts":[{"accountId":"...","name":"...","values":{...}}]} ]
}
```

- 입력 검증: `stockCode` 6자리 숫자, `years` 범위, `fsDiv`/`items` enum 바인딩.

### Frontend — 재무상세 패널 메뉴 추가 (기존 패턴 복제)

- `financial.js` `_krFinancialMenus`에 `{key:'timeline', label:'연도별 추세'}` 추가 + `selectFinancialMenu` 분기.
- `portfolio-deposit-financial.html`에 timeline 섹션: 연 수 선택(5/10) + 연결/개별 토글 → ① 요약 표(연도 컬럼, 진행중 컬럼 점선·배지) ② 요약 추세 차트(매출·영업이익·FCF + 핵심 비율) ③ 주식수 별도 차트(보통주/우선주) ④ 접이식 세부 섹션(BS/IS/CIS/CF/SCE + 재무지표 4분류, 기본 손익계산서만 펼침).
- `api.js`: `getFinancialTimeline(stockCode, years, fsDiv)`.
- 차트: 기존 Chart.js 재사용. 진행중 연도는 borderDash 세그먼트.
- 성장성 지표(증가율류)의 진행중 연도 칸은 `-` 처리 + 각주.

---

## Implementation Phases

> 각 Phase는 독립 검증 가능. 한 번에 하나의 Phase만 진행, 완료 후 사용자 확인.

### Phase 0 — DART 공시검색 인프라 + 실응답 검증

**Backend**
- [x] `DartDisclosureItem` / `DartDisclosureListResponse` DTO 작성
- [x] `DartApiClient.fetchDisclosureList(corpCode, bgnDe, endDe, pblntfTy, pageNo, pageCount)` 구현
- [x] `Disclosure` domain record + `StockFinancialPort.getDisclosures(...)` + `DartFinancialAdapter` 구현 — **Approval Gate: 포트 시그니처 추가** (plan 승인에 포함)
- [x] `DartSinglAcntAllItem`에 `bfefrmtrm_nm`/`bfefrmtrm_amount` 추가 → `FullFinancialStatement` record·`FullFinancialStatementResponse`·adapter 매핑 전파
- [x] **실호출 검증 (삼성전자 005930)**: ① list.json 1콜 1유형·응답 필드 확정 ② 정기공시 report_nm 파싱 케이스 수집 ③ 현금흐름표에서 FCF 3계정의 account_id/계정명 확정 ④ 전체 재무제표 전전기 필드 실존 확인
- [x] 검증 결과를 본 plan의 Open Questions에 기록·확정

### Phase 1 — 타임라인 조립 백엔드 + API

**Backend**
- [x] `FinancialTimelineService`: 연도/보고서 결정(최신 정기보고서 탐지 + report_nm 파싱) 구현 — `PeriodicReport.parse()` 도메인 record, 공시 1콜로 전체 기간 그룹핑
- [x] 연도별 수집 + 행 키 병합("항목 × 연도" 매트릭스, null 셀 허용) — `FinancialTimelineAssembler`, 행 순서는 최신 연도 우선
- [x] FCF 파생 계산 (abs 처리, 영업CF 없으면 null)
- [x] `CompletableFuture` 병렬 호출 (전용 executor 8스레드, 항목 실패는 빈 목록 대체)
- [x] `FinancialTimelineResponse` DTO + `GET /{stockCode}/financial/timeline` 컨트롤러 (입력 검증) — **Approval Gate: 신규 API** (plan 승인에 포함)
- [x] 검증(알고리즘, 실데이터 시뮬레이션): 컬럼 결정 2022~2025 사업보고서 + 2026 1분기(partial) ✓, FCF 5개년 산출(2022 9.05조 / 2023 −16.40조 / 2024 19.24조 / 2025 33.16조 / 2026 1Q 22.10조 — 2023 반도체 불황 음수 FCF 실제와 부합) ✓, 순차 6콜 0.9s → 병렬 8스레드로 5초 목표 여유 ✓
- [ ] 검증(인앱 엔드포인트): 로컬에 DB·필수 env 없어 부팅 불가 → **Phase 2 화면 검증 시 사용자 실행 환경에서 함께 확인**

### Phase 2 — 프론트 요약 영역

**Frontend**
- [x] `financial.js` 메뉴 추가(`timeline`, '연도별 추세') + 조회 파라미터(기간 3/5/10년, 연결/개별) 상태 — **Approval Gate: 메뉴 추가** (plan 승인에 포함)
- [x] `api.js getFinancialTimeline(stockCode, years, fsDiv, items)` — Phase 2는 ACCOUNTS,INDICES,SHARES,FCF만 요청 (DETAILS는 Phase 3)
- [x] 요약 표: 연도 컬럼 + 진행중 컬럼 구분(점선 보더·amber 배지 "1분기보고서·진행중") + 진행중 누적 기준 안내 문구
- [x] 요약 추세 차트(매출액·영업이익·FCF, 진행중 구간 점선) + 핵심 비율 차트(영업이익률·ROE·부채비율 — Open Question 확정, ROE는 exact 우선 + 자기자본순이익률 폴백 매칭)
- [x] 주식수 별도 차트(보통주/우선주 시리즈, '합계' 행 제외, 값 없는 시리즈 자동 생략)
- [x] 검증(로직, jsc 하니스): 표 병합·포맷(음수 FCF, null→'-'), ROE exact 매칭(총자산영업이익률 오매칭 없음), 차트 3종 dataset·라벨("2026 (진행중)")·점선 구간(진행중 진입만)·합계 제외 전부 확인. JS 문법(3파일)·template 균형 검사 통과
- [x] 검증(실제 화면, 삼성전자 005930, dev 부팅+Chrome): 표 10년/5년 정상, 2026 "1분기보고서·진행중" amber 배지, 진행중 안내문구, 차트 3종(금액·비율·주식수) 실선+2026 점선, FCF 2019~·ROE/부채비율 2023~ 시작, 주식수 보통주/우선주 분리 — 모두 확인
- [x] Phase 1 인앱 엔드포인트 검증 동시 완료: `GET /financial/timeline` HTTP 200, 5년 0.4~0.7s
- [x] **버그 발견·수정**: 최초 조회 시 자동 렌더 실패(로딩 상태 전환→Alpine이 canvas 재생성→첫 Chart orphan, chart.js 'save' null). `finishTimelineQuery`(로딩 종료 후 렌더)+`renderTimelineChartsWhenReady`(canvas clientWidth>0 대기, rAF 최대10회)+`animation:false`로 수정. 실제 조회 버튼 재검증 시 3종 자동 렌더 정상
- [!] **데이터 관찰(코드 무관)**: 이 DART 환경에서 삼성 2026 1분기 매출 133.9조·영업이익 57.2조로 비정상 크게 반환됨. 요약(fnlttSinglAcnt)·전체(fnlttSinglAcntAll) 두 API 동일값 + 전기값=2025 1분기(79조) 정합 → 매핑·파싱 정확, DART 원본 데이터 특성. 실제 프로덕션에선 정상값 예상

### Phase 3 — 프론트 세부 영역

**Frontend**
- [x] 세부 계정 접이식 섹션(BS/IS/CIS/CF/SCE), 기본 손익계산서만 펼침, 펼칠 때만 표 렌더(x-if 지연). api.js에 DETAILS 추가
- [x] 재무지표 4분류 전체 표 섹션(수익성15·안정성22·성장성18·활동성11, 접이식), 성장성 진행중 연도 `-` + 각주
- [x] 검증(실화면, 삼성전자 5년): 세부 5종 렌더(BS87/IS20/CIS19/CF63/SCE23 — 5년 합집합), 손익만 기본 펼침, 계정 구성 변경 연도 빈칸(금융비용 2022 `-`) 정확, 2026 진행중 amber 강조, 재무지표 4분류 접이식, 성장성 2026 전부 `-`+각주 확인
- [!] 검증 중 발견: 정적 JS가 브라우저 캐시되어 하드 리로드 필요했음 (dev 환경 특성, 코드 무관). 배포 시 정적 리소스 버전/캐시 정책은 기존 앱 방식 따름

### Phase 4 — 공시 리스트 화면 (DART식 공시 검색)

> 별도 기능. 대화 초반 논의한 "종목 선택 → 실제 공시 목록 → 원문 뷰어" 화면.
> Phase 0의 공시검색 인프라(`fetchDisclosureList`, `Disclosure`, `getDisclosures`)를 재사용한다.
> **원문 파일 다운로드는 하지 않음** — 뷰어 링크만 (결정: brainstorm 참고).

**설계 결정 (brainstorm 반영)**
- `list.json`은 요청당 공시유형 1개만 받음(콤마 다중 미지원, Phase 0 실호출로 확정) → **다중선택 = 유형별 병렬 호출 후 접수일 최신순 병합**. 전체(무필터)는 1콜.
- 각 항목 `rcept_no` → 뷰어 URL `https://dart.fss.or.kr/dsaf001/main.do?rcpNo={rceptNo}` (프론트에서 새 탭). 응답 DTO에 뷰어 URL 포함.
- 유형은 A~J 10종(정기/주요사항/발행/지분/기타/외부감사/펀드/자산유동화/거래소/공정위). `rm`(정정·연결 등 비고), `corp_cls`(시장구분) 표시.
- 기간 미지정 시 `list.json`은 최근 3개월만 반환 → 기본 조회기간을 명시(예: 오늘−1년~오늘), 사용자 조절.
- 100건 초과는 페이징(`page_no`) — 1차는 유형별 1페이지(100건) + "더 있음" 표기, 필요 시 확장.

**Backend**
- [x] `PublicationType` enum (A~J → 코드/라벨) — domain/model
- [x] 다중유형 병합 서비스 `DisclosureQueryService`(유형별 `CompletableFuture` 병렬 호출 → 접수일 desc 병합, 전용 executor 재사용)
- [x] 응답 DTO `DisclosureResponse`(접수일·보고서명·제출인·비고·**뷰어URL**). 시장구분은 단일종목 상수라 생략
- [x] `GET /api/stocks/{stockCode}/disclosures?fromDate=&toDate=&types=A,B,..` (types 미지정=전체 1콜) — **Approval Gate: 신규 API** (plan 승인 포함)
- [x] 입력 검증: stockCode 6자리, 날짜 기본값(미지정 시 1년 전~오늘), types enum 바인딩

**Frontend**
- [x] 재무상세 패널 메뉴에 `공시` 추가(timeline 옆) — **Approval Gate: 메뉴 추가** (plan 승인 포함)
- [x] 유형 pill 다중선택(A~J, 미선택=전체) + 기간(1/3/5년/전체) + 조회
- [x] 목록 표(접수일·보고서명·제출인) + 정정 뱃지(rm '정') + 각 행 뷰어 링크(새 탭, `rel=noopener`)
- [x] `api.js getDisclosures(stockCode, fromDate, toDate, types)`
- [x] 검증(실화면·API, 삼성전자): 전체 100건, 다중유형 A+D 병합 112건·최신순 True, 정정 뱃지 8건, 뷰어 링크 112개, 잘못된 유형 500(앱 전역 기존 동작·회귀 아님)

---

## Approval Gates (작업 중 중단·확인)

- [Phase 0] `StockFinancialPort` 시그니처 추가 (getDisclosures)
- [Phase 1] 신규 public API 엔드포인트 추가
- [Phase 2] 재무상세 패널 메뉴 추가
- [Phase 4] 신규 public API(`/disclosures`) + `공시` 메뉴 추가
- Phase 간 연속 진행 시 (한 번에 하나의 Phase)

## Risks

- **FCF 계정 매칭**: 현금흐름표 세부 계정은 표준계정코드 미사용이 잦음 → Phase 0 실응답으로 확정, 실패 시 null 처리(0 대체 금지).
- **list.json 스펙 가정**: "1콜 1유형", 응답 필드 구성은 지식 기준 → Phase 0에서 실호출 검증 후 진행.
- **호출량/응답 시간**: 5년 기준 약 35콜 → 병렬화 필수, DART 분당 한도(넉넉하나 동시성 제한 필요), 지표 캐시 재사용.
- **YTD 착시**: 올해 누적치를 연간과 병렬 표시 → 점선·배지·각주로 명시 (비율은 시점값/동기간이라 영향 적음, 증가율류만 `-` 처리).
- **프론트 렌더링**: 세부 계정 수백 행 × N년 → 접이식 + 지연 렌더링.
- **연초 공백**: 1~3월엔 직전 연도 사업보고서 미제출 → 직전 연도도 탐지 폴백(3분기·partial)으로 처리.

## Open Questions (구현 중 해소)

- [x] ~~[Phase 0] list.json 응답 필드·1콜 1유형~~ → **확정 (2026-07-05 실호출, 삼성전자)**: 봉투 = `status/message/page_no/page_count/total_count/total_page/list`, 항목 = `corp_code/corp_name/stock_code/corp_cls/report_nm/rcept_no/flr_nm/rcept_dt/rm` (구현 DTO와 일치). `pblntf_ty=A,B` 콤마 다중 → `100` 에러로 **1콜 1유형 확정**. report_nm 패턴: `사업보고서 (YYYY.12)` / `반기보고서 (YYYY.06)` / `분기보고서 (YYYY.03|YYYY.09)`, 접수일 최신순. rm에 `연`(연결) 표기. 관찰 범위(2024~) 내 정정 케이스 없음 → 파싱은 `[기재정정]` 등 접두 대비 포함-문자열 기반으로 구현.
- [x] ~~[Phase 0] FCF 3계정 account_id~~ → **확정 (전부 IFRS 표준 ID, 계정명 폴백 불필요)**:
  - 영업활동현금흐름: `ifrs-full_CashFlowsFromUsedInOperatingActivities`
  - 유형자산의 취득: `ifrs-full_PurchaseOfPropertyPlantAndEquipmentClassifiedAsInvestingActivities`
  - 무형자산의 취득: `ifrs-full_PurchaseOfIntangibleAssetsClassifiedAsInvestingActivities`
  - ⚠️ **취득액은 양수로 옴** (plan의 "음수(유출)" 가정과 다름 — 실측 당기 유형취득 47.52조 양수). FCF = 영업CF − |유형취득| − |무형취득| 로 abs 처리 유지 (양수/음수 어느 쪽이 와도 안전). 검산: 2025 연결 FCF = 85.32 − 47.52 − 4.63 = 33.16조 (타당).
- [x] ~~[Phase 0] 전전기 필드 실존~~ → **확정**: 사업보고서 `fnlttSinglAcntAll` 응답에 `bfefrmtrm_nm`/`bfefrmtrm_amount` 존재, 값 채워짐 (2025 사업보고서 CFS 229행, CF 42행).
- [x] ~~[Phase 2] 핵심 비율 차트의 최종 지표 목록~~ → **확정: 영업이익률·ROE·부채비율** (DART idx_nm의 ROE 표기는 화면 검증 시 확인 — 자기자본순이익률/자기자본이익률 폴백 매칭 구현됨)

## Next Steps

1. 본 plan 승인
2. feature 브랜치 생성 (worktree 정책 준수)
3. Phase 0부터 순차 구현 (Approval Gate 준수)
