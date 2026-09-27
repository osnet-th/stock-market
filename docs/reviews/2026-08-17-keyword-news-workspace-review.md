# 키워드 뉴스 워크스페이스 통합 리뷰

gate: docs/gates/2026-08-17-keyword-news-workspace-gates.md
plan: docs/plans/2026-08-17-002-feat-keyword-news-workspace-plan.md
issue: https://github.com/osnet-th/stock-market/issues/115

검토 대상 커밋: `873a189` · `a11afc5` · `013149d` (병합 커밋 `93c14a3` 제외)

## 조치 결과 (2026-09-24)

태형님 지시로 **H1 · M1 · M2 · M3 조치 완료**. L1 · L2 는 보류.

| Finding | 조치 |
|---|---|
| H1 | 이관 조건에 "대상 키워드에 다른 구독자 없음" 추가. 단독 구독자인데 대상이 남의 키워드면 기사가 삭제되므로 수정 자체를 막고 안내 문구를 던진다 |
| M1 | `api.js` 가 오류 본문의 `message` 를 `error.userMessage` 로 꺼내고, 호출부 3곳이 이를 우선 사용 |
| M2 | `reassignKeywordId` 에 `@Modifying(clearAutomatically = true, flushAutomatically = true)` |
| M3 | ES 재색인을 `TransactionSynchronization.afterCommit` 으로 이동 |

검증: `compileJava` · `compileTestJava` 통과, 테스트 137건 전건 PASS.
하네스 실측으로 400 응답 시 사용자에게 JSON 원문이 아닌 서버 문구가 뜨는 것과
정상 수정·등록·삭제 경로 무회귀를 확인했다.

**H1 조치의 트레이드오프**: 막는 쪽을 택했다. 옮기면 남의 목록이 오염되고,
안 옮기면 단독 구독자 해제 시 기사가 삭제된다. 어느 쪽도 사용자가 원한 결과가 아니라
"등록 후 삭제"라는 명시적 경로로 안내한다. 실사용상 남과 키워드가 겹칠 때만 발생한다.

## Findings

### H1. 키워드 수정이 남의 키워드로 기사를 옮길 수 있다

`KeywordServiceImpl.updateKeyword()` 253~266행.

재구독 방식에서 이관 조건을 **기존 키워드의 단독 구독자 여부**로만 판정한다.
그런데 새 `(이름, 지역)` 이 **다른 사용자가 이미 구독 중인 키워드**일 수 있다.
2번 중복 검증은 "내가" 구독 중인 경우만 막는다.

재현 시나리오
1. 사용자 A 가 `삼성전자`(id=2) 단독 구독, 기사 412건 보유
2. 사용자 B 가 `삼성`(id=9) 구독 중
3. A 가 `삼성전자` → `삼성` 으로 수정
4. 2번 검증 통과(A 는 id=9 미구독) → `soleSubscriber=true` → **412건이 id=9 로 이관**
5. **B 의 `삼성` 키워드에 A 가 모으던 기사 412건이 갑자기 나타난다**

기사 자체는 공개 뉴스라 유출은 아니지만, 다른 사용자의 키워드 내용과 건수가
본인 조작 없이 바뀐다. 의도한 동작이 아니다.

조치안: 이관 조건에 **대상 키워드에 다른 구독자가 없을 것**을 추가한다.
대상이 이미 남의 키워드면 기사를 옮기지 않고 구독만 이동한다(모달 문구도 그에 맞게 분기).

### M1. 서버 오류 메시지가 사용자에게 JSON 원문으로 노출된다

`keyword-news.js` `saveKwnEdit()` 의 `alert(e.message)`.

`api.js:47` 이 오류를 `new Error('API Error 400: ' + errorText)` 로 던지고,
`errorText` 는 `GlobalExceptionHandler` 의 JSON 본문이다.
따라서 "이미 등록된 키워드입니다." 를 의도한 자리에 아래가 그대로 뜬다.

```
API Error 400: {"code":"BAD_REQUEST","message":"이미 등록된 키워드입니다.",...}
```

조치안: `api.js` 가 오류 본문의 `message` 를 파싱해 `Error.message` 에 담거나,
호출부에서 JSON 을 꺼내 쓴다. 전자가 다른 화면에도 이득이라 낫다.

### M2. `@Modifying` 벌크 UPDATE 후 영속성 컨텍스트를 비우지 않는다

`NewsJpaRepository.reassignKeywordId()` 에 `clearAutomatically` 가 없다.
바로 뒤 `findAllByKeywordId()` 로 재색인 대상을 읽는데, 같은 트랜잭션에서 이미 적재된
`NewsEntity` 가 있으면 1차 캐시의 **옛 keywordId 인스턴스**가 반환된다.

현재 `updateKeyword` 흐름에서는 이관 전에 뉴스를 적재하는 경로가 없어 실제로는 드러나지 않는다.
다만 앞 단계에 뉴스 조회가 하나만 추가돼도 조용히 깨지는 구조다.

조치안: `@Modifying(clearAutomatically = true, flushAutomatically = true)`.

### M3. ES 재색인이 트랜잭션 안에 있어 롤백 시 어긋난다

`updateKeyword` 263행의 `newsIndexPort.indexAll(...)` 이 `@Transactional` 안에서 실행된다.
이후 5번 `unsubscribeKeyword` 가 실패하면 DB 는 롤백되지만 **ES 는 이미 새 keywordId 로 갱신**돼
색인과 DB 가 어긋난다.

`NewsSaveService.saveBatch()` 는 같은 문제를 피하려고 ES 인덱싱을 트랜잭션 밖으로 빼두었다
(해당 파일 주석: "ES 인덱싱은 트랜잭션 밖에서 수행"). 같은 원칙을 따르지 않았다.

