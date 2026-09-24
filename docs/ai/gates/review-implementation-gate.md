# Review And Implementation Gate

이 문서는 아키텍처, 코드 리뷰, 구현 품질, 성능 상세 규칙이다. 최상위 계약은 [agent-harness.md](../agent-harness.md)이며, 충돌 시 더 보수적인 규칙을 따른다.

---

## Architecture Harness

- 전체 아키텍처 규칙은 [ARCHITECTURE.md](../../../ARCHITECTURE.md)를 따른다.
- 의존성 방향은 `presentation -> application -> domain <- infrastructure`를 지킨다.
- `infrastructure`는 Repository 구현과 Entity ↔ Domain Model 변환(`mapper/`)을 담당한다.
- `domain`은 Spring, JPA, 외부 시스템에 의존하지 않는다.
- `presentation`은 infrastructure 계층(Entity, Repository 구현체)에 직접 접근하지 않는다.
- `@Transactional`은 application 계층에서만 사용한다.
- JPA Entity는 ID 기반 참조만 허용하고 연관관계를 만들지 않는다.
- 다른 도메인은 식별자(ID/Code)만 참조하고, 조합은 application 계층에서 수행한다.
- API 응답에 Entity 또는 domain model을 직접 노출하지 않는다. DTO 변환 흐름은 `Request DTO -> Application DTO -> Domain Model -> Entity`를 따른다.

## Review Harness

- 태형님이 "리뷰", "리뷰해줘", "검토", "`ce:review`" 등 리뷰 요청을 하면 `/ce:review`를 수행한다.
- `/ce:review` 결과에 actionable issue가 있으면 목록을 태형님에게 제시하고, 수정할 이슈 선택을 먼저 받는다.
- 리뷰 finding은 표 형태로 보고하고, 각 actionable issue마다 코드 위치 근거를 포함한다.
- 리뷰 표의 필수 컬럼은 `No`, `Severity`, `Issue`, `관련 요구사항`, `위치`, `코드 맥락`, `영향`, `수정 방향`이다.
- `관련 요구사항`에는 root plan `## 요구사항 원장`의 `REQ-n` ID를 우선 적는다. 리뷰 finding과 구현 이해 확인 Gate의 요구사항 커버리지가 같은 축으로 묶여 추적된다.
- 원장이 없거나 해당 finding에 대응하는 REQ가 없으면 이슈 완료 조건, root plan 작업 항목, API 계약, 테스트 시나리오 중 연결되는 것을 자연어로 적는다.
- 기능 요구사항을 특정해야 판단 가능한 finding인데 요구사항 출처가 불명확하면 actionable issue 표에 섞지 않고 `요구사항 확인 필요`로 분리한다.
- 기능 요구사항이 아니라 공통 품질 기준 위반이면 REQ ID를 붙이지 않고 `관련 요구사항`에 `공통 아키텍처 기준`처럼 기준명을 적는다.
- `위치`에는 파일 경로와 line을 우선 적는다. line을 확정하기 어려우면 클래스/메서드명 또는 호출 부위를 적고, 관련 호출 흐름이 있으면 `A -> B -> C` 형식으로 1줄만 덧붙인다.
- 코드 위치를 특정할 수 없는 finding은 actionable issue 표에 섞지 않고 `위치 확인 필요`로 분리한다.
- 리뷰 보고 시 전체 코드를 길게 인용하지 않는다. 필요한 경우 문제가 발생한 메서드명, 호출 부위, 조건문/쿼리 fragment 이름처럼 판단 가능한 최소 코드 맥락만 제공한다.

리뷰 finding 보고 표준:

| No | Severity | Issue | 관련 요구사항 | 위치 | 코드 맥락 | 영향 | 수정 방향 |
|---|---|---|---|---|---|---|---|
| 1 | P1 | 문제 요약 | `REQ-3` (원장 없으면 이슈 완료 조건 또는 plan 작업 항목 1줄) | `path/to/File.java:123` 또는 `Class.method()` | 호출 흐름/문제 조건 1줄 | 사용자/권한/데이터 영향 | 수정 방향 1줄 |

- 태형님이 선택한 `/ce:review` issue만 root plan 범위 안에서 수정한다.
- 비선택 issue는 임의 수정하지 않고 보류로 기록한다.
- 선택된 `/ce:review` 이슈 수정이 root plan 범위를 벗어나면 수정하지 않고 root plan 업데이트, 검증, 재승인을 요청한다.
- 리뷰와 선택 이슈 수정까지 끝나면 구현 이해 확인 Gate(`explain` 단계) 진입 전에 [notion-guide.md](../notion-guide.md)에 따라 리뷰 결과를 해당 작업의 Review 페이지에 동기화한다.
- 코드 리뷰와 `ce:review`는 기능 동작보다 아키텍처 의존성 위반을 먼저 확인한다.
- 변경 파일의 `package`와 `import`를 먼저 확인하고, 필요하면 `rg`로 계층 간 참조를 스캔한다.
- `presentation -> application -> domain <- infrastructure` 의존 방향 위반은 P1로 보고한다.
- `presentation`이 `infrastructure`, Repository 구현체, Entity를 직접 참조하면 P1로 보고한다.
- `application`이 HTTP/Servlet 타입 또는 JPA Entity를 직접 참조하면 P1로 보고한다.
- `domain`이 Spring, JPA, 외부 API DTO를 참조하면 P1로 보고한다.
- Entity 또는 domain model을 API 응답으로 직접 노출하면 P1로 보고한다.
- `@Transactional`이 application 계층 외부에 선언되면 P1로 보고한다.
- `@Transactional` 메서드를 같은 클래스 내부에서 직접 호출해 Spring proxy/AOP가 적용되지 않는 self-invocation은 P1로 보고한다.
- 코드 컨벤션 위반은 리뷰마다 확인하고 기본 P3로 보고한다.
- 코드 컨벤션 위반이 버그, 회귀, 아키텍처 위반, 트랜잭션 누락으로 이어지면 P1/P2로 올린다.

## Implementation Harness

- 기존 패턴, 패키지 구조, 네이밍을 우선한다.
- 행위 중심 모델링을 사용하고, 도메인 규칙은 가능한 도메인 메서드로 표현한다(Anemic Model 지양).
- YAGNI: 현재 plan 작업 범위에 없는 메서드, 클래스, 인터페이스를 미리 만들지 않는다.
- getter/setter는 Lombok 애노테이션(`@Getter`, `@RequiredArgsConstructor` 등)을 사용하고 수동으로 작성하지 않는다.
- `record`와 `inner static class`는 DTO/Value Object 용도일 때만 내부 선언한다.
- 독립 역할의 `@Service`, `@Component`, 도메인 로직 클래스는 별도 파일로 분리한다.
- import는 사용처 코드를 먼저 작성한 뒤 추가한다.
- 상태 값, 매직 넘버, 중요한 문자열은 상수 또는 enum으로 정의한다.
- 테스트를 작성하지 않는 구현이라도 테스트 가능한 구조(의존성 주입, 단일 책임)로 구현한다.

## Performance Harness

- DB 쿼리는 페이징을 기본으로 고려한다.
- JPA 배치 조회, QueryDSL 활용, `hibernate.jdbc.batch_size=1000` 기준을 우선 검토한다.
- Caffeine 캐싱은 TTL과 무효화 흐름을 함께 고려한다.
- 외부 API와 대량 처리는 비동기화(Spring Scheduler, WebFlux)를 검토한다.
- 대용량 데이터는 로그에 그대로 남기지 않고 요약한다.
