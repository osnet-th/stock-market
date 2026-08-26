# 뉴스 기록 마스터-디테일 리디자인 Commit

gate: docs/gates/2026-08-21-news-journal-redesign-gates.md

## 승인

- 태형님 개별 승인 없음 — 원격 자율 세션. 최초 요청문
  ("현재 뉴스기록 화면을 이 html 로 변경하고 부족한 기능은 백엔드 코드 추가 또는 수정해서
  맞춰줘")을 포괄 승인으로 해석 (gate 문서 "세션 특성" 참조). 최종 수용은 푸시된 브랜치
  검토로 판단.

## 커밋 구성

- 단일 커밋, 브랜치 `claude/news-record-html-update-wsz95j`
- 메시지:
  `feat(newsjournal): 뉴스 기록 마스터-디테일 리디자인 — 통합 검색·키워드 AND 필터·인라인 편집·초안 보관·키워드 관계도`

## 포함 파일 (26)

백엔드 수정 10:
- newsjournal/application/NewsEventReadService.java (findStats + 헬퍼 3종)
- newsjournal/domain/repository/{NewsEventListFilter, NewsEventRepository, NewsEventKeywordRepository}.java
- newsjournal/infrastructure/persistence/{NewsEventJpaRepository, NewsEventRepositoryImpl,
  NewsEventKeywordJpaRepository, NewsEventKeywordRepositoryImpl}.java
- newsjournal/presentation/{NewsJournalController, NewsJournalExceptionHandler}.java

백엔드 신규 5:
- newsjournal/application/dto/NewsJournalStatsResult.java
- newsjournal/domain/repository/{NewsEventImpactCount, NewsEventKeywordRow}.java
- newsjournal/presentation/NewsJournalStatsController.java
- newsjournal/presentation/dto/NewsJournalStatsResponse.java

프론트 4:
- static/partials/news-journal.html (전면 교체)
- static/js/components/news-journal.js (전면 교체)
- static/js/api.js (+getNewsJournalStats)
- static/css/custom.css (+.nj-* 섹션)

문서 7:
- docs/{brainstorms,issues,plans,works,reviews,validations,gates}/2026-08-21-news-journal-redesign-*.md
- (commit/push 문서 2건은 본 커밋에 함께 포함 — 총 9 문서)

## 제외 파일

- 스크래치패드 하네스(harness*.js, 스크린샷, node_modules, 추출 목업) — 세션 임시 산출물
- build/ 산출물

## 근거

- 검증: docs/validations/2026-08-21-news-journal-redesign-validation.md
  (gradle 전체 PASS · 목 하네스 61 PASS · 실서버 API/프론트 통합 PASS)
- 리뷰: docs/reviews/2026-08-21-news-journal-redesign-review.md (High 1 포함 5건 조치 완료)
