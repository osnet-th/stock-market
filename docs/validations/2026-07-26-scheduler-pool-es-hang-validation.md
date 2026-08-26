# ES 클라이언트 행 스케줄러 풀 고갈 수정 Validation 기록

**Date:** 2026-07-26
**Issue:** #96
gate: docs/gates/2026-07-26-scheduler-pool-es-hang-gates.md

## 실행한 검증

| 검증 | 명령/방법 | 결과 |
|------|-----------|------|
| Java 컴파일 (work 후) | `./gradlew compileJava -q` | 통과 (기존과 무관한 realestate deprecation note 1건뿐) |
| Java 컴파일 (리뷰 반영 후) | `./gradlew compileJava -q` | 통과 |
| 동시성 정합성 | 4-agent 리뷰 라인 추적 (docs/reviews/2026-07-26-scheduler-pool-es-hang-review.md) | CAS 레이스 없음, lock 내 ES 호출 없음(데드락 불가), executor 큐 유계, 요청 유실 창 없음 확인 |
| 범위 정합성 | 리뷰 diff 대조 | plan 변경 파일 표와 정확히 일치, 숨은 동작 변경 없음 |

## 진단 근거 재확인 (운영 서버, 태형님 직접 실행)

- 스레드 덤프(kill -3): scheduling-3·5가 `ElasticsearchClient.bulk` → `BasicFuture.get()` 영구 블로킹 (기동 5시간 만에 2/5)
- `I/O reactor has been shut down` WARN + ES 서버 컨테이너 5주 무중단 정상 → 앱 측 reactor 사망 확정
- docker logs 72h: ECOS·글로벌 cron 로그 전무 / DB: `global_indicator` 620건 전부 2026-04-11 07:30 생성

## 미검증 항목 (운영 배포 후 확인 — 이 환경에서 불가)

1. **앱 기동 + `log-flush` 스레드 생성 확인** — 배포·재시작 후 스레드 덤프 또는 정상 로그 적재로 확인
2. **cron 재개** — 다음 배치 사이클(서버 07:00/07:30 UTC = KST 16:00/16:30) 후 `docker logs hubth-app | grep "배치 저장 시작"`에 ECOS·글로벌 등장 확인
3. **글로벌 히스토리 적재 재개** — `SELECT max(created_at) FROM global_indicator;` 갱신 확인 (#51 진행의 전제)
4. **ES 정상 상태 로그 적재 동등성** — 배포 후 ES에 로그가 기존처럼 쌓이는지 (5s 주기·트리거 의미 불변 확인)
5. reactor 사망 재현 검증 — 재현 수단 없음. 격리 로직은 리뷰로 검증 완료, 재발 시 기대 동작: 로그 flush만 정지·드롭(카운터/WARN), cron은 지속

## 판단

이 환경에서 가능한 정적 검증은 모두 통과. 미검증 항목은 전부 운영 배포 후 확인 성격이며 확인 절차를 위에 명시 — commit/push 진행 가능. 잔여 리스크(잔존 ES 행 경로 M3, 카운터 도메인 미분리 M4, DART 타임아웃 값 L3)는 review 문서·work 문서에 후속 과제로 기록됨.
