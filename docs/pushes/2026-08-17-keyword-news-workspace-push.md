# 키워드 뉴스 워크스페이스 통합 푸시·PR 기록

gate: docs/gates/2026-08-17-keyword-news-workspace-gates.md
issue: https://github.com/osnet-th/stock-market/issues/115

승인: 태형님 "올려" (PR 생성), "병합해줘" (병합).

## 브랜치 푸시

- remote/branch: `origin` / `feat/issue-115-keyword-news-workspace`
- upstream 신규 설정 (`push -u`). 기존 원격 브랜치 없음.
- 결과: `[new branch]` 생성 성공.

## main 병합은 사전에 수행했다

이번에는 PR 직전이 아니라 **작업 중간에** main 을 먼저 병합했다 (`93c14a3`).
태형님이 "main 한번 병합하고 진행하자 혹시 충돌 날 수도 있으니까" 로 지시한 건이다.

- 분기 이후 main 이 27커밋 앞서 있었다
- 사전 분석: 내 변경 파일과 main 변경 파일의 **교집합이 공집합**이라 코드 충돌을 예상하지 않았다
- 결과: 충돌 0건
- **판단이 적중했다** — main 이 `static/js/app.js` · `api.js` · `css/custom.css` 를 건드렸는데
  셋 다 Phase 1(화면)에서 수정할 파일이었다. 화면 작업 전에 병합해 충돌을 피했다
- 병합 후 재검증: `compileJava` · `test` 134건 PASS

## PR

- https://github.com/osnet-th/stock-market/pull/123
- 82 files, +5,210 / −717
- `mergeable=MERGEABLE` (충돌 없음)

## CI

최초 생성 시 `documented-workflow-check` 가 실패했다.

```
[check-documented-workflow] Documented workflow requires at least one
docs/works/*.md file through push against origin/main
```

documented 워크플로우가 요구하는 산출물 7종 중 **work · commit · push 3종을 누락**했다.
브레인스토밍·이슈·plan·리뷰·validation·게이트만 작성한 상태였다.
누락분을 작성해 재푸시하고 CI 통과 후 병합한다.

## 운영 반영 시 확인 필요

**ES 뉴스 인덱스 상태를 반드시 확인해야 한다.**

```
curl -s http://<운영ES>:9200/news/_settings
```

- 인덱스가 **없으면** → 기존 stoptags 버그로 생성이 실패해 왔던 것이고, 이번 수정 후 재기동하면 생성된다
- **있으면** → 옛 stoptags 로 만들어진 것이라 이 수정만으로 바뀌지 않는다. **재색인이 필요하다**

`NewsElasticsearchSearcher` 가 예외를 흡수해 빈 결과를 주므로, 잘못된 상태라도
오류가 아니라 "검색 결과 없음"처럼 보인다.

## 그 밖의 운영 영향

- `GET /api/news/search` 가 인증 필수로 바뀐다 (`permitAll` → `authenticated`).
  소비자는 프론트 `api.js` 뿐이라 영향 범위는 닫혀 있다.
- 신규 테이블 2종·컬럼 1개는 `ddl-auto: update` 가 생성한다. 마이그레이션 백업 SQL 3종 포함.
