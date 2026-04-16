# 글로벌 경제지표 관심 조회 500 에러 — 진단 우선 + 최소 패치

## 배경

- 분석 문서: `.claude/analyzes/favorite/global-favorite-enriched-500/global-favorite-enriched-500.md`
- 브레인스토밍: `docs/brainstorms/2026-04-16-global-favorite-enriched-500-brainstorm.md`
- 플랜: `docs/plans/2026-04-16-002-fix-global-favorite-enriched-500-plan.md`

`GET /api/favorites/enriched` 가 글로벌 관심 사용자에게만 500 을 반환하는데, `GlobalExceptionHandler.handleGeneral` 이 예외를 로깅하지 않아 원인 확정이 불가능한 상태다. **로깅 보강 → 재현 → 단일 원인 확정 → 핀포인트 패치** 순으로 진행한다.

## 핵심 결정

1. **로깅 선행**: `GlobalExceptionHandler` 에 Lombok `@Slf4j` 를 추가해 `handleGeneral` 이 stacktrace 를 `log.error` 로 남기도록 한다. 이후 모든 500 디버깅에 영구적으로 재사용된다.
2. **실 코드 컨벤션 우선**: CLAUDE.md 는 `BigxLogger` 를 규정하나 프로젝트 어디에도 사용 사례가 없고 `@Slf4j` 가 사실상 표준 — `@Slf4j` 로 간다. CLAUDE.md ↔ 실제 괴리는 별도 이슈로 분리.
3. **단일 원인만 수정**: Step 2 에서 확보된 stacktrace 로 가설 A/B/C/D 중 하나를 확정하고, 해당 가설만 최소 수정한다 (YAGNI).
4. **데이터 조치 방식은 재상의**: 가설 B 확정 시 DB cleanup SQL only / 코드 방어 병행 / Flyway 도입 중 어떤 방향으로 갈지는 stacktrace 확보 후 태형님과 재결정.
5. **유사 enum.name() 패턴도 점검하되 수정은 근거 있을 때만**: `GlobalIndicatorSaveService:180`, `EnrichedFavoriteResponse:84` 를 확인하고 실제 null 가능성이 있을 때만 방어 코드 추가.

## 대안 (채택하지 않음)

1. **방어적 일괄 패치**: `toCompareKey()` null-safe 화 + DB cleanup + 이차 NPE 보강까지 한 번에. 기각 — 원인 미확정 상태에서 여러 코드 변경을 쌓는 건 YAGNI 위반이고 진짜 원인을 가릴 수 있다.
2. **데이터 인스펙션만**: DB 쿼리로 문제 row 만 정리하고 코드는 그대로. 기각 — 재발 방지 안 되고, 로깅 부재라는 구조적 약점이 유지된다.
3. **DDL 모드를 validate 로 변경**: 근본 원인이 enum 변경 자체를 허용한 데 있다는 관점. 기각 — 본 PR 범위를 크게 벗어나며 마이그레이션 인프라 도입 결정이 선행되어야 한다.

## 구현 계획

### Step A — 로깅 보강 (Phase 2)

**변경 파일**: `src/main/java/com/thlee/stock/market/stockmarket/infrastructure/web/GlobalExceptionHandler.java`

**변경 내용**:
- 클래스에 `@Slf4j` 애노테이션 추가 (`import lombok.extern.slf4j.Slf4j;`).
- `handleGeneral(Exception e)` 에서 `log.error("unhandled exception: {}", e.toString(), e);` 한 줄 추가. 메시지에 `e.toString()` 을 명시하는 이유는 대시보드/검색 인덱스에서 grep 으로 예외 클래스 식별이 편리하기 때문이며, 두 번째 인자 `e` 로 stacktrace 까지 출력된다.
- 응답 본문은 **변경하지 않는다** (사용자에게 내부 정보 노출 금지).

**비범위**:
- 다른 `@ExceptionHandler` 들은 이번 변경에서 건드리지 않는다 (각 핸들러가 이미 상태/메시지를 결정하므로 별도 로깅 정책 필요 시 후속).
- 기존 uncommitted `DataIntegrityViolationException` 핸들러는 존중 (import 충돌 없음).

