---
title: "feat: 뉴스 검색 UI 페이지 추가"
type: feat
status: active
date: 2026-04-12
origin: docs/brainstorms/2026-04-12-news-search-ui-requirements.md
---

# feat: 뉴스 검색 UI 페이지 추가

## Overview

ES 기반 뉴스 전문 검색 API(`GET /api/news/search`)를 호출하는 프론트엔드 검색 페이지를 추가한다. 사이드바에 "뉴스 검색" 메뉴를 추가하고, `#news-search` 해시 라우트로 검색창 + 필터 + 결과 목록 페이지를 구성한다.

## Problem Frame

뉴스 전문 검색 API가 구현되었지만 이를 호출하는 UI가 없어 사용자가 검색 기능을 사용할 수 없다. (see origin: docs/brainstorms/2026-04-12-news-search-ui-requirements.md)

## Requirements Trace

- R1. 사이드바에 "뉴스 검색" 메뉴 추가, `#news-search` 해시 라우트
- R2. 검색창 + Enter/버튼으로 검색 실행
- R3. 날짜 범위 필터 (시작일~종료일)
- R4. 지역 필터 (국내/해외)
- R5. 검색 결과 카드 (제목, 발행일, 지역)
- R6. 뉴스 클릭 시 원문 URL 새 탭 이동
- R7. 페이지네이션 (기존 키워드 페이지 패턴)
- R8. 검색 결과 없을 때 메시지 표시
- R9. 로딩 상태 표시

## Scope Boundaries

- 검색 자동완성/추천 미포함
- 검색 히스토리 저장 미포함
- 무한 스크롤 미포함

## Context & Research

### Relevant Code and Patterns

- **컴포넌트 패턴**: `js/components/news.js` — `const NewsComponent = { news: {...}, async loadNews() {...} }` 형태
- **라우팅**: `js/app.js` — `menus` 배열 + `navigateTo()` switch + `validPages` 배열
- **API 호출**: `js/api.js` — `API.request(method, url)` 패턴
- **UI**: Alpine.js x-data + Tailwind CSS, 기존 뉴스 목록/페이지네이션 패턴은 `index.html` keywords 섹션 참고
- **아이콘**: 기존 메뉴는 SVG 인라인 아이콘 사용 (`icon` 키로 분기)

## Key Technical Decisions

- **별도 컴포넌트 파일**: `js/components/news-search.js`로 분리 — 기존 `NewsComponent`와 관심사 분리. `app.js`에 spread로 통합
- **API 메서드 추가**: `api.js`에 `searchNews()` 메서드 추가 — 기존 패턴(`getNewsByKeyword`)과 동일하게 query parameter 기반
- **인증 불필요**: 검색 API는 permitAll이므로 비로그인 상태에서도 동작해야 하지만, 현재 앱은 로그인 필수 구조이므로 기존 흐름 유지

## Implementation Units

- [x] **Unit 1: API 메서드 및 컴포넌트 추가**

**Goal:** 검색 API 호출 메서드와 Alpine.js 검색 컴포넌트를 생성한다.

**Requirements:** R2, R3, R4, R5, R7, R8, R9

**Dependencies:** None

**Files:**
- Modify: `src/main/resources/static/js/api.js` (searchNews 메서드 추가)
- Create: `src/main/resources/static/js/components/news-search.js`

**Approach:**
- `api.js`에 `searchNews(query, startDate, endDate, region, page, size)` 추가 — `GET /api/news/search` 호출, query parameter 조합
- `NewsSearchComponent` 생성 — 상태: `newsSearch.query`, `newsSearch.startDate`, `newsSearch.endDate`, `newsSearch.region`, `newsSearch.list`, `newsSearch.page`, `newsSearch.totalPages`, `newsSearch.totalElements`, `newsSearch.loading`, `newsSearch.searched` (검색 실행 여부)
- `searchNews()` 메서드: API 호출 후 결과 반영
- `loadNewsSearchPage(page)` 메서드: 페이지 변경 시 재검색

