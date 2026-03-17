---
title: "feat: ECOS 국내 경제지표 대시보드 개선 (카드 + 테이블 하이브리드)"
type: feat
status: active
date: 2026-03-16
origin: docs/brainstorms/2026-03-16-ecos-indicator-dashboard-brainstorm.md
---

# ECOS 국내 경제지표 대시보드 개선

## Enhancement Summary

**Deepened on:** 2026-03-16
**Sections enhanced:** 7
**Research agents used:** architecture-strategist, performance-oracle, data-integrity-guardian, julik-frontend-races-reviewer, code-simplicity-reviewer, best-practices-researcher (x3)

### Key Improvements

1. **아키텍처 정제**: `KeyStatIndicator`를 API 전용 모델로 유지, 별도 enriched 도메인 모델 도입 vs 직접 필드 추가 트레이드오프 정리
2. **프론트엔드 레이스컨디션 해결**: 빠른 탭 전환 시 stale response 방지를 위한 generation counter 패턴 필수 적용
3. **단순화 적용**: description/tooltip 단일 필드 통합, 테이블 그룹 헤더 → className 정렬로 대체
4. **데이터 무결성**: 첫 배포 시 NULL 처리 전략, 트랜잭션 경계 명확화

### New Considerations Discovered

- 기존 `loadEcosIndicators()`에 stale response 레이스컨디션이 이미 존재 (현재 플랜과 무관하게 수정 필요)
- `EcosIndicatorLatestRepositoryImpl.saveAll()`에 기존 N+1 패턴 존재 (101행이라 당장 문제 아님, 향후 개선 대상)
- 카드 접근성: `<ul>/<li>` 시맨틱 마크업 + `aria-label` + `sr-only` 적용 필요

---

## Overview

현재 플랫 테이블로만 표시되는 국내 경제지표(ECOS) 화면을 **카드 대시보드 + 상세 테이블 하이브리드** 방식으로 개선한다. 선택된 카테고리의 핵심 지표를 카드로 요약하고, 하단 테이블에는 전기 대비 변화와 툴팁 해설을 추가한다.

## Problem Statement / Motivation

1. 숫자만 나열되어 값이 좋은 건지 나쁜 건지 맥락 없음
2. 지표명만 봐서는 경제적 의미 파악 어려움
3. 모든 행이 동일한 모양으로 중요도 구분 불가

(see brainstorm: docs/brainstorms/2026-03-16-ecos-indicator-dashboard-brainstorm.md)

## Proposed Solution

### 카드 대시보드 (상단)

- 현재 선택된 카테고리의 핵심 지표 2~4개를 카드로 표시
- 각 카드: 지표명 + 현재 값 + 전기 대비 변화(▲/▼ + 수치) + 간단 해설 (1줄)
- 색상: 화살표는 방향(상승=빨강/하락=파랑), 카드 왼쪽 테두리(`border-l-4`)로 긍정/부정 구분
- 카드 클릭 동작 없음 (정보 표시 전용)

### Research Insights — 카드 UI

**Best Practices (Alpine.js + Tailwind CSS):**
- 반응형 그리드: `grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4` (사이드바 `w-56` 고려)
- `x-for`에 반드시 `:key="ind.keystatName"` 지정 (DOM diffing 최적화)
- 로딩 시 `animate-pulse` 스켈레톤 카드 표시 (빈 상태 방지)
- 카드에 `<ul>/<li>` + `<article>` 시맨틱 마크업 적용 (접근성)
- 장식용 화살표에 `aria-hidden="true"`, 스크린 리더용 `sr-only` 텍스트 제공

**Performance:**
- 카테고리당 2~4개 카드 = 30-50개 DOM 요소. Alpine.js에 전혀 부담 없는 규모 (성능 이슈는 500+ 요소부터)
- 변화율/방향 등 비즈니스 로직은 서버에서 pre-computed하여 전달. 프론트엔드는 표시만

### 상세 테이블 (하단)

- 기존 테이블 유지 + className 기준 정렬 (시각적 그룹핑 효과)
- 전기 대비 변화 컬럼 추가 (변화량 + 변화율)
- 행 호버 시 순수 CSS 툴팁으로 해설 표시

