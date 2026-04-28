---
status: pending
priority: p1
issue_id: 021
tags: [code-review, security, xss, newsjournal]
dependencies: []
---

# 뉴스 저널 link.url scheme 화이트리스트 부재 → Stored XSS (`javascript:`)

## Problem Statement

`NewsEventLink.url` 의 입력 검증이 길이만 수행되고 URL scheme allowlist 가 없다. 사용자가 `javascript:alert(document.cookie)` 같은 값을 저장하면, 타임라인 카드 렌더에서 `<a :href="link.url" target="_blank" rel="noopener noreferrer">` 로 그대로 바인딩되어 본인 클릭 시 자기 세션에서 스크립트가 실행된다. 본인 데이터에 한정된 self-XSS 지만 JWT 탈취 경로가 되므로 머지 차단.

## Findings

- 백엔드 길이만 검증: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/domain/model/NewsEventLink.java:39-53`
- 프론트 DTO 길이만 검증: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/presentation/dto/NewsEventLinkDto.java:12`
- 렌더 위치 (Alpine `:href` 는 sanitize 안 함): `src/main/resources/static/index.html` 의 `NEWS JOURNAL` 섹션 → `<a :href="link.url" target="_blank">` 라인
- 보안 에이전트 P1-1 보고와 일치

## Proposed Solutions

### Option A — 도메인 + 프론트 양단에서 scheme allowlist (Recommended)
- `NewsEventLink.create` 에서 `url.startsWith("http://") || url.startsWith("https://")` 검증 (대소문자 무시, leading whitespace 제거 후).
- 프론트 `news-journal.js` 에 `newsJournalSafeHref(url)` 헬퍼: 미허용 scheme 은 `'#'` 반환. 마크업의 `:href="link.url"` → `:href="newsJournalSafeHref(link.url)"`.
- 효과: 신규 데이터 차단 + 기존(있다면) 위험 데이터 렌더 무력화.

### Option B — 도메인만 차단
- 백엔드 검증만 추가, 프론트는 그대로.
- 단점: 기존 저장된 위험 URL 이 있으면 여전히 클릭 가능. defense-in-depth 미흡.

### Option C — DOMPurify 등 sanitize 라이브러리 도입
- 프론트 외부 라이브러리 추가. CDN/번들 변경 필요.
- 본 변경은 단일 필드라서 over-kill. CLAUDE.md 신규 JS 라이브러리 도입 사전 승인 정책에 걸림.

## Recommended Action

A 적용. 분석/설계 문서를 `.claude/analyzes/newsjournal/url-scheme-xss/`, `.claude/designs/newsjournal/url-scheme-allowlist/` 에 작성 → 승인 후 적용.

## Technical Details

- 백엔드 변경: `NewsEventLink.java` (validation only), 도메인 단위 테스트 가능.
- 프론트 변경: `news-journal.js` 에 헬퍼 추가 + `index.html` 두 군데 (`<a :href>`) 바인딩 수정.
- DB 영향: 신규 테이블이라 backfill 불필요.
- 호환성: 기존 사용자 입력에 `mailto:` 등을 합법으로 보고 싶다면 allowlist 확장 가능.

## Acceptance Criteria

- [ ] `NewsEventLink.create("evt", "title", "javascript:foo", 0)` 가 `IllegalArgumentException` 던짐
- [ ] `NewsEventLink.create("evt", "title", "https://example.com", 0)` 정상 통과
- [ ] 프론트 `newsJournalSafeHref('javascript:alert(1)')` 가 `'#'` 반환
- [ ] 타임라인 카드의 모든 `:href` 가 헬퍼 경유

## Work Log

- 2026-04-28: 발견 및 todo 생성 (ce-review)

## Resources

- 보안 리뷰 P1-1
- OWASP: `javascript:` URL XSS — https://owasp.org/www-community/attacks/Reflected_XSS
- Alpine `:href` sanitize 미적용은 의도된 동작 (Alpine docs)