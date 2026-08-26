---
title: R-ONE (한국부동산원) Open API 명세
source: R-ONE 공식 명세서 (XLS)
captured-at: 2026-05-24
related-plan: docs/plans/2026-05-24-001-feat-reb-adapter-redesign-plan.md
issue: 58
---

# R-ONE Open API 명세

본 문서는 R-ONE Open API 3-step 호출 모델의 공식 명세를 정리한 것이다.
`Unit 4 (RebClient/Cache/Adapter 재작성)` 구현 시 참조 권위 문서.

- 호출 모델: `SttsApiTbl` (목록) → `SttsApiTblItm` (항목) → `SttsApiTblData` (데이터)
- 응답 포맷: `Type=xml | json` 기본인자로 선택
- 페이징: `pIndex` / `pSize` (sample key 사용 시 각각 1 / 5 고정)
- 호출 제한: 명시 제한 없음 (단 `ERROR 336` 1,000건/요청 상한, `ERROR 337` 일별 트래픽 상한)

---

## 공통

### resultCode 매핑

| 구분 | 코드 | 의미 | adapter 처리 |
|---|---|---|---|
| INFO | 000 | 정상 처리 | `FetchResult.success(...)` |
| INFO | 200 | 해당하는 데이터가 없습니다 | `FetchResult.success(List.of())` (정상 빈 응답) |
| INFO | 300 | 관리자에 의해 인증키 사용 제한 | `FetchResult.failure(authDisabled)` |
| ERROR | 290 | 인증키 유효하지 않음 | `FetchResult.failure(invalidKey)` |
| ERROR | 300 | 필수 값 누락 | `FetchResult.failure(badRequest)` — 구현 결함 신호 |
| ERROR | 310 | 서비스 미존재 | `FetchResult.failure(badRequest)` |
| ERROR | 333 | 요청위치 값 타입 오류 | `FetchResult.failure(badRequest)` |
| ERROR | 336 | 한 번에 1,000건 초과 | `FetchResult.failure(retryWithSmallerPage)` |
| ERROR | 337 | 일별 트래픽 제한 초과 | `FetchResult.failure(rateLimited)` — 오늘 추가 호출 차단 |
| ERROR | 500 | 서버 오류 | `FetchResult.failure(serverError)` — 재시도 후보 |
| ERROR | 600 | DB 연결 오류 | `FetchResult.failure(serverError)` |
| ERROR | 601 | SQL 오류 | `FetchResult.failure(serverError)` |

> 학습 `#11` (`resultCode "00"/"000"`)와 정합: R-ONE은 INFO "000"을 사용한다. 다른 출처(`MOLIT` 등)의 "00"과 호환되도록 둘 다 success로 처리.

### 기본 인자 (모든 엔드포인트 공통)

| 변수 | 타입 | 비고 |
|---|---|---|
| `Key` | STRING (필수) | 인증키 (sample key 가능, 페이징 1/5 고정) |
| `Type` | STRING (필수) | `xml` 또는 `json` |
| `pIndex` | INTEGER (필수) | 페이지 위치 (기본 1) |
| `pSize` | INTEGER (필수) | 페이지 당 건수 (기본 100, sample key는 5 고정) |

---

## 1. `SttsApiTbl` — 서비스 통계 목록

- **요청 URL**: `https://www.reb.or.kr/r-one/openapi/SttsApiTbl`
- **용도**: STATBL_ID 카탈로그 조회 (선택적 활용 — 본 구현은 STATBL_ID를 yml로 고정)

### 선택 요청 인자

| 변수 | 타입 | 비고 |
|---|---|---|
| `STATBL_ID` | STRING (선택) | 특정 통계표만 조회 |

### 출력 (주요 컬럼)

| 컬럼 | 타입 (길이) | 설명 |
|---|---|---|
| `STATBL_ID` | CHAR(50) | 통계표 ID |
| `STATBL_NM` | VARCHAR(300) | 통계표명 |
| `DTACYCLE_CD` | CHAR(11) | **주기코드** (Data 호출 필수 입력) |
| `DTACYCLE_NM` | CHAR(15) | 주기명 (월간/분기/연간 등 사람 표현) |
| `STAT_ID` | CHAR(50) | 통계메타 ID |
| `TOP_ORG_NM` | VARCHAR(500) | 제공기관 |
| `OPEN_STATE` | CHAR(1) | 공개여부 |
| `DATA_START_YY` | CHAR(4) | 자료 시작년도 |
| `DATA_END_YY` | CHAR(4) | 자료 종료년도 |
| `STATBL_IDTFR` | VARCHAR(10) | 주석 식별자 |
| `STATBL_CMMT` | VARCHAR(4000) | 통계표 주석 |
| `V_ORDER` | NUMBER(10) | 출력순서 |
| `RPSTUI_NM` | VARCHAR(300) | 기준시점 |

