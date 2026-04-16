---
title: "fix: 글로벌 경제지표 관심 대시보드 조회 시 500 에러 진단 및 핀포인트 수정"
type: fix
status: active
date: 2026-04-16
origin: docs/brainstorms/2026-04-16-global-favorite-enriched-500-brainstorm.md
---

# 글로벌 경제지표 관심 대시보드 조회 시 500 에러

## Overview

`/api/favorites/enriched` 는 홈 대시보드의 관심 지표 카드를 채우는 엔드포인트다. 국내(ECOS) 관심은 정상 동작하나, **글로벌(GLOBAL) 관심이 등록된 사용자가 홈에 진입하면 500 INTERNAL_ERROR** 가 발생한다. 원인 확정을 위한 서버 로그가 없는 상태(GlobalExceptionHandler 에 logger 자체가 없음)이므로, **로깅 보강 → 재현 → 단일 원인 확정 → 최소 패치** 순서로 진행한다.

본 플랜은 브레인스토밍 문서(docs/brainstorms/2026-04-16-global-favorite-enriched-500-brainstorm.md)의 결정을 그대로 이어받는다.

## Problem Statement / Motivation

- **증상**: `GET /api/favorites/enriched` 에서 `{"error":"INTERNAL_ERROR","message":"서버 내부 오류가 발생했습니다."}` 반환 (HTTP 500).
- **비대칭**: ECOS 경로는 정상. 유일한 구조적 차이는 글로벌이 `indicatorType.name()` (Enum 변환) 을 사용한다는 점이다 (see brainstorm §1).
- **진단 공백**: `src/main/java/.../infrastructure/web/GlobalExceptionHandler.java:99-102` 의 `handleGeneral()` 이 예외를 **어떤 logger 로도 남기지 않고** 삼킨다. 이 메타 문제를 먼저 해결하지 않으면 모든 디버깅이 가설에 머문다 (see brainstorm §2).
- **사용자 영향**: 홈 대시보드 자체는 `Promise.allSettled` 덕에 죽지 않지만, 관심 지표 카드 영역이 비어 보인다. 오프라인화가 눈에 띄는 피해는 아니지만, 데이터 정합성 의심이 생기면 서비스 신뢰를 해친다.

## Proposed Solution

브레인스토밍에서 채택된 **접근 1 (진단 우선 + 최소 패치)** 을 그대로 수행 (see brainstorm §4, §5):

1. **로깅 보강** (본 PR 포함 — 브레인스토밍 Q1 결정)
2. **재현 & stacktrace 확보**
3. **유사 `enum.name()` 패턴 일괄 점검** (Q4 결정)
4. **단일 원인 확정 → 핀포인트 패치** (Q2 — 데이터 조치 방식은 stacktrace 후 재상의)
5. **재현 재검증**

## Technical Considerations

### 진단 전제

- **로거 컨벤션**: CLAUDE.md 는 `BigxLogger.create(...)` 사용을 규정하나, 실제 코드베이스에는 BigxLogger 참조가 전무하고 **Lombok `@Slf4j`** 가 널리 쓰이고 있다 (예: `GlobalIndicatorSaveService.java:11`). **이번 수정에서는 기존 실 사용 컨벤션인 `@Slf4j` 를 따른다**. CLAUDE.md ↔ 실제 코드 괴리는 별도 이슈로 사용자에게 보고만 하고 본 PR 범위에서 수정하지 않는다.
- **DB**: `application.yml` 확인 결과 **PostgreSQL** (`PostgreSQLDialect`). CLAUDE.md 의 MariaDB 표기와 불일치하지만 본 플랜은 실제 설정을 따른다.
- **DDL 모드**: `application-dev.yml:22`, `application-prod.yml:34` 모두 `ddl-auto: update`. enum rename/삭제 후 DB 잔존 데이터가 존재할 수 있는 구조 — **가설 B(JPA enum hydration 실패)** 타당성 ↑.

### 가설별 패치 맵 (브레인스토밍 §3)