### Research Insights — 테이블 & 툴팁

**단순화 결정 (code-simplicity-reviewer 권장):**
- ~~테이블 className별 그룹 헤더~~ → **className 기준 정렬만으로 충분**. 그룹 헤더는 collapsed/expanded 상태 관리가 필요하여 복잡도 증가. className 컬럼이 이미 그룹 정보를 전달
- 툴팁은 **순수 CSS `group-hover` 방식** (외부 라이브러리 불필요): `invisible opacity-0 group-hover:visible group-hover:opacity-100` + `transition-opacity`

### 데이터 전략

- `ecos_indicator_latest` 테이블에 `data_value` + `previous_data_value` 컬럼 추가
- 배치 시 cycle 변경 감지 → 현재 dataValue를 previousDataValue로 이전, 새 dataValue 저장
- 캐시에도 이전값 포함 (배치 적재 시 pre-merge) → 매 요청마다 DB 조회 불필요
- 기존 API (`/api/economics/indicators?category=`) 확장: IndicatorResponse에 필드 추가

### Research Insights — 데이터 전략

**캐시 구조 (Caffeine 리서치):**
- 배치 적재 시 `previousDataValue`를 pre-merge하여 캐시에 저장 (조회 서비스 단순화)
- 수동 `Cache.put()` 유지 (현재 패턴이 배치 적재에 최적)
- `KeyStatIndicator` record에 `previousDataValue` 필드 추가 (Caffeine은 in-memory라 직렬화 호환 문제 없음)

**데이터 무결성 (data-integrity-guardian):**
- 첫 배포 시 기존 행의 `data_value`, `previous_data_value`는 NULL → 다음 배치에서 `data_value` 채워짐
- `previous_data_value`는 첫 번째 cycle 변경이 발생해야 비로소 값이 채워짐 (정상 동작)
- NULL은 "이전 관측값 없음"을 의미, 절대 0으로 취급하지 않음
- 히스토리 INSERT + latest UPSERT는 **반드시 동일 트랜잭션** 내에서 수행

**아키텍처 결정 — 도메인 모델:**

두 가지 접근법이 있으며, 구현 시 설계 문서에서 최종 결정:

| 접근법 | 장점 | 단점 |
|--------|------|------|
| **A: KeyStatIndicator에 직접 추가** | 단순, 변경 최소 | API 원본과 DB 파생 데이터 혼재 |
| **B: 별도 IndicatorSnapshot 도메인 모델** | 모델 의미 명확, 단일 출처 원칙 | 클래스 추가, 변환 로직 필요 |

- 아키텍처 리뷰어는 **접근법 B (별도 모델)** 권장 (도메인 순수성)
- 단순성 리뷰어는 **접근법 A (직접 추가)** 권장 (101개 지표 규모에서 과잉 설계 회피)
- **권장**: 접근법 A로 시작. 향후 모델이 복잡해지면 B로 리팩토링

### 지표 메타데이터

- `src/main/resources/ecos-indicator-metadata.yml` 에 YAML 설정 파일 신규 생성
- `spring.config.import: classpath:ecos-indicator-metadata.yml` 로 로딩 (Spring Boot 공식 메커니즘)
- `@ConfigurationProperties(prefix = "ecos.indicator.metadata")` 로 바인딩
- 위치: `economics/infrastructure/korea/ecos/config/EcosIndicatorMetadataProperties.java` (기존 EcosProperties 패턴)

### Research Insights — 메타데이터

**Spring Boot YAML 로딩:**
- `spring.config.import` 방식이 `@PropertySource` + 커스텀 Factory보다 권장 (공식 메커니즘, 보일러플레이트 없음)
- `@Validated` + `@Valid` (중첩 객체)로 기동 시 fast-fail 검증 가능
- 핫 리로드 불필요 (경제지표 메타데이터는 거의 변하지 않는 정적 데이터)

**단순화 고려사항:**
- 단순성 리뷰어는 Java static Map을 권장했으나, YAML의 장점(비개발자 편집, 가독성)과 프로젝트의 기존 `@ConfigurationProperties` 패턴 일관성을 고려하여 **YAML 유지**
- ~~별도 `description`과 `tooltip` 필드~~ → **단일 `description` 필드**로 통합 (카드에서는 부제, 테이블에서는 툴팁으로 동일 텍스트 사용)

