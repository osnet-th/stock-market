# 기업 리포트 입력 개선 Validation 기록

work: docs/works/2026-07-19-company-report-input-improvements-work.md
review: docs/reviews/2026-07-19-company-report-input-improvements-review.md
gate: docs/gates/2026-07-19-company-report-input-improvements-gates.md
issue: https://github.com/osnet-th/stock-market/issues/84

## 환경 기동 (태형님 승인 "전체 기동")
- Docker Desktop 시작(데몬 꺼져 있었음) → 기존 `local-postgres`(stocks/root/root, dev 설정 일치) 재사용. docker-compose에 postgres 서비스 없음 확인.
- `SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun` (KAKAO_REDIRECT_URI=localhost override) → Tomcat 8080, "Started StockMarketApplication in 4.3s", `/actuator/health` 200(DB PostgreSQL UP). ES 미기동 WARN은 비치명적(재시도).
- UI는 Kakao OAuth 로그인 필요 → dev 사용자(user_entity id=1)용 JWT를 앱 시크릿(HS256)으로 발급해 `localStorage.accessToken` 주입(로그인 우회, 검증 목적). API GET 200 확인.

## 백엔드 컴파일
- `./gradlew compileJava` → exit 0 (리뷰 반영 후 재컴파일 포함).

## 브라우저 실동작 (Browser pane, 삼성전자 005930 신규 리포트)
| # | 시나리오 | 결과 |
|---|---------|------|
| 1 | 연혁 내용 여러 줄(textarea) + 날짜 `예: 2024.03` placeholder | 통과 — 여러 줄 입력·저장, DB에 `\n` 보존 |
| 2 | 연혁 날짜 정렬(2023.11 / 2024 / 2024.03) | 통과 — 조회에서 2023.11 → 2024 → 2024.03 숫자 정렬 |
| 3 | 판매처=제품 / 매입처=원자재 품목 칸 | 통과 — 작성·조회 모두 품목 열 노출, DB `item` 필드 저장 |
| 4 | 주가지표 EPS 공식(당기순이익÷유통주식수) | 통과 — EPS 행 "당기순이익 ÷ 유통주식수 = 45.2조 ÷ 58.3억주 = 7,757" |
| 5 | **유통주식수만 변경 → 연쇄 재계산** | 통과 — 2.915e9주 입력 시 EPS 7,757→**15,508**(2배), BPS·PER·PBR·시총 즉시 재계산 |
| 6 | 적자(음수 당기순이익) 입력 | 통과 — `-30000` 입력 허용 → EPS **-1,029**(음수), PER·PER×PBR "—" |
| 7 | F1 안내문구(자동 vs 내계산 divergence) | 통과 — 작성 intro + 조회 "내 계산" 라벨에 안내 노출 |
| 8 | 저장/영속 | 통과 — DB manual `schemaVersion:2`, `item`, `netIncome/equity` 키, 커스텀 `shares:2915000000` |
| 9 | 조회 "내 계산" 커스텀 유통주식수 반영 | 통과 — 시총 743.3조·PER 16.44배(커스텀 주식수 파생) |

## 하위호환 (v1 저장분)
- id=9 manual을 v1 형태(`schemaVersion:1`, 레거시 eps/bps, item/netIncome/equity 없음)로 DB 다운그레이드 후 검증.
- 백엔드: `GET /api/company-reports/9` → HTTP 200(무크래시). 응답 manual: schemaVersion 1 유지, 레거시 `eps:5000`/`bps:70000` 읽힘, `item/netIncome/equity` null(Jackson 누락→null).
- 프론트(실제 로드된 `CompanyReportComponent` 함수 직접 호출): `eps=5000 (source=legacy)`, `bps=70000 (source=legacy)`, `crFormulaText='직접 입력값'`, PER=51, PBR=3.64, 시총 743.3조 — 레거시 값 우선(무손실 폴백), 자동 공식이 덮어쓰지 않음.

## 미검증 / 한계
- Kakao OAuth 실로그인은 미수행(자격증명 불가) → JWT 주입으로 대체. 인증 자체 로직은 본 작업 범위 밖.
- ES 미기동 상태로 검증(company report 기능은 ES 비의존).
- 예상 매출 기반 예상 PSR/PER, EV/EBITDA 등 이번 변경과 무관한 항목은 별도 검증 안 함.

## 정리
- 테스트 리포트(id=9) 삭제, bootRun 앱 종료(8080 해제). `local-postgres` 컨테이너·Docker Desktop은 기동 상태로 둠(기존 개발 인프라).
