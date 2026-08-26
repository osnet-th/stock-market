# 기업분석리포트 월봉 주가 차트 검증

gate: docs/gates/2026-07-16-company-report-monthly-price-chart-gates.md

## 실행 명령 / 결과

- `./gradlew compileJava` → pass (기존 realestate deprecated 경고만).
- 잔여 참조 grep(`getDailyHistory`/`getDomesticDailyChart`/`DAILY_CHUNK_*`) → 0건.
- HTML 태그 균형(python): div 337/337, template 90/90, canvas 4/4 등 전 태그 일치 → pass.
- JS 괄호 균형: 추가 구간 38/38·24/24·3/3 균형. 파일 전체 `()` 1건 차이는 base(HEAD)부터 동일(문자열 내 괄호) → 무관.
- curl (worktree 앱, dev 프로파일):
  - `GET /api/stocks/005930/price-history` → 200, **period=M, 499포인트, 1985-01-31 ~ 2026-07-24 (41년), 0.5s**.
  - `GET .../price-history?period=D&from=2026-06-01` → 38포인트 (짧은 구간 정상).
  - `period=X` → 400, 종목코드 `12345` → 400 (검증 동작).
  - 미인증(prod 프로파일 시) → 401 (보안 유지).
- 라이브 브라우저(dev, 삼성전자):
  - 상세 뷰 "6. 주가지표" 섹션: "주가 추이 (월봉)" 카드 + 41년 종가 라인 차트 렌더 확인(스크린샷).
  - 위저드 5단계(기업가치): 동일 차트 렌더 확인. 종목 데이터 재사용(상세→위저드 중복 호출 없음) — 차트 인스턴스 검사로 499포인트 데이터 확인.
  - 기존 실적 추이 차트(상세) 회귀 없음.
  - 신규 콘솔 에러 0. (기존 `nextScheduledAt` 에러 1건은 realestate 파티션 소속, 본 변경 파일 아님 — 무관 확인)

## 검증 환경 메모

- 검증은 worktree 앱(dev 프로파일, permitAll)에서 수행. UI 접근용으로 `.env` JWT 시크릿으로 로컬 dev 검증 토큰을 발급해 사용 후 브라우저에서 제거함.
- 검증용으로 생성한 리포트(id 10)는 검증 후 DELETE(204) — dev DB 리포트 0건 복원.

## 미검증 / 리스크

- 테스트 코드 없음(명시 요청 없음) — 수동 검증으로 대체.
- KIS 미가동 시간대의 실제 장애 폴백(부분 반환)은 코드 경로 검토로만 확인(강제 재현 안 함).
- prod 프로파일 실행 시 인증 헤더 포함 호출은 로그인 세션에서 자동 처리(기존 api.js 공통 헤더) — 별도 미검증 항목 없음.
