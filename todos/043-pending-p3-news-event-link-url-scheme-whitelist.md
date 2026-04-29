---
status: pending
priority: p3
issue_id: 043
tags: [code-review, security, xss, newsjournal]
dependencies: []
---

# `NewsEventLink` URL 스킴 화이트리스트 부재 — `javascript:` / `data:` 허용 가능

## Problem Statement

`NewsEventLink.create` 가 url 형식 검증을 하지 않음. `@Size(max=2000)` 만 적용. 사용자가 본인의 사건에 `javascript:alert(1)` URL 을 입력하면 그대로 저장됨. 현재 프론트는 `<a :href="link.url">` 로 출력하므로 클릭 시 self-XSS 가능. 토큰/세션 탈취로 이어질 여지.

## Findings

- 위치: `src/main/java/.../newsjournal/domain/model/NewsEventLink.java:39-53`
- 출력: `static/index.html:3786-3793` (link.url 을 `<a href>` 로 직접 출력)

확인 보고:
- security-sentinel P3 (#5)

## Proposed Solutions

### Option A — 도메인 검증 추가 (권장)
- `NewsEventLink.create` 에서 url 이 `http://` 또는 `https://` 로 시작하는지 강제.
```java
if (!url.startsWith("http://") && !url.startsWith("https://")) {
    throw new IllegalArgumentException("url 은 http(s):// 로 시작해야 합니다.");
}
```
- 장점: self-XSS 차단.
- 단점: 도메인 검증 추가.

### Option B — 프론트 출력에 `rel="noopener noreferrer"` 만 강화
- 이미 `target="_blank" rel="noopener noreferrer"` 적용. 추가 보완으로 `URL` 객체 검증.
- 장점: 도메인 변경 없음.
- 단점: 서버 검증 부재 → 외부 API 호출 시 우회 가능.

## Recommended Action

(Option A 권장)

## Technical Details

- 영향 파일: `NewsEventLink.java`
- 기존 데이터 영향: 이미 저장된 데이터는 변경 없음

## Acceptance Criteria

- [ ] http(s) 외 스킴 거부
- [ ] 정상 URL 영향 없음

## Work Log

- 2026-04-29: ce-review 발견 (security P3)

## Resources

- `src/main/java/.../newsjournal/domain/model/NewsEventLink.java`
- `src/main/resources/static/index.html` (출력 위치)