---
status: pending
priority: p2
issue_id: 023
tags: [code-review, security, rate-limit, newsjournal]
dependencies: []
---

# 쓰기 엔드포인트 사용자별 레이트리밋 부재

## Problem Statement

`/api/news-journal/events` 의 POST/PUT/DELETE 에 사용자별 레이트리밋이 없다. 인증된 사용자가 자동화 스크립트로 본인 행을 무제한 생성·갱신해 디스크/DB 를 고갈시킬 수 있다. 한 사건 ≈ 12KB (본문 4000×3 + 링크 20×2200) → 하루 수만 건 가능.

## Findings

- 위치: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/presentation/NewsJournalController.java:45-83`
- 본 프로젝트의 다른 인증 도메인(stocknote, portfolio 등) 일관 정책 검토 필요. 동일하게 미적용이면 본 todo 는 전 도메인 정책 결정 영역으로 확장.

## Proposed Solutions

### Option A — Bucket4j 기반 인터셉터 (전 도메인 공통)
- `infrastructure/security/RateLimitInterceptor` 신설. user_id 기준 분당 N회.
- POST/PUT/DELETE 별도 정책 가능.
- 다른 도메인에도 일괄 적용 → 본 변경 단독으로 처리하면 일관성 깨짐.

### Option B — 본 컨트롤러만 한정
- newsjournal 만 적용. 단기 차단 유효, 장기 일관성 부담.

### Option C — 보류 (현 1인용 사용 가정)
- 본 프로젝트가 1인 사용 (카카오 OAuth 단일 계정) 이면 위협 모델 한정. 정책 결정 후 일괄 도입.

## Recommended Action

C 우선 (1인 사용 가정 확인) → A 로 일괄 도입 시점 별도 결정. 분석 단계에서 다른 도메인 일관성 점검 결과를 부속 문서로 남길 것.

## Technical Details

- 라이브러리 도입 시 `build.gradle` 의존성 추가 필요.
- 적용 범위: 모든 POST/PUT/DELETE — 도메인별 differential limit 가능.
- 모니터링: 거부 카운터를 메트릭으로 노출.

## Acceptance Criteria

- [ ] 정책 결정: 1인 사용 한정 vs 멀티 사용 대비
- [ ] (적용 시) user_id 기준 분당 상한 설정
- [ ] (적용 시) 거부 시 429 응답 + 통일된 에러 shape

## Work Log

- 2026-04-28: 발견 (ce-review 보안 P2-2)

## Resources

- security-sentinel 보고 P2-2
- Bucket4j: https://bucket4j.com/