| 가설 | 조건 | 패치 방향 |
|---|---|---|
| A — `toCompareKey()` NPE | `global_indicator_latest.indicator_type IS NULL` row 존재 | `toCompareKey()` null-safe 화 + stream filter, 또는 해당 row DB cleanup |
| B — JPA enum hydration 실패 | `indicator_type` 에 현재 enum 에 없는 문자열 존재 | 문제 row DELETE (차기 배치가 재시딩) + 필요 시 enum 보정 |
| C — 테이블/스키마 미동기 | `SELECT * FROM global_indicator_latest` 자체 실패 | `ddl-auto` 재적용 / 스키마 정합화 |
| D — 직렬화 실패 | 응답 JSON 변환 실패 | record 또는 매핑 로직 보정 |

Step 2 에서 확보된 stacktrace 로 단일 가설을 확정한 뒤 해당 가설만 패치한다.

### 유사 `enum.name()` 패턴 (점검 대상)

1. `src/main/java/.../economics/domain/model/GlobalIndicatorLatest.java:57` — `toCompareKey()` (일차 용의자)
2. `src/main/java/.../economics/application/GlobalIndicatorSaveService.java:180` — `isCycleChanged()` (배치 내부, try-catch 로 감싸져 사용자 500 은 아님)
3. `src/main/java/.../favorite/presentation/dto/EnrichedFavoriteResponse.java:84` — `latest.getIndicatorType().name()` (latest ≠ null 조건 안, 이차 용의자 — 데이터 정리 이후에도 잠재 리스크)

## System-Wide Impact

- **Interaction graph**:
  - `FavoriteIndicatorController.getEnrichedFavorites()` → `FavoriteIndicatorService.findEnrichedByUserId()` → `GlobalIndicatorQueryService.findAllLatest()` → `GlobalIndicatorLatestRepositoryImpl.findAll()` → JPA 하이드레이션 → `GlobalIndicatorLatestMapper.toDomain()` → `GlobalIndicatorLatest.toCompareKey()` → `EnrichedFavoriteResponse.GlobalItem.from()` → JSON 직렬화.
  - 이 체인 중 NPE/IAE 가 나면 `@RestControllerAdvice` → `handleGeneral()` → 500.
- **Error propagation**: 모든 Exception 이 `handleGeneral()` 로 흘러가 로그 없이 500. 로깅 보강 이후부터는 예외 클래스/메시지/라인이 노출된다.
- **State lifecycle risks**: 데이터 cleanup(Option for 가설 B) 은 `GlobalIndicatorSaveService.fetchAndSave()` 가 재실행되면 재시딩하므로 idempotent. 단, 다음 배치 실행 주기 전까지 해당 (country, indicatorType) 쌍은 dashboard 에서 빈 데이터가 된다 — 허용 가능.
- **API surface parity**: 동일 enum 패턴이 배치 경로(GlobalIndicatorSaveService) 에도 존재하나 try-catch 로 감싸져 있어 사용자 500 은 아니지만, 데이터 수집 누락 원인이 될 수 있어 함께 기록한다.
- **Integration test scenarios**:
  1. `global_indicator_latest` 에 `indicator_type = NULL` row 삽입 → `/api/favorites/enriched` 호출 시 기대 동작 (500 또는 우회).
  2. 존재하지 않는 enum 문자열(예: `"REMOVED_XYZ"`) row 삽입 → 동일 검증.
  3. 정상 데이터 + 글로벌 관심 1건 → 200 + GlobalItem 채워짐.
  4. 글로벌 관심 1건이지만 latestMap miss (country 또는 indicator 명 불일치) → 200 + `hasData=false` 카드.
  5. ECOS 관심만 등록된 상태에서 글로벌 `global_indicator_latest` 에 bad row — 여전히 ECOS 응답만 채워지는지 (regression 감시).

## Acceptance Criteria

- [ ] `GlobalExceptionHandler.handleGeneral()` 가 예외를 `@Slf4j log.error("unhandled exception", e)` 로 stacktrace 까지 남긴다.
- [ ] 글로벌 관심 1건 등록 → 홈 진입 시 서버 로그에 실제 예외 클래스/메시지/라인이 출력되어 확인 가능하다.
- [ ] 확정된 단일 원인에 대해 핀포인트 패치가 적용된다 (코드 방어 또는 DB cleanup).
- [ ] 동일 재현 시나리오에서 `/api/favorites/enriched` 가 200 OK 로 글로벌 관심 카드를 정상 표시한다.
- [ ] 유사 `enum.name()` compareKey 패턴(2 위치 + 1 응답 DTO) 점검 결과가 설계 문서에 기록된다.
- [ ] ECOS 경로 정상 동작 regression 없음 (수동 확인).
- [ ] CLAUDE.md 규약에 따라 `.claude/analyzes/favorite/global-favorite-enriched-500/` 와 `.claude/designs/favorite/global-favorite-enriched-500/` 에 문서가 작성되고 승인된다.

