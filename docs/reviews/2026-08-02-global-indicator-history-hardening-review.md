# 글로벌 경제지표 히스토리 적재 보강 Review 기록

**Date:** 2026-08-02
**Issue:** #51
gate: docs/gates/2026-08-02-global-indicator-history-hardening-gates.md
**방식:** 셀프 리뷰 (#100 선례 — 태형님 일괄 진행 지시)

## Findings

명시적 findings 없음. 검토 경로:

- **트랜잭션 경계**: writer 가 별도 빈이라 프록시 정상 적용 (자기 호출 아님). HTTP 수집·sleep 은 트랜잭션 밖 — 커넥션 점유가 지표당 저장 순간으로 축소. 저장 로직(initialSeed/saveChanged/isCycleChanged)은 diff 대조로 무변경 이동 확인
- **부분 성공**: 지표 A 커밋 성공 후 지표 B 가 DB 예외를 던져도 A 는 유지 — PostgreSQL abort 가 트랜잭션 단위로 격리됨. SaveService 의 지표별 try-catch 가 이제 실제로 의미를 가짐
- **latestMap 정합**: 커밋 실패 지표의 latestMap 선반영은 run-로컬이며 지표 간 키 불겹침(compareKey 에 indicatorType 포함) — 다른 지표 판정에 영향 없음, 다음 실행에서 DB 기준 재구성
- **정렬**: cycle 은 (국가, 지표) 안에서 순차 등장하므로 rn=1 행의 snapshot_date 오름차순 = 시간순. 동률은 id 안정화. 응답 구조 불변 — 프론트 수정 불필요 확인
- **catch-up 조건**: NULL(기존 행)·20h+ 만 실행 — 부트스트랩과 재배포 중복 방지 양립. daemon 스레드라 ECOS warmup 등 다른 리스너 비블로킹, 앱 종료도 안 막음. LoggingContext 로 requestId 부여 (#96 스케줄러 관례)
- **Entity 수정 안전성**: nullable 컬럼 추가만 — 기존 행·기존 쿼리 경로 완전 호환, ddl-auto=update 자동 반영
- **프론트**: 1포인트 시 pointRadius 3 (기존 0이라 렌더해도 안 보이는 문제까지 함께 처리), countries 그룹핑 특성상 0포인트 카드는 실제로 발생하지 않으나 가드 유지

남은 리스크 (결함 아님):

- [nit] catch-up·정규 배치 동시 실행 창에서 동일 cycle 히스토리 중복 행 가능 — 조회 dedup 흡수, 저장량 미미 (문서화됨)
- [nit] 놓친 중간 cycle 은 여전히 소급 불가 (원천이 현재값만 제공 — 설계 한계 유지, 범위 밖)

## Open Questions / Assumptions

- catch-up 임계 20h 는 하루 1회 배치 전제 — 배치 주기를 바꾸면 함께 조정 필요
- Entity 컬럼 추가는 태형님 "한번에 처리" 일괄 지시를 포괄 승인으로 처리 — 최종 보고에 명시, 이견 시 정정

## Change Summary

#51 잔여 결함 4건을 일괄 보강: 지표 단위 독립 트랜잭션(부분 성공 실제 보장 + 요약 로그), 히스토리 시간순 정렬, `last_collected_at` 기반 조건부 catch-up(수집 관측성 확보), 1포인트 점 표시. 저장 규칙·API 응답 구조는 불변이며 compileJava·node --check 통과.
