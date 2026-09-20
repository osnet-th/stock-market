# 기업 리포트 S-RIM 커밋 기록

gate: docs/gates/2026-09-19-company-report-srim-gates.md
plan: docs/plans/2026-09-19-001-feat-company-report-srim-plan.md
issue: https://github.com/osnet-th/stock-market/issues/122

승인: 태형님 "됐어 커밋하고 main 에 병합해줘" + AskUserQuestion "전부 진행" / "그대로 커밋".

## 커밋
- 해시: `4c075c7` (작성자 정정 전 `070facd`)
- 브랜치: `codex/issue-122-company-report-srim`
- base: `cfc14fb`
- 파일: 38개 (변경 18 / 신규 20), 1506 insertions / 25 deletions

## 포함 파일
- domain: SrimInput/Valuation/Calculator/Validator, StockMarketCode, CompanyAnalysisReport, repository 포트
- application: SrimService, SrimData/InputData/DecimalSerializer/YearDeserializer, CompanyReportSnapshotPersistenceService, 기존 쓰기/조회 서비스·DTO, KrReportSnapshotAssembler
- infrastructure: Entity, Mapper, SrimJsonConverter, JpaRepository, RepositoryImpl
- presentation: Controller, Create/Update 요청 DTO
- static: company-report.html, company-report.js, api.js
- ARCHITECTURE.md
- docs: brainstorm, issue, plan, work, review, validation, gate

## 제외 파일
- `docs/plans/.2026-09-19-001-feat-company-report-srim-plan.md.swp` — vim 활성 세션(PID 10473)이 보유 중인 스왑 파일. 잔여물이 아니므로 삭제하지 않고 커밋에서만 제외했다.
- `.env` — gitignore 대상. 스테이징 후 미포함을 명시적으로 확인했다.

## 커밋 메시지
```
feat(companyreport): #122 기업 리포트 S-RIM 적정주가 — 연도별 ROE 3시나리오·독립 JSONB 저장·재무 새로고침 보존
```
본문에 구현 5항목과 2차 리뷰 findings 7건 반영 내역, Plan 문서 경로, Co-Authored-By 포함.

## 작성자 정정
최초 커밋이 저장소에 `user.name`/`user.email`이 없어 `tang <tang@tangui-MacBookAir.local>`로 자동 생성됐다.
푸시 전에 발견해 태형님 확인 후 `TaeHyung <osnet.th@gmail.com>`(히스토리 최다 101건, osnet-th 계정 일치)으로 설정하고 `--amend --reset-author` 했다.
저장소 local config에만 설정했으며 global config는 변경하지 않았다.
