# 홈 대시보드 경제 대시보드형 리디자인 Commit (#114)

gate: docs/gates/2026-08-17-home-dashboard-redesign-gates.md
validation: docs/validations/2026-08-17-home-dashboard-redesign-validation.md

## 대상

- branch: `feat/issue-114-home-dashboard-redesign`
- worktree: `/Users/tang/Documents/workspace/wt-issue-114-feat-issue-114-home-dashboard-redesign`
- 단일 커밋으로 처리 (37 files, +1,100 / −1,148 · 문서 7종 별도)

## 포함 파일

### 백엔드 — 관심 지표 표시 모드 폐지 (12)

| 파일 | 변경 |
|---|---|
| `favorite/domain/model/FavoriteDisplayMode.java` | **삭제** |
| `favorite/presentation/dto/FavoriteDisplayModeRequest.java` | **삭제** |
| `favorite/application/FavoriteIndicatorService.java` | `changeDisplayMode` 제거, `reorder` 3인자화, 인터리브 헬퍼 6종 제거, history 무조건 첨부 |
| `favorite/domain/model/FavoriteIndicator.java` | `displayMode` 필드 제거 |
| `favorite/domain/repository/FavoriteIndicatorRepository.java` | `updateDisplayMode` 제거 |
| `favorite/infrastructure/persistence/FavoriteIndicatorRepositoryImpl.java` | 동일 |
| `favorite/infrastructure/persistence/UserFavoriteIndicatorJpaRepository.java` | 동일 |
| `favorite/infrastructure/persistence/UserFavoriteIndicatorEntity.java` | 매핑 제거 (**컬럼은 유지**) |
| `favorite/infrastructure/persistence/mapper/FavoriteIndicatorMapper.java` | 매핑 제거 |
| `favorite/presentation/FavoriteIndicatorController.java` | `PUT /display-mode` 제거 |
| `favorite/presentation/dto/FavoriteOrderRequest.java` | `displayMode` 필드 제거 |
| `favorite/presentation/dto/EnrichedFavoriteResponse.java` | 응답에서 `displayMode` 제거 |

### 백엔드 — 키워드 뉴스 통합 피드 (7)

| 파일 | 변경 |
|---|---|
| `news/application/dto/KeywordNewsFeedResponse.java` | **신규** |
| `news/presentation/NewsSecurityContext.java` | **신규** — 인증 주체 일치 확인 (review H1) |
| `news/application/NewsQueryService.java` | `getKeywordNewsFeed` 추가 |
| `news/presentation/NewsController.java` | `GET /api/news/feed` 추가 (403 가드 포함) |
| `news/domain/repository/NewsRepository.java` | 포트 2종 추가 |
| `news/infrastructure/persistence/NewsRepositoryImpl.java` | 구현 2종 |
| `news/infrastructure/persistence/repository/NewsJpaRepository.java` | 쿼리 2종 |

### 프론트엔드 (11)

| 파일 | 변경 |
|---|---|
| `static/partials/home.html` | 셸 재작성 (496 → 71줄) |
| `static/partials/home-indicators.html` | **신규** — 비교 보기 + 지표 카드 그리드 |
| `static/partials/home-side.html` | **신규** — 우측 패널 3종 |
| `static/partials/_sidebar.html` | 목업 스타일 재작성 (157 → 123줄) |
| `static/index.html` | 2단 그리드 래퍼, IBM Plex 폰트, `body` 클래스 |
| `static/css/custom.css` | `--dc-*` 디자인 토큰 + 실측 기반 클래스 |
| `static/js/components/home.js` | 홈 대시보드 상태·헬퍼 전면 확장 |
| `static/js/components/favorite.js` | 컨테이너 단일화, 표시 모드·죽은 차트 헬퍼 제거 |
| `static/js/components/dashboardSummary.js` | 월급만 남기고 축소 (181 → 67줄) |
| `static/js/api.js` | `getKeywordNewsFeed` 추가, `changeFavoriteDisplayMode` 제거 |
| `static/js/app.js` | partial 2종 등록, cleanup 훅 정리 |

### 문서 (7)

`docs/brainstorms` · `docs/issues` · `docs/plans` · `docs/works` · `docs/reviews` ·
`docs/validations` · `docs/gates` 각 1건 (모두 `2026-08-17-home-dashboard-redesign-*`)

## 제외 파일

| 대상 | 사유 |
|---|---|
| 마이그레이션 SQL | **없음** — `display_mode` 는 DB 기본값이 있어 매핑 제거만으로 안전. 컬럼 DROP 은 롤백 여지 확보 위해 후속으로 미룸 |
| `.claude/launch.json` | main worktree 의 로컬 설정 파일(untracked). 이번 브랜치와 무관 |
| 스크래치패드 목 하네스 | 세션 임시 디렉터리에만 존재 |

## 커밋 메시지

```
feat(home): #114 홈 대시보드 경제 대시보드형 리디자인 — 2단 레이아웃·지표 비교·표시모드 폐지·키워드 뉴스 통합

- 홈을 "오늘의 시장" 단일 화면으로 재구성 — 2단 레이아웃(메인 + 320px 패널), 브리핑 카드, 포트폴리오 요약 카드
- 지표 비교 보기 신설 — 관심 지표 최대 3개 오버레이, 변화율(%)/자체 스케일 정규화, 주기 다른 지표는 전 구간 리샘플링
- 국내·글로벌 지표를 단일 카드 그리드로 통합 — 인라인 SVG 스파크라인·카테고리 필터·드래그 순서 편집
- 우측 패널 신설 — 확인이 필요한 것(배분/추세/만기) · 내 키워드 뉴스 · 월급 사용 비율 스택 바
- 관심 지표 표시 모드(displayMode) 폐지 — API 2종 정리, reorder 컨테이너가 sourceType 하나로 단순화(인터리브 헬퍼 6종 제거), history 를 전 항목에 첨부
- 키워드 뉴스 통합 엔드포인트 신설 — GET /api/news/feed (활성 키워드 전체 병합, userId 는 인증 주체와 일치 검증)
- 기존 홈 섹션 제거 — 기능 요약 3카드·요약 카드 4장·최근 업데이트 박스, 월급 도넛 차트. 홈 진입 API 호출 2건 감소
- 목업 실측 디자인 토큰 적용 — IBM Plex Sans KR/Mono, 페이지 배경 #eef0f3, 카드/셀 규격, RED #c02a22 · BLUE #1f4f9e · AMBER #b5854a. 사이드바도 목업 스타일(접힘 모드는 아이콘 유지)
- review 반영 — H1 타 사용자 피드 조회 차단(403), 미사용 import·죽은 로더 제거, 필터 복구, 조회 실패/빈 상태 문구 분리
- validation 반영 — cycle 이 날짜 문자열인 점, 기간 라벨이 실제 구간보다 과장되던 점 수정

Plan 문서: docs/plans/2026-08-17-001-feat-home-dashboard-redesign-plan.md

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
```

## 승인

- 태형님 승인: **완료** (2026-08-17, "맞아 커밋해" — 커밋 메시지 및 표시 모드 폐지 의도 확인)

## 정정 기록

커밋 직전 요약에서 표시 모드 폐지를 "비가역"이라고 설명했으나 **부정확했다.**
`display_mode` 컬럼과 저장된 값이 DB 에 그대로 있고(`NOT NULL DEFAULT 'INDICATOR'`, CHECK 제약 포함)
코드에서 매핑만 제거했으므로, revert 하면 기존 사용자 설정이 그대로 살아난다.
정확한 표현은 "되돌릴 수 있으나 현재는 그 값을 아무도 읽지 않는 상태"다.
