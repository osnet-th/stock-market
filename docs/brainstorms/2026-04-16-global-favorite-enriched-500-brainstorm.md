# Brainstorm: 글로벌 경제지표 관심 등록 후 대시보드 조회 시 500 에러

- **작성일**: 2026-04-16
- **대상 모듈**: favorite, economics, infrastructure/web
- **에러 응답**: `500 INTERNAL_ERROR — "서버 내부 오류가 발생했습니다."`
- **재현 시나리오**: 글로벌 경제지표 ★ 클릭으로 관심 등록 → 홈 대시보드 진입 시 `GET /api/favorites/enriched` 가 500 반환 (국내 ECOS 관심은 정상)

---

## 1. 현상 요약

| 항목 | ECOS (국내) | GLOBAL (글로벌) |
|---|---|---|
| 관심 등록 (POST `/api/favorites`) | 200 OK | 200 OK |
| 대시보드 enriched 조회 | 200 OK + 정상 표시 | **500 INTERNAL_ERROR** |
| 식별 키 형태 | `className::keystatName` (둘 다 String) | `countryName::indicatorType.name()` (Enum 변환) |

비대칭 포인트는 **단 하나** — 글로벌 경로만 `Enum.name()` 호출이 들어간다.

---

## 2. 진단을 가로막고 있는 메타 문제