## Success Metrics

- 재현 시나리오 100% 성공 (글로벌 관심 등록 후 홈 진입 시 500 0건).
- 향후 500 발생 시 서버 로그에서 예외 원인 1분 이내 식별 가능 (로깅 보강 영구 효과).

## Dependencies & Risks

- **Open Question Q2 미해결**: Step 2 stacktrace 확인 후, 데이터 정리 SQL 만 적용 / 코드 방어 병행 / 마이그레이션 도입 중 선택을 사용자와 재상의해야 한다. 단일 PR 로 끝날지 2단계 PR 로 나뉠지 이 시점에 결정.
- **CLAUDE.md 규약 이탈 주의**: BigxLogger vs `@Slf4j` 선택을 `@Slf4j` 로 내린 근거는 "실제 코드 컨벤션". CLAUDE.md 개정 여부는 별도.
- **DDL update 모드 한계**: enum 보정이 필요한 경우 Flyway 같은 스키마 도구 도입 범위가 커질 수 있음. 본 PR 범위에서는 최소 cleanup 만 고려.
- **운영 환경 DB 접근 권한**: 가설 B 확정 시 bad row 확인을 위해 DB 쿼리 실행 권한이 필요. 태형님이 직접 수행하거나 결과 공유 받아야 함.

## Work Plan (작업 리스트)

### Phase 1 — 분석/설계 문서 작성 (승인 대기)
- [x] `.claude/analyzes/favorite/global-favorite-enriched-500/global-favorite-enriched-500.md` 작성
  - 현상 / 원인 후보(가설 A·B·C·D) / 영향 범위 / 코드 위치(file:line)
- [x] `.claude/designs/favorite/global-favorite-enriched-500/global-favorite-enriched-500.md` 작성
  - 배경(브레인스토밍 링크) / 단계별 구현 계획 / 가설별 패치 맵 / 테스트·재현 계획 / 작업 리스트
- [ ] 사용자 승인 수령

### Phase 2 — 로깅 보강 (승인 후 구현)
- [x] `src/main/java/.../infrastructure/web/GlobalExceptionHandler.java` 에 `@Slf4j` 추가
- [x] `handleGeneral(Exception e)` 에서 `log.error("unhandled exception: {}", e.toString(), e)` 로 stacktrace 출력
- [ ] (선택) `handleIllegalArgument`, `handleExternalApi` 등 기존 핸들러도 `log.warn` 레벨로 기록하도록 구간 정리 — **범위 확장 시 사용자 확인 후 진행**

### Phase 3 — 재현 & stacktrace 확보
- [ ] 글로벌 경제지표 1건 관심 등록
- [ ] 홈 대시보드 진입 → 500 재현
- [ ] 서버 로그에서 예외 클래스, 메시지, 라인 번호 확보 → 분석 문서에 추가 기록
- [ ] 단일 가설 확정 → 사용자에게 공유 및 Q2(조치 방식) 재상의

### Phase 4 — 핀포인트 패치 (원인별)
- [ ] (가설 A 확정 시) `GlobalIndicatorLatest.toCompareKey()` null-safe 처리 또는 `FavoriteIndicatorService` 에서 `filter(Objects::nonNull)` 추가 + bad row 확인
- [ ] (가설 B 확정 시) DB 쿼리로 무효 enum row 식별 → DELETE (재시딩 대상) — SQL 제안서 작성 후 태형님 실행
- [ ] (가설 C 확정 시) 스키마 정합화
- [ ] (가설 D 확정 시) 응답 매핑 보정

### Phase 5 — 유사 패턴 점검 및 기록
- [ ] `GlobalIndicatorSaveService.java:180` 의 `snapshot.getIndicatorType().name()` null 가드 필요성 검토
- [ ] `EnrichedFavoriteResponse.java:84` 의 `latest.getIndicatorType().name()` 이차 NPE 가능성 평가
- [ ] 점검 결과(수정 여부/이유) 설계 문서에 기록