### 구현 활용

- `DTACYCLE_CD`는 `SttsApiTblData` 호출의 필수 입력이며, **통계표마다 다를 수 있다.** 따라서 `RebMetadataCache`가 STATBL_ID별 `DTACYCLE_CD`를 보관해야 한다 (yml 고정 매핑 외에).
- 본 호출은 옵션. yml에 STATBL_ID + `DTACYCLE_CD`를 함께 박아두면 1단계는 생략 가능 (`RebMetadataCache.warmup()`이 2단계 `SttsApiTblItm`만 호출).

---

## 2. `SttsApiTblItm` — 통계 세부 항목 목록

- **요청 URL**: `https://www.reb.or.kr/r-one/openapi/SttsApiTblItm`
- **용도**: STATBL_ID 안에 있는 모든 `ITM_ID` 목록 + 지역명 매핑 (region 매핑 핵심)

### 요청 인자

| 변수 | 타입 | 비고 |
|---|---|---|
| `STATBL_ID` | STRING (**필수**) | |
| `ITM_TAG` | STRING (선택) | 항목 태그로 필터 |

### 출력

| 컬럼 | 타입 (길이) | 설명 |
|---|---|---|
| `STATBL_ID` | CHAR(50) | 통계표 ID |
| `ITM_TAG` | CHAR(5) | 항목정보 (계층 태그) |
| `ITM_ID` | NUMBER(8) | **항목 ID** (region 매핑 key) |
| `PAR_ITM_ID` | NUMBER(8) | 상위 항목 ID |
| `ITM_NM` | VARCHAR(100) | 항목명 (예: "서울", "경기") |
| `ITM_FULLNM` | VARCHAR(500) | 전체 항목명 (예: "수도권>서울특별시") |
| `UI_NM` | CHAR(100) | 단위명 (예: "지수", "건") |
| `ITM_CMMT_IDTFR` | VARCHAR(10) | 주석 식별자 |
| `ITM_CMMT_CONT` | VARCHAR(2000) | 항목 주석 |
| `V_ORDER` | NUMBER(8) | 출력순서 |

### 구현 활용

- **시도 매핑**: `ITM_NM` 또는 `ITM_FULLNM`에서 시도명 추출. KOSIS 함정 `#14` 동일 (한글 단축명 vs 공식명).
- `RebRegionNameResolver`에 매핑 테이블 필요: `"서울" → "11"`, `"부산" → "26"`, ..., `"제주" → "50"`, `"강원" → "51"`, `"전북" → "52"` (시도 17개).
- 단, `ITM_TAG`/`PAR_ITM_ID` 계층을 활용하면 상위 그룹("수도권"/"지방")을 자연 필터링 가능.
- 캐시 단위: `STATBL_ID` → `List<SttsApiTblItmRow>` (Caffeine maximumSize ≈ STATBL 수 + 여유 = 50 권장).
- TTL: 통계표 메타는 ~연 단위 변경 — 24h 또는 1주 권장. 신규 ITM 추가 시 운영 알림 + 수동 invalidate API 필요.

---

## 3. `SttsApiTblData` — 통계 데이터

- **요청 URL**: `https://www.reb.or.kr/r-one/openapi/SttsApiTblData`
- **용도**: 실제 시계열 데이터 (Data 호출). adapter `fetch()`의 본체.

### 요청 인자

| 변수 | 타입 | 비고 |
|---|---|---|
| `STATBL_ID` | STRING (**필수**) | yml `RebStatblProperties`에서 매핑 |
| `DTACYCLE_CD` | STRING (**필수**) | 1단계 `SttsApiTbl` 응답의 `DTACYCLE_CD` (yml에 함께 박는 것 권장) |
| `WRTTIME_IDTFR_ID` | STRING (선택) | 자료작성 시점 (단일 시점 조회) |
| `GRP_ID` | STRING (선택) | 그룹 ID |
| `CLS_ID` | STRING (선택) | 분류 ID |
| `ITM_ID` | STRING (선택) | 항목 ID — region 단위 필터 (지정 시 해당 ITM만) |
| `START_WRTTIME` | STRING (선택) | 시작일 (시계열 윈도우) |
| `END_WRTTIME` | STRING (선택) | 종료일 |