`src/main/java/.../infrastructure/web/GlobalExceptionHandler.java:99-102` 의 `handleGeneral()` 이
**stacktrace 를 어떤 logger 로도 남기지 않고** 오직 사용자에게 generic 메시지만 반환한다.

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");
}
```

→ 서버 콘솔에도, 파일 로그에도, 응답에도 **진짜 예외 정보가 남지 않음**. 이걸 먼저 손대지 않으면 모든 후속 디버깅이 가설에 머문다.

---

## 3. 근본 원인 후보 (가설)

### 가설 A — `GlobalIndicatorLatest.toCompareKey()` NPE ⭐ 최유력
- 위치: `economics/domain/model/GlobalIndicatorLatest.java:56-58`
  ```java
  public String toCompareKey() {
      return countryName + "::" + indicatorType.name();
  }
  ```
- `FavoriteIndicatorService.findEnrichedByUserId()` 내 `Collectors.toMap(GlobalIndicatorLatest::toCompareKey, ...)` 에서 한 row 라도 `indicatorType == null` 이면 NPE → 일반 Exception 핸들러 → 500.
- DB 컬럼은 `nullable = false` 이지만, **자동 DDL 환경에서 과거 시딩된 row 가 enum rename/제거 후 남아있을 가능성** 존재.
- ECOS 가 정상인 이유와 정확히 부합 (ECOS 는 enum 미사용).

### 가설 B — JPA Entity 하이드레이션 시 enum 매핑 실패
- `GlobalIndicatorLatestEntity.indicator_type` 컬럼에 **현재 `GlobalEconomicIndicatorType` 에 존재하지 않는 enum 이름** 이 한 row 라도 저장돼 있으면 `IllegalArgumentException` 이 발생.
- 단, 이는 `IllegalArgumentException` 핸들러(400)가 잡을 수 있어 500 이 아닐 가능성도 있음. **Hibernate 가 wrapping 하면 500 으로 빠지는 케이스가 더 흔함.**
- 36개 enum 상수 중 과거 이름이 변경되었는지 git history 확인 필요.

### 가설 C — `global_indicator_latest` 테이블/스키마 미동기
- 자동 DDL 환경에서 컬럼 길이/타입 변경이 누락돼 SELECT 자체가 실패.
- 단, `GlobalIndicatorSaveService.fetchAndSave()` 가 정상 동작 중이라면 가능성은 낮음.

### 가설 D (낮음) — 응답 직렬화 실패
- `EnrichedFavoriteResponse.GlobalItem` 직렬화 실패. record + 단순 타입이라 가능성 낮음.

---

## 4. 접근 방안

### 접근 1 — 진단 우선 (최소 패치 후 원인 확정 후 본수정) ⭐ 추천
1. `GlobalExceptionHandler.handleGeneral()` 에 **stacktrace 로깅 추가** (BigxLogger 또는 SLF4J).
2. 글로벌 관심 1건 등록 후 홈 진입 → 서버 로그에서 정확한 예외 클래스/메시지/라인 확인.
3. 확정된 원인에 따라 핀포인트 패치 (예: A 면 toCompareKey null-safe + filter, B 면 DB 데이터 정리 또는 enum 보정).

장점: 헛수정 0, 디버깅 인프라 영구 개선.
단점: 두 단계로 나뉨 (재현 1회 필요).

### 접근 2 — 방어적 일괄 패치 (원인 확정 없이)
- `toCompareKey()` 를 null-safe 화 + map 빌드 시 null 키 필터.
- DB 의 인기 row 들을 검증/정리하는 SQL 작성.
- enum 매핑 실패도 별도 try-catch 로 wrapping.

장점: 재현 없이 한 번에 마무리.
단점: **원인이 무엇이든 무관하게 코드를 손대므로 YAGNI 위반 가능성**, 진짜 원인이 가려진 채 봉합될 위험.

### 접근 3 — 데이터 인스펙션 우선
- DB 에 직접 접속해 `SELECT countryName, indicator_type FROM global_indicator_latest WHERE indicator_type IS NULL OR indicator_type NOT IN (…)` 실행.
- 이상 row 발견 시 정리 후 코드는 그대로.

장점: 코드 변경 최소.
단점: 재발 방지 안 됨, 로깅 부재 문제는 그대로 남음.

---

## 5. 추천 (사용자 선택사항 반영)

> 사용자 선택: **"정확한 근본 원인 파악 후 최소 패치"**

→ **접근 1** 채택. 단계는:

1. **(필수)** `GlobalExceptionHandler` 에 예외 로깅 추가 — 한 줄짜리 영구 개선이지만 향후 모든 500 디버깅 비용을 낮춤.
2. **(재현)** 글로벌 관심 등록 → 홈 진입 → 서버 로그 stacktrace 확인.
3. **(원인별 핀포인트 패치)**
   - 가설 A 확정 시: `toCompareKey()` 호출부에서 null indicatorType row 를 filter, 또는 데이터 cleanup.
   - 가설 B 확정 시: 어떤 enum 이름이 사라졌는지 식별 → DB row 정정 또는 enum 복원.
   - 가설 C 확정 시: 스키마 정합화.

---

## 6. 핵심 결정 사항

- 코드 수정에 앞서 **로깅 추가가 선행** — 가설 기반 추측 수정은 하지 않는다.
- 재현 1회를 거쳐 **단일 원인 확정 후** 단일 수정만 진행 (YAGNI).
- 분석/설계 문서는 CLAUDE.md 규칙대로 `.claude/analyzes/favorite/...` 및 `.claude/designs/favorite/...` 에 작성 후 승인 대기.
- `GlobalExceptionHandler` 의 로깅 부재는 **이번 건과 별개로도 영구 개선이 필요한 결함**으로 분리 인지.

---

## 7. 해결된 질문 (Resolved Questions)

- **Q1 (로깅 범위)** → **이번 건에 포함**. 진단을 위해 로깅 추가가 선행되어야 하므로 동일 PR 에서 처리.
- **Q3 (DDL 모드)** → **자동 DDL (update/create)**. ⇒ **가설 B(과거 enum 잔존)** 가능성이 한층 더 유력해짐 — Flyway 컬럼 정리 절차가 없어 enum 변경 이력이 그대로 남기 쉬움.
- **Q4 (유사 패턴 점검)** → **유사 패턴 탐색 후 일괄 대응**. enum.name() 기반 키 생성/조회 위치를 grep 으로 식별 후 동일 위험 평가.

## 미해결 (조건부)

- **Q2 (데이터 조치 방식)** → **원인 확정 후 재상의**. 단계 2(stacktrace 확보) 결과에 따라 SQL-only / 코드 방어 병행 / 마이그레이션 중 결정.

---

## 8. 작업 윤곽 (다음 단계로 넘기기 위한 정리)

1. **로깅 보강** — `GlobalExceptionHandler.handleGeneral()` 에 BigxLogger 로 stacktrace 출력.
2. **재현 + 원인 확정** — 글로벌 관심 1건 등록 → 홈 진입 → 서버 로그 stacktrace 확인.
3. **유사 패턴 점검** — `enum.name()` 기반 compareKey/조회 키 사용 위치 grep:
   - `economics/domain/model/GlobalIndicatorLatest.toCompareKey`
   - `economics/application/GlobalIndicatorSaveService` (라인 180 부근)
   - 기타 발견되는 모든 위치 목록화.
4. **핀포인트 패치** — Q2 재상의 결과에 따라 코드/데이터 수정.
5. **재현 검증** — 글로벌 관심 1건 + 홈 진입 → 200 OK 확인.

→ 이 윤곽을 바탕으로 `/ce:plan` 진행 시 `.claude/analyzes/favorite/global-favorite-enriched-500/` 및 `.claude/designs/favorite/global-favorite-enriched-500/` 작성 + 승인 대기.