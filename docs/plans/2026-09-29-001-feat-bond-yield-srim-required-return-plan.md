---
title: "feat: 한국자산평가 채권금리 날짜별 조회·캐싱 및 S-RIM 요구수익률 선택"
type: feat
issue: 131
issue_url: https://github.com/osnet-th/stock-market/issues/131
status: active
date: 2026-09-29
approved: "2026-09-29 태형님 승인 (기능 plan + KTD11 구조 분리 + 단위 테스트 시나리오)"
workflow_exception: "클라우드 세션에 compound-engineering(/ce:plan·/ce:work·/ce:review)이 없어 planning-gate·briefing·review 게이트 절차를 수동 적용한다 (2026-09-29 태형님 승인)"
branch: claude/inspiring-wright-rljbsn
branch_exception: "클라우드 세션은 지정 브랜치에만 push할 수 있어 issue/131-{slug} 대신 세션 브랜치를 쓴다 (2026-09-29 태형님 승인). validate-plan.sh의 branch 형식 검사 1건은 이 예외로 실패한다."
worktree: /home/user/stock-market-issue-131
worktree_note: "/home/user/stock-market을 가리키는 심볼릭 링크. checkpoint-guard.sh가 경로에서 이슈 번호를 읽기 때문에 둔다."
brainstorm: docs/brainstorms/2026-09-28-bond-yield-srim-required-return-brainstorm.md
test_plan_status: approved
schema_plan_status: none
allowed_paths:
  - src/main/java/com/thlee/stock/market/stockmarket/economics/domain/model/BondYieldType.java
  - src/main/java/com/thlee/stock/market/stockmarket/economics/domain/model/BondYield.java
  - src/main/java/com/thlee/stock/market/stockmarket/economics/domain/model/BondYieldSnapshot.java
  - src/main/java/com/thlee/stock/market/stockmarket/economics/domain/model/BondYieldLookup.java
  - src/main/java/com/thlee/stock/market/stockmarket/economics/domain/model/BondCreditGrade.java
  - src/main/java/com/thlee/stock/market/stockmarket/economics/domain/service/BondYieldPort.java
  - src/main/java/com/thlee/stock/market/stockmarket/economics/domain/exception/BondYieldFetchException.java
  - src/main/java/com/thlee/stock/market/stockmarket/economics/domain/exception/BondYieldParseException.java
  - src/main/java/com/thlee/stock/market/stockmarket/economics/application/BondYieldQueryService.java
  - src/main/java/com/thlee/stock/market/stockmarket/economics/infrastructure/korea/koreaap/**
  - src/main/java/com/thlee/stock/market/stockmarket/economics/presentation/BondYieldController.java
  - src/main/java/com/thlee/stock/market/stockmarket/economics/presentation/dto/BondYieldResponse.java
  - src/main/java/com/thlee/stock/market/stockmarket/infrastructure/web/GlobalExceptionHandler.java
  - src/main/resources/application.yml
  - src/main/resources/static/js/api.js
  - src/main/resources/static/js/components/company-report.js
  - src/main/resources/static/js/components/stock-eval.js
  - src/main/resources/static/partials/company-report.html
  - src/main/resources/static/partials/stock-eval.html
  - src/test/java/com/thlee/stock/market/stockmarket/economics/application/BondYieldQueryServiceTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/economics/infrastructure/korea/koreaap/KoreaApBondRateParserTest.java
  - src/test/resources/koreaap/**
  - docs/plans/tests/2026-09-29-131-bond-yield-srim-required-return-test-plan.md
  - docs/plans/2026-09-29-001-feat-bond-yield-srim-required-return-plan.md
  - docs/brainstorms/2026-09-28-bond-yield-srim-required-return-brainstorm.md
  - .claude/issues/131/**
blocked_paths:
  - src/main/java/com/thlee/stock/market/stockmarket/companyreport/**
  - src/main/java/com/thlee/stock/market/stockmarket/stockevaluation/**
  - src/main/java/com/thlee/stock/market/stockmarket/economics/infrastructure/korea/ecos/**
  - src/main/java/com/thlee/stock/market/stockmarket/economics/infrastructure/global/**
  - src/main/java/com/thlee/stock/market/stockmarket/economics/infrastructure/persistence/**
  - src/main/java/com/thlee/stock/market/stockmarket/economics/infrastructure/scheduler/**
  - src/main/java/com/thlee/stock/market/stockmarket/infrastructure/security/**
  - src/main/resources/db/**
  - src/main/resources/static/index.html
  - src/main/resources/static/js/app.js
  - build.gradle
---

# 한국자산평가 채권금리 날짜별 조회·캐싱 및 S-RIM 요구수익률 선택 (#131)

## 요약
S-RIM 요구수익률을 채울 수 있도록 한국자산평가 기준수익률을 날짜별로 조회하는 API를 `economics` 도메인에 추가한다.
- **조회 대상:** 국고채 6개 만기, 공모 무보증 회사채 AAA~BBB-
- **캐시와 폴백:** 요청할 때만 외부를 조회한다. 결과는 기준일 단위로 메모리에 캐싱한다. 날짜 전체 데이터가 없으면 최대 10일 전까지 거슬러 올라간다.
- **화면:** 기업 리포트와 종목 평가의 S-RIM에 금리 선택 영역을 붙인다. 종류·등급·만기를 고르고 적용 버튼을 누르면 요구수익률이 채워진다.
- **바뀌지 않는 것:** S-RIM 계산, 저장 구조, 기존 API
- **출처 분리:** 금리 조회는 도메인 포트(interface)로 분리하고, 외부 요청은 infrastructure 어댑터가 전담한다. 출처 API를 바꿔도 서비스·API·화면은 바뀌지 않는다.

## 작업 리스트
- [x] M0 한국자산평가 응답 실측 (2026-09-29, 결과는 구현 단위 M0 절)
- [x] U1 도메인 모델·금리 조회 포트·출처 무관 예외
- [x] U3 날짜 폴백·이중 캐시 조회 서비스 (2026-09-29 태형님 승인으로 U2보다 먼저. S1~S15 테스트가 U3 없이는 컴파일되지 않아 U2 테스트도 돌릴 수 없음)
- [x] U2 한국자산평가 어댑터 + 설정 + 예외 등록 → 체크포인트 CP1
- [x] U4 금리 조회 API → 체크포인트 CP2
- [x] U5 기업 리포트 S-RIM 금리 선택 UI
- [x] U6 종목 평가 S-RIM 금리 선택 UI → 체크포인트 CP3
- [x] 단위 테스트 — 서비스 S1~S15는 U1·U3 구현 전에, 파서 P1~P7은 M0 후 U2 구현 전에 작성 (승인된 시나리오). S1~S15: 작성·실패 확인 후 U1·U3 구현으로 15건 통과(5회 반복 안정). P1~P7: 실측 fixture로 작성·실패 확인 후 U2 구현으로 7건 통과 (2026-09-29)
- [ ] 검증 (태형님 선택 방식)

## 배경 / 현재 상태
**현재 흐름**
1. 두 화면 모두 요구수익률(%)을 사용자가 직접 입력한다. 기본값은 없다.
2. 프론트가 소수로 변환한다(`_crSrimInput` / `_seSrimInput`, 자리 이동 -2).
3. `POST /api/company-reports/srim/calculate`를 호출한다.
4. `SrimInput.requiredReturn`은 0보다 커야 하고, draft가 아니면 필수다.

**문제점**
- 시스템에 채권 기준수익률 출처가 없다.
- ECOS 100대 지표는 최신값 3종만 있어 날짜·등급·만기를 고를 수 없다.
- (구현 중 발견, 2026-09-29) JDK HttpClient 기본값(HTTP/2)으로 한국자산평가를 요청하면 응답 없이 타임아웃된다.
  - HTTP/2: 10초 타임아웃. HTTP/1.1: 1.2초에 200.
  - U2 스모크 테스트에서 확인하고 바로 HTTP/1.1로 고정했다(6452799). 승인 전 수정이라 절차를 어겼고, CP1에서 태형님 사후 승인을 받았다.

**확인된 수집 계약** (2026-09-28 실측, brainstorm)
- 요청: `GET /vl/valuation01_01/bondRates?ymd=yyyyMMdd&type=Y&searchType=0&lang=kor` → JSON 63행
- 행 식별: 국고채 `GMRI_CODE=A101`. 회사채 공모 무보증은 `GMRI_TYPE=회사채<br/>(공모)`, `GMRI_SUBTYPE=무보증`, `GMRI_BOND=등급`으로 구분한다(M0에서 `<br/>` 확인).
- 만기 키: `M036`=3년, `M060`=5년 형식

**M0에서 확인한 것** (2026-09-29, 상세는 구현 단위 M0 절)
- 응답 envelope: 최상위 JSON 배열
- 전체 만기 키: `M003`~`M600` 15개
- 휴일·과거 날짜: 휴일은 `[]`, 과거 날짜는 그날 데이터
- 요청 헤더: 필요 없음

## 요구사항 원장

| ID | 요구사항 | 출처 | 이번 범위 | 근거 / 담당 |
|---|---|---|---|---|
| REQ-1 | 국고채 1·3·5·10·20·30년 기준수익률을 요청 시 조회한다 | 이슈 작업1 | 포함 | U2, U3 |
| REQ-2 | 공모 무보증 회사채 AAA~BBB- 10개 등급의 제공 만기(15개) 기준수익률을 요청 시 조회한다 | 이슈 작업1 · brainstorm 범위 | 포함 | U2, U3 |
| REQ-3 | 동일 기준일 전체 응답을 캐싱해 재요청과 등급·만기 변경에 재사용한다 | 이슈 작업2·완료조건2 | 포함 | U3 서버 캐시, U5·U6 화면은 받은 전체 응답에서 선택 |
| REQ-4 | 일별 배치와 금리 DB 저장을 하지 않는다 | 이슈 작업2 | 포함 | 신규 스케줄러·저장소 없음 (blocked_paths) |
| REQ-5 | 두 S-RIM 화면에서 직접 입력을 유지하고, 종류·등급·만기 선택 후 적용 버튼으로 요구수익률을 채운다 | 이슈 작업3·완료조건5 | 포함 | U5, U6 |
| REQ-6 | S-RIM 계산식을 유지한다 | 이슈 작업3 | 포함 | companyreport 수정 없음 (blocked_paths) |
| REQ-7 | 요청 날짜 전체 데이터가 없으면 하루씩 최대 10일 이전까지(요청일 포함 11개 날짜) 조회한다 | 이슈 작업4·완료조건3 · 폴백 정책 승인 | 포함 | U3 |
| REQ-8 | 특정 등급·만기만 결측이면 날짜 폴백 없이 미제공으로 표시하고 적용할 수 없다 | 이슈 작업4 · brainstorm 범위 | 포함 | U2 null 정규화, U3 폴백은 날짜 전체 기준, U5·U6 |
| REQ-9 | 통신·파싱 오류를 빈 데이터와 구분해 실패로 처리한다 | 이슈 작업5·완료조건4 | 포함 | U2, U3, U4 |
| REQ-10 | 요청일·실제 적용 기준일·출처를 표시한다 | 이슈 작업5·완료조건5 | 포함 | U4, U5, U6 |
| REQ-11 | 실제 기준일 금리 캐시와 요청일→적용일 연결 캐시를 분리하고 폴백 연결은 짧게 캐싱한다 | 이슈 작업6 | 포함 | U3 |
| REQ-12 | 국고채·공모 무보증 회사채가 금융채·사모채와 섞이지 않고 날짜·등급·만기가 올바르게 매핑된다 | 이슈 완료조건1 | 포함 | U2 |
| REQ-13 | 동시 요청 합치기와 캐시 만료 후 재조회가 동작한다 | 이슈 완료조건2 | 포함 | U3 |
| REQ-14 | 한도 초과 시 데이터 없음 안내와 직접 입력을 제공한다 | 이슈 완료조건3 · 폴백 정책 | 포함 | U4, U5, U6 |
| REQ-15 | 외부 오류·잘못된 응답을 정상 금리로 캐싱하지 않는다 | brainstorm 범위 | 포함 | U3 |
| REQ-16 | 적용하면 기존 계산 결과를 무효화하고, 사용자가 명시적으로 재계산한다 | brainstorm 범위 | 포함 | U5, U6 |
| REQ-17 | 외화(USD) S-RIM에는 원화 채권금리를 적용하지 않는다 | brainstorm 범위 | 포함 | U5 (종목 평가는 국내 전용) |
| REQ-18 | 원본 % 단위와 S-RIM 소수 단위 변환을 경계에서 명시한다 | brainstorm 값 규칙 | 포함 | U4는 %로 응답, 변환은 기존 `_crSrimInput`/`_seSrimInput` |
| REQ-19 | 금리 선택 근거는 저장하지 않고 입력 화면에서만 표시한다 | 2026-09-29 태형님 결정 | 포함 | U5, U6 (S-RIM 저장 구조 수정 없음) |
| REQ-20 | 캐시 TTL·최대 크기를 설정으로 바꿀 수 있다 | brainstorm 권장 접근 | 포함 | U2 application.yml, U3 |
| REQ-21 | 금리 DB 저장·배치·일괄 과거 소급·차트 | 이슈 제외 범위 | 제외 | 이슈 제외 |
| REQ-22 | S-RIM 공식 변경 및 보류한 수익 상태 컬럼 재설계 | 이슈 제외 범위 | 제외 | 이슈 제외 |
| REQ-23 | 종목 신용등급 자동 판정 | brainstorm 제외 범위 | 제외 | 선택은 사용자 몫 |
| REQ-24 | Excel 파일 수집 | brainstorm 후보 3 | 제외 | 파일 구조 미검증, 우선 구현 대상 아님 |
| REQ-25 | 금리 조회 interface를 분리하고 외부 요청은 infrastructure가 전담해, 출처 API를 바꿔도 서비스 로직에 영향이 없게 한다 | 2026-09-29 태형님 요청 (plan 승인 조건) | 포함 | U1 포트·도메인 예외, U2 어댑터, U3는 포트에만 의존 (KTD11) |

## 핵심 기술 결정
**KTD1 — `economics` 도메인에 둔다.**
- 금리는 시장 지표이고, 외부 수집 선례(ECOS, TradingEconomics)가 같은 도메인에 있다.
- 신규 패키지는 `economics.infrastructure.korea.koreaap`이다. companyreport·stockevaluation은 수정하지 않는다.

**KTD2 — 공개 화면이 쓰는 JSON(`bondRates`)을 전용 RestClient로 호출한다.**
- 클라이언트는 JDK HttpClient 기반이고, connect 3s / read 7s다. 선례는 `dartRestClient`다. 기본 `restClient` 빈에는 타임아웃이 없어서 쓰지 않는다.
- HTML·Excel은 쓰지 않는다.
- `maxDate`도 쓰지 않는다. 폴백이 하루 단위 조회로 승인됐기 때문이다(YAGNI).
- HTTP/1.1로 고정한다. HTTP/2 요청이 타임아웃되기 때문이다(문제점 절, 2026-09-29 사후 승인).

**KTD3 — 행 분류는 조합으로 판정하고, 등급 문자열만으로 매핑하지 않는다.**
- 국고채: `GMRI_CODE=A101`
- 공모 무보증 회사채: 세 조건을 모두 만족하는 행
  - `GMRI_TYPE`에서 태그·공백을 제거한 값이 `회사채(공모)`
  - `GMRI_SUBTYPE=무보증`
  - `GMRI_BOND`가 10개 등급 중 하나
- 판정 결과:
  - 행이 0개면 "빈 날짜"로 본다.
  - 행은 있는데 대상 행이 하나도 없으면 구조 변경을 의심해 파싱 오류로 본다.
  - 같은 대상 행이 중복되면 파싱 오류로 본다.
  - 대상 행은 있지만 제공된 값이 하나도 없으면(전부 `-`·공백·null) 빈 날짜로 보고 폴백한다. 구조 변경(키 누락·숫자 아님·대상 행 없음·중복)은 파싱 오류로 잡는다(2026-09-29 CP1 확정).

**KTD4 — 값 규칙.**
- `-`와 공백은 null(미제공)로 바꾼다. 숫자는 % 단위 `BigDecimal`로 둔다. 그 밖의 문자열은 파싱 오류다.
- 추출 범위:
  - 국고채: `M012·M036·M060·M120·M240·M360`
  - 회사채: 15개 만기 키 전부(`M003`~`M600`, 개월 수 3자리. M0 확인)
- API도 %로 응답한다. 소수 변환은 기존 프론트 변환이 맡는다.

**KTD5 — 캐시 2개 (application, Caffeine 직접 빌드, 선례 `GlobalIndicatorCacheService`).**
- 금리 캐시는 기준일 → 스냅샷이다. 데이터 있음과 빈 날짜를 모두 담고, Expiry로 TTL을 나눈다.

  | 값 | TTL |
  |---|---|
  | 데이터 있음, 기준일 < 오늘 | 24h |
  | 데이터 있음, 기준일 = 오늘 | 10분 (당일 부분 공시 대비) |
  | 빈 날짜 | 10분 |

- 연결 캐시는 요청일 → 적용일 또는 한도 초과다.

  | 값 | TTL |
  |---|---|
  | 요청일 = 적용일 < 오늘 | 24h |
  | 요청일 = 적용일 = 오늘 | 10분 |
  | 폴백 연결 | 10분 |
  | 한도 초과 | 10분 |

- 동시 요청 합치기: 해석 전체를 연결 캐시 `get(요청일, 해석)`으로 감싸고, 날짜별 조회는 금리 캐시 `get(기준일, 로더)`로 감싼다.
- 로더 예외는 캐시되지 않는다(Caffeine 계약). 그래서 오류 결과는 캐시에 남지 않는다.
- 실패 캐시는 도입하지 않는다. 단일 사용자 수준 트래픽이고, 벽시계 상한이 대기 시간을 제한한다.
- 연결은 있는데 적용일 금리를 다시 불러온 결과가 비어 있으면, 연결을 무효화하고 한 번 다시 해석한다.
- 두 캐시의 최대 크기는 각 100이다.
- TTL·크기·상한은 application.yml 값을 `@Value`로 읽는다. 선례는 news application이다. 테스트를 위해 Clock·Ticker를 주입할 수 있게 한다.

**KTD6 — 폴백 중 오류가 나면 즉시 실패한다.**
- 통신·파싱 오류가 나면 다음 날짜로 건너뛰지 않는다.
- 전체 해석에 벽시계 상한 20초를 둔다. 다음 날짜를 조회하기 전에 상한을 넘겼으면 조회 시간 초과로 실패한다.
- 이 경우 통신 실패와 같은 도메인 예외 `BondYieldFetchException`을 쓰고, 메시지로 시간 초과를 알린다(KTD11).

**KTD7 — 요청일 규칙.**
- 생략하면 오늘(KST)로 본다.
- 형식 오류와 미래 날짜는 400이다.
- 과거 하한은 두지 않는다.

**KTD8 — 오류 응답.**
- 통신 실패와 파싱 실패는 모두 502 `EXTERNAL_API_ERROR`로 응답하고, 메시지로 구분한다. 한도 초과는 200 NOT_FOUND다(API 계약 참고).
- 출처와 무관한 도메인 예외 2종을 `GlobalExceptionHandler`의 외부 API 그룹에 등록한다(KTD11).
  - `BondYieldFetchException`: 연결·타임아웃·HTTP 오류, 조회 시간 초과
  - `BondYieldParseException`: 응답 해석 실패
- 예외 메시지는 사용자용 문구로 쓰고, 세부 원인은 cause와 로그로 남긴다.
  - 메시지에는 출처의 필드명·만기 키·원문 값을 넣지 않는다. 이런 세부는 경고 로그로만 남긴다(2026-09-29 CP1 가드 리뷰 반영).

**KTD9 — 프론트는 두 화면에 복제하고, 공용 헬퍼는 company-report.js에 둔다.**
- 선례: #124 KTD8, 기존 `_crSrim*` 재사용 방식
- `index.html`·`app.js`는 바꾸지 않는다.
- 금리 선택 영역은 래퍼에서 `@input.stop @change.stop`으로 막는다. 섹션 단위 결과 초기화를 피하기 위해서다. 결과 초기화(`*SrimChanged()`)는 적용 버튼만 호출한다.

**KTD10 — 선택 근거는 저장하지 않는다.**
- 적용 근거(종류·등급·만기·기준일)는 요구수익률 값이 적용한 값 그대로일 때만 입력 화면에 표시한다.
- 직접 수정하면 표시가 사라진다.

**KTD11 — 금리 조회 포트로 출처를 격리한다.** (2026-09-29 태형님 요청)
- 금리 정보 조회 interface는 도메인 포트 `BondYieldPort`다. 서비스(`BondYieldQueryService`)는 이 포트에만 의존한다.
  - 포트 계약: 기준일 하루치를 도메인 스냅샷으로 돌려준다. 데이터가 없으면 빈 스냅샷, 실패하면 도메인 예외다. 출처 이름도 포트가 제공한다.
- 외부 요청과 응답 해석은 infrastructure 어댑터(`economics.infrastructure.korea.koreaap`)가 전담한다.
  - 어댑터 밖으로 나가지 않는 것: HTTP 호출, 요청 파라미터·헤더, JSON 필드(`GMRI_*`), 만기 키(`M036` 등)
  - 어댑터는 자기 오류를 도메인 예외(`BondYieldFetchException` / `BondYieldParseException`)로 바꿔 던진다. 출처별 예외 클래스는 두지 않는다.
- 도메인 모델(종류·등급·만기 개월·수익률 %)과 조회 대상(국고채 6개 만기, 공모 무보증 10개 등급)은 출처와 무관한 도메인 개념이다.
- 설정을 나눈다.
  - 출처 접속 설정: `economics.api.korea.koreaap.*`
  - 캐시·폴백 정책: 출처 무관 `economics.bond-yield.*`
- 응답의 `source`는 포트가 준 출처 이름을 쓴다. 서비스·API·화면에는 출처 이름을 하드코딩하지 않는다.
- 출처 교체 시 할 일: 포트를 구현한 새 어댑터를 추가하고 한국자산평가 어댑터를 빼는 것뿐이다. 서비스·캐시·폴백·API·화면·예외 매핑은 바뀌지 않는다.
- 출처 선택 설정(여러 어댑터 전환)은 지금 두지 않는다(YAGNI).

## API 계약
`GET /api/economics/bond-yields?date=YYYY-MM-DD` — 로그인 필요(기존 보안 설정 그대로)

**응답 필드**

| 필드 | 값 |
|---|---|
| `source` | 포트가 제공한 출처 이름 (현재 `한국자산평가`) |
| `requestedDate` | 요청일. 생략했으면 오늘(KST) |
| `baseDate` | 실제 적용 기준일. NOT_FOUND면 null |
| `status` | `FOUND`(요청일 데이터) / `FALLBACK`(이전 날짜 적용) / `NOT_FOUND`(한도 초과) |
| `maxFallbackDays` | 10 |
| `yields[]` | `type`(`TREASURY`/`CORPORATE_PUBLIC_UNSECURED`), `grade`(회사채만, 예: `BBB-`), `maturityMonths`, `rate`(% 숫자, 미제공이면 null) |

**오류**
- 400 `BAD_REQUEST`: 날짜 형식 오류, 미래 날짜
- 502 `EXTERNAL_API_ERROR`: 통신 실패(연결·타임아웃·HTTP 오류·조회 시간 초과) 또는 응답 해석 실패. 메시지로 구분한다.

## 구현 단위

### M0 응답 실측 (선행, 네트워크 허용 필요)
- 실측할 응답:
  - 최근 평일
  - 주말 1건
  - 공휴일 1건
  - 30일 이상 지난 과거 평일
  - `maxDate` (참고)
- 확인할 항목:
  - envelope
  - 행 필드와 전체 만기 키
  - 빈 응답 형태
  - 휴일에 직전 영업일 값을 돌려주는지
  - 필요한 요청 헤더
  - 대상 행 개수(국고채 1, 공모 무보증 10)
- 가정과 다르면 중단하고 plan을 갱신해 재승인받는다. 예: 휴일에 이전 날짜 데이터를 돌려주는 경우

**실측 결과 (2026-09-29, 가정과 일치 → plan 변경 없음)**
- envelope: 최상위 JSON 배열이다. 평일은 63행(25,777B)이다. 행에 날짜 필드가 없으므로 기준일은 요청한 `ymd`다.
- 빈 날짜: 주말(9/26·9/27), 평일 공휴일(5/5), 공시 전 오늘(9/29 11시 KST) 모두 `[]`다. 직전 영업일 값을 돌려주지 않는다.
- 과거 날짜: 8/20은 그날 데이터가 온다. 날짜 파라미터가 반영된다.
- 요청 헤더: 헤더 없이도 200이다. 응답은 헤더가 있을 때와 같다. `maxDate`는 `text/plain`으로 `2026-09-28`을 준다(KTD2대로 쓰지 않는다).
- 행 필드:
  - 분류: `GMRI_SEQ`, `P_TYPE`, `GMRI_CODE`, `GMRI_TYPE`, `GMRI_SUBTYPE`, `GMRI_BOND`, `GMRI_INT`, `SECTOR_NAME`
  - 만기 키 15개: `M003·M006·M009·M012·M018·M024·M030·M036·M048·M060·M084·M120·M240·M360·M600`
- 값: 앞에 공백이 붙은 숫자 문자열(`" 4.527"`)이고, 미제공은 `-`다.
  - 국고채(A101)는 15개 만기 모두 값이 있다.
  - 공모 무보증 10개 등급은 30년·50년이 `-`다(9/28, 8/20 동일).
- 대상 행:
  - 국고채: `A101`(국채/국고채) 1행
  - 공모 무보증: `F212·F221·F222·F223·F231·F232·F233·F241·F242·F243` 10행
  - `GMRI_TYPE`은 `회사채<br/>(공모)`다. brainstorm 표기 `<br>`와 달리 `<br/>`이지만, KTD3의 태그 제거 규칙으로 처리된다.
  - 같은 등급 문자열이 있는 행: 사모(`회사채<br/>(사모)`/무보증 AAA~BBB-), 금융채·특수채·커버드본드. `GMRI_TYPE` 조합으로 구분된다.
- 검증값: 9/28 공모 무보증 BBB- 3년 10.588%, 5년 10.986%. brainstorm 실측값과 같다.
- fixture: `src/test/resources/koreaap/bond-rates-20260928.json`(평일 원문), `bond-rates-20260505-holiday.json`(공휴일 원문 `[]`)

### U1 도메인 모델·금리 조회 포트·출처 무관 예외 (선행 없음)
- `economics.domain.model` (순수 Java):
  - `BondYieldType`: 국고채 / 공모 무보증 회사채. 국고채 조회 대상 만기(12·36·60·120·240·360개월)를 도메인 상수로 둔다.
  - `BondCreditGrade`: AAA~BBB- 10개, 표시 문자열 포함
  - `BondYield`: 종류, 등급, 만기 개월, 수익률 %(nullable)
  - `BondYieldSnapshot`: 기준일, 금리 목록, 빈 날짜 판정
  - `BondYieldLookup`: 요청일, 적용일, 상태, 출처 이름, 스냅샷
- `economics.domain.service.BondYieldPort` (금리 정보 조회 interface, KTD11)
  - 기준일 하루치를 조회한다.
  - 데이터가 없으면 빈 스냅샷을 돌려준다.
  - 통신·파싱 실패는 도메인 예외로 알린다.
  - 출처 이름을 제공한다.
- `economics.domain.exception`:
  - `BondYieldFetchException`
  - `BondYieldParseException`

### U2 한국자산평가 어댑터 (선행 M0, U1)
- `koreaap/config`:
  - Properties: base-url, connect/read timeout, user-agent, M0에서 필요하다고 확인된 헤더
  - 전용 RestClient 설정
- `koreaap` 클래스:
  - 클라이언트: 날짜 → 응답 본문. `RestClientException`은 도메인 `BondYieldFetchException`으로 바꾼다.
  - 파서: 본문 → 도메인 스냅샷. KTD3·KTD4 규칙을 따르고, 실패 시 도메인 `BondYieldParseException`을 던진다.
  - 어댑터: `BondYieldPort` 구현, 출처 이름 `한국자산평가`
- 출처별 예외 클래스는 만들지 않는다(KTD11).
- application.yml 설정을 두 섹션으로 나눈다(KTD11).
  - `economics.api.korea.koreaap`: 접속 설정
  - `economics.bond-yield.cache`: ttl-hours 24, short-ttl-minutes 10, max-size 100, lookup-timeout-seconds 20
- `GlobalExceptionHandler` 외부 API 그룹에 도메인 예외 2종을 등록한다(KTD8).

### U3 조회 서비스 (선행 U1)
- `BondYieldQueryService.lookup(요청일)`: KTD5·KTD6·KTD7을 따른다.
- `BondYieldPort`에만 의존하고, infrastructure 클래스는 참조하지 않는다(KTD11).
- 해석 순서:
  1. 연결 캐시를 먼저 본다.
  2. 없으면 요청일부터 D-10까지 금리 캐시를 거쳐 하루씩 조회한다.
  3. 첫 번째로 데이터가 있는 날짜를 적용일로 연결한다.
  4. 11개 날짜 모두 비어 있으면 한도 초과(NOT_FOUND)다.
- 부분 결측은 폴백 사유가 아니다. 날짜 전체가 비었을 때만 폴백한다.

### U4 조회 API (선행 U3)
- `BondYieldController`
  - `GET /api/economics/bond-yields`
  - 날짜 문자열 파싱은 presentation이 맡는다. 형식 오류는 `IllegalArgumentException` → 400이다.
- `BondYieldResponse`: 위 API 계약 필드

### U5 기업 리포트 S-RIM 금리 선택 (선행 U4)
- `api.js`: `getBondYields(date)`를 추가한다. `timeoutMs` 40000으로, 서버 벽시계 상한 20초 + 조회 1회분보다 길게 잡는다.
- `company-report.js` 공용 헬퍼(두 화면이 함께 씀):
  - 상태 초기값
  - 조회 (세대 번호로 늦게 온 응답 무시)
  - 종류·등급·만기 옵션
  - 선택 금리
  - 만기 표시(개월 → "3개월"/"1.5년")
  - 출처·요청일·기준일 안내 문구
  - 적용 근거 표시
- `company-report.js` 화면 메서드:
  - 조회: `crSrimRateFetch`
  - 적용: `crSrimRateApply`. 요구수익률을 `String(rate)`로 채우고 `crSrimChanged()`를 호출한다.
- `_crSrimEmpty()`에 금리 선택 상태를 추가한다. 리셋·편집 불러오기 때 초기화된다.
- `company-report.html` S-RIM 입력 격자 아래 금리 선택 영역:
  - 기준일 입력(기본 오늘)과 조회 버튼
  - 출처·요청일·기준일 표시. 폴백이면 강조한다.
  - 종류 → 등급(회사채만) → 만기 선택, 금리 표시
  - 미제공이면 적용 버튼 비활성
  - 한도 초과·오류 안내
  - 안내 문구: "시장 기준수익률이며 기업 신용등급 판정이 아닙니다 · 선택 근거는 저장되지 않습니다"
- 기본 선택은 두지 않는다.
- `crSrimCurrency() !== 'KRW'`이면 영역을 비활성화하고 "원화 S-RIM에서만 사용" 안내를 표시한다.

### U6 종목 평가 S-RIM 금리 선택 (선행 U5)
- `stock-eval.js`:
  - 초기 상태와 `_seSrimEmpty()`에 금리 선택 상태를 추가한다.
  - `seSrimRateFetch` / `seSrimRateApply`를 추가한다. 적용 시 `seSrimChanged()`를 호출한다.
  - U5 공용 헬퍼를 재사용한다.
- `stock-eval.html`: U5와 같은 영역을 복제한다. 국내 전용이므로 통화 비활성 조건은 없다.

## 시스템 전반 영향
- 새 외부 연동 1개(한국자산평가)와 새 공개 API 1개가 생긴다.
- 기존 S-RIM 계산·저장·리포트 API·JSONB는 바뀌지 않는다. DB 스키마 변경 없음.
- 두 S-RIM 화면에 입력 영역이 추가된다.
- 결과 초기화 규칙은 그대로다. 적용 시에만 결과를 초기화한다.
- `GlobalExceptionHandler` 외부 API 그룹에 도메인 예외 2종이 추가된다. 기존 매핑은 바뀌지 않는다.
- 출처를 교체할 때는 어댑터만 바꾼다(KTD11).

## 위험과 완화
| 위험 | 완화 |
|---|---|
| 비공식 endpoint 변경·차단(WAF) | 어댑터로 격리, 파싱 오류로 명확히 실패, 직접 입력 유지 |
| 출처 교체 시 서비스까지 수정이 번짐 | KTD11 포트·도메인 예외·설정 분리, 어댑터 교체만으로 전환 |
| 이용 조건 미확인 (자동 조회·재게시 허가) | 태형님 위험 수용 (2026-09-29), 캐시로 호출 최소화 |
| 휴일 응답이 빈 응답이 아님 (직전 영업일 값 반환 등) | M0 실측, 다르면 중단 후 plan 갱신 |
| 응답 envelope·만기 키 미확인 | M0 실측 후 파서 확정 |
| 당일 부분 공시가 24h 고정됨 | 당일 기준일 데이터는 10분 TTL |
| 순차 폴백 지연 (최대 11회) | 조회 1회 ≤ 10s, 전체 상한 20s, 프론트 40s |
| 캐시 로더 안 외부 호출로 같은 캐시의 다른 키 갱신이 막힘 (Caffeine 권고) | 선례와 같은 수준으로 수용 (트래픽 낮음, 상한 있음) |
| 날짜를 바꿔 가며 반복 요청해 외부 호출 증가 | 로그인 필요, 요청당 최대 11회, 캐시 최대 크기 제한 |
| 등급 문자열만으로 매핑해 금융채·사모채와 섞임 | KTD3 조합 판정, 중복·대상 없음은 파싱 오류 |
| 섹션 이벤트 버블링으로 계산 결과가 지워짐 | 래퍼 `.stop`, 적용 시에만 초기화 |
| 외화 리포트에 원화 금리 적용 | USD면 비활성 + 안내 |
| 화면 복제로 두 화면이 어긋남 | 공용 헬퍼 재사용, 후속 작업은 두 화면을 함께 수정 |

## 열린 질문
- 없음. M0 결과는 2026-09-29에 확인했다(구현 단위 M0 절).

## 단위 테스트 계획
- 테스트 작성: 작성함
- 테스트 계획 문서: [테스트 계획](./tests/2026-09-29-131-bond-yield-srim-required-return-test-plan.md)
- 사용자 승인: 테스트 시나리오 승인됨 (2026-09-29)
- 승인된 테스트 시나리오: 정상 13건, 예외 9건 (파서 P1~P7, 조회 서비스 S1~S15)
- 검증 명령: `./gradlew test --tests "*KoreaApBondRateParserTest" --tests "*BondYieldQueryServiceTest"`
- 작성 순서: 서비스 테스트는 U1·U3 구현 전에 작성하고, 파서 테스트는 M0 fixture 확보 후 U2 구현 전에 작성한다.

## 작업 진행 방식 (워크플로우 예외)
이 세션에는 compound-engineering(`/ce:work`·`/ce:review`)이 없다. 그래서 같은 절차를 수동으로 적용한다(2026-09-29 승인).
- **구현:** 작업 리스트 순서대로 한 단위씩 진행한다.
  - 단위마다 변경 파일이 allowed_paths 안이고 blocked_paths 밖인지 확인한다.
  - 테스트 대상 단위는 테스트를 먼저 작성하고 실패를 확인한다.
  - 각 단위를 마치면 태형님께 다음 진행 여부를 확인한다.
- **체크포인트:** CP마다 `checkpoint-guard.sh`로 경로를 검사하고, read-only 가드 리뷰를 수행한다.
- **리뷰:** 리뷰 게이트 문서 기준으로 finding 표를 만들고, 태형님이 고른 이슈만 수정한다.

## 검증
검증 방식은 verify 단계에서 태형님이 고른다. 후보와 이 세션의 제약:
- `./gradlew compileJava`, `./gradlew test`: 이 세션에서 실행할 수 있다.
- M0 실측 fixture로 파서 결과를 대조한다. 네트워크 허용이 필요하다.
- `bootRun` + curl: DB·환경변수와 한국자산평가 접속이 필요하다. 이 세션에서 안 되면 로컬 실행으로 대체하거나 미실행 사유를 기록한다.
- 두 화면 수동 확인. 대상 동작:
  - 조회, 선택, 미제공 비활성, 적용, 직접 수정
  - 재계산, 폴백 표시, USD 비활성

## 체크포인트
- **CP1 (U1·U3·U2 후):** 폴백·캐시 TTL, 수집 계약·파서 규칙·예외 구분 (2026-09-29 순서 변경 반영)
- **CP2 (U4 후):** API 계약
- **CP3 (U5·U6 후):** 두 화면 동작

각 체크포인트에서 `scripts/checkpoint-guard.sh /home/user/stock-market-issue-131`로 경로를 검사하고 가드 리뷰를 수행한다.

**CP1 결과 (2026-09-29)**
- 경로 검사: 22개 파일 모두 plan 범위 안
- 가드 리뷰: DIRECTION_DRIFT 1건 — 파서 오류 메시지에 만기 키·원문 값이 들어감. 태형님 결정으로 수정(KTD8)
- 태형님 결정:
  - HTTP/1.1 고정을 사후 승인(KTD2)
  - 값이 전부 미제공인 행은 빈 날짜로 본다(KTD3)
- 실제 연결 스모크 테스트(Spring 없이 어댑터+서비스): 9/28 금리 156건, 폴백 오늘→9/28·9/27→9/23·5/5→5/4, 재조회 캐시 0ms

**CP2 결과 (2026-09-29)**
- 경로 검사: U4 변경 2개 파일 모두 plan 범위 안
- 가드 리뷰: CLEAN
  - API 계약 필드와 오류 응답(400·502)이 plan과 일치한다.
  - presentation은 infrastructure를 참조하지 않고, 출처 이름을 하드코딩하지 않는다.
  - 보안 설정과 기존 매핑은 바뀌지 않았다.
- 응답 JSON 확인: 실제 조회 결과를 Jackson 3 기본 매퍼로 직렬화했다.
  - 날짜는 ISO 문자열이다.
  - 9/28 BBB- 3년은 10.588, 미제공인 AAA 30년은 null이다.
  - 날짜를 생략하면 FALLBACK으로 기준일 9/28이 나온다.

**CP3 결과 (2026-09-29)**
- 경로 검사: U6 변경 2개 파일 모두 plan 범위 안. U5·U6 전체 6개 파일도 범위 안이다.
- 가드 리뷰: CLEAN
  - KTD9: 공용 헬퍼와 화면별 복제, 래퍼의 이벤트 차단을 확인했다.
  - KTD10: 근거는 값이 그대로일 때만 표시하고 저장 페이로드는 바뀌지 않았다.
  - REQ-8·10·14·16·17·18과 기본 날짜 오늘(KST), 타임아웃 40초, 출처 비하드코딩을 확인했다.
- 가드 리뷰 참고(리뷰 단계 후보, plan 밖): 새 리포트 작성 중 1단계에서 원화 종목을 USD 종목으로 바꾸면, 기존 코드가 S-RIM을 초기화하지 않는다. 그래서 적용한 원화 금리와 근거 문구가 남는다.
- 브라우저 확인(Playwright + Chromium, 실측 응답 JSON)
  - 방법: 저장소 밖 하네스에서 실제 JS와 partial을 쓰고, API 응답만 대체했다.
  - 결과: 기업 리포트 21/21, 종목 평가 19/19 통과, 콘솔 오류 없음
  - 확인 동작: 조회·선택·적용·근거·직접 수정·미제공·국고채·폴백·한도 초과·오류·USD 비활성

## 수정 범위
- **수정 가능:** 위 allowed_paths 범위
  - economics 신규 클래스(도메인 모델·포트·예외, 서비스, 어댑터, API)
  - `GlobalExceptionHandler` 외부 API 그룹 등록
  - application.yml 신규 섹션
  - 두 S-RIM 화면과 `api.js`
  - 단위 테스트 2종과 fixture
- **수정 금지:** 위 blocked_paths 범위
  - companyreport·stockevaluation 백엔드
  - ECOS·TradingEconomics·영속성·스케줄러
  - 보안 설정, DB 리소스
  - `index.html`·`app.js`·`build.gradle`

## 완료 정의
- REQ-1~20과 REQ-25를 충족하고, REQ-21~24는 제외로 유지한다.
- 휴일·미발표 날짜가 최대 10일 폴백되고, 요청일과 실제 기준일이 함께 표시된다. 11개 날짜 모두 비면 안내와 직접 입력이 유지된다.
- 통신·파싱 오류가 "데이터 없음"과 다른 응답·안내로 드러나고, 캐시에 남지 않는다.
- 두 화면에서 금리 선택·적용·직접 입력·재계산이 동작하고, 계산식과 저장 구조는 바뀌지 않는다.
