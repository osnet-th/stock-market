# 키워드 뉴스 워크스페이스 통합 커밋 기록

gate: docs/gates/2026-08-17-keyword-news-workspace-gates.md
plan: docs/plans/2026-08-17-002-feat-keyword-news-workspace-plan.md
issue: https://github.com/osnet-th/stock-market/issues/115

승인: 태형님 "진행해" (체크포인트 커밋 + main 병합), 이후 각 단계마다 "진행해".

## 커밋 목록

단일 커밋이 아니라 단계별로 나눴다. 백엔드가 먼저 완성되고 화면이 마지막에 붙는 순서라
중간 상태를 되짚을 수 있어야 했다.

| 해시 | 내용 |
|---|---|
| `873a189` | 백엔드 — 언론사·검색확장·키워드통계·CRUD·수집이력(WIP). 51 files |
| `93c14a3` | main 병합 (27커밋, 충돌 0건) |
| `a11afc5` | 수집 이력·스케줄 상태·읽음/저장 — 백엔드 완료 |
| `013149d` | 키워드 뉴스 통합 화면 |
| `52e0cca` | 리뷰 조치 — 이관 안전조건·오류 메시지·영속성 컨텍스트·재색인 시점 |
| `ec5ab4c` | validation — 선택 조건 파라미터 타입 추론 실패 수정 |
| `aaa1320` | ES 뉴스 인덱스 생성 실패 수정 — nori stoptags 교체 |

base: `cfc14fb` → 병합 후 `79e79ec` 기준. 전체 82 files, +5,210 / −717.

## 체크포인트 커밋을 먼저 한 이유

세션 재개 시점에 백엔드 51개 파일이 커밋되지 않은 상태였고, 태형님이 main 병합을 먼저
요청했다. 더티 워킹 트리로 27커밋을 병합하면 충돌 해소가 꼬이므로 먼저 커밋했다.

## 스키마 변경 포함

- `keyword_collection_history` 신규 (Entity + 백업 SQL)
- `user_news_state` 신규 (Entity + 백업 SQL)
- `news.source` 컬럼 추가 (nullable, 백업 SQL)

권위는 Entity 어노테이션(`ddl-auto: update`)이고 SQL 3종은 운영 DBA 수동 적용/롤백 백업용이다.
기존 행 영향 없음 — `source` 는 null 로 남고 표시 측에서 URL 도메인으로 폴백한다.

## 삭제한 파일

- `partials/keywords.html` · `partials/news-search.html`
- `js/components/keyword.js` · `news.js` · `news-search.js`

통합 화면 하나로 대체됐다. 접근 경로는 해시 리다이렉트로 유지된다.

## 커밋하지 않은 것

- 검증용 임시 통합 테스트 2종 (`Issue115QueryValidation`, `Issue115EsValidation`) —
  저장소의 "통합테스트 미작성" 정책에 따라 검증 종료 후 삭제했다.
  이 테스트들이 제품 버그 2건을 잡았으므로 회귀 가드로 남길지는 별도 판단이 필요하다.
- 검증용 ES 컨테이너 — 제거했다. 이미지(`stock-market-es:validation`)는 재검증용으로 보존.
- `.claude/launch.json` 의 하네스 서버 항목 — 검증 후 원복했다.
