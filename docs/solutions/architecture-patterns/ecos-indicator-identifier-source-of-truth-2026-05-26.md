---
title: ECOS 지표 식별자는 metadata yml이 정본 — 하드코딩 참조는 런타임 검증·필터로 방어
date: 2026-05-26
category: architecture-patterns
module: economics
problem_type: best_practice
component: service_object
severity: medium
applies_when:
  - ECOS 국내경제지표를 코드에서 (className, keystatName)으로 참조할 때
  - 지표명을 코드 상수/프리셋/하드코딩으로 들고 있을 때
  - 사용자 정의 수식·파생지표 등 지표 식별자를 영속 저장할 때
related_components:
  - EcosIndicatorMetadataService
  - EcosIndicatorService
  - UserDerivedIndicatorService
tags: [ecos, indicator-identifier, metadata, source-of-truth, runtime-validation, graceful-degradation, compare-key]
---

# ECOS 지표 식별자는 metadata yml이 정본 — 하드코딩 참조는 런타임 검증·필터로 방어

## Context

이슈 #36(사용자 커스텀 파생지표) 구현 중, 대표 spread를 "프리셋"으로 코드에 전사했다.
프리셋 operand의 지표명은 `ecos-indicator-metadata.yml`과 기존 `EcosDerivedIndicatorService`의
`find("국고채수익률(5년)")` 호출을 보고 전사했는데, **실제 구동 환경에서 일부 프리셋이 조용히 사라졌다.**

런타임 검증(`./gradlew bootRun` 후 실제 API 호출)으로 원인을 추적한 결과,
**같은 지표인데 환경마다 철자가 달랐다:**

| 코드/yml 전사값 | 실제 DB 메타데이터(getMetadataMap) |
|---|---|
| `M2(광의통화, 평잔)` | `M2(광의통화)(평잔)` |
| `코스피지수` / `코스닥지수` | `KOSPI` / `KOSDAQ` |

`ecos-indicator-metadata.yml`(시드 정본)과, 먼저 시드된 기존 DB의 `ecos_indicator_metadata`
행이 다른 철자였던 것. 지표 식별자가 한글 표시명 기반(`(className, keystatName)` 복합키, 안정적 코드 없음)
이라 이런 drift가 silently 발생한다.

## Guidance

ECOS 지표를 참조하는 기능을 만들 때:

1. **식별자 정본은 `ecos-indicator-metadata.yml`** — `EcosIndicatorMetadataInitializer`가 이 yml을
   `count==0`일 때만 시드한다. 즉 **이미 시드된 DB는 yml을 반영하지 않는다.** 정본은 yml이되,
   런타임 진실은 `EcosIndicatorMetadataService.getMetadataMap()`(DB 적재분)이다. 둘이 다를 수 있음을 전제하라.

2. **하드코딩한 지표 참조는 런타임 메타데이터로 검증·필터(graceful)** — 코드 상수(프리셋 등)가
   현재 메타데이터에 존재하는 식별자만 노출되도록 거른다. 깨진 항목은 500/빈 목록 대신 조용히 제외 + 로그.

   ```java
   // UserDerivedIndicatorService.presets()
   public List<DerivedIndicatorPreset> presets() {
       Map<String, EcosIndicatorCategory> meta = buildAvailableMeta(); // getMetadataMap() 기반(캐시 독립)
       return presetProvider.all().stream()
               .filter(p -> validator.validate(p.formula(), meta, true).valid()) // 메타에 없는 지표 → 제외
               .toList();
   }
   ```

3. **검증 화이트리스트 소스 = 고정 메타데이터, 평가값 소스 = 최신값 캐시 — 분리하라.**
   존재 검증은 `getMetadataMap()`(캐시 독립 고정 메타)로, 값 계산은 `findAllLatest()`(최신값)로.
   최신값이 비어도 "존재하지 않는 지표"로 오판하지 않는다.

4. **식별자는 `(className, keystatName)` 복합키로** — `keystatName` 단독은 동명 충돌 위험.
   `EcosIndicatorLatest.toCompareKey()`(= `className + "::" + keystatName`)와 동일 규칙을 공유하라.

## Why This Matters

- 지표명이 안정적 코드가 아니라 한글 표시명이라, 시드 시점·출처에 따라 drift가 발생한다.
  하드코딩이 정본과 어긋나면 **예외 없이 조용히** 검증 실패 → 기능이 "왜 안 보이지" 상태가 된다.
- 빈화면 방지(프리셋)처럼 사용자 가치를 담보하는 메커니즘이 이 drift로 무력화되면,
  테스트는 통과(자기 정합 메타로 검증)하는데 운영에서만 깨진다 — **런타임 검증으로만 잡힌다.**
- graceful 필터를 두면 어떤 환경(stale/부분 시드)에서도 유효한 항목만 노출되어 자가 교정된다.

## When to Apply

- ECOS 지표명을 코드/프리셋/시드 SQL에 하드코딩하는 모든 경우
- 사용자가 지표 식별자를 입력·저장하는 기능(파생지표, 관심지표 등) — 저장 시 검증 + 로드 시 graceful
- 신규 환경 배포 시: yml과 DB `ecos_indicator_metadata` 철자 일치 여부를 1회 점검

## Examples

**문제 (자기 정합 테스트는 통과, 운영에서 silently 실패):**
```java
// 프리셋 operand를 yml 전사값으로 하드코딩
ind("통화량", "M2(광의통화, 평잔)")   // ← 이 DB에는 "M2(광의통화)(평잔)"로 적재됨
// 단위 테스트는 프리셋 operand로 만든 meta로 검증 → 통과(거짓 안심)
// 운영: getMetadataMap()에 해당 키 없음 → UNKNOWN_INDICATOR → 프리셋 목록에서 사라짐
```

**해결:**
```java
// 1) presets()가 실제 메타로 필터 → 안 맞는 환경에선 조용히 제외(빈화면/500 방지)
// 2) 메타에 확실히 존재하는 지표만 프리셋으로 전사(금리 카테고리는 yml=DB 일치 확인됨)
ind("시장금리", "국고채수익률(5년)") // available-indicators API로 실제 철자 확인 후 전사
```

**검증 방법 (런타임만이 진실):**
```bash
# 실제 메타데이터 철자 확인 — 추측 금지
curl -H "Authorization: Bearer $JWT" \
  "http://localhost:8080/api/economics/derived-indicators/available-indicators?category=MONEY_FINANCE"
# → 통화량 :: M2(광의통화)(평잔)  (yml의 'M2(광의통화, 평잔)'와 다름)
```

## Related Issues

- 이슈 #36 — 사용자 커스텀 파생지표 (PR #69)
- [global-indicator-history-mirroring.md](./global-indicator-history-mirroring.md) — compareKey 기반 3테이블(history/latest/metadata) 미러링 패턴 (식별자 일관성의 인접 맥락)
- [ecos-timeseries-chart-visualization.md](./ecos-timeseries-chart-visualization.md) — `(className, keystatName)` 복합키 + ROW_NUMBER 최신값 추출
