---
date: 2026-05-24
topic: realestate-admin-batch-trigger-ui-fix
issue: 41
status: fix
---

# fix: realestate 관리자 수동 배치 트리거 프론트 UI 누락

## 문제

PR #57 (`feat(realestate)`)에 **백엔드 + admin guard path** 는 머지됐으나,
**프론트 UI(api 헬퍼 + 버튼 + 호출 액션)** 가 누락되어 관리자가 화면에서 일배치를
즉시 실행할 방법이 없음.

확인:
- 백엔드: `POST /api/admin/realestate/batch/run` (`RealEstateAdminController` +
  `RealEstateBatchTriggerService` in-flight guard) ✅ main 존재
- AdminGuardInterceptor path: `/api/admin/realestate/**` ✅ 등록됨
- 프론트 `api.js` / `realestate.js` / `realestate.html` 어디에도 호출 코드 없음 ❌

## 결정

realestate 페이지 헤더에 **관리자 전용 "지금 동기화" 버튼** 추가:
- `auth.isAdmin` 으로 노출 가드 (사이드바 admin-logs 메뉴와 동일 패턴)
- 응답 분기: 202 triggered (성공 메시지) / 409 rejected (경고) / 401·403 (에러)
- 동시 클릭 방지: `realestate.adminBatch.triggering` flag 로 disabled

## 검증

- 비관리자 계정 로그인 시 버튼 미노출
- 관리자 클릭 시 202 → "동기화가 시작되었습니다…" 메시지
- 연속 클릭 / 다른 트리거 중 클릭 → 409 → "다른 배치가 이미 실행 중입니다…"