**메타데이터 머지 위치:**
- 아키텍처 리뷰: **application 레이어** (`EcosIndicatorService`)에서 머지 (controller는 HTTP 처리만)
- 성능 리뷰: 101개 × HashMap lookup ≈ 2μs. 요청 시 머지해도 성능 영향 없음
- 캐시 리뷰: 배치 시 pre-merge 권장 (조회 서비스 단순화)
- **결정**: 배치 시 `previousDataValue`는 pre-merge, 메타데이터(description, positiveDirection)는 application 레이어에서 요청 시 머지 (메타데이터 변경 시 캐시 무효화 불필요)

## Technical Considerations

### 전기 비교 주기 처리

- 히스토리 테이블은 cycle 변경 시에만 새 행을 INSERT하므로, snapshotDate 순으로 정렬 시 "직전 레코드 = 전기"로 처리
- cycle 값 파싱 불필요 — snapshotDate 기준 정렬로 충분
- 주기별 라벨은 프론트엔드에서 cycle 표시 (일간: 전일 대비, 월간: 전월 대비 등)

### 캐시 구조 변경

- `KeyStatIndicator` record에 `previousDataValue` 필드 추가
- 배치 시 latest 테이블에서 현재 `dataValue`를 읽어 `previousDataValue`로 병합 후 캐시 적재
- 전체 지표를 캐시에 저장, 핵심 지표 필터링은 조회 시 수행 (캐시 키 분리 불필요)
- API fallback (캐시 miss) 시 `previousDataValue`는 unavailable → null 허용, 프론트 graceful 처리

### 프론트엔드 레이스컨디션 (Critical — 기존 코드에도 해당)

**현재 `loadEcosIndicators()`에 stale response 레이스컨디션 존재:**
- 빠른 탭 전환 시 느린 응답이 나중에 도착하여 현재 탭의 데이터를 덮어쓸 수 있음
- `finally` 블록에서 loading을 false로 설정하면, stale 응답이 실제 요청 중 로딩을 해제

**필수 수정 — generation counter 패턴:**
```javascript
// ecos state에 추가
ecos: { ..., _requestGeneration: 0 }

async loadEcosIndicators() {
    const thisGeneration = ++this.ecos._requestGeneration;
    this.ecos.loading = true;
    try {
        const result = await API.getEcosIndicators(this.ecos.selectedCategory) || [];
        if (thisGeneration !== this.ecos._requestGeneration) return; // stale → 폐기
        this.ecos.indicators = result;
    } catch (e) {
        if (thisGeneration !== this.ecos._requestGeneration) return;
        this.ecos.indicators = [];
    } finally {
        if (thisGeneration === this.ecos._requestGeneration) {
            this.ecos.loading = false;
        }
    }
}
```

**선택적 개선 — AbortController**: 실제 HTTP 요청도 취소하여 서버 부하 감소

### 툴팁 구현

- 순수 CSS 방식 (`group-hover`)으로 외부 라이브러리 없이 구현
- `:key` 지정 필수 (데이터 갱신 시 툴팁이 잘못된 행에 표시되는 문제 방지)
- 데이터 리프레시 중 tooltip은 자연스럽게 사라짐 (배열 교체 시 `_showTooltip` 초기화)

### 설정 파일 미정의 지표 처리

- 메타데이터에 정의되지 않은 지표: 카드에 표시 안 됨, 테이블에는 정상 표시 (해설 없음)
- positiveDirection 미정의 시 기본값: 중립 (회색 테두리)

### 히스토리 1건만 존재하는 경우

- 전기 대비 변화 표시 불가 → "-" 표시
- 카드에서 현재 값만 표시, 변화 화살표 미표시

### dataValue가 숫자가 아닌 경우

- 기존 `Format.number()`, `Format.changeRate()` 유틸리티가 이미 NaN 체크 처리
- ECOS API가 비숫자 값(N/A, p 등)을 반환할 가능성 있음 → VARCHAR 유지, 프론트엔드에서 graceful 처리

## Acceptance Criteria

### 백엔드

