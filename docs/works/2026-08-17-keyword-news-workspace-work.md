# 키워드 뉴스 워크스페이스 통합 구현 결과

gate: docs/gates/2026-08-17-keyword-news-workspace-gates.md
plan: docs/plans/2026-08-17-002-feat-keyword-news-workspace-plan.md
issue: https://github.com/osnet-th/stock-market/issues/115

## 구현

`키워드` · `뉴스 검색` 두 화면을 `키워드 뉴스` 단일 워크스페이스로 통합했다.
좌측 키워드 레일이 곧 필터이고 우측에 날짜별 뉴스 스트림이 붙는다.
검색어가 없으면 "저장된 전체 뉴스"(내 구독 키워드 전체)가 최신순으로 보인다 —
이전에는 검색어 없이 아무것도 볼 수 없었다.

- 통합 검색 · 제목만/제목+본문 · 기간 5종 · 지역 · 안 읽은 것만
- 레일: 총 건수 · 오늘 수집 · 마지막 성공 · 7일 스파크라인 · 케밥 메뉴 · 벌크(수집/중단/삭제)
- 기사: 언론사 · 읽음 표시 · ☆저장 · 모두 읽음
- 헤더: 수집 스케줄 상태(마지막/다음 실행) · 연속 실패 뱃지
- 메뉴 13개 → 12개, 기존 `#keywords` / `#news-search` 해시는 새 화면으로 리다이렉트

## 실행 순서 변경 (plan 이탈)

plan 은 Phase 1(화면) 선행이었으나 **백엔드(Phase 2~7) 완료 후 화면을 한 번에** 만드는 순서로 진행했다.
Phase 1 을 먼저 하면 Phase 2 가 바로 지울 임시 코드(단일 키워드 조회를 N회 호출해 합치는 등)를
쓰게 되기 때문이다. 결과물의 범위는 plan 과 같고 순서만 바뀌었다.

## 실행 중 확정한 것

- **키워드 수정 = 재구독**: `Keyword` 가 공유 불변 리소스라 UPDATE 하면 다른 구독자까지 바뀐다.
  `Keyword` 도메인에 변경 메서드를 추가하지 않고 불변을 유지했다 — 공유 리소스 보호가 안전장치다.
- **검색 2경로**: 검색어 없으면 DB 최신순, 있으면 ES 후 `originalUrl` 로 DB 재조회 보강.
  ES 문서 `_id` 가 `originalUrl` 이라 뉴스 id·언론사를 갖고 있지 않다.
- **`안 읽은 것만` 페이징**: 후처리 필터를 쓰지 않았다. DB 경로는 `NOT EXISTS` 로 SQL 안에서,
  ES 경로는 읽은 기사 URL 을 `must_not` 으로 쿼리에 넣어 `totalHits` 자체를 맞춘다.
  ES 제외 목록은 최근 2000건 상한을 두고 코드에 명시했다.
- **배치 이력의 `savedCount`**: 배치는 전 키워드를 한 번에 저장해 저장 결과를 키워드별로 쪼갤 수 없다.
  "새로 저장 요청한 건수"로 정의하고 javadoc 에 명시했다. 성공/실패 판정과 시각에는 영향이 없다.
- **cron → 한국어 라벨**: 실제 쓰는 두 형태(매시 정각 / 매일 HH:MM)만 번역하고
  나머지는 cron 문자열을 그대로 노출한다. 억지 해석으로 틀린 주기를 보여주지 않으려는 의도다.
- **포트폴리오 `newsEnabled`**: 수정 시에도 기존 역방향 동기화를 그대로 탄다(끄기).
  항목명으로 키워드를 만드는 구조라, 이름을 바꾸면 그 항목을 수집하던 키워드가 없어지므로
  플래그를 켜 둔 채로 두면 "뉴스 ON 인데 아무것도 안 모임"이 된다.

## 실행 중 발견한 것

