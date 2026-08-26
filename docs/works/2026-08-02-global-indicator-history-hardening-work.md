# 글로벌 경제지표 히스토리 적재 보강 Work 기록

**Date:** 2026-08-02
**Issue:** #51
gate: docs/gates/2026-08-02-global-indicator-history-hardening-gates.md
**Plan:** docs/plans/2026-08-02-001-fix-global-indicator-history-hardening-plan.md

## 변경 파일 (백엔드 8 + 프론트 2)

- `application/GlobalIndicatorSaveService.java` — 클래스 @Transactional 제거. 수집·필터는 트랜잭션 밖, 저장은 `snapshotWriter.persistIndicator` 위임. 지표별 성공/실패 카운트 + 종료 시 요약 로그("히스토리 N건 (지표 성공 a / 실패 b / 전체 c)")
- `application/GlobalIndicatorSnapshotWriter.java` **신규** — 지표 단위 `@Transactional` 저장. `initialSeedIndicator` / `saveChangedIndicator` / `isCycleChanged` 를 기존 로직 그대로 이동, latest upsert + latestMap 갱신은 `upsertLatest` 로 공통화
- `infrastructure/scheduler/GlobalIndicatorWarmupListener.java` **신규** — ApplicationReadyEvent 시 `max(last_collected_at)` 이 없거나 20시간+ 경과 시에만 catch-up. 전용 daemon 스레드(`global-indicator-warmup`) + `LoggingContext.forScheduler`
- `domain/model/GlobalIndicatorLatest.java` — `lastCollectedAt` 필드 (fromSnapshot 에서 updatedAt 과 동일 시각 스탬프)
- `infrastructure/persistence/GlobalIndicatorLatestEntity.java` — nullable `last_collected_at` 컬럼, 생성자·`update()` 파라미터 추가 (**Entity 수정** — ddl-auto=update 로 자동 반영)
- `infrastructure/persistence/mapper/GlobalIndicatorLatestMapper.java` — 양방향 매핑 추가
- `domain/repository/GlobalIndicatorLatestRepository.java` / `...RepositoryImpl.java` / `...LatestJpaRepository.java` — `findMaxLastCollectedAt()`(JPQL select max) 추가, saveAll 갱신 경로에 lastCollectedAt 반영
- `infrastructure/persistence/GlobalIndicatorJpaRepository.java` — 히스토리 조회 정렬 `ORDER BY t.country_name, t.cycle` → `t.snapshot_date, t.id` (시간순)
- `static/js/components/global.js` — 1포인트 렌더링 허용, 단일 포인트 시 `pointRadius: 3` (기존 0이라 점이 안 보였음)
- `static/partials/global.html` — 차트/"데이터 부족" 분기 기준 2포인트 → 1포인트

## 동작 의미 변화

- 배치 저장: 하루치 전체 단일 트랜잭션 → **지표 단위 커밋** (한 지표의 DB 에러가 다른 지표를 롤백시키지 않음). cycle 감지·저장 규칙 자체는 무변경
- 히스토리 조회 응답: 구조 동일, 정렬만 사전순 → 시간순 교정
- 앱 시작: 마지막 수집 20시간+ 경과 시 백그라운드 catch-up (약 2분, 다른 리스너 비블로킹)

## work 단계 자체 검증

- `./gradlew compileJava` 통과, `node --check global.js` 통과

## 특이사항

- 기존 latest 행의 `last_collected_at` NULL → 배포 후 첫 기동에서 catch-up 1회 실행되며 자연 부트스트랩
- catch-up·정규 배치 동시 실행 창(시작 직후 배치 시각): 동일 cycle 중복 히스토리 가능 → 조회 dedup(ROW_NUMBER)이 흡수 (brainstorm Edge Case)
- 지표 커밋 실패 시 latestMap 은 이미 갱신된 상태로 남으나 run-로컬 맵이며 지표 간 키가 겹치지 않아 무해, 다음 실행에서 DB 기준 재적재

## 미검증 항목 (validation 단계 과제)

- 운영 배포 후: 기동 로그("catch-up 시작/불필요"), 배치 요약 로그, `last_collected_at` 컬럼 자동 생성, 그래프 시간순·1포인트 표시 육안 확인