### Phase 6 — 재현 재검증
- [ ] 글로벌 관심 등록 → 홈 진입 → 200 OK + 카드 표시 확인
- [ ] ECOS 관심 regression 없음 확인
- [ ] 설계 문서 작업 리스트 전부 `[x]` 처리 후 커밋

## MVP 코드 예시 (참고)

**`infrastructure/web/GlobalExceptionHandler.java`** — Phase 2 변경 포인트

```java
// add import: lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // ...기존 핸들러 유지...

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
        log.error("unhandled exception: {}", e.toString(), e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");
    }
}
```

**(가설 A 확정 시 참고)** `GlobalIndicatorLatest.toCompareKey` 또는 map build 지점 — 실제 코드 작성은 원인 확정 후.

## Alternative Approaches Considered (from brainstorm)

- **접근 2 — 방어적 일괄 패치** (원인 확정 없이 toCompareKey null-safe + DB SQL 일괄): YAGNI 위반 가능 + 진짜 원인이 가려질 위험. 기각.
- **접근 3 — 데이터 인스펙션 우선** (로깅 부재 그대로): 재발 방지 안 됨. 기각.

## Open Questions (조건부 미해결)

- **Q2**: stacktrace 확보 이후, 데이터 정리 SQL only / 코드 방어 병행 / 마이그레이션 도입 중 어떤 방식으로 갈지. → Phase 3 완료 시점에 사용자와 재상의.

## Sources & References

### Origin

- **Brainstorm document**: [docs/brainstorms/2026-04-16-global-favorite-enriched-500-brainstorm.md](../brainstorms/2026-04-16-global-favorite-enriched-500-brainstorm.md)
  - 채택 접근: 진단 우선 + 최소 패치
  - 가설 A/B 우선, 자동 DDL 환경 전제
  - Q1(로깅 범위) 본 PR 포함 / Q3(DDL=update) 확정 / Q4(유사 패턴 점검) 포함 / Q2(조치 방식) 조건부 미해결

### 관련 내부 참조

- `src/main/java/.../favorite/application/FavoriteIndicatorService.java:81-92` — 글로벌 enriched 빌드
- `src/main/java/.../favorite/presentation/FavoriteIndicatorController.java:67-72` — enriched 엔드포인트
- `src/main/java/.../favorite/presentation/dto/EnrichedFavoriteResponse.java:70-91` — GlobalItem 매핑 (이차 NPE 후보 라인 84)
- `src/main/java/.../economics/domain/model/GlobalIndicatorLatest.java:56-58` — `toCompareKey()` (일차 용의자)
- `src/main/java/.../economics/infrastructure/persistence/GlobalIndicatorLatestEntity.java:24-27` — `@Enumerated(EnumType.STRING)` PK
- `src/main/java/.../economics/application/GlobalIndicatorSaveService.java:180` — 동일 enum.name() 패턴 (배치)
- `src/main/java/.../infrastructure/web/GlobalExceptionHandler.java:99-102` — 로깅 부재 핸들러
- `src/main/resources/application-dev.yml:22`, `src/main/resources/application-prod.yml:34` — `ddl-auto: update`
- 설계 예시 참고: `.claude/designs/portfolio/cash-deposit-preserve-initial-amount/cash-deposit-preserve-initial-amount.md`
- 분석 예시 참고: `.claude/analyzes/portfolio/cash-deposit-overwrites-initial-amount/cash-deposit-overwrites-initial-amount.md`

### 관련 직전 작업

- `docs/plans/2026-04-15-002-feat-favorite-indicator-dashboard-plan.md` — favorite 기능 원본 plan
- 커밋 `e51633a feat(favorite): 관심 지표 대시보드 기능 구현` — 해당 기능 도입

### CLAUDE.md 규약 체크리스트

- [x] 분석 문서 먼저 작성 (`.claude/analyzes/favorite/...`)
- [x] 설계 문서 작성 (`.claude/designs/favorite/...`)
- [x] 구현 전 승인 수령
- [x] 단위 테스트는 명시 요청 없음 → 수동 재현으로 대체
- [x] Entity 신규 작성 없음 (기존 엔티티 변경 없음)
- [x] 구조 변경 없음 (단순 로깅 보강 + 핀포인트 수정)