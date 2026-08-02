# 기업 분석 리포트 미국 주식 확장 Validation 기록

gate: docs/gates/2026-08-02-us-company-report-gates.md
review: docs/reviews/2026-08-02-us-company-report-review.md

## 실행한 검증

### 1. 빌드·테스트

- `./gradlew compileJava` PASS
- `./gradlew test` **BUILD SUCCESSFUL** — 기존 테스트 전체 통과. 테스트 중 Spring 웹 컨텍스트 + JPA 부팅 성공 → 신규 빈 배선(US Assembler·전략 라우터·`StockPriceService`/`UsReportSnapshotAssembler`의 `StockPort` 주입, `@Primary` KisStockAdapter) 이상 없음 확인
- `jsc`(JavaScriptCore) 구문 검증: `company-report.js`·`api.js` PASS (node 미설치로 대체)

### 2. KR 회귀 — 정적 diff 검증

- `KrReportSnapshotAssembler` ↔ 기존 `CompanyReportSnapshotService`(HEAD) diff 비교: 차이는 의도된 변경뿐 — `@Service`→`@Component`, javadoc, `implements ReportSnapshotAssembler`+`supports()`, 생성자명, 스냅샷 생성자에 country/currency/unsupportedSections 3인자 추가. **조립 로직 변경 없음 확인**

### 3. SEC 실호출 조립 검증 (스크래치패드 하네스, Spring 미기동·KIS 스텁)

`SecApiClient`+`SecCikCache`+`SecFinancialAdapter`+`UsSnapshotFinancialExtractor`+`UsReportSnapshotAssembler`를 실제 SEC API로 직접 와이어링:

| 종목 | 결과 |
|---|---|
| AAPL | 10년 컬럼(2016~2025), 매출 416.2B·영업이익 133.1B·FCF 98.8B (실제 공시값 일치), EPS 7.46·BPS 4.99, 배당 3개년, 위험시그널 유동역전 true(실제와 일치)·희석 false(-9.94%), 태그 세대교체(H1 수정) 정상 동작 |
| MSFT | 6월 결산 → baseYear FY2026 정상 처리, 프로필(SIC 7372)·전 지표 산출 |
| JPM (금융주) | 부분 스냅샷 설계 동작 — 영업이익·유동비율·재고·매출채권 null 허용, 매출 182.4B·순이익 57.0B·ROE 15.74·배당 정상 |

- 현재가는 KIS 스텁(조회 실패 경로) → per/marketCap null로 부분 스냅샷 생성 확인 (설계 의도)

### 4. 프론트 표시 헬퍼 하네스 (jsc)

- 21/22 PASS — USD 포맷($B/M·음수), `_crBdVal` 단위별, 미지원 섹션 판별·배너 문구, 거래소 배지, 공시 패널 제목 분기(KR/US), 위험 라벨 분기, 툴팁 SEC 오버라이드(L1), v1 스냅샷 KRW 하위 호환
- FAIL 1건은 하네스 기대값 오류(`Format.number(1.5, 2)`가 '1.5' 반환 — 기존 Format 유틸 동작, KR/US 동일, 회귀 아님)

## 미검증 항목

1. **KIS 해외 현재가·기간별시세 실호출** — 로컬에서 토큰을 발급하면 운영(hubth) 서버의 KIS 토큰이 무효화될 수 있어 보류. 실서버 배포 후 확인 필요. **계정의 해외주식 시세 권한 신청 여부도 확인 필요** (미신청 시 미국 현재가·월봉 차트 공란, 스냅샷 자체는 생성)
2. **브라우저 실제 렌더** (배지·배너·섹션 숨김·차트 라벨) — dev 로그인 제약·worktree 프리뷰 샌드박스 차단으로 로직 하네스로 대체. 실서버 확인 항목
3. **KR 리포트 런타임 회귀** (005930 preview) — 정적 diff로 로직 불변은 확인, 실서버에서 화면 확인 권장
4. **stock_code 컬럼 확대 운영 반영** — ddl-auto: update가 길이 확대를 반영하는지 배포 시 확인 (백업 SQL 준비됨)
