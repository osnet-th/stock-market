# 포트폴리오 목표 자산 배분 비율 Validation 기록 (#102)

gate: docs/gates/2026-07-30-asset-allocation-target-gates.md

## 실행한 명령과 결과

| 명령 | 결과 |
|------|------|
| `./gradlew compileJava` | PASS |
| `./gradlew compileTestJava` | 최초 **FAIL 9건** — 기존 테스트 4파일이 `PortfolioItem` 재구성 생성자 옛 시그니처 사용. 테스트 호출부에 goldDetail 인자 추가 + `PortfolioEvaluationServicePerItemTest`에 `GoldPriceProvider` mock 추가 후 **PASS** |
| `./gradlew test` | **117개 중 116 PASS, 1 FAIL** — 실패는 `StockMarketApplicationTests.contextLoads` 단 1건으로 localhost:5432 PostgreSQL 연결 불가(세션 환경 제약, 본 변경과 무관). 포트폴리오 관련 단위 테스트 전부 PASS |
| `node --check` (api.js·portfolio.js·home.js) | PASS |
| Node 로직 하네스 (scratchpad/allocation-harness.js, 목 API·alert) | **27/27 PASS** — 합계 반올림, 투자 비율 자동 계산, submit 검증 3종(안전 비율·밴드·합 100), 저장 payload(반올림·빈 행 제외·합 100), 저장 후 재조회·모달 닫힘, 목표 프리필/204 기본값, 편차 포맷, 막대/마커 스타일, GOLD 등록·수정·해제 payload, 대시보드 버킷 조회·경고 판정 |
| KRX 금시세 엔드포인트 probe (curl) | 시도 — 세션 네트워크 정책이 `apis.data.go.kr` 차단(프록시 403)으로 확인 불가 |

## 수정 사항 (validation 중 발견)

- 기존 테스트 4파일 컴파일 깨짐(재구성 생성자 시그니처) → 테스트 호출부 9곳 수정 + 평가 서비스 테스트에 GoldPriceProvider mock 추가. 신규 테스트 작성 아님(기존 테스트 유지보수)

## 미검증 항목 (배포/로컬 확인 필요)

1. **KRX 금시세 API 실호출** — 세션에 `DATAGOKR_SERVICE_KEY` 없음 + 네트워크 정책 차단. 운영 키의 "금융위원회_일반상품시세정보" 활용신청 여부 확인 필요. 미구독/실패 시에도 원금 평가 fallback으로 기능 자체는 동작
2. **브라우저 실제 렌더링** — CDN(alpine·tailwind 등) 차단으로 브라우저 하네스 불가. 배분 섹션·설정 모달·대시보드 카드의 실제 화면 확인은 배포 후 필요
3. **contextLoads (Spring 컨텍스트 기동)** — 로컬 DB 필요. `ddl-auto: update`의 신규 테이블(allocation_target·allocation_target_asset)·컬럼(gold_detail.quantity_grams) 생성은 배포 기동 시 적용 (백업 SQL 2건 준비됨)
