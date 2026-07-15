# 기업분석리포트 Commit 기록

gate: docs/gates/2026-07-12-company-analysis-report-gates.md
validation: docs/validations/2026-07-12-company-analysis-report-validation.md

## 태형님 승인

- 2026-07-15 "커밋하고 푸시해서 main 병합까지 해줘" — commit · push · main 병합 전체 승인.
- 커밋 구성(코드 A + 문서 B 2커밋)·제외 파일은 직전 응답에서 제시 후 승인.

## 커밋 구성 (2커밋)

### 커밋 A — feat(companyreport): 기업분석리포트 등록·조회 기능 신설

포함:

- `src/main/java/.../companyreport/**` — 도메인/애플리케이션/영속성/프레젠테이션 신규 (30파일). 주가지표 `PriceMetrics.breakdowns`(계산 근거값) + EPS/PER 폴백 포함.
- `src/main/java/.../stock` DART 확장: `CompanyInfoService`, DTO 3(`CompanyProfileResponse`·`MajorShareholderResponse`·`BulkHoldingReportResponse`), domain model 3(`CompanyProfile`·`MajorShareholder`·`BulkHoldingReport`), `CompanyDisclosurePort`, `DartCompanyAdapter` + DART DTO 3.
- (수정) `stock/.../dart/DartApiClient.java` — 기업개황·최대주주·5%룰 대량보유 호출.
- `static/js/components/company-report.js`, `static/partials/company-report.html` — 7단계 위저드 + 상세, 주가지표 툴팁 팝오버(공식+근거), 재무제표 BS/IS/CF 분리.
- (수정) `static/index.html`, `static/js/api.js`, `static/js/app.js`, `static/partials/_sidebar.html`.

### 커밋 B — docs(companyreport): workflow 산출물

포함:

- `docs/{brainstorms,issues,plans,works,reviews,validations,gates,commits,pushes}/2026-07-12-company-analysis-report-*.md` (플랜은 `2026-07-12-001-...`).

## 제외 파일

- `.claude/launch.json` — 검증용 임시 파일 (프라이머리·worktree 공통 커밋 제외).