- [ ] `ecos_indicator_latest` 테이블에 `data_value`, `previous_data_value` 컬럼 추가 (hibernate ddl-auto)
- [ ] `EcosIndicatorLatest` 도메인 모델에 `dataValue`, `previousDataValue` 필드 추가
- [ ] `EcosIndicatorLatestEntity` 에 해당 컬럼 매핑 + `updateCycle()` → `update()` 확장 (이전값 보존)
- [ ] `EcosIndicatorSaveService.fetchAndSave()` — 배치 시 이전값 보존 로직 추가
  - cycle 변경 시: 현재 dataValue → previousDataValue, 새 dataValue 저장
  - cycle 미변경 시: dataValue만 갱신 (수정 발표 대응)
  - **히스토리 INSERT + latest UPSERT는 동일 트랜잭션 내에서 수행**
- [ ] `KeyStatIndicator` record에 `previousDataValue` 필드 추가 (nullable)
- [ ] `IndicatorResponse` DTO에 `previousDataValue`, `description`, `positiveDirection`, `keyIndicator` 필드 추가
- [ ] `EcosIndicatorService.getIndicatorsByCategory()` — 메타데이터 머지 로직 추가 (application 레이어)
- [ ] `ecos-indicator-metadata.yml` 설정 파일 생성 + `EcosIndicatorMetadataProperties` 바인딩 클래스
  - 위치: `economics/infrastructure/korea/ecos/config/`
  - `application.yml`에 `spring.config.import: classpath:ecos-indicator-metadata.yml` 추가
- [ ] 캐시 적재 시 `previousDataValue` pre-merge (배치 시점에 병합)

### 프론트엔드

- [ ] **레이스컨디션 수정**: `loadEcosIndicators()`에 generation counter 패턴 적용 (기존 버그 수정)
- [ ] 카드 대시보드 UI 구현 (선택 카테고리의 keyIndicator=true 지표)
  - 반응형 그리드: `grid-cols-1 md:grid-cols-2 xl:grid-cols-4`
  - `<ul>/<li>/<article>` 시맨틱 마크업 + `aria-label`
  - 로딩 시 스켈레톤 카드 (`animate-pulse`)
- [ ] 카드 색상 코딩: 화살표 방향(빨강/파랑) + 카드 왼쪽 테두리(`border-l-4`, 긍정=초록/부정=빨강/중립=회색)
- [ ] 카드 내 해설 텍스트 표시 (`description` 필드)
- [ ] 테이블 className 기준 정렬 (시각적 그룹핑)
- [ ] 테이블에 전기 대비 변화 컬럼 추가 (Format.change, Format.changeRate 활용)
- [ ] 테이블 행 호버 시 순수 CSS 툴팁 (`description` 필드)
- [ ] 이전값 없는 경우 graceful fallback ("-" 표시)

## Dependencies & Risks

### 의존성

- `ecos_indicator_latest` 테이블 스키마 변경 (hibernate ddl-auto:update로 자동 처리)
- 메타데이터 YAML 파일의 초기 데이터 작성 (15개 카테고리별 핵심 지표 선정)

### 리스크

- **초기 배포 후 첫 배치 전**: `data_value`와 `previous_data_value` 모두 NULL → 프론트 graceful fallback
- **첫 배치 실행 시**: `data_value` 채워지지만 `previous_data_value`는 첫 cycle 변경까지 NULL (정상 동작)
- **캐시 구조 변경**: JVM 재시작 시 캐시 초기화 → 배치 또는 warmup listener에서 재적재
- **API fallback (캐시 miss)**: previousDataValue 조회 불가 → null 반환, 프론트에서 "-" 표시

### Research Insights — 추가 리스크

**데이터 무결성:**
- NULL `previousDataValue`를 절대 0으로 암묵 변환하지 않도록 전 코드 경로에서 null 체크
- `data_value`가 VARCHAR — ECOS API가 비숫자 값 반환 가능성 있으므로 유지 (타입 변환은 불필요)

**프론트엔드:**
- 카테고리 전환 중 이전 데이터를 유지 (배열을 clear하지 않음) → 로딩 오버레이와 함께 stale data 표시가 빈 화면보다 나은 UX

---

## Appendix A: 배치 로직 상세 설계

