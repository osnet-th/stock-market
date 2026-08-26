# 투자판단 제안 등급 사유 패널 Commit 기록

**Date:** 2026-07-26
**Issue:** #93
gate: docs/gates/2026-07-26-grade-reason-panel-gates.md

## 포함 파일

- src/main/resources/static/js/components/company-report.js — 사유 패널 상태·기준표 상수·계산식/원천 표 조립 (리뷰 L1~L4·N1 반영)
- src/main/resources/static/partials/company-report.html — 화살표 버튼 2곳 + 우측 슬라이드 패널
- src/main/java/.../companyreport/application/GradeSuggestionCalculator.java — 표시용 사본 역참조 주석 1줄 (리뷰 M1)
- src/main/java/.../companyreport/application/SnapshotFinancialExtractor.java — 판정 임계값 사본 역참조 주석 1줄 (리뷰 M1)
- docs/{brainstorms,issues,plans,works,reviews,validations,commits,pushes,gates}/2026-07-26-grade-reason-panel-*.md — workflow 산출물 9종

## 제외 파일

- 없음 (worktree 내 변경은 위가 전부. 로컬 하네스·launch.json은 저장소 외부/미추적)

## 커밋 메시지

```
feat(companyreport): 투자판단 제안 등급 사유 패널 — 화살표 클릭 시 사유·기준·계산식·원천 데이터 (#93)

- 정량 5항목 제안 배지 옆 › 화살표 → 우측 슬라이드 패널 (위저드 7단계·상세 뷰 공통)
- 패널 4섹션: 산출 사유(판정 색 배지) / 등급 기준(A~E 하이라이트+임계값) / 계산식(응답 값 그대로, 재계산 없음) / 원천 데이터(관련 계정 연도별·청산가치 라인·위험 시그널)
- 등급 기준표는 프런트 표시용 상수(백엔드 기준과 이중 관리, 양방향 역참조 주석)
- ce:review 대체 4-agent 리뷰 findings 반영 (M1·L1~L4·N1) — API·동작 변경 없음
```

- trailer: Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>

## 태형님 승인

- 승인 여부: 승인 (2026-07-26, "main 병합까지 해줘" — 커밋·푸시·PR·병합 일괄 승인)