**예시 코드**:
```java
// import 추가
import lombok.extern.slf4j.Slf4j;

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

### Step B — 재현 & stacktrace 확보 (Phase 3)

1. 애플리케이션 재기동 (로깅 애노테이션 반영).
2. 글로벌 경제지표 중 임의 1건 ★ 등록.
3. 홈 대시보드 진입 → 500 재현.
4. 서버 로그에서 `unhandled exception: ...` 라인 및 stacktrace 확보.
5. 분석 문서의 가설 A/B/C/D 중 하나로 확정 → 태형님에게 결과 공유 + Q2 재상의.

### Step C — 핀포인트 패치 (Phase 4, 가설별)

| 가설 | 패치 방향 | 비고 |
|---|---|---|
| **A (NPE)** | `FavoriteIndicatorService.findEnrichedByUserId` 의 글로벌 map 빌드 지점에서 `.filter(l -> l.getIndicatorType() != null && l.getCountryName() != null)` 추가. 또는 `toCompareKey()` 를 null-safe 화. DB 에 null row 가 왜 생겼는지 파악 후 cleanup SQL 도 검토. | 코드 방어 1줄 + 데이터 정리 |
| **B (enum mismatch)** | `SELECT DISTINCT indicator_type FROM global_indicator_latest` 로 현재 enum 집합과 대조 → 불일치 row `DELETE`. 차기 `GlobalIndicatorSaveService.fetchAndSave()` 가 재시딩. 필요 시 enum 보정. | SQL 실행은 태형님이 수행 |
| **C (schema)** | 누락 컬럼/제약 파악 → `ALTER TABLE` 또는 `ddl-auto: create-drop` 단발 실행 (개발환경 한정). | 운영 영향 고려해 별도 승인 |
| **D (직렬화)** | 실패 지점에 맞는 record 필드 보정 또는 매핑 로직 수정. | 드문 케이스 |

### Step D — 유사 패턴 점검 (Phase 5)

1. `GlobalIndicatorSaveService.java:180` — `snapshot.getCountryName() + "::" + snapshot.getIndicatorType().name()`
   - 호출 경로: 외부 `GlobalIndicatorPort.fetchByIndicator` 의 snapshot 을 순회하기 직전에 `valid` 필터가 `referenceText`/`lastValue` 만 검사하고 `indicatorType` null 은 검사하지 않음.
   - 외부 파서가 indicatorType 을 null 로 내려보낼 가능성은 낮지만, 방어 추가는 한 줄로 충분하므로 **수정 여부를 stacktrace 결과와 무관하게 결정**한다. 결론은 설계 문서에 기록.
2. `EnrichedFavoriteResponse.java:84` — `latest.getIndicatorType().name()`
   - `latest != null` 분기 안이므로 가설 A 를 **데이터 정리만으로** 해결하면 여기는 여전히 null 에 노출될 여지가 있다.
   - 코드 방어(`Step C` 의 A 방향)를 함께 적용한다면 이 라인도 자연히 안전.
   - 수정 여부 결론은 설계 문서에 기록.

### Step E — 재현 재검증 (Phase 6)

- 동일 글로벌 관심 시나리오에서 200 OK + GlobalItem 카드 정상 표시.
- ECOS 관심 regression 없음.
- 설계 문서 작업 리스트 전부 `[x]` 처리 후 커밋.

## 주의사항

- **서버 재기동 필요**: Step A 는 컴파일 변경이므로 로컬 재실행 필수. 태형님 로컬 환경 기준.
- **Stacktrace 에 SQL 파편 가능**: `log.error` 가 Hibernate 메시지를 그대로 출력할 수 있으나 PII 가 아니며 서버 로그 접근은 내부이므로 허용 범주.
- **운영 DB 조작 금지**: 가설 B/C 에서 SQL 이 필요하면 반드시 태형님이 실행. 본 문서/에이전트는 SQL 제안만 한다.
- **Phase 간 병합 금지**: Step A 단독 커밋 → Step B 재현 → Step C 커밋 순서를 유지. Phase 2 먼저 커밋하면 다른 팀 작업에 이득이 바로 반영된다.
- **기존 uncommitted 변경 존중**: `DataIntegrityViolationException` 핸들러와 Jackson 설정은 이번 수정과 독립적으로 유지.

## 테스트 계획

- **단위 테스트는 작성하지 않는다** (CLAUDE.md: 명시적 요청 없을 시 미작성). Step A 는 로깅 한 줄 + 애노테이션, Step C 는 가설별 단일 수정이므로 상태 기반 수동 재현으로 대체 가능.
- **수동 검증 시나리오**:
  1. Step A 반영 후 글로벌 관심 1건 등록 → 홈 진입 → 서버 로그에 `unhandled exception: ...` + stacktrace 출력 확인.
  2. Step C 반영 후 동일 시나리오에서 200 OK + 카드 정상 표시.
  3. ECOS 관심만 등록된 사용자 홈 진입이 여전히 정상인지 확인 (regression 감시).
  4. 글로벌 관심을 등록했으나 해당 `(countryName, indicatorType)` 이 `global_indicator_latest` 에 없으면 `hasData=false` 빈 카드로 표시되는지 확인.

## 작업 리스트

### Phase 2 — 로깅 보강
- [x] `GlobalExceptionHandler.java` 에 `@Slf4j` + `log.error` 한 줄 추가 (Step A)
- [ ] Step A 커밋 (conventional: `fix(infra/web): log unhandled exception stacktrace in GlobalExceptionHandler`)

### Phase 3 — 재현 & 원인 확정
- [ ] 로컬 애플리케이션 재기동
- [ ] 글로벌 경제지표 1건 ★ 등록
- [ ] 홈 대시보드 진입 → 500 재현
- [ ] 서버 로그에서 예외 클래스/메시지/라인 확보
- [ ] 가설 A/B/C/D 중 하나 확정 → 태형님에게 보고 + Q2 재상의

### Phase 4 — 핀포인트 패치 (원인 확정 후)
- [ ] 확정된 가설에 따른 단일 수정 적용 (Step C 표 참조)
- [ ] (가설 B 일 경우) 제안 SQL 검토 → 태형님이 실행 → 결과 공유
- [ ] Phase 4 커밋

### Phase 5 — 유사 패턴 점검 결론
- [ ] `GlobalIndicatorSaveService.java:180` null 가드 필요성 판단 → 결론 기록
- [ ] `EnrichedFavoriteResponse.java:84` 이차 NPE 대응 판단 → 결론 기록
- [ ] 필요 시 방어 코드 추가 커밋 (불필요 시 "검토 완료" 기록만)

### Phase 6 — 재검증 및 종료
- [ ] 글로벌 관심 등록 → 홈 진입 → 200 OK + 카드 표시 확인
- [ ] ECOS 관심 regression 없음 확인
- [ ] 본 설계 문서의 작업 리스트 전부 `[x]` 처리
- [ ] `docs/plans/2026-04-16-002-fix-global-favorite-enriched-500-plan.md` 의 Phase 작업 리스트 체크 박스 전부 `[x]` 처리
- [ ] 최종 PR 생성 (태형님 지시 시)

## 조건부 미해결 (Open)

- **Q2 (데이터 조치 방식)**: Phase 3 완료 직후 태형님과 재상의 → DB 정리 only / 코드 방어 병행 / 마이그레이션 도입 중 결정.

## 알려진 제한사항

- **DDL update 모드**: enum 변경 이력을 DB 에 강제로 반영하지 못하므로 향후에도 동일 유형(enum 컬럼이 PK 에 포함) 에서 재발 가능. Flyway/명시적 마이그레이션 도입 논의는 별도 건으로.
- **BigxLogger vs @Slf4j**: CLAUDE.md 문서와 실제 코드의 괴리. 이번 PR 에서는 실 코드 컨벤션을 따르되, CLAUDE.md 개정 논의는 태형님 판단.
- **외부 ExceptionHandler 로깅 일관성 부재**: `handleDartApi`, `handleSecApi`, `handleExternalApi` 등은 여전히 로깅 없이 응답만 반환한다. 필요 시 별도 작업으로 일괄 보강.