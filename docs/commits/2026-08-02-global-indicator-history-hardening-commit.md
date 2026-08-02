# 글로벌 경제지표 히스토리 적재 보강 Commit 기록

**Date:** 2026-08-02
gate: docs/gates/2026-08-02-global-indicator-history-hardening-gates.md

## 포함 파일

- 백엔드 8파일 (SaveService 재구성, SnapshotWriter·WarmupListener 신규, Latest 도메인/Entity/매퍼/리포지토리 3종, 히스토리 조회 정렬)
- 프론트 2파일 (global.js, global.html)
- workflow 문서 9종 (brainstorm/issue/plan/gates/work/review/validation/commit/push)

## 제외 파일

- 없음 (작업 외 변경 없음 — git status 확인)

## 커밋 메시지

```
fix(economics): 글로벌 지표 히스토리 보강 — 지표 단위 트랜잭션·시간순 정렬·조건부 catch-up·1포인트 표시 (#51)
```

## 승인

- 태형님 "한번에 처리하지 왜 이걸 나눠서 처리하는거야" (2026-08-02) — 전체 범위·전 단계 일괄 승인 (Entity nullable 컬럼 추가 포함, 최종 보고에 명시)
