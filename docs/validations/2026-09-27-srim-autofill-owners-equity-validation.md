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

## 2. Mac 로컬 실검증 체크리스트 (DART 키 있는 환경)

### 준비
```bash
./gradlew bootRun   # profile dev, .env에 DART_API_KEY 등 설정
TOKEN=...           # 로그인 후 발급된 access token
BASE=http://localhost:8080/api/company-reports
```

### V1. 국내 연결 종목 (삼성전자 005930)
```bash
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/preview?stockCode=005930" > /tmp/p.json
jq '.snapshot | {schemaVersion, roeBasis, srimBasis}' /tmp/p.json
jq '.snapshot.statements[] | select(.key|test("owners|nonControlling")) | {key,name,values}' /tmp/p.json
jq '.snapshot.ratios[] | select(.key=="roe")' /tmp/p.json
jq '.snapshot.priceMetrics | {marketCap, eps, bps, per, pbr, psr, pcfr, warnings}' /tmp/p.json
jq '.suggestedGrades.profitability' /tmp/p.json
```
- [ ] `schemaVersion` = 3, `roeBasis` = `OWNERS`, ROE 행 이름 `ROE(지배주주, %)`
- [ ] 지배주주지분·비지배지분·지배주주순이익 행이 연도별로 있다 (2022년 이전 연도가 비는지 기록 — 표준 계정 ID 부재 여부)
- [ ] `srimBasis.equity`·`equityDate`: 최신 컬럼이 분기면 분기 값·분기말 일자인지, 비면 연간 값·12-31인지 기록 (분기 값 실측)
- [ ] `srimBasis.shares` = 합계 유통주식수, `sharesReport` = 최신 사업·반기 보고서, `sharesDate` = 결산기준일
- [ ] `priceMetrics.warnings`에 "유통주식수는 … 반기보고서(…) 기준입니다." (반기 구간일 때)

### V2. 전후 비교 (REQ-16)
`main` 브랜치로 같은 요청을 한 번 더 실행해 아래를 표로 기록한다.

| 지표 | main | #124 | 변화 원인 |
|---|---|---|---|
| ROE(기준연도) | | | 지배주주 기준 교체 |
| 시가총액·PSR·PCR·EV/EBITDA | | | 반기 주식수 |
| EPS·BPS·PER·PBR | | | 반기 주식수 (주가지표 서비스) |
| 자기주식수 | | | 반기 기준 |
| 수익성 제안 등급 | | | ROE 기준 |

### V3. 개별재무제표만 있는 종목 1개
- [ ] `roeBasis` = `TOTAL`, 지배주주 행 없음, `srimBasis.equity` = null

### V4. 미국 종목 1개 (예: AAPL)
```bash
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/preview?stockCode=AAPL" | jq '.snapshot | {roeBasis, srimBasis, roe: (.ratios[]|select(.key=="roe"))}'
```
- [ ] `roeBasis` = `OWNERS` (NetIncomeLoss·StockholdersEquity 존재), `srimBasis` = null
- [ ] SEC 태그 의미 확인: 비지배지분이 있는 종목 1개에서 ROE 값이 지배주주 기준인지

### V5. 화면
- [ ] 기업 리포트 작성 5단계: "재무 데이터 가져오기" → 4칸·ROE 계산 행 전기말 지분 채움, 출처 표시, 직접 고친 칸 유지, 억/조 단위 전환 후 값 일관
- [ ] 기존(v2) 리포트 편집: 버튼 비활성 + "재무 새로고침 후 사용 가능", 재무 새로고침 후 활성
- [ ] 수익성 근거 패널: `OWNERS`면 지배주주 산식·행, v2 리포트는 기존 산식
- [ ] 종목 평가 → DART → S-RIM 적정주가: 가져오기 로딩·채움·기준 주가, 계산 3시나리오, 새로고침 후 입력 사라짐(저장 없음)
- [ ] 챗봇 가치평가 컨텍스트에 반기 기준 안내 한 줄 (선택)

## 3. 참고 (범위 밖 발견)
- `POST /srim/calculate` 응답의 금액·비율이 JSON 숫자로 직렬화된다 (`"price":76143.332810`). `SrimDecimalSerializer`(`com.fasterxml` 애노테이션)가 적용되지 않는 것으로 보이며 #122부터의 기존 동작이다. 이번 변경의 `srimBasis`는 문자열 필드로 설계해 영향이 없다.
