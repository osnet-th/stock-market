# 뉴스 기록 마스터-디테일 리디자인 Issue 기록

gate: docs/gates/2026-08-21-news-journal-redesign-gates.md

## GitHub Issue
- status: created
- issue_number: 117
- issue_url: https://github.com/osnet-th/stock-market/issues/117
- title: [enhancement] 뉴스 기록 마스터-디테일 리디자인 — 통합 검색·키워드 필터·인라인 편집·초안 보관·키워드 관계도
- label: enhancement
- 경위: 최초 시도 시 GitHub MCP 자격 증명 오류(Bad credentials, 2회)로 실패해 구현이 선행됨.
  세션 GitHub 연결 복구 후 **사후 등록** (2026-08-21, push 이후).

## 근거
- brainstorm: docs/brainstorms/2026-08-21-news-journal-redesign-brainstorm.md (Status: Decided)
- 태형님이 목업 HTML(업로드 `b18ca2f9-*.html`) 제시 —
  "현재 뉴스기록 화면을 이 html 로 변경하고 부족한 기능은 백엔드 코드 추가 또는 수정해서 맞춰줘"

## Branch
- branch: claude/news-record-html-update-wsz95j (원격 세션 지정 브랜치 — worktree 스크립트 미사용)
- base: main

## 작업 범위 요약
1. 프론트 전면 리라이트 (`partials/news-journal.html`, `components/news-journal.js`)
   - 마스터-디테일 + 인라인 보기/편집, 새 기록 초안 보관(localStorage)·이어쓰기 칩
   - 통합 검색, 시장영향 세그먼트(건수), 분류 드롭다운(검색·건수), 기간 지정 토글
   - 키워드 칩(AND 필터)·초성 검색·추천/가나다 패널, 키워드 관계도 뷰
   - 월별/분류별 그룹 목록(sticky 헤더), 좁은 화면 단일 pane, 더 불러오기
2. 백엔드 확장
   - `GET /api/news-journal/events` 에 `q` + `keywords` 파라미터 추가
   - `GET /api/news-journal/stats` 신설 — 임팩트/분류 건수 + 사건별 키워드 목록
3. Entity/스키마 변경 없음 (마이그레이션 불필요)

## 참고 자료
- 목업: 업로드 `b18ca2f9-*.html` (번들 → 템플릿 330KB / 로직 28KB 추출 대조)
- 선행: #114 (홈 대시보드 리디자인 — `--dc-*` 토큰·IBM Plex 폰트 도입, 본 작업이 계승)
