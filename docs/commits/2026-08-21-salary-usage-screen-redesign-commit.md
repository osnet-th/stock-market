# 월급 사용 비율 화면 목업 기반 재설계 Commit 기록

- gate: docs/gates/2026-08-21-salary-usage-screen-redesign-gates.md
- 승인: 원격 자율 세션 — 태형님 세션 지시(구현·커밋·푸시 포함 일괄 지시)에 근거

## 포함 파일
- 백엔드 salary 도메인 전반 (신규 12 + 수정 9): SpendingItem/SpendingItemSet 도메인·포트·엔티티·
  JPA·매퍼·RepositoryImpl, SpendingConfig(budget), SalaryService, SalaryController,
  DTO(SaveMonthlyCommand/SaveMonthlyRequest/PreviousMonthResponse/CategoryAmountResponse/
  SpendingItemResponse + 기존 응답 확장), GlobalExceptionHandler(HttpMessageNotReadable→400)
- 프론트: partials/salary.html, js/components/salary.js 전면 재작성, js/api.js(+saveSalaryMonthly),
  js/app.js(salary 차트 destroy 훅 제거), js/components/portfolio.js(주석 정리)
- 문서: docs/{brainstorms,issues,plans,works,reviews,validations,commits,pushes,gates}/
  2026-08-21-salary-usage-screen-redesign-*.md

## 제외 파일
- 없음 (스크래치패드 검증 산출물은 저장소 외부)

## 커밋 메시지
feat(salary): 월급 사용 비율 화면 목업 기반 재설계 — 카테고리 하위 항목·예산·일괄 저장·SVG 추이 3모드

## 검증 근거
- docs/validations/2026-08-21-salary-usage-screen-redesign-validation.md
  (compileJava·test 117/117·API E2E·브라우저 E2E 통과)

---

## 2차 커밋 (범위 확장 — 태형님 지시)
- 포함: 커스텀 카테고리(user_spending_category, category String 전환, SalaryCategoryService,
  구조 포함 일괄 저장, 프론트 동적 카테고리 UI) + 저축률 목표 설정화(user_salary_setting,
  savingTarget, 인라인 입력) + `salary_category_check_drop_2026_08_21.sql` + 문서 갱신
- 커밋 메시지: feat(salary): #116 커스텀 카테고리·저축률 목표 사용자 설정화 — enum→사용자 정의 테이블 전환