### 현재 배치 플로우

```
fetchAndSave() {
  1. API 1회 조회 → EcosKeyStatResult (101개 지표)
  2. putCacheByCategory() → 15개 카테고리별 캐시 적재
  3. validIndicators() → 유효 카테고리 필터링
  4. existsAny() → 히스토리 존재 확인
  5a. 초기 시딩: 전체 히스토리 INSERT + Latest INSERT
  5b. 기존 로직: Latest 벌크 조회 → Java cycle 비교 → 변경분 히스토리 INSERT + 전체 Latest UPSERT
}
```

### 변경된 배치 플로우

**핵심 변경**: Latest UPSERT 시 `dataValue`와 `previousDataValue`를 함께 관리

```
fetchAndSave() {
  1. API 1회 조회 → EcosKeyStatResult
  2. Latest 벌크 조회 → Map<compareKey, LatestSnapshot> (cycle + dataValue)  ← 변경
  3. validIndicators() → 유효 카테고리 필터링

  For each indicator:
    if (cycle 변경됨):
      - previousDataValue = latest의 현재 dataValue
      - dataValue = API의 새 dataValue
      - 히스토리 INSERT
    else:
      - previousDataValue = latest의 현재 previousDataValue (유지)
      - dataValue = API의 dataValue (수정 발표 대응)

  4. Latest 전체 UPSERT (dataValue, previousDataValue, cycle, updatedAt)
  5. putCacheByCategory() → previousDataValue 포함하여 캐시 적재  ← 변경
}
```

### EcosIndicatorLatest 도메인 모델 변경

```
기존: (className, keystatName, cycle, updatedAt)
변경: (className, keystatName, dataValue, previousDataValue, cycle, updatedAt)
```

- `fromKeyStatIndicator()` 팩토리 메서드에 `dataValue` 파라미터 추가
- `updateCycle()` → `update(dataValue, previousDataValue, cycle, updatedAt)` 확장

### EcosIndicatorLatestEntity 변경

```
추가 컬럼:
  @Column(name = "data_value", length = 50)
  private String dataValue;

  @Column(name = "previous_data_value", length = 50)
  private String previousDataValue;
```

- `updateCycle()` → `update()` 메서드 확장: dataValue shift 로직 포함

### 캐시 적재 변경 (putCacheByCategory)

```
기존: API 응답의 KeyStatIndicator를 그대로 캐시에 저장
변경: Latest 테이블의 previousDataValue를 병합하여 캐시에 저장

1. Latest 벌크 조회 → Map<compareKey, previousDataValue>
2. API 지표 + previousDataValue 병합 → 새 KeyStatIndicator 생성
3. 카테고리별 그룹핑 → 캐시 적재
```

### 도메인 모델 결정: 접근법 A (KeyStatIndicator 직접 확장)

**최종 결정**: `KeyStatIndicator`에 `previousDataValue` 필드 직접 추가

근거:
- 101개 규모에서 별도 모델은 과잉 설계
- record에 nullable 필드 1개 추가는 기존 호출부에 최소 영향
- API에서 생성 시 `previousDataValue = null`, 캐시 적재 시 값 병합
- 향후 모델이 복잡해지면 (3개 이상 파생 필드) 별도 모델로 리팩토링

```
// 변경 후
public record KeyStatIndicator(
    String className,
    String keystatName,
    String dataValue,
    String previousDataValue,  // nullable, 캐시 적재 시 병합
    String cycle,
    String unitName
) { ... }
```

**API Adapter 변경**: `previousDataValue = null`로 생성 (API는 이전값 미제공)
**캐시 적재 시**: Latest 테이블에서 읽은 previousDataValue로 새 record 생성

---

## Appendix B: YAML 메타데이터 스키마

### 파일 위치

`src/main/resources/ecos-indicator-metadata.yml`

### 스키마 구조

```yaml
ecos:
  indicator:
    metadata:
      indicators:
        "시장금리::한국은행 기준금리":
          description: "한국은행이 금융기관과 환매조건부채권(RP) 매매 시 기준이 되는 금리"
          positive-direction: DOWN    # 금리 인하 = 긍정
          key-indicator: true
        "시장금리::CD(91일)":
          description: "91일 만기 양도성예금증서 유통수익률"
          positive-direction: DOWN
          key-indicator: true
        # ... 이하 지표별 정의
```

