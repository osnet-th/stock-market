---
title: "REB 광역(SIDO) 카드가 화면에 노출되지 않음 — metadata indicator_code 불일치 + 시군구→광역 attach 누락"
date: 2026-05-30
category: ui-bugs
module: realestate, source-adapter, market-query
problem_type: ui_bug
component: service_object
symptoms:
  - 화면에서 "광역 통계 준비중" 또는 빈 광역 섹션이 표시됨
  - DB의 real_estate_market_indicator에 source=REB 행 153건이 있는데 GET /api/realestate/market/tabs/{category}?regionCode=11 의 cards=[]
  - 시군구 카드 진입(예: regionCode=11110) 시 광역(REB) 카드가 응답 cards 배열에 포함되지 않음
root_cause: config_error
resolution_type: code_fix
severity: high
related_components:
  - frontend_stimulus
  - documentation
tags:
  - realestate
  - reb
  - r-one
  - sido
  - indicator-code
  - metadata-mapping
  - region-attachment
  - query-service
---

# REB 광역(SIDO) 카드가 화면에 노출되지 않음

## Problem

REB R-ONE 어댑터 재설계(이슈 #58, PR #70) 머지 후 운영 검증에서 REB 광역(SIDO) 카드가 화면에 노출되지 않았다. DB에는 `source=REB` 행 153건이 정상 적재되어 있고 `/api/realestate/sources/availability` 는 `reb.available=true` 를 반환하지만, 카테고리 탭 조회 API의 `cards` 배열에는 REB 카드가 포함되지 않았다.

근본 원인은 두 가지가 겹친 결과였다.
1. metadata yml의 indicator-code(예: `SALE_PRICE_INDEX`)와 어댑터가 저장하는 indicator_code(예: `REB_A_2024_00017`)가 불일치
2. `RealEstateMarketQueryService.getTab(regionCode)`이 시군구 코드 입력 시 상위 광역(SIDO) 카드를 함께 조회하지 않음

## Symptoms

- 화면(`partials/realestate.html`) 광역 섹션이 빈 상태 또는 "광역 통계 준비중" 안내로 표시됨
- `GET /api/realestate/market/tabs/PRICE_INDEX?regionCode=11` 응답이 `{"cards":[], "sources":[]}` (lastUpdatedAt은 정상 회신)
- `GET /api/realestate/market/tabs/PRICE_INDEX?regionCode=11110` 응답에 MOLIT 시군구 카드만 포함, REB 광역 카드 누락
- `availability.reb.available=true` 임에도 cards가 비어 보이는 모순 상태

## What Didn't Work

- **availability API 점검**: `reb.available=true`였으므로 api-key 미설정/외부 호출 실패 가설은 기각. 적재는 정상이고 조회 단계에서 막혔다는 신호.
- **batch 재실행**: 배치는 이미 성공(success=536, savedRows=153). 재실행해도 같은 indicator_code(`REB_A_2024_xxxxx`)로만 저장되어 화면 결과 불변.
- **availability 응답 캐싱 의심**: 프론트 캐시 무효화로 해결되지 않음. 백엔드 응답 자체가 빈 배열.

## Solution

### Fix 1 — metadata yml의 의미 코드와 어댑터 저장 코드 일치

`RebStatblProperties.Entry`에 `indicatorCode` 필드를 추가하고, `application.yml`의 STATBL entry에서 metadata yml의 indicator-code와 매핑한다. 4종 묶음(주택종합/아파트/연립/단독) 중 **주택종합 1종만** 매핑(Q1=A 정책) — 미매핑 STATBL은 어댑터 단계에서 skip.

**RebStatblProperties.java** (Entry에 필드 추가):

```java
@Getter
@Setter
public static class Entry {
    private String statblId;
    private String dtacycleCd;
    /**
     * realestate-indicator-metadata.yml의 indicator-code와 매핑되는 의미 코드.
     * null 또는 blank이면 본 STATBL은 적재 대상에서 제외 (대표 1종만 사용 — Q1=A 정책).
     */
    private String indicatorCode;
}
```

**application.yml** (entry에 indicator-code 추가):

```yaml
realestate:
  reb:
    statbl:
      sale-price-total:        { statbl-id: A_2024_00017, dtacycle-cd: MM, indicator-code: SALE_PRICE_INDEX }
      sale-price-apt:          { statbl-id: A_2024_00048, dtacycle-cd: MM }   # 미매핑 — skip
      sale-price-rowhouse:     { statbl-id: A_2024_00083, dtacycle-cd: MM }
      sale-price-singlehouse:  { statbl-id: A_2024_00117, dtacycle-cd: MM }
      rent-price-total:        { statbl-id: A_2024_00020, dtacycle-cd: MM, indicator-code: JEONSE_PRICE_INDEX }
      rent-price-apt:          { statbl-id: A_2024_00053, dtacycle-cd: MM }
      rent-price-rowhouse:     { statbl-id: A_2024_00088, dtacycle-cd: MM }
      rent-price-singlehouse:  { statbl-id: A_2024_00122, dtacycle-cd: MM }
      trade-volume-total:      { statbl-id: A_2024_00604, dtacycle-cd: MM }
      trade-volume-apt:        { statbl-id: A_2024_00612, dtacycle-cd: MM, indicator-code: APT_TRANSACTION_COUNT }
      land-price-change:       { statbl-id: A_2024_00903, dtacycle-cd: MM, indicator-code: LAND_PRICE_CHANGE_RATE }
```

**RebAdapter.java** (의미 코드로 저장 + 미매핑 skip):

```java
private StatblTarget of(RebStatblProperties.Entry entry) {
    if (entry == null || entry.getStatblId() == null || entry.getDtacycleCd() == null) {
        return null;
    }
    // Q1=A 정책: indicator-code 매핑이 있는 STATBL만 적재 (4종 묶음 중 대표 1종만).
    if (entry.getIndicatorCode() == null || entry.getIndicatorCode().isBlank()) {
        return null;
    }
    return new StatblTarget(entry.getStatblId(), entry.getDtacycleCd(), entry.getIndicatorCode());
}

private RealEstateMarketIndicator buildIndicator(RegionCode region,
                                                 RealEstateMarketCategory category,
                                                 StatblTarget target,
                                                 SttsApiTblDataRow row) {
    return RealEstateMarketIndicator.builder()
            .regionCode(region.value())
            .category(category)
            .source(RealEstateMarketSource.REB)
            .indicatorCode(target.indicatorCode())  // 의미 코드 (예: SALE_PRICE_INDEX)
            // ...
            .build();
}

private record StatblTarget(String statblId, String dtacycleCd, String indicatorCode) {}
```

### Fix 2 — 시군구 진입 시 상위 광역(SIDO) 카드 attach

`RealEstateMarketQueryService.getTab`이 시군구 코드(5자리) 입력 시 상위 SIDO 코드(앞 2자리)로 추가 조회해 cards에 합친다. 프론트는 응답 카드를 `regionLevel === 'SIDO'`로 필터하여 광역 섹션에 분리 렌더링.

```java
public TabResponse getTab(String regionCode,
                          RealEstateMarketCategory category,
                          PeriodOption period) {
    Map<MetaKey, RealEstateMarketMetadata> metaMap = loadMetadataMap();
    List<IndicatorCard> cards = new ArrayList<>(
            buildCardsFor(regionCode, category, metaMap, HISTORY_LIMIT, null));

    // 시군구 진입 시 상위 광역(SIDO) 카드도 함께 노출 — R-ONE은 SIDO 단위만 적재.
    // 프론트는 regionLevel='SIDO' 필터로 광역 섹션에 분리 렌더링.
    if (regionCode != null && regionCode.length() == 5) {
        String sidoCode = regionCode.substring(0, 2);
        cards.addAll(buildCardsFor(sidoCode, category, metaMap, HISTORY_LIMIT, null));
    }
    // ...
}
```

### 검증

DB 정리 후 배치 재실행:

```bash
# DB 정리 (기존 REB_A_2024_xxxxx 행 제거)
docker exec postgres psql -U root -d stocks \
  -c "DELETE FROM real_estate_market_latest WHERE source='REB';" \
  -c "DELETE FROM real_estate_market_indicator WHERE source='REB';"

# 앱 재기동 후 화면 동기화 트리거 → 68 rows 적재
# (17 SIDO × 4 indicator = SALE_PRICE_INDEX / JEONSE_PRICE_INDEX / APT_TRANSACTION_COUNT / LAND_PRICE_CHANGE_RATE)
```

API 검증:

```bash
# 광역 직접 진입
curl -s "http://localhost:8080/api/realestate/market/tabs/PRICE_INDEX?regionCode=11" \
  | jq '.cards[] | {indicatorCode, regionLevel, source}'
# → {"indicatorCode":"SALE_PRICE_INDEX","regionLevel":"SIDO","source":"REB"}

# 시군구 진입 → MOLIT 시군구 카드 + REB SIDO 카드 함께 응답
curl -s "http://localhost:8080/api/realestate/market/tabs/PRICE_INDEX?regionCode=11110" \
  | jq '.cards[] | {indicatorCode, regionLevel, source}'
# → {"indicatorCode":"PRICE_PER_SQM","regionLevel":"SIGUNGU","source":"MOLIT"}
# → {"indicatorCode":"SALE_PRICE_INDEX","regionLevel":"SIDO","source":"REB"}
```

## Why This Works

**Fix 1**: `RealEstateMarketQueryService.buildCardsFor()`는 `findHistory(regionCode, category, source, indicatorCode, limit)`를 호출하는데, indicator_code 인자는 metadata yml의 의미 코드(`SALE_PRICE_INDEX` 등)에서 온다. 어댑터가 STATBL ID(`REB_A_2024_00017`)로 저장하면 metadata key와 indicator row의 indicator_code가 불일치해 매칭이 0건이 된다. metadata yml과 어댑터 저장 코드를 일치시키면 즉시 조회가 동작한다. yml에서 미매핑 entry는 어댑터가 skip하여 무관한 STATBL을 무의미하게 적재하지 않는다.

**Fix 2**: 도메인 모델은 시군구(5자리) + 광역(2자리) hybrid (Q1=B 계획). MOLIT/KOSIS는 시군구 단위, REB는 광역 단위로 적재된다. 프론트가 한 화면에서 두 단위를 모두 렌더링하려면 백엔드가 두 단위 row를 함께 응답해야 한다. `regionCode.length()==5`일 때 앞 2자리로 SIDO 조회를 추가하면, 클라이언트는 `regionLevel` 필드만으로 광역/시군구 섹션을 분기 렌더링할 수 있다.

## Prevention

1. **metadata yml과 adapter indicator_code는 동일 소스에서 파생**. metadata yml을 정본으로 두고 adapter가 yml의 indicator-code 필드를 직접 읽어 저장. STATBL ID 같은 외부 식별자는 내부 indicator_code로 그대로 쓰지 말 것.

2. **부팅 시 metadata ↔ adapter 매핑 자기 검증 추가 권장**. `RebStatblProperties.@PostConstruct` 또는 `RealEstateMarketMetadataInitializer`에서 metadata yml의 source=REB indicator-code 집합과 STATBL entry의 indicator-code 집합이 충분히 겹치는지 검증 (현재는 mismatch가 부팅 시 발견되지 않고 조회 시점에 빈 결과로만 드러남).

3. **hybrid region 모델은 query 레이어에서 명시적 attach**. REB가 SIDO만 적재한다는 사실은 어댑터 레벨 결정이지만 화면 렌더링은 시군구 진입에서 시작한다. QueryService가 두 레벨을 함께 응답하는 패턴을 표준화하고, `regionLevel` 필드를 신뢰할 수 있는 라우팅 키로 유지.

4. **integration 검증 시 API 응답 키 카운트만 보지 말고 cards 배열까지 확인**. `lastUpdatedAt`이 채워져도 cards가 빈 경우가 있다 — availability와 별개로 tab API의 cards 배열을 직접 검증하는 smoke test가 필요.

## Related Issues

- 이슈 #58: REB R-ONE 어댑터 광역시도 재설계 (본 fix는 #58 머지 후 운영 검증에서 발견된 후속)
- PR #70: feat(realestate) REB R-ONE 광역시도 어댑터 재설계
- 관련 학습: `docs/solutions/architecture-patterns/realestate-public-open-api-pitfalls-2026-05-07.md` (다중 공공 출처 어댑터링 일반 함정)
- 관련 학습: `docs/solutions/architecture-patterns/ecos-indicator-identifier-source-of-truth-2026-05-26.md` (metadata yml 정본 원칙 — 본 버그는 동일 원칙 위반의 다른 발현)
