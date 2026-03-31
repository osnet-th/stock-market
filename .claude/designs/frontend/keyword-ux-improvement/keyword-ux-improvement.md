# 키워드 페이지 UX 개선

## 배경

키워드 페이지에 두 가지 UX 문제가 있음:
1. 키워드 목록 로딩 전 "등록된 키워드가 없습니다" 빈 상태가 먼저 노출됨
2. 키워드 카드 클릭 시 뉴스가 전체 목록 하단에 표시되어 직관적이지 않음

## 변경 사항

### 1. 키워드 로딩 상태 추가

**현재**: `keywords.list = []` 초기값 → 로딩 완료 전에 빈 목록 판정 → "등록된 키워드가 없습니다" 노출

**변경**:
- `keywords` 객체에 `loading: false` 프로퍼티 추가
- `loadKeywords()` 시작 시 `loading = true`, 완료 시 `loading = false`
- HTML에서 `keywords.loading` 일 때 스피너 표시, 로딩 완료 후에만 빈 상태 메시지 표시

**수정 파일**: `keyword.js`, `index.html`

### 2. 뉴스를 키워드 카드 아래 아코디언으로 이동

**현재**: 뉴스 영역이 키워드 목록 `x-for` 밖(line 282~353)에 별도 섹션으로 존재

**변경**:
- 뉴스 영역을 키워드 `x-for` 루프 안으로 이동
- 각 키워드 카드 바로 아래에 `x-show="news.selectedKeywordId === kw.id"`로 조건부 표시
- 같은 키워드 재클릭 시 접힘 (토글 동작 - 이미 `selectNewsKeyword`에 구현됨)
- 기존 하단 뉴스 영역 제거

**수정 파일**: `index.html`

## 작업 리스트

- [x] `keyword.js`: `keywords` 객체에 `loading` 프로퍼티 추가, `loadKeywords()`에 로딩 상태 처리
- [x] `index.html`: 키워드 목록에 로딩 스피너 추가, 빈 상태 조건에 `!keywords.loading` 추가
- [x] `index.html`: 뉴스 영역을 키워드 카드 x-for 루프 안으로 이동 (아코디언)
- [x] `index.html`: 기존 하단 뉴스 영역 제거
- [x] `index.html`: Alpine.js collapse 플러그인 CDN 추가