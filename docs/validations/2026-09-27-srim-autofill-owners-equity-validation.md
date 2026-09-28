# S-RIM 자동 채움 · 지배주주지분 · 주식총수 반기 전환 검증 (#124)

plan: docs/plans/2026-09-27-001-feat-srim-autofill-owners-equity-plan.md
issue: https://github.com/osnet-th/stock-market/issues/124

선택: 태형님 "3번" (bootRun + curl 실검증, 2026-09-28).

## 1. cloud 세션에서 실행한 검증

### 환경
- 로컬 PostgreSQL 16 (세션 임시 클러스터, `localhost:5432/stocks`, profile `dev`, `ddl-auto: update`). 운영 아님. 검증 후 종료.
- 외부 API 키는 모두 더미 값. `opendart.fss.or.kr`·`data.sec.gov`는 네트워크 정책으로 접속 불가.
- 인증: `application.yml` 기본 JWT secret으로 서명한 dev 토큰(userId 1, role USER).

### 결과

| # | 항목 | 결과 |
|---|---|---|
| 1 | `./gradlew compileJava` | 성공 |
| 2 | `./gradlew test` | 158건 중 157건 통과. 실패 1건 `StockMarketApplicationTests.contextLoads`는 변경 전 코드에서도 동일하게 실패(환경 의존) |
| 3 | 승인된 단위 테스트 23건 | 전부 통과 |
| 4 | `./gradlew bootRun` | 기동 성공 (`Started StockMarketApplication`, `GET /` 200) |
| 5 | `POST /api/company-reports/srim/calculate` (토큰) | 200. 3시나리오 적정주가·괴리율 정상 반환 (종목 평가 S-RIM 탭이 쓰는 경로) |
| 6 | `GET /api/company-reports/preview` 미인증 | 401 |
| 7 | `GET /api/company-reports/preview?stockCode=005930` (토큰) | **502 — "DART 고유번호 캐시가 초기화되지 않았습니다"**. DART 키·접속이 없어 기업코드 캐시를 받지 못함. 이번 변경과 무관한 환경 제약 |
| 8 | 정적 자산 서빙 | `company-report.html`에 자동 채움·출처·유통주식수 바인딩, `stock-eval.html`에 S-RIM 탭 바인딩, JS 두 파일에 신규 메서드가 서빙됨 |
| 9 | 프론트 로직 (node 하네스) | 자동 채움·단위 환산·수동 입력 보존·v2 안내·종목 평가 계산 요청 형식 통과 |

### 이 세션에서 확인하지 못한 것
미리보기(스냅샷 조립)가 DART 없이 실행되지 않아, 실제 데이터가 필요한 항목은 아래 2절에서 Mac 로컬로 확인한다.

## 2. Mac 로컬 실검증 (DART 키 있는 환경, 2026-09-28 실행)

### 준비
```bash
./gradlew bootRun   # profile dev, .env에 DART_API_KEY 등 설정
TOKEN=...           # 로그인 후 발급된 access token
BASE=http://localhost:8080/api/company-reports
```

### 실행 환경
- 브랜치 `claude/amazing-johnson-0m2ean`(a82a88b). 비교 기준은 merge-base인 `origin/main`(d4d6734)을 별도 worktree에서 같은 설정으로 기동했다.
- `./gradlew compileJava` 성공. plan 검증 명령(승인 단위 테스트 6개 클래스) 21건 통과.
- bootRun: profile `dev`, 포트 8080, `.env-local` 값 사용. 실행 인자로 cron 스케줄러 9종을 `-`로 끄고 메일 계정을 비웠다(15:00 뉴스 수집·15:30 알림 메일·15:40 스냅샷 저장 부작용 방지). DART·KIS·SEC는 실호출.
- DB: dev 기본 DB(`localhost:5432/stocks`, root)가 인증 실패해 일회용 `postgres:17` 컨테이너(127.0.0.1:55432, `stocks`)로 대체하고 검증 후 삭제했다. 8080을 쓰던 Dataworks tomcat은 태형님 승인으로 종료했다.
- 인증: 태형님 카카오 로그인(브라우저). API는 로그인된 페이지 안에서 `fetch`로 호출해 토큰을 셸·문서에 남기지 않았다. 미인증 `GET /preview`·`POST /srim/calculate`는 401.