### 키 규칙

- 키 형식: `"{className}::{keystatName}"` (기존 `toCompareKey()` 패턴 활용)
- YAML에서 `::` 포함 키는 따옴표로 감싸야 함

### Properties 바인딩 클래스

```
위치: economics/infrastructure/korea/ecos/config/EcosIndicatorMetadataProperties.java

@ConfigurationProperties(prefix = "ecos.indicator.metadata")
- Map<String, IndicatorMeta> indicators
  - IndicatorMeta:
    - String description
    - PositiveDirection positiveDirection (enum: UP, DOWN, NEUTRAL)
    - boolean keyIndicator
```

### 카테고리별 핵심 지표 후보 (설계 단계에서 확정)

ECOS API의 `KeyStatisticList` 엔드포인트에서 반환하는 지표 중, 각 카테고리의 대표 지표:

| 카테고리 | className | 핵심 지표 후보 (keystatName) | positiveDirection |
|----------|-----------|---------------------------|-------------------|
| 금리 | 시장금리 | 한국은행 기준금리, CD(91일) | DOWN |
| 통화/금융 | 통화량 | M2 | 컨텍스트 의존 (NEUTRAL) |
| 환율 | 환율 | 원/달러 | DOWN (원화 강세 = 긍정) |
| 물가 | 소비자/생산자 물가 | 소비자물가지수 | DOWN |
| 성장/소득 | 성장률 | GDP 성장률 | UP |
| 고용/노동 | 고용 | 실업률 | DOWN |
| 경기심리 | 심리지표 | BSI(전산업), CSI | UP |
| 주식/채권 | 주식 | KOSPI | UP |

> **참고**: 실제 keystatName 값은 ECOS API 응답에서 확인 필요. 위 목록은 일반적인 한국은행 주요 경제지표 기준.

### application.yml 변경

```yaml
spring:
  config:
    import:
      - optional:classpath:ecos-indicator-metadata.yml
```

`optional:` 접두사 사용 → 파일 부재 시에도 애플리케이션 기동 가능 (개발 환경 편의)

---

## Appendix C: 카드 UI 상세 설계

### 카드 레이아웃 구조

```
┌──────────────────────────────────────────────────────┐
│ 카테고리 탭 바                                        │
├──────────────────────────────────────────────────────┤
│ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐     │
│ │ 카드 1   │ │ 카드 2   │ │ 카드 3   │ │ 카드 4   │     │
│ │ 기준금리  │ │ CD(91일) │ │ CP(91일) │ │          │     │
│ │ 3.50%   │ │ 3.45%   │ │ 3.52%   │ │          │     │
│ │ ▼ -0.25 │ │ ▼ -0.12 │ │ ▲ +0.05 │ │          │     │
│ │ 금리인하  │ │ 단기금리  │ │ 기업어음  │ │          │     │
│ └─────────┘ └─────────┘ └─────────┘ └─────────┘     │
├──────────────────────────────────────────────────────┤
│ 분류       │ 지표명      │ 값    │ 변화    │ 주기 │ 단위 │
│ ──────────┼────────────┼──────┼────────┼─────┼─────│
│ 시장금리   │ 기준금리     │ 3.50 │ ▼-0.25 │ 월  │ %   │
│ 시장금리   │ CD(91일)    │ 3.45 │ ▼-0.12 │ 일  │ %   │
│ ...       │ ...         │ ...  │ ...    │ ... │ ... │
│ 여수신금리 │ 예금금리     │ 3.20 │ ▼-0.10 │ 월  │ %   │
│ ...       │ ...         │ ...  │ ...    │ ... │ ... │
└──────────────────────────────────────────────────────┘
```

### 카드 컴포넌트 구조

```
<article> (border-l-4 + 조건부 색상)
  ├─ <div> 헤더: 지표명 + ⓘ 툴팁 아이콘
  ├─ <p> 현재 값 (text-2xl font-bold) + 단위
  ├─ <div> 변화 정보: 화살표 심볼 + 변화율 + "vs 전기"
  └─ <p> 해설 텍스트 (text-xs text-gray-400)
```

