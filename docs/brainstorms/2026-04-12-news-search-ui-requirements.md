---
date: 2026-04-12
topic: news-search-ui
---

# 뉴스 검색 UI 페이지

## Problem Frame

ES 기반 뉴스 전문 검색 API(`GET /api/news/search`)가 구현되었지만, 이를 호출하는 프론트엔드 UI가 없다. 사용자가 자유 텍스트로 뉴스를 검색할 수 있는 별도 페이지가 필요하다.

## Requirements

**검색 기능**
- R1. 사이드바에 "뉴스 검색" 메뉴를 추가하고, `#news-search` 해시 라우트로 별도 페이지를 구성한다
- R2. 검색창에 키워드를 입력하고 검색 버튼 또는 Enter 키로 검색을 실행한다
- R3. 날짜 범위(시작일~종료일) 필터를 제공한다
- R4. 지역(국내/해외) 필터를 제공한다

**검색 결과**
- R5. 검색 결과는 제목, 발행일, 지역을 카드 형태로 표시한다
- R6. 뉴스 클릭 시 원문 URL로 새 탭 이동한다
- R7. 페이지네이션을 제공한다 (기존 키워드 페이지 패턴과 동일)
- R8. 검색 결과가 없을 때 "검색 결과가 없습니다" 메시지를 표시한다
- R9. 검색 중 로딩 상태를 표시한다

## Success Criteria

- 검색창에 "삼성전자"를 입력하면 관련 뉴스 목록이 표시된다
- 날짜/지역 필터가 검색 결과에 반영된다
- 기존 페이지(키워드, 포트폴리오 등)에 영향 없음

## Scope Boundaries

- 검색 자동완성/추천 미포함
- 검색 히스토리 저장 미포함
- 무한 스크롤 미포함 (기존 페이지네이션 패턴 사용)

## Dependencies / Assumptions

- 백엔드 `GET /api/news/search` API가 이미 구현되어 있다
- 기존 프론트엔드 패턴 (Alpine.js + Tailwind CSS + api.js) 을 따른다

## Next Steps

→ `/ce:plan` for structured implementation planning