### 출력

| 컬럼 | 타입 (길이) | 설명 |
|---|---|---|
| `STATBL_ID` | CHAR(50) | 통계표 ID |
| `DTACYCLE_CD` | CHAR(50) | 주기코드 |
| `WRTTIME_IDTFR_ID` | CHAR(8) | **자료작성 시점** (예: "202604" = 2026년 4월) |
| `GRP_ID` | NUMBER(8) | 그룹 ID |
| `GRP_NM` | VARCHAR2(300) | 그룹명 |
| `CLS_ID` | NUMBER(8) | 분류 ID |
| `CLS_NM` | VARCHAR2(300) | 분류명 |
| `ITM_ID` | NUMBER(8) | **항목 ID** (region 식별 — Itm 응답과 join) |
| `ITM_NM` | VARCHAR2(300) | 항목명 |
| `DTA_VAL` | NUMBER(22) | **통계 자료값** (실제 수치) |
| `UI_NM` | VARCHAR(100) | 단위명 |
| `GRP_FULLNM` | VARCHAR2(1000) | 그룹 전체명 |
| `CLS_FULLNM` | VARCHAR2(1000) | 분류 전체명 |
| `ITM_FULLNM` | VARCHAR2(1000) | 항목 전체명 |
| `WRTTIME_DESC` | VARCHAR2(100) | 자료시점 설명 |

### 구현 활용

- **Q4=D1 전략 (모든 ITM 호출 + 메모리 필터)**: `ITM_ID` 인자를 **비워두고** STATBL 전체 데이터를 받은 뒤 `RebMetadataCache`에서 시도 17개의 `ITM_ID` 집합으로 메모리 필터. 호출 부하 1회/일/통계표.
- **시계열 윈도우**: `START_WRTTIME`/`END_WRTTIME`를 30일 batch window에 맞춰 설정 (Scheduler `FetchWindow.lastNDays(30)`).
- **페이지 상한**: `ERROR 336` 한 번에 1,000건 초과. `pSize=100` 기준 1 페이지 = 100 row. 11개 STATBL × 시도 17개 × 윈도우 30일 = 페이지 다수 필요. **페이징 루프 필수**.
- 응답이 페이지를 넘어가면 `pIndex` 증가시키며 누적, 빈 페이지(`INFO 200`) 또는 `pSize` 미만 응답에서 종료.

---

## 운영 함정 (`docs/solutions/architecture-patterns/realestate-public-open-api-pitfalls-2026-05-07.md` 매핑)

| # | 항목 | R-ONE 적용 |
|---|---|---|
| #8 | User-Agent | `RealEstateRestClientConfig` 통과 |
| #11 | resultCode "00"/"000" | R-ONE은 "000" — adapter에서 "00"/"000" 둘 다 success 처리 |
| #12 | Content-Type 무시 | `Type=json` 명시해도 서버가 다른 헤더 반환 가능 — String body + ObjectMapper |
| #14 | 시도 한글명 vs 공식명 | `ITM_NM` 단축명 → `sidoCode` 매핑 테이블 필요 (KOSIS와 동일 패턴) |
| #15 | contextPath | base-url `https://www.reb.or.kr/r-one/openapi` + endpoint `SttsApiTbl{Itm,Data}` |
| #16 | 부분 실패 허용 | 11개 STATBL 중 일부 실패해도 다른 STATBL은 계속 적재 |

---

## Unit 4 진입 시 결정 사항 (개방 질문 갱신)

- **`DTACYCLE_CD` yml 박기**: 현재 `RebStatblProperties`는 STATBL_ID만 보유. `DTACYCLE_CD`도 함께 박을지 (1단계 호출 생략) 결정.
- **`pSize` 기본값**: 본 명세상 100 (sample key는 5 고정). 실제 인증키 발급 후 `pSize=1000` 한계 활용 검토.
- **시도 17개 매핑 테이블의 `ITM_NM` 단축명 확정**: 실제 응답 샘플에서 단축명 정확히 무엇이 오는지 (예: "서울" vs "서울특별시" vs "서울시") — Unit 4 구현 초반 1회 호출로 확인 필요.
- **Caffeine 캐시 key 구조**: `STATBL_ID → List<ItmRow>` 권장 (entry count ≈ 11 + 여유).