### 색상 코딩 규칙

| 조건 | 카드 왼쪽 테두리 | 화살표 색상 | 의미 |
|------|-----------------|------------|------|
| positiveDirection=DOWN, 값 하락 | `border-green-500` | `text-blue-500` ▼ | 긍정적 변화 |
| positiveDirection=DOWN, 값 상승 | `border-red-500` | `text-red-500` ▲ | 부정적 변화 |
| positiveDirection=UP, 값 상승 | `border-green-500` | `text-red-500` ▲ | 긍정적 변화 |
| positiveDirection=UP, 값 하락 | `border-red-500` | `text-blue-500` ▼ | 부정적 변화 |
| NEUTRAL 또는 변화 없음 | `border-gray-300` | `text-gray-500` - | 중립 |

**참고**: 화살표 색상은 기존 `Format.change()` 패턴 유지 (상승=빨강, 하락=파랑). 카드 테두리만 positiveDirection 반영.

### 로딩 상태

- 로딩 중: `animate-pulse` 스켈레톤 카드 (카테고리별 예상 카드 수만큼)
- 카테고리 전환 시: 이전 데이터 유지 + 로딩 오버레이 (빈 화면 방지)
- 이전값 없음: 카드에 현재 값만 표시, 변화 영역에 "-" 표시

### 프론트엔드 state 변경

```javascript
// app.js ecos state 확장
ecos: {
    categories: [],
    selectedCategory: null,
    indicators: [],
    loading: false,
    _requestGeneration: 0  // 레이스컨디션 방지
}

// ecos.js 메서드 추가
getKeyIndicators() {
    return this.ecos.indicators.filter(ind => ind.keyIndicator);
},

getCardBorderClass(ind) {
    // positiveDirection + 변화 방향 조합으로 테두리 색상 결정
},

getSortedIndicators() {
    return [...this.ecos.indicators].sort((a, b) => a.className.localeCompare(b.className));
}
```

---

## Sources & References

### Origin

- **Brainstorm document**: [docs/brainstorms/2026-03-16-ecos-indicator-dashboard-brainstorm.md](../brainstorms/2026-03-16-ecos-indicator-dashboard-brainstorm.md)
- Key decisions carried forward: 하이브리드 레이아웃, latest 테이블 확장, YAML 설정 파일, 주기별 자동 대응

### Internal References

- EcosIndicatorSaveService (배치 로직): `economics/application/EcosIndicatorSaveService.java`
- EcosIndicatorService (조회): `economics/application/EcosIndicatorService.java`
- EcosIndicatorLatestEntity: `economics/infrastructure/persistence/EcosIndicatorLatestEntity.java`
- EcosIndicatorController: `economics/presentation/EcosIndicatorController.java`
- IndicatorResponse DTO: `economics/presentation/dto/IndicatorResponse.java`
- KeyStatIndicator: `economics/domain/model/KeyStatIndicator.java`
- EcosIndicatorLatest: `economics/domain/model/EcosIndicatorLatest.java`
- Format.change / Format.changeRate: `static/js/utils/format.js`
- 프론트엔드 컴포넌트: `static/js/components/ecos.js`
- HTML 템플릿 ECOS 섹션: `static/index.html` (lines 322-385)
- 기존 설계 문서 참고: `.claude/designs/economics/ecos-indicator-persistence/`
- EcosProperties 패턴: `economics/infrastructure/korea/ecos/config/EcosProperties.java`

### External References (Research)

- [Spring Boot spring.config.import](https://docs.spring.io/spring-boot/reference/features/external-config.html) — YAML 커스텀 파일 로딩
- [Caffeine Cache Wiki](https://github.com/ben-manes/caffeine/wiki) — 캐시 구조 진화, 배치 적재 패턴
- [Alpine.js x-for](https://alpinejs.dev/directives/for) — :key 지정, DOM diffing
- [Tailwind CSS Grid](https://tailwindcss.com/docs/grid-template-columns) — 반응형 그리드 패턴
- [UC Berkeley Accessible Card Patterns](https://dap.berkeley.edu/web-a11y-basics/accessible-card-ui-component-patterns) — 접근성