- **`NewsSearchService` 가 모든 예외를 삼키고 있었다** (`catch (Exception ignored)`).
  "수집 실패"와 "새 기사 없음"이 호출부에서 구분되지 않아, 그대로 두면 Phase 6 의
  연속 실패 판정이 **항상 0 인 죽은 기능**이 된다. 반환형을 `NewsSearchOutcome` 으로 바꿔 해결했고
  포트 폴백(첫 비어 있지 않은 결과 사용) 동작은 그대로 유지했다.
- **레일 메타가 실데이터에서 2줄로 깨졌다**. 레일 296px 에서 스파크라인·건수·케밥을 빼면
  텍스트에 쓸 수 있는 폭이 117px(한글 12자 남짓)뿐이라
  `연속 실패 N회 · 마지막 성공 X` 가 넘쳐 행 높이가 58px → 72px 로 어긋났다.
  목업은 고정 데이터라 드러나지 않던 문제다. 한 줄로 끊고 전체 내용은 hover 툴팁에 담았다.
- **`KeywordNewsCount.lastCollectedAt` 이 죽은 필드가 됐다**. Phase 3 에서 `MAX(news.created_at)` 로
  근사했는데 Phase 6 의 수집 이력이 들어오며 대체됐다. 집계 쿼리와 함께 제거했다.
- **`_comment` 키를 인덱스 설정 JSON 에 넣을 수 없다**. ES 클라이언트가
  `Unknown field '_comment'` 로 거부한다. 설명은 커밋 메시지와 문서로 옮겼다.

## 변경 파일 묶음

- domain: `NewsSearchCriteria`/`Field`/`Sort`, `CollectionStatus`, `KeywordCollectionHistory`,
  `UserNewsState`, `News`·`NewsSearchResult` 에 `source` 추가, 리포지토리 포트 4종
- application: `NewsSearchApplicationService`(2경로), `CollectorScheduleService`,
  `UserNewsStateService`, `KeywordServiceImpl`(재구독·통계·벌크), `KeywordNewsBatchServiceImpl`(이력),
  `NewsSearchService`(Outcome), DTO 7종
- infrastructure: `NewsElasticsearchSearcher`(스코프·필드·정렬·제외), Entity 2종 신규 +
  `NewsEntity.source`, Mapper·RepositoryImpl·JpaRepository, `NewsSourceResolver`,
  수집 어댑터 3종, 이력 정리 스케줄러
- presentation: `NewsSearchController`(소유권 검증), `NewsController`(스케줄·읽음/저장),
  `KeywordController`(통계·수정·벌크), `ProdSecurityConfig`
- static: `partials/keyword-news.html` 신규, `js/components/keyword-news.js` 신규,
  `app.js`·`api.js`·`custom.css`·`home-side.html`, 기존 partial 2·컴포넌트 3 삭제
- SQL: `news_source_2026_08_17.sql`, `keyword_collection_history_2026_08_17.sql`,
  `user_news_state_2026_08_17.sql`

## work 단계 확인

- `./gradlew compileJava` · `compileTestJava` · `test`: 성공 (137건)
- 기존 테스트 3종은 시그니처 변경으로 컴파일이 깨져 호출부를 고쳤고,
  변경된 동작(2경로·상태 결합·읽은 기사 제외)만 보강했다. 신규 테스트 파일은 추가하지 않았다.
- 프론트: 목 하네스(실제 partial + 실제 컴포넌트 + 고정 응답)로 격리 확인
  - 레이아웃 — 1440px 2단(레일 296px sticky), 1000px 에서 레일이 스트림 아래로
    (order 2/1, maxHeight 620→300, static), 모바일 375px 가로 스크롤 0
  - 필터 — 키워드 스코프·검색어·제목만·기간 프리셋 날짜 변환(`7D`→`2026-09-18`)·안 읽은 것만 전송
  - CRUD — 케밥 열기/바깥 클릭 닫기·수정 모달·재구독 후 선택 id 동기화·단건/다건 삭제 확인
  - 읽음/저장 — 카드 열기 시 읽음·저장 토글·모두 읽음
  - 빈 상태 3종·필터 초기화, 행 높이 6건 58px 균일, 콘솔 오류 0건
- 목 하네스는 서버 연동 검증을 대체하지 않는다. 실 DB·ES 검증은 validation 단계에서 수행했다.