**Patterns to follow:**
- `js/components/news.js` — 컴포넌트 구조, 상태 관리, 로딩 패턴
- `API.getNewsByKeyword()` — API 호출 패턴

**Test expectation:** none — 프론트엔드 JS, 수동 브라우저 검증

**Verification:**
- `NewsSearchComponent` 객체가 정상 생성됨
- `API.searchNews()` 호출 시 올바른 URL과 query parameter 조합

---

- [x] **Unit 2: 라우팅 및 사이드바 메뉴 등록**

**Goal:** app.js에 뉴스 검색 라우트를 등록하고 사이드바 메뉴에 추가한다.

**Requirements:** R1

**Dependencies:** Unit 1

**Files:**
- Modify: `src/main/resources/static/js/app.js` (menus 배열, validPages, navigateTo switch, component spread 추가)
- Modify: `src/main/resources/static/index.html` (script 태그 추가, 사이드바 아이콘 분기)

**Approach:**
- `menus` 배열에 `{ key: 'news-search', label: '뉴스 검색', icon: 'search' }` 추가
- `validPages`에 `'news-search'` 추가
- `navigateTo()` switch에 `case 'news-search'` 추가
- `...NewsSearchComponent` spread 추가
- `index.html`에 `<script src="/js/components/news-search.js">` 추가
- 사이드바 아이콘 분기에 `search` 아이콘 SVG 추가

**Patterns to follow:**
- 기존 `menus` 배열 항목 구조
- 기존 `navigateTo()` switch 케이스 패턴
- `index.html` script 태그 순서

**Test expectation:** none — 라우팅 설정, 브라우저 검증

**Verification:**
- `#news-search` 해시 이동 시 검색 페이지 표시
- 사이드바에 "뉴스 검색" 메뉴 아이콘 표시

---

- [x] **Unit 3: 검색 UI HTML 템플릿**

**Goal:** index.html에 뉴스 검색 페이지의 HTML 마크업을 추가한다.

**Requirements:** R2, R3, R4, R5, R6, R7, R8, R9

**Dependencies:** Unit 1, Unit 2

**Files:**
- Modify: `src/main/resources/static/index.html` (news-search 페이지 섹션 추가)

**Approach:**
- `x-show="currentPage === 'news-search'"` 섹션 추가
- 검색 영역: 검색 input (`x-model="newsSearch.query"`, Enter 이벤트 바인딩) + 검색 버튼
- 필터 영역: 시작일/종료일 date input + 지역 select (전체/국내/해외)
- 결과 영역: 뉴스 카드 리스트 (`x-for`), 각 카드에 제목·발행일·지역 표시, `<a>` 태그로 원문 링크 (target="_blank")
- 페이지네이션: 이전/다음 버튼 (기존 키워드 뉴스 페이지네이션 패턴)
- 빈 결과: `newsSearch.searched && newsSearch.list.length === 0` 조건으로 메시지 표시
- 로딩: `newsSearch.loading` 조건으로 로딩 인디케이터 표시

**Patterns to follow:**
- `index.html`의 기존 키워드 뉴스 목록 마크업 (카드 스타일, 페이지네이션 버튼)
- Tailwind CSS 클래스 패턴

**Test expectation:** none — HTML 마크업, 브라우저 검증

**Verification:**
- 검색창에 "삼성전자" 입력 후 Enter → 결과 카드 표시
- 날짜/지역 필터 적용 시 결과 변경
- 뉴스 클릭 시 새 탭에서 원문 열림
- 결과 없을 때 메시지 표시
- 로딩 중 인디케이터 표시
- 페이지네이션 동작

## Sources & References

- **Origin document:** [docs/brainstorms/2026-04-12-news-search-ui-requirements.md](../brainstorms/2026-04-12-news-search-ui-requirements.md)
- Related code: `js/components/news.js`, `js/app.js`, `js/api.js`