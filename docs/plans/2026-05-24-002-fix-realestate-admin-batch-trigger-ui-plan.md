---
title: "fix: realestate 관리자 수동 배치 트리거 프론트 UI 추가"
type: fix
status: completed
date: 2026-05-24
origin: docs/brainstorms/2026-05-24-realestate-admin-batch-trigger-ui-fix.md
issue: 41
---

# fix: realestate 관리자 수동 배치 트리거 프론트 UI 추가

## Overview

PR #57 머지 후 누락된 관리자 수동 배치 트리거 프론트 UI 를 추가. 백엔드/admin guard
는 변경 없이 frontend 만 수정 (api 헬퍼 + 컴포넌트 액션 + 헤더 버튼/메시지).

## Scope Boundaries

- 백엔드/Controller/Service 변경 없음 (이미 main 에 존재)
- 다른 도메인 영향 없음

## Implementation Units

- [x] **Unit 1: api.js 에 `triggerRealEstateBatch()` 헬퍼 추가**

`POST /api/admin/realestate/batch/run` 호출. realestate 섹션 끝에 한 줄 추가.

- [x] **Unit 2: realestate.js 에 `triggerRealEstateAdminBatch()` 액션 + 상태 추가**

`realestate.adminBatch = { triggering, message, messageType }` 상태와 토글/응답
분기(202 triggered / 409 rejected / 401·403 / 기타) 처리.

- [x] **Unit 3: realestate.html 헤더에 관리자 버튼 + 결과 메시지 박스 추가**

`x-show="auth.isAdmin"` 가드, disabled while triggering, success/warning/error
색상별 메시지 박스(emerald/amber/rose).

## Verification

- 헤더 우측에 "지금 동기화" 버튼이 관리자에게만 표시
- 클릭 시 disabled 상태로 전환, 응답 후 메시지 박스에 결과 표시
- 동시/연속 클릭 차단 (frontend triggering flag + backend in-flight guard 양면 가드)

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| 관리자 권한 미부여 사용자가 버튼 노출 회피 후 직접 호출 | 백엔드 `AdminGuardInterceptor` 가 userId 화이트리스트로 가드 (이미 PR #57에서 적용) |
| 트리거 후 진행 상황 polling 부재 | MVP 한정. 추후 진행 상태/완료 알림은 별도 이터레이션 |