# 기업 분석 리포트 미국 주식 확장 Commit 기록

gate: docs/gates/2026-08-02-us-company-report-gates.md

## 포함 파일

- 백엔드 수정 18 + 신규 12: SEC 어댑터/클라이언트/DTO·도메인 모델(UsCompanyFacts 등)·조립 전략(KR/US Assembler·라우터)·스냅샷 v2·검증 완화·Entity(stock_code 20)·SEC filings API·KIS 해외 기간별시세
- 프론트 수정 3: company-report.js, api.js, company-report.html
- 백업 SQL 1: db/migration/company_analysis_report_stock_code_length_2026_08_02.sql
- 문서 8: brainstorm/gates/issue/plan/work/review/validation/commit(본 문서)·push

## 제외 파일

- build/, .gradle/ (빌드 산출물), .env (gitignore)
- 검증 하네스(UsSnapshotHarness·파이썬 스크립트)는 세션 스크래치패드에만 존재 — 저장소 미포함

## 커밋 메시지

feat(company-report): 기업 분석 리포트 미국 주식 확장 — SEC 스냅샷·통화 지원·매출 태그 폴백 수정 (#105)

## 승인

- 태형님 "진행해 / main 까지 병합해줘" (2026-08-02) — commit·push·PR 생성·main 병합 일괄 승인
- validation 미검증 4건(KIS 실호출·브라우저 일부·KR 런타임·ddl 반영)은 태형님 로컬 8080 직접 테스트 및 배포 후 확인으로 수용