### V1. 국내 연결 종목 (삼성전자 005930)
```bash
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/preview?stockCode=005930" > /tmp/p.json
jq '.snapshot | {schemaVersion, roeBasis, srimBasis}' /tmp/p.json
jq '.snapshot.statements[] | select(.key|test("owners|nonControlling")) | {key,name,values}' /tmp/p.json
jq '.snapshot.ratios[] | select(.key=="roe")' /tmp/p.json
jq '.snapshot.priceMetrics | {marketCap, eps, bps, per, pbr, psr, pcfr, warnings}' /tmp/p.json
jq '.suggestedGrades.profitability' /tmp/p.json
```
- [x] `schemaVersion` = 3, `roeBasis` = `OWNERS`, ROE 행 이름 `ROE(지배주주, %)` (응답 약 2초)
- [x] 지배주주지분·비지배지분·지배주주순이익 행이 연도별로 있다 (2022년 이전 연도가 비는지 기록 — 표준 계정 ID 부재 여부)
  - 삼성전자는 2017~2026 전 연도 존재. 전 연도 `자본총계 = 지배주주지분 + 비지배지분`이 원 단위로 일치한다.
  - 연결 종목 11개 표본 중 2개에 공백: NAVER 지배주주순이익 2018~2021, KB금융 지배주주지분·순이익 2017~2022. 해당 연도 ROE(지배주주)가 비고, main(전체 기준)에는 값이 있다. 나머지 9개(SK하이닉스·현대차·카카오·POSCO홀딩스·현대모비스·LG화학·LG전자·셀트리온 포함)는 전 연도 존재 → 3절 F1
- [x] `srimBasis.equity`·`equityDate`: 565,064,740,000,000 · 2026-06-30 · "2026 반기보고서". 최신 컬럼이 반기(3분기보고서 공시 전)라 반기 값·반기말 일자다. 1Q·3Q 분기 컬럼은 이번 시점에 관측할 수 없었다.
- [x] `srimBasis.shares` = 6,566,563,106 (합계, 자기주식 차감), `sharesReport` = "2026 반기보고서", `sharesDate` = 2026-06-30
- [x] `priceMetrics.warnings`에 "유통주식수는 2026 반기보고서(2026-06-30) 기준입니다." 주당 지표 주식수는 보통주(5,764,191,903)를 유지한다 (REQ-10).

### V2. 전후 비교 (REQ-16)
`main` 브랜치로 같은 요청을 한 번 더 실행해 아래를 표로 기록한다.

