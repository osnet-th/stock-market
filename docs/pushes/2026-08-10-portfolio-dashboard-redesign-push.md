# 포트폴리오 화면 대시보드형 재설계 Push 기록 (#110)

gate: docs/gates/2026-08-10-portfolio-dashboard-redesign-gates.md
commit: docs/commits/2026-08-10-portfolio-dashboard-redesign-commit.md

## 승인

태형님 "적용 했어 main 에 병합해줘" (2026-08-11) — 커밋·푸시·main 병합 일괄 승인

## 대상

| 단계 | remote/branch | 결과 |
|---|---|---|
| 작업 브랜치 푸시 | `origin/feat/issue-110-portfolio-dashboard-redesign` | 신규 브랜치 생성 + upstream 설정 완료 |
| main 병합 | 로컬 `main` ← `feat/issue-110-...` (`--no-ff`) | **충돌 2건 발생 → 해소 후 병합 커밋 `ce51527`** |
| main 푸시 | `origin/main` | `e902831..ce51527` 완료 |

## 병합 충돌 및 해소

브랜치 분기 이후 main 에 #111(`c4504d4` 납입 리마인더 — 당일 항목 표시·이력 0건 판정 수정)이 들어와 있었고,
#110 과 **같은 파일·같은 로직**을 건드려 충돌이 났다.

### 1. `PortfolioService.java` — `isDepositOverdue()` 의 납입일 조회

- main(#111): 납입일 조회를 `resolveDepositDay(item)` 헬퍼로 추출 + `effectiveDepositDay` / `hasNoDepositThisMonth` 분리, `isDepositDueToday()` 신설
- 브랜치(#110): 같은 자리에 인라인 if-else 로 PENSION 분기 추가
- **해소**: main 의 헬퍼 추출 구조를 채택하고, PENSION 분기를 `resolveDepositDay()` **안으로 이동**. 결과적으로 `isDepositOverdue`·`isDepositDueToday` 양쪽이 연금을 함께 지원한다

### 2. `portfolio-deposit-financial.html` — 납입 리마인더 뱃지

- main(#111): `미납` / `오늘 납입일` 상태 뱃지 추가
- 브랜치(#110): 자산군 뱃지를 `getAssetTypeLabel()` 로 일반화(연금 대응)
- **해소**: 둘 다 유지 — 자산군 라벨(#110) 다음에 상태 뱃지(#111) 를 나란히 노출

### 해소 후 검증

- `./gradlew compileJava` · `compileTestJava` PASS
- `./gradlew test --tests "*Portfolio*"` PASS (59 tests, 0 failures)
- `#111` 심볼 잔존 확인: `resolveDepositDay` · `isDepositDueToday` · `effectiveDepositDay` · `hasNoDepositThisMonth`
- `#110` 심볼 잔존 확인: `PENSION` 4개소(배치 조회 필터 · 납입일 조회 · 납입 대상 검증)

## 푸시 의도

`#110` 4 Phase 구현 + review 수정 + validation 결과를 main 에 반영. 배포는 별도.

## 배포 전 필수 조치

운영 DB 에 아래 SQL 을 **먼저 적용한 뒤** 앱을 배포해야 한다. (`pension_detail_2026_08_10.sql` 상단)

```sql
ALTER TABLE portfolio_item DROP CONSTRAINT IF EXISTS portfolio_item_asset_type_check;
ALTER TABLE portfolio_item ADD CONSTRAINT portfolio_item_asset_type_check
    CHECK (asset_type IN ('BOND','CASH','COMMODITY','CRYPTO','FUND',
                          'GOLD','OTHER','PENSION','REAL_ESTATE','STOCK'));
```

`ddl-auto: update` 는 기존 CHECK 제약을 갱신하지 않아, 없으면 연금 등록이 409 로 실패한다.
나머지 스키마(테이블 2개·컬럼 1개)는 앱 기동만으로 자동 반영된다 (validation 실측 확인).
