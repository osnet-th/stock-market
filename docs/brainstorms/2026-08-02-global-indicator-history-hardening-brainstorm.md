# 글로벌 경제지표 히스토리 적재 보강 - Brainstorm

**Date:** 2026-08-02
**Status:** Decided (태형님 "한번에 처리하지 왜 이걸 나눠서 처리하는거야" — #51 전체 범위 일괄 진행 승인, 2026-08-02)
gate: docs/gates/2026-08-02-global-indicator-history-hardening-gates.md

## 배경

- 기존 open 이슈 #51 "global 경제 지표 히스토리 처리 불가" (2026-05-10 태형님 등록)
- 직접 원인이던 스케줄러 전체 정지(#96, ES 클라이언트 행 → 풀 고갈)는 수정·병합 완료 (PR #98). cron 재개로 히스토리 적재 자체는 복구 경로에 있음
- #96 진단 과정에서 확인된 글로벌 히스토리 파이프라인 자체의 잔여 결함 4가지가 #51 범위로 남아 있었음:
  1. **배치 전체 단일 트랜잭션** — PostgreSQL 은 트랜잭션 내 SQL 에러 1건이면 전체 abort. 지표별 try-catch 가 있어도 하루치 저장 전체가 롤백될 수 있어 "부분 성공 허용"이 실제로 동작하지 않음
  2. **그래프 시간순 정렬 버그** — 히스토리 조회가 `ORDER BY cycle`(문자열 사전순). cycle 이 "10월 2026" 같은 텍스트라 데이터가 쌓이는 즉시 x축이 뒤섞여 보임
  3. **catch-up 부재** — 배치 시각(07:30 UTC)에 앱이 내려가 있으면 그날 수집을 건너뜀. ECOS 는 warmup 이 있어 재시작마다 보충되는 것과 비대칭. 또한 "마지막 수집 시각"을 알 방법이 없어 #96 진단 때 관측이 어려웠음
  4. **1포인트 국가 미표시** — 차트가 2포인트부터 렌더링(`pointRadius: 0` 이라 1포인트는 그릴 수 없음). 시딩 직후 상태에서는 그래프 탭 전체가 "데이터 부족"

## What We're Building (전체 일괄)

1. **지표 단위 독립 트랜잭션**: 클래스 레벨 `@Transactional` 제거. HTTP 수집은 트랜잭션 밖, 저장은 신규 `GlobalIndicatorSnapshotWriter`(@Transactional, 지표 단위)로 분리. 한 지표의 DB 실패가 다른 지표를 롤백시키지 않음. 배치 요약 로그(성공/실패/전체 지표 수) 추가
2. **시간순 정렬**: `ORDER BY t.country_name, t.cycle` → `ORDER BY t.country_name, t.snapshot_date, t.id` (cycle 은 겹치지 않고 순차 등장하므로 대표 snapshot 의 관측 시각 오름차순 = 시간순)
3. **조건부 catch-up + 수집 시각 컬럼**: `global_indicator_latest`에 `last_collected_at` 추가(nullable — 기존 행 호환). cycle 변경 여부와 무관하게 수집 성공마다 갱신. 앱 시작 시 `GlobalIndicatorWarmupListener`가 max(last_collected_at)이 없거나 20시간 이상 경과 시에만 fetchAndSave 실행 — 전용 daemon 스레드(ECOS warmup 등 다른 리스너 블로킹 방지)
4. **1포인트 표시**: 히스토리 1포인트 국가도 차트 렌더링(점 pointRadius 3). "데이터 부족"은 0포인트일 때만

## 확정된 결정

| 항목 | 결정 |
|------|------|
| 범위 | 위 4개 전부 일괄 (태형님 "한번에 처리" 지시) |
| catch-up 방식 | 조건부(20h) + `last_collected_at` 컬럼 — 이전 질의에서 Recommended 로 제시했던 안. **Entity 수정 승인 게이트 항목**: 태형님 일괄 진행 지시를 포괄 승인으로 기록, 최종 보고에 명시 |
| 트랜잭션 분리 방식 | 별도 @Transactional 빈(`GlobalIndicatorSnapshotWriter`) — 자기 호출 프록시 우회 문제 없음, 기존 initialSeed/saveChanged/isCycleChanged 로직은 그대로 이동 |
| 1포인트 UI | 포함 (이전 질의 Recommended, 태형님 no preference → 일괄 지시로 포함) |
| 놓친 중간 cycle | 소급 불가 수용 (스크래핑은 현재값만 제공 — 기존 설계 한계 유지) |

## 검토한 대안 (채택 안 함)

- **무조건 시작 시 warmup (ECOS 방식)** — 재배포마다 TradingEconomics 41회 순차 스크래핑(약 2분) 발생. 배포가 잦아 차단 위험
- **catch-up 없이 cron만** — 배치 시각에 앱이 꺼져 있던 날 복구 불가, "마지막 수집 시각" 관측 공백 지속
- **TransactionTemplate 로 단일 클래스 유지** — 별도 빈이 Spring 관례에 부합하고 저장 로직 책임 분리가 명확

## Edge Cases

- 기존 latest 행의 `last_collected_at` NULL → max NULL → 첫 기동 시 catch-up 실행 → 이후 전부 스탬프 (자연 부트스트랩)
- catch-up 과 정규 배치가 드물게 겹치면 동일 cycle 히스토리 중복 INSERT 가능 → 조회의 cycle 단위 dedup(ROW_NUMBER)이 흡수, 저장도 지표 단위 트랜잭션이라 무해
- #25 로 추가된 10개 지표(시딩 누락): latestMap 에 없으므로 첫 정상 수집에서 자동 시딩 (별도 코드 불필요)
- 시딩일(04-11) 동일 snapshot_date 다수 행: 국가당 cycle 1개뿐이라 정렬 동률 없음, id 보조 정렬로 안정화

## 범위 밖 (하지 않음)

- 놓친 중간 cycle 소급 수집 (원천 데이터가 현재값뿐)
- ECOS 쪽 동일 패턴 적용 (ECOS 는 단일 API 호출로 이미 짧은 트랜잭션)
- 수집 주기 변경, TradingEconomics 외 소스 추가