조치안: 커밋 후 인덱싱(`TransactionSynchronization` 또는 호출부 분리).
영향 범위가 작아 L 로 둘 수도 있으나, 기존 코드가 명시적으로 세운 원칙을 어긴 점에서 M 로 둔다.

### L1. 벌크 처리 중 한 건이 실패하면 전체가 400 으로 끝난다

`deactivateUserKeywords()` · `unsubscribeKeywords()` 가 사전 검증 없이 순회하며,
내 구독이 아닌 id 가 섞이면 `IllegalArgumentException` → 400.
`@Transactional` 이라 부분 적용은 없고 롤백되므로 데이터 정합성 문제는 아니다.
다만 어떤 id 가 문제인지 알려주지 않아 원인 파악이 어렵다.

### L2. `todayTotal` 이 레일 필터와 무관하게 전체 기준이다

헤더의 `오늘 수집 N건` 은 `KeywordStatsResponse.todayTotal`(내 전체 키워드 합계)이다.
레일에서 `비활성`만 보거나 지역을 좁혀도 이 값은 그대로다.
목업도 전체 기준이라 의도와 일치하지만, 화면상 "필터가 안 먹는 숫자"로 읽힐 수 있다.

## 검토 범위와 근거

- `NewsSearchCriteria` / `NewsSearchApplicationService`: 검색어 유무로 DB·ES 2경로가 갈리고,
  ES 경로만 `enrichFromDatabase` 를 탄다. ES 순서를 유지한 채 URL 로 행을 교체하며,
  DB 에 없는 URL 은 결과에서 사라지지 않고 ES 값을 유지한다(테스트 `search_whenStoredRowMissing_shouldKeepEsHit`).
- 소유권: `NewsSearchController.resolveKeywordScope()` 가 요청 `keywordIds` ⊆ 내 구독을 강제하고,
  아니면 403. 요청이 비면 내 전체 구독으로 채운다. 키워드 0개면 조회 없이 빈 결과다.
  `/api/news/search` 를 `authenticated()` 로 바꾼 것은 이 스코프 전환의 결과다.
- `안 읽은 것만` 페이징: DB 경로는 `NOT EXISTS` 로 SQL 안에서, ES 경로는 읽은 URL 을
  `must_not` 으로 쿼리에 넣어 `totalHits` 자체를 맞춘다. 후처리 필터를 쓰지 않아
  페이지 건수와 전체 건수가 어긋나지 않는다. ES 제외 목록 상한 2000건은 코드에 명시돼 있다.
- 수집 이력: `NewsSearchService` 가 `NewsSearchOutcome` 으로 성공/실패를 구분하게 바뀌어
  연속 실패 판정이 성립한다. 포트 폴백(첫 비어 있지 않은 결과 사용)은 그대로다.
  이력 저장 실패는 `recordHistory` 에서 흡수해 수집을 막지 않는다.
- 통계: 집계 쿼리 2회 + 이력 요약 1회로 전 키워드를 처리해 N+1 이 없다.
  수집이 없던 날은 0 으로 메워 스파크라인이 항상 7칸이다.
- `Keyword` 도메인은 불변을 유지했고 이름·지역 변경 메서드를 추가하지 않았다(공유 리소스 보호).
- 프론트: 목 하네스 실측으로 레이아웃·필터·CRUD·읽음/저장·빈 상태·반응형을 확인했다
  (work 문서 참조). 행 높이 6건 58px 균일, 콘솔 오류 0건.

## Code Convention

- Entity 연관관계 없음(ID 참조만), 수동 getter/setter 없음(Lombok `@Getter` 사용).
- 의존성 방향 유지: `presentation → application → domain ← infrastructure`.
  `NewsSearchCriteria` · `NewsSearchField` · `NewsSearchSort` 를 domain 에 두어
  포트 시그니처가 infrastructure 타입에 의존하지 않는다.
- 신규 Entity 2종은 plan 의 승인 범위 안이며, 마이그레이션 SQL 은 기존 관례
  ("권위는 Entity 어노테이션, 본 파일은 운영 DBA 수동 적용/롤백 백업용")를 따랐다.
- 테스트는 기존 파일이 컴파일되도록 고치고 변경된 동작(2경로·상태 결합·읽은 기사 제외)만 보강했다.
  상태 기반 검증이며 호출 여부 검증은 쓰지 않았다.

## Open Questions / Assumptions

- **실 DB 미검증**: 아래는 컨텍스트 로딩(쿼리 파싱)까지만 확인했고 실제 실행은 validation 몫이다.
  - `findLatestByScope` 의 `:unreadOnly = false OR NOT EXISTS (...)` 파라미터 비교
  - 연속 실패 집계 네이티브 CTE(`FILTER` · 상관 서브쿼리) — Postgres 전용 문법
  - `CAST(n.createdAt AS LocalDate)` 일자 그룹핑
  - 신규 테이블 2종·`news.source` 컬럼의 `ddl-auto: update` 생성
- ES `_id` 에 대한 `terms` 제외 쿼리는 정적 검토만 했고 실 ES 응답으로 확인하지 않았다.
- 배치 이력의 `savedCount` 는 "새로 저장 요청한 건수"이며 `ON CONFLICT` 이후 실제 삽입 수와
  다를 수 있다. 성공/실패 판정과 시각에는 영향이 없고 javadoc 에 명시했다.
- 포트폴리오 `newsEnabled` 는 수정 시에도 기존 역방향 동기화를 그대로 탄다(끄기).
  기존 모델의 의도된 동작으로 판단했으나 사용자 체감상 놀라울 수 있어 validation 에서 확인이 필요하다.
- 목 하네스는 실제 UI 코드 + 고정 응답이라 서버 연동 검증을 대체하지 않는다.
