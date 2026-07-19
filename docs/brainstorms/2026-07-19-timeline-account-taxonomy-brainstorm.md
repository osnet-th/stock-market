# 재무 타임라인 계정 taxonomy 불일치 - Brainstorm

**Date:** 2026-07-19
**Status:** Decided (원인 실측 확정 + 수정 범위 "근본 수정" 승인, 2026-07-19)
**Gate:** docs/gates/2026-07-19-timeline-account-taxonomy-gates.md
**Issue:** https://github.com/osnet-th/stock-market/issues/85

## 배경
현대차(005380) 기업 리포트/타임라인에서 연도별 당기순이익·영업활동현금흐름이 "—"로 누락되어 파생 계산이 끊긴다는 태형님 지적. 원천 DART까지 실측해 원인을 확정.

## 확정 원인 (원천 DART 실측)

### A. 영업활동현금흐름 2017·2018 "—" — 세부계정 id 접두사 불일치
- 2017·2018 사업보고서(11011, CFS): `id=ifrs_CashFlowsFromUsedInOperatingActivities` (구 접두사), 값 3.92조·3.76조 존재.
- 2019~: `id=ifrs-full_CashFlowsFromUsedInOperatingActivities` (신 접두사).
- `FinancialTimelineAssembler.detailRowKey`(FinancialTimelineAssembler.java:172)가 accountId로 키잉 → 구/신 접두사 **별도 행 분리**.
- `SnapshotFinancialExtractor.detailSeries(CF, "ifrs-full_…", "영업활동")`가 `findFirst()`로 신 접두사 행만 선택 → 2017·2018 누락.

### B. 당기순이익 2018~2022 "—" — 요약 소스(주요계정) 결측
- `fnlttSinglAcnt`(주요계정)에 현대차 2018~2022 당기순이익 행 **부재**(2020 실측). 2024는 "당기순이익(손실)"로 존재.
- 값은 `fnlttSinglAcntAll`(전체재무제표) IS "연결당기순이익"에 존재(2020=1.92조, id="-표준계정코드 미사용-").
- 요약(`toSummaryRows`, accountName 키잉) 기반 `summarySeries("당기순이익")`가 결측 연도 보강 못함.

### (버그 아님) 영업CF 음수 2020~2025
- DART 실제 값. 현대차 연결은 캐피탈/금융리스 채권 증가가 영업활동 유출로 반영 → 연결 OCF 음수/소액. 정상.

## 수정 방향 (근본 — 조립기, 태형님 "근본 수정" 승인)
1. **A**: IFRS 계정 id 접두사 정규화(`ifrs_` ↔ `ifrs-full_` 동일 요소)로 세부행 병합 → BS/IS/CF 전체 세부계정 적용. 종목 평가·재무 타임라인·기업 리포트 모두 정상화.
2. **B**: 요약 당기순이익 결측 시 전체재무제표 IS의 **연결 총계 당기순이익**(`ifrs-full_ProfitLoss` / "연결당기순이익")으로 보강. 지배·비지배·계속영업 하위 라인 제외.

## Open Questions (plan에서 확정)
1. id 정규화 지점: `detailRowKey` + `matches`/`detailSeries`만? 아니면 파싱 단계에서 canonical id? `dart_` 접두사·"-표준계정코드 미사용-"는 이름 키 유지.
2. B의 보강을 조립기(summaryRows에 net income backfill)에서 할지, 추출기에서 detail 참조로 할지. 근본 수정 취지상 조립기.
3. B에서 "총계 당기순이익" 식별 규칙: id `ifrs-full_ProfitLoss` 우선, 무표준코드면 이름 "당기순이익"/"연결당기순이익"이되 "지배기업소유주지분"·"비지배지분"·"계속영업" 제외.
4. 요약 결측이 당기순이익 외 다른 계정(매출액·영업이익·자산총계 등)에도 있는지 → 연도별 실측 후 보강 범위 결정.

## 검증 기준
- 현대차 005380: 영업CF·당기순이익 2017~2026 전부 채워짐, FCF·성장률·EPS 파생 정상.
- 회귀 없음: 삼성전자 등 기존 정상 종목 값 불변(스냅샷 재검증).

## 범위 밖
- 영업CF 음수(정상), #84.