삼성전자 기준. 두 실행의 주가가 달라(main 271,250원 · #124 272,500원) 주가가 들어가는 지표는 #124 값을 main 주가로 환산해 비교했다. #124 앱 원값(272,500원)은 시가총액 1,570.74조·PSR 4.71·PCR 18.41·PER 34.74·PBR 3.60이다.

| 지표 | main | #124 | 변화 원인 |
|---|---|---|---|
| ROE(기준연도 2025) | 10.36 (당기순이익 ÷ 자본총계) | 10.43 (지배주주순이익 ÷ 지배주주지분) | 지배주주 기준 교체 |
| 주당 지표 주식수(보통주 유통) | 5,827,808,935 (2025 사업보고서) | 5,764,191,903 (2026 반기보고서, −1.09%) | 반기 주식수 |
| 시가총액·PSR·PCR·EV/EBITDA | 1,580.79조 · 4.74 · 18.53 · — | 1,563.54조 · 4.69 · 18.33 · — (각 −1.09%) | 반기 주식수 |
| EPS·BPS·PER·PBR | 7,757 · 74,869 · 34.97 · 3.62 | 7,843 · 75,695 · 34.59 · 3.58 (EPS·BPS +1.10%) | 반기 주식수 (주가지표 서비스) |
| 자기주식수 | 91,828,987 | 82,086,705 | 반기 기준 |
| 수익성 제안 등급 | B | B (근거 ROE만 지배주주 10.43으로 바뀜) | ROE 기준 |

- 경고: main은 "당기순이익 값을 찾지 못했습니다." 1건, #124는 여기에 반기 기준 안내 1건이 더해진다.
- 미국 종목은 기준 표기만 바뀐다. AAPL ROE 151.91·WMT ROE 21.98은 main과 같고 행 이름이 `ROE(%)` → `ROE(지배주주, %)`로 바뀐다.

### V3. 개별재무제표만 있는 종목 1개
- [x] 리노공업(058470): `fsDiv` OFS, `roeBasis` = `TOTAL`, 지배주주 행 없음, ROE 행 `ROE(%)`, `srimBasis.equity` = null. `srimBasis` 객체는 null이 아니고 합계 유통주식수(75,895,600, 반기)만 채워진다 → 3절 F3

### V4. 미국 종목 1개 (예: AAPL)
```bash
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/preview?stockCode=AAPL" | jq '.snapshot | {roeBasis, srimBasis, roe: (.ratios[]|select(.key=="roe"))}'
```
- [x] AAPL: `roeBasis` = `OWNERS`, `srimBasis` = null, ROE(2025) 151.91 = 112,010 ÷ 73,733 (백만 달러)
- [x] WMT(비지배지분 보유): 앱 ROE 21.36%(FY2025)·21.98%(FY2026) = NetIncomeLoss ÷ StockholdersEquity (19,436 ÷ 91,013 · 21,893 ÷ 99,617). ProfitLoss ÷ 비지배 포함 자본이면 20.69%·21.03%다. SEC companyfacts 원천값 대조로 지배주주 기준임을 확인했다.

### V5. 화면
- [x] 기업 리포트 작성 5단계 (삼성전자, 신규)
  - 버튼 → 지배주주지분·자본 기준일·유통주식수·주식수 기준일이 채워지고, ROE 계산 행(2026) 전기말 지분 = 2025년 말 424,313,255,000,000. 직접 입력 행(2027)은 그대로다.
  - 출처: "지배주주지분 · 2026 반기보고서 · 2026-06-30", "합계 유통주식수(자기주식 차감) · 2026 반기보고서 · 2026-06-30", "2025년 말 지배주주지분 (사업보고서)". 라벨 "유통주식수 (주)".
  - 직접 고친 칸(유통주식수·전기말 지분)은 다시 눌러도 유지되고 출처가 사라진다. 비운 칸만 다시 채운다.
  - 원→조→원→억 전환: 565064740000000 ↔ 565.06474 ↔ 5650647.4, 전기말 지분도 함께 환산된다. 전환 후 다시 눌러도 이중 환산이 없다.
- [x] 기존(v2) 리포트 편집: main에서 만든 v2 리포트의 편집 5단계에서 버튼 비활성 + "재무 새로고침 후 사용 가능". 상세 "데이터 새로고침" 후 `schemaVersion` 3·`OWNERS`로 바뀌고 버튼 활성·안내 사라짐. 저장된 S-RIM 입력은 새로고침으로 바뀌지 않는다. 상세 라벨 "유통주식수: 5,919,637,922주" → 3절 F2
- [x] 수익성 근거 패널: 신규(`OWNERS`) "ROE = 지배주주순이익 ÷ 지배주주지분 = 44.26조 ÷ 424.31조 = 10.43%", 원천 데이터에 지배주주순이익·지배주주지분 행. v2 리포트 "ROE = 당기순이익 ÷ 자본총계 = 45.2조 ÷ 436.32조 = 10.36%", 지배주주 행 없음
- [x] 종목 평가 → DART → S-RIM 적정주가 (삼성전자)
  - 탭 진입만으로는 미리보기를 호출하지 않고, 버튼을 누를 때마다 1회 호출한다. 로딩 중 "불러오는 중" 표시.
  - 지배주주지분·기준일·유통주식수·기준 주가(272,500 · "리포트 기준 주가 · 2026-09-28")와 ROE 계산 행 전기말 지분이 채워진다.
  - 3시나리오: 2026(ROE 계산 19.53%) 210,023 / 135,640 / 114,388원, 괴리 −22.93% / −50.22% / −58.02% — 수기 검산 일치. 2027(직접 12%) 129,078 / 103,262 / 95,886원.
  - 호출은 `POST /srim/calculate`뿐이고 리포트 행 0건. 새로고침 후 입력·결과가 사라지고, 종목 전환(→ 리노공업) 시 S-RIM 상태가 초기화된다.
- [ ] 챗봇 가치평가 컨텍스트에 반기 기준 안내 한 줄 (선택) — 실호출 미실행(Gemini 비용·외부 전송). 코드 경로만 확인: `ChatContextBuilder.appendValuation`이 `ValuationMetricService` 경고를 "- 참고:" 줄로 붙이며, 같은 경고가 V1 `priceMetrics.warnings`에 나온다.

## 3. 발견 사항

| # | 내용 | 결정 |
|---|---|---|
| F1 | 지배주주 계정 ID가 없는 과거 연도는 ROE(지배주주)가 빈칸이다 (NAVER 2018~2021, KB금융 2017~2022). 과거 ROE는 재무지표 표·근거 패널 표시와 수익성 A 판정(기준연도 포함 최근 3개 연도)에만 쓰여, 공백이 판정 범위 밖이면 등급 영향이 없다 | 수용 (태형님, 2026-09-28) |
| F2 | S-RIM 값이 저장된 리포트에서 가져오기를 누르면 모든 칸이 직접 입력으로 간주돼 아무것도 바뀌지 않고 안내도 없다 (REQ-17대로). 최신 값으로 바꾸려면 칸을 비우고 눌러야 한다 | 보류 (태형님, 2026-09-28) |
| F3 | 개별재무제표 종목은 지배주주지분이 안내 없이 빈다 (리포트·종목 평가 공통). `ReportSnapshot.srimBasis` 주석("국내 연결재무제표 종목만, 그 외 null")과 달리 OFS 종목도 객체를 만든다 | 보류 (태형님, 2026-09-28) |
| F4 | plan·테스트 계획 요약·1절의 "23건(정상 17·예외 6)"은 테스트 계획 표(21건: 정상 13·예외 8)·실제 테스트(21건)와 다르다 | 보류 (태형님, 2026-09-28) |
| F5 | 수익성 등급 기준 E 문구가 `roeBasis`와 무관하게 "지배주주 기준 순적자"로 고정이다. 판정 기준 주석이 이전 리포트 기준을 따로 설명한다 | 보류 (태형님, 2026-09-28) |

## 4. 참고 (범위 밖 발견)
- `POST /srim/calculate` 응답의 금액·비율이 JSON 숫자로 직렬화된다 (`"price":76143.332810`). `SrimDecimalSerializer`(`com.fasterxml` 애노테이션)가 적용되지 않는 것으로 보이며 #122부터의 기존 동작이다. 이번 변경의 `srimBasis`는 문자열 필드로 설계해 영향이 없다.
- "당기순이익 값을 찾지 못했습니다." 경고는 main에서도 같다 (`ValuationMetricService`의 계정명 정확 일치 조회). 이번 변경과 무관하다.
- 종목 평가 헤더 가격은 요약 API의 종가 필드(전일 종가와 같은 값)이고, S-RIM 기준 주가는 KIS 현재가라 장중에는 다르게 보인다 (삼성전자 285,500 vs 272,500). 기존 동작이다.
