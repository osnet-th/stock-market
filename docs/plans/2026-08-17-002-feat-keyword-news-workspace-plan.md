---
issue: 115
issue_url: https://github.com/osnet-th/stock-market/issues/115
branch: feat/issue-115-keyword-news-workspace
gate: docs/gates/2026-08-17-keyword-news-workspace-gates.md
brainstorm: docs/brainstorms/2026-08-17-keyword-news-workspace-brainstorm.md
issue_doc: docs/issues/2026-08-17-keyword-news-workspace-issue.md
---

# 키워드 뉴스 워크스페이스 통합 (#115)

## Overview

`키워드` · `뉴스 검색` 두 화면을 목업(`키워드 뉴스 워크스페이스 (단일파일).html` v2)의
**단일 워크스페이스**로 통합한다.

7개 Phase로 나눈다. Phase 1 은 프론트 전용이고, Phase 2~7 은 API·Entity 변경을 포함해
**Phase 착수 전 개별 승인**을 받는다.

| Phase | 내용 | 성격 | 승인 |
|---|---|---|---|
| 1 | 통합 화면 셸 — 2단 레이아웃 · 검색창 · 필터 바 · 레일 · 날짜별 스트림 | 프론트 | 불필요 |
| 2 | 검색 확장 — 빈 검색어 · 키워드 스코프 · 제목만 · 정렬 | **API 변경** | 필요 |
| 3 | 키워드 통계 API — 건수 · 오늘 · 마지막 성공 · 스파크라인 | **신규 API** | 필요 |
| 4 | 키워드 CRUD — 케밥 메뉴 · 수정(재구독) · 삭제 · 벌크 | **신규 API + 로직 변경** | 필요 |
| 5 | 언론사 `source` | **Entity 수정** | 필요 |
| 6 | 수집 이력 + 스케줄 상태 | **Entity 신규** | 필요 |
| 7 | 읽음/저장 + 안 읽은 것만 | **Entity 신규** | 필요 |

### 공통 규칙

- 색·타이포는 `css/custom.css` 의 `--dc-*` 를 쓴다. **목업과 동일한 팔레트가 #114 에서 이미 도입돼 있어
  신규 토큰을 만들지 않는다** (`--dc-bg #eef0f3` · `--dc-blue #1f4f9e` · `--dc-red #c02a22` ·
  `--dc-muted #8b9299` · IBM Plex Sans KR / Mono)
- 목업에 없는 토큰만 추가한다: 레일 선택 배경 `#eff3fa`(= `--dc-nav-active`), 지역 뱃지 해외 `#f4f1eb`/`#8a6a34`,
  케밥 hover `#e9edf3`, 삭제 테두리 `#f0c9c6`
- 스파크라인은 키워드 수만큼 렌더되므로 **인라인 SVG**로 그린다(#114 와 동일한 판단)
- 반응형 기준선은 목업을 따른다 — `vw < 1120` 이면 레일이 스트림 **아래**로 내려가고 레일 목록 `max-height:300px`
- Phase 진행 중에도 **기존 CRUD 접근 경로를 잃지 않는다**. Phase 1 에서 케밥 메뉴 자리를 만들고
  기존 activate/deactivate/delete 를 연결해 두고, Phase 4 에서 목업 사양으로 교체한다
- 목업의 오타 `함게` 는 `함께` 로 고쳐 구현한다

### 범위 제외

- **유사 기사 클러스터링** (태형님 "빼고 진행해") — 동반해서 아래 2곳을 렌더하지 않는다
  1. 기사 카드의 `유사 기사 N건` 버튼 + 접힘 영역
  2. 등록 모달의 안내 문구 박스("비슷한 제목의 기사는 자동으로 묶여서…") — 기능 없이 두면 사실과 다른 안내
- **일괄 활성화(재개)** — 목업 로직에 `bulkOn` 이 있으나 버튼이 없다

---

## Phase 1 — 통합 화면 셸

기존 API 범위에서 목업 레이아웃을 완성한다. 이 Phase 종료 시점에 화면은 목업과 같은 모양이지만
통계·언론사·읽음 같은 미구현 데이터는 표시하지 않는다(자리만 비워둔다).

### 변경 파일

| 파일 | 변경 |
|---|---|
| `partials/keyword-news.html` | **신규** — 목업 2단 구조 전체(헤더 · 필터 바 · 레일 · 스트림 · 등록/수정/삭제 모달) |
| `partials/keywords.html` | **삭제** |
| `partials/news-search.html` | **삭제** |
| `js/components/keyword-news.js` | **신규** — `keyword.js`+`news.js`+`news-search.js` 통합 |
| `js/components/keyword.js` | **삭제** |
| `js/components/news.js` | **삭제** |
| `js/components/news-search.js` | **삭제** |
| `js/app.js` | `menus` 13→12 · `validPages` · `partialNames` · `navigateTo` 분기 · 해시 리다이렉트 |
| `static/index.html` | 마운트 지점 2개 → 1개, script 태그 3개 → 1개 |
| `partials/home-side.html` | `navigateTo('keywords')` 2곳 → `'keyword-news'` |

### 화면 규칙

- 헤더: `키워드 뉴스` + `등록 N개 · 활성 N개 · 오늘 수집 N건` + `지금 수집` + `+ 키워드 등록`
  - 스케줄 상태 박스와 연속 실패 뱃지는 **Phase 6 까지 미노출**(데이터 없음)
  - `오늘 수집 N건` 은 `GET /api/news/feed` 의 `todayCount` 로 채운다(#114 에서 이미 있음)
- 필터 바: 검색창 · `제목+본문|제목만` · `1D 7D 1M ALL 직접지정` · `전체|국내|해외` · `안 읽은 것만`
  - Phase 1 에서는 **검색어가 있을 때만** 결과가 나온다(빈 검색어 허용은 Phase 2)
  - `제목만` · `안 읽은 것만` 은 자리만 만들고 `disabled`(Phase 2 · 7 에서 활성)
- 레일: 키워드 목록 · `⌕ 키워드 찾기` · `전체|활성|비활성` · 정렬 토글
  - 건수 · 오늘 증가분 · 마지막 성공 · 스파크라인은 **Phase 3 까지 미노출**
  - `⋯` 케밥 메뉴는 만들고 `지금 수집` · `수집 중단↔재개` · `삭제`를 기존 API로 연결.
    `이름 · 수집 범위 수정`은 Phase 4 까지 `disabled`
- 스트림: 날짜별 그룹 헤더(`오늘 · 2026.08.17 (월) N건`) + 기사 카드
  - 언론사는 **Phase 5 까지 미노출**, 읽음 점·☆저장은 **Phase 7 까지 미노출**
- 빈 상태 3종을 정의한다(목업 미정의 — 여기서 확정)
  - 키워드 0개: 레일에 `키워드를 등록하면 여기에 쌓입니다` + 등록 버튼
  - 필터 결과 0개: 목업 `조건에 맞는 키워드가 없습니다` 그대로
  - 기사 0건: 목업 빈 상태 카드 그대로(`필터 초기화` + `이 검색어를 키워드로 등록`)

### 라우팅 규칙

- 새 페이지 키: `keyword-news`
- `validPages` 에서 `keywords` · `news-search` 제거 후 `keyword-news` 추가
- **기존 해시 유입 처리**: `location.hash` 가 `#keywords` 또는 `#news-search` 면 `keyword-news` 로 치환.
  `currentPage` 초기화 IIFE 와 `popstate` 양쪽에 적용해야 한다(북마크·뒤로가기 모두 커버)
- `navigateTo('keyword-news')` 진입 시 `loadKeywords()` + 초기 스트림 로드

### Implementation Steps

- [ ] `keyword-news.html` 신규 — 목업 마크업을 `--dc-*` 토큰으로 옮긴다
- [ ] `keyword-news.js` 신규 — 3개 컴포넌트의 상태를 하나로 합친다
      (`sel[]` · `query` · `kwFilter` · `kwState` · `region` · `field` · `period` · `sort` · `kwSort` · `from`/`to` · `menuId` · 모달 상태)
- [ ] `app.js` — `menus`·`validPages`·`partialNames`·`navigateTo` 갱신 + 해시 리다이렉트
- [ ] `index.html` — 마운트·script 정리
- [ ] `home-side.html` — `navigateTo` 대상 교체
- [ ] 기존 partial·컴포넌트 6개 삭제
- [ ] 빈 상태 3종 구현
- [ ] 검증: 목 하네스로 레이아웃·2단/1단 전환·필터·해시 리다이렉트·CRUD 회귀

### 검증 항목

- 목업과 나란히 놓고 레이아웃 대조(레일 296px · 스트림 flex:3 · 1660px 최대폭)
- `#keywords` · `#news-search` 로 진입 시 새 페이지로 전환되는지
- 기존 CRUD(등록·활성화·비활성화·삭제·수집)가 전부 동작하는지 — **회귀 없어야 한다**
- 콘솔 오류 0건, 타 화면 회귀 없음

---

## Phase 2 — 검색 확장 *(승인 필요: public API 변경)*

### 변경 파일

| 파일 | 변경 |
|---|---|
| `presentation/dto/NewsSearchRequest.java` | `query` `@NotBlank` 제거 · `keywordIds` · `field` · `sort` 추가 |
| `presentation/NewsSearchController.java` | 파라미터 전달 + **키워드 소유권 검증** |
| `application/NewsSearchApplicationService.java` | 2경로 분기(검색어 유무) |
| `domain/service/NewsFullTextSearchPort.java` | 시그니처 확장 |
| `infrastructure/elasticsearch/NewsElasticsearchSearcher.java` | 조건부 `must` · `keywordId` terms 필터 · 필드 선택 · 정렬 |
| `domain/repository/NewsRepository.java` | 키워드·기간·지역 조건 최신순 조회 추가 |
| `infrastructure/persistence/NewsRepositoryImpl.java` + `NewsJpaRepository.java` | 위 구현 |
| `js/api.js` · `js/components/keyword-news.js` | 파라미터 연결 |

### API 규칙

`GET /api/news/search`

| 파라미터 | 변경 | 비고 |
|---|---|---|
| `query` | **`@NotBlank` → 선택** | 없으면 "저장된 전체 뉴스" |
| `keywordIds` | **신규** (`List<Long>`) | 다중. 내 구독 키워드인지 검증 |
| `field` | **신규** (`TITLE` \| `TITLE_CONTENT`) | 기본 `TITLE_CONTENT` |
| `sort` | **신규** (`LATEST` \| `RELEVANCE`) | 검색어 없으면 `LATEST` 강제 |
| `startDate`/`endDate`/`region`/`page`/`size` | 유지 | 기간 프리셋은 프론트에서 날짜로 변환 |

**2경로 분기** (brainstorm 사실 2)
- `query` 없음 → **DB 최신순** 조회. ES 를 거치지 않아 정렬이 안정적이다
- `query` 있음 → ES 전문 검색. `field` 에 따라 `multiMatch` 필드를 `title^2,content` 또는 `title` 로

**소유권 검증** — `keywordIds` 를 클라이언트가 임의로 넣을 수 있다. #114 리뷰 H1 과 같은 문제이므로
`UserKeywordRepository` 로 내 구독 키워드인지 확인하고, 아니면 403. `NewsSecurityContext` 를 재사용한다.

### Implementation Steps

- [ ] `NewsSearchRequest` 확장 + enum 2종 신설
- [ ] `NewsFullTextSearchPort` 시그니처 확장 → ES 어댑터 구현
- [ ] DB 최신순 경로 추가(repository + service 분기)
- [ ] 컨트롤러 소유권 검증
- [ ] 프론트 연결 — 기간 프리셋 → `startDate`/`endDate` 변환, `제목만` · 정렬 활성화
- [ ] `compileJava` · `test` + 실측 검증

### 검증 항목

- 검색어 없이 200 + 최신순 정렬(이전에는 400)
- `keywordIds` 다중 필터 동작 / **타인 키워드 id → 403**
- `제목만` 이 본문 매칭을 제외하는지
- 기간 프리셋 5종이 실제 날짜 범위로 변환되는지(`직접 지정` 포함)

---

## Phase 3 — 키워드 통계 API *(승인 필요: 신규 API)*

### 변경 파일

| 파일 | 변경 |
|---|---|
| `presentation/KeywordController.java` | `GET /api/keywords/stats` 추가 |
| `application/dto/KeywordStatsResponse.java` | **신규** |
| `application/KeywordService.java` + `Impl` | 통계 조합 |
| `NewsJpaRepository.java` · `NewsRepositoryImpl.java` | `GROUP BY` 집계 2종 |
| `js/components/keyword-news.js` | 레일 건수·오늘·마지막성공·스파크라인 + 정렬 |

### API 규칙

`GET /api/keywords/stats?userId=` → 키워드별 `{ keywordId, totalCount, todayCount, lastCollectedAt, daily[7] }`

- **쿼리 2방으로 끝낸다** (brainstorm 제약 2)
  1. `GROUP BY keyword_id` → `COUNT(*)`, `MAX(created_at)`
  2. `GROUP BY keyword_id, DATE(created_at)` (최근 7일) → 스파크라인 + `todayCount`
- `lastCollectedAt` 은 `MAX(news.created_at)` **근사값**이다. 진짜 "마지막 수집 시도 성공"은
  Phase 6 의 수집 이력이 들어와야 정확해진다 → Phase 6 에서 이력 기준으로 교체
- 키워드가 0개면 조회 없이 빈 응답
- `userId` 는 인증 주체와 일치 검증(#114 H1 패턴)

### Implementation Steps

- [ ] 집계 쿼리 2종 + 인덱스 확인(`idx_news_keyword_published` 로 커버되는지, `created_at` 인덱스 필요 여부)
- [ ] `KeywordStatsResponse` + 서비스 조합
- [ ] 컨트롤러 + 소유권 검증
- [ ] 레일 렌더 — 건수 · `+오늘` 뱃지 · 마지막 성공 · 인라인 SVG 스파크라인
- [ ] 레일 정렬 토글(`뉴스많은순` ↔ `오늘많은순`)
- [ ] `compileJava` · `test` + 실측

### 검증 항목

- 키워드 14개 기준 쿼리 수가 **2방인지**(N+1 없음)
- 스파크라인이 7일 구간을 맞게 그리는지(수집 없는 날 0 처리)
- 정렬 토글 결과가 목업과 같은지

---

## Phase 4 — 키워드 CRUD *(승인 필요: 신규 API + 비즈니스 로직 변경)*

### 변경 파일

| 파일 | 변경 |
|---|---|
| `presentation/KeywordController.java` | `PUT /api/keywords/{id}` · 벌크 2종 추가 |
| `application/dto/UpdateKeywordRequest.java` | **신규** |
| `application/KeywordService.java` + `Impl` | 재구독 로직 · 벌크 |
| `domain/repository/UserKeywordRepository.java` | 구독자 수 조회 보강 |
| `NewsRepository` + `Impl` | `news.keyword_id` 이관 |
| `partials/keyword-news.html` | 수정 모달 · 삭제 확인 모달 · 케밥 메뉴 완성 · 벌크 바 |
| `js/components/keyword-news.js` | 모달 상태 · 재구독 호출 |

### 재구독 규칙 (brainstorm 충돌 1 — 태형님 "A. 재구독 방식")

`PUT /api/keywords/{id}` 는 **UPDATE 가 아니다.** `Keyword` 도메인은 **불변 유지**한다.

1. 이름·지역이 **둘 다 그대로**면 활성 토글만 반영하고 재구독을 건너뛴다
2. 바뀌었으면 새 `(이름, 지역)` 을 find-or-create 하고 구독을 만든다. `active` 는 모달 토글 값
3. 기존 키워드의 **다른 구독자 유무로 분기**
   - 단독 구독자: `news.keyword_id` 를 새 키워드로 **이관** → 기존 기사 유지(목업 약속 충족).
     이관 후 옛 키워드 행을 정리한다
   - 다중 구독자: 옛 키워드·기사는 그대로 두고 구독만 해제 → **모달 문구를 조건부로** 바꾼다
4. 새 `(이름, 지역)` 이 이미 내 구독이면 병합이 되므로 **사전 검증으로 막는다**
   (`이미 등록된 키워드입니다`)

### ⚠ 발견한 함정 — 포트폴리오 `newsEnabled` 가 꺼진다

`KeywordServiceImpl.unsubscribeKeyword()` 와 `deactivateUserKeyword()` 는
`disablePortfolioNewsByKeywordId()` 를 호출해 **키워드 이름과 같은 포트폴리오 항목의 `newsEnabled` 를 OFF** 한다
(`findByUserIdAndItemNameAndNewsEnabled(userId, keyword.getKeyword(), true)` — **이름 기반 매칭**).

재구독 방식으로 이름을 바꾸면 옛 키워드 구독 해제 단계에서 이 로직이 돌아
**사용자가 요청하지 않은 포트폴리오 연동 해제**가 일어난다.

→ 수정 경로는 `unsubscribeKeyword` 를 그대로 재사용하지 않는다.
포트폴리오 연동을 **새 이름으로 이관**하거나(항목명이 옛 키워드와 같을 때),
최소한 수정 경로에서는 `disablePortfolioNews` 를 **건너뛴다**. 어느 쪽으로 갈지는 착수 시 확정하고
게이트에 기록한다.

### 벌크 규칙

- `수집` — 선택 키워드 순차 수집(기존 단건 API 반복 호출로 시작, 느리면 벌크 엔드포인트 검토)
- `중단` — 선택 키워드 일괄 비활성화
- `삭제` — 선택 키워드 일괄 삭제. **되돌릴 수 없다**
  - 목업에 **다건 확인 모달이 없다** → 단건 모달을 확장한다:
    `키워드 N개를 삭제합니다` + 대상 이름 목록 + 합계 기사 건수
- `bulkOn`(일괄 재개)은 범위 밖

### Implementation Steps

- [ ] 포트폴리오 연동 처리 방식 확정 → 게이트 기록
- [ ] `PUT /api/keywords/{id}` — 재구독 4규칙 + 소유권 검증
- [ ] `news.keyword_id` 이관 쿼리(단독 구독자 경로)
- [ ] 벌크 중단·삭제 API
- [ ] 케밥 메뉴 완성 + 바깥 클릭 닫힘(목업 `closeMenus`)
- [ ] 수정 모달(이름 · 지역 2버튼 · 활성 토글 스위치 · 좌측 삭제 버튼)
- [ ] 삭제 확인 모달 단건 + **다건 확장**, `z-index` 90 으로 수정 모달 위에
- [ ] 등록 모달 — 수집 범위 **2버튼**(`국내+해외` 제거) + 안내 문구 박스 제거
- [ ] `compileJava` · `test` + 실측

### 검증 항목

- 단독 구독자 rename → **기사가 유지되는지** (`news.keyword_id` 이관 확인)
- 다중 구독자 rename → 다른 사용자 키워드가 **안 바뀌는지**, 모달 문구가 조건부로 바뀌는지
- **포트폴리오 `newsEnabled` 가 rename 후에도 유지되는지** (위 함정)
- 중복 키워드로 수정 시 차단되는지
- 삭제 확인 모달의 기사 건수가 실제와 맞는지
- 벌크 삭제 후 스트림에서 해당 기사가 사라지는지(목업 `ITEMS` 필터와 동일)

---

## Phase 5 — 언론사 `source` *(승인 필요: Entity 수정)*

### 변경 파일

| 파일 | 변경 |
|---|---|
| `infrastructure/persistence/NewsEntity.java` | `source` 컬럼 추가 |
| `domain/model/News.java` · `NewsSearchResult.java` | 필드 추가 |
| `application/dto/NewsResultDto.java` · `NewsSaveRequest.java` · `NewsDto.java` | 필드 추가 |
| `application/vo/KeywordSearchContext.java` | 전달 |
| `naver/dto/NaverNewsItem.java` · `NaverNewsSearchPort.java` | `originallink` 도메인 파생 |
| `gnews/dto/GNewsArticle.java` · `GNewsSearchPort.java` | `source.name` 매핑 |
| `newsapi/dto/NewsApiArticle.java` · `NewsApiSearchPort.java` | `source.name` 매핑 |
| `persistence/mapper/NewsMapper.java` · `elasticsearch/.../NewsDocumentMapper.java` | 매핑 |
| `partials/keyword-news.html` | 기사 카드 언론사 노출 |

### 규칙

- **Naver 는 API 응답에 언론사가 없다** → `originallink` 도메인에서 파생.
  도메인→언론사명 매핑 테이블을 두고, 미등록 도메인은 도메인 문자열을 그대로 보여준다
- GNews · NewsAPI 는 `source.name` 이 응답에 있으나 **DTO 가 버리고 있다** → 필드 추가로 살린다
- **기존 데이터는 `source` 가 null** → 프론트는 null 이면 `originalUrl` 도메인으로 폴백해 표시.
  백필은 하지 않는다(별도 이슈 후보)
- 컬럼은 `nullable` 로 추가한다 → 마이그레이션이 안전하고 기존 INSERT 가 깨지지 않는다

### Implementation Steps

- [ ] `NewsEntity.source` 컬럼(nullable) + 마이그레이션 확인
- [ ] 도메인 모델 → DTO → 저장 체인에 필드 전달(7개 클래스)
- [ ] 어댑터 3종 매핑 + Naver 도메인 파생 매핑 테이블
- [ ] 프론트 표시 + null 폴백
- [ ] `compileJava` · `test` + 신규 수집으로 실측

### 검증 항목

- 신규 수집분에 언론사가 붙는지(국내·해외 각각)
- 기존 데이터(null)가 도메인 폴백으로 표시되는지
- 기존 INSERT 경로(`insertIgnoreDuplicate`)가 깨지지 않는지

---

## Phase 6 — 수집 이력 + 스케줄 상태 *(승인 필요: Entity 신규)*

### 변경 파일

| 파일 | 변경 |
|---|---|
| `news/domain/model/KeywordCollectionHistory.java` | **신규 도메인 모델** |
| `news/infrastructure/persistence/KeywordCollectionHistoryEntity.java` 등 | **신규 Entity + Repository + Mapper** |
| `application/KeywordNewsBatchServiceImpl.java` | 수집 성공/실패 기록 |
| `presentation/NewsController.java` | `GET /api/news/collector/schedule` 추가 |
| `application/dto/CollectorScheduleResponse.java` | **신규** |
| `partials/keyword-news.html` | 헤더 스케줄 박스 · 연속 실패 뱃지 · 레일 실패 메타 |

### 규칙

- 기록 필드: `keywordId` · `attemptedAt` · `status(SUCCESS/FAILURE)` · `savedCount` · `ignoredCount` · `errorMessage`
- 기록 지점은 `executeKeywordNewsBatch()`(배치)와 `collectByKeyword()`(즉시 수집) 양쪽
- **연속 실패 판정**: 마지막 SUCCESS 이후 FAILURE 연속 일수. 목업 표기는 `연속 실패 3일 · 마지막 성공 X`
- **스케줄 다음 실행**: cron `batch.schedule.economics-sync-cron:0 0 * * * *` 를
  `CronExpression.parse().next()` 로 계산. **cron 을 하드코딩하지 않고 설정값을 읽는다**
- **마지막 실행**: 수집 이력 `MAX(attemptedAt)`
- Phase 3 의 `lastCollectedAt`(= `MAX(news.created_at)` 근사)을 **이력 기준으로 교체**한다.
  기사가 0건이어도 "수집은 성공했다"를 구분할 수 있어야 정확하다
- 이력 무한 증가 → 보존 기간 정책 필요(예: 90일). 착수 시 확정

### Implementation Steps

- [ ] Entity + Repository + Mapper 신규 (스키마 착수 전 재확인)
- [ ] 배치·즉시수집에 기록 추가 — **기록 실패가 수집을 깨뜨리지 않게** 예외 흡수
- [ ] 연속 실패 판정 쿼리
- [ ] 스케줄 상태 API(cron 설정값 파싱)
- [ ] 헤더 스케줄 박스 · 연속 실패 뱃지 · 레일 실패 메타
- [ ] Phase 3 `lastCollectedAt` 을 이력 기준으로 교체
- [ ] `compileJava` · `test` + 실측

### 검증 항목

- 수집 성공·실패가 각각 기록되는지
- 연속 실패 일수가 맞는지(성공 후 리셋 확인)
- 다음 실행 시각이 cron 과 맞는지 / 설정 변경 시 따라가는지
- 기록 실패가 수집 자체를 막지 않는지

---

## Phase 7 — 읽음/저장 *(승인 필요: Entity 신규)*

### 변경 파일

| 파일 | 변경 |
|---|---|
| `news/domain/model/UserNewsState.java` | **신규 도메인 모델** |
| `news/infrastructure/persistence/UserNewsStateEntity.java` 등 | **신규 Entity + Repository + Mapper** |
| `presentation/NewsController.java` | 읽음·저장 토글 · 모두 읽음 |
| `application/NewsSearchApplicationService.java` | 검색 결과에 상태 결합 |
| `partials/keyword-news.html` | 읽음 점 · 굵게 · ☆저장 · `안 읽은 것만` 활성 |

### 규칙

- 단일 테이블 `user_news_state(user_id, news_id, read, saved)` + 유니크 `(user_id, news_id)`
- **ES 결과에는 뉴스 id 가 없다**(brainstorm 사실 1) → 검색 결과의 `originalUrl` 목록으로
  `findByOriginalUrlIn` 재조회해 id 를 얻고 상태를 결합한다. **전체 재색인을 피한다**
  - 이 재조회는 언론사(Phase 5) 표시에도 쓰이므로 **경로를 하나로 합친다**
- `안 읽은 것만` 필터는 DB 경로에서 처리한다. ES 경로에서는 결합 후 필터링하면 페이징이 어긋나므로
  **페이징 정합성 처리 방식을 착수 시 확정**한다(후처리 필터 대신 id 목록 선조회 등)
- `모두 읽음` 은 현재 스트림에 보이는 범위만 처리한다(전체 뉴스 대상이면 의도치 않게 광범위)

### Implementation Steps

- [ ] Entity + Repository + Mapper 신규 (스키마 착수 전 재확인)
- [ ] `originalUrl` → id 재조회 경로 신설(Phase 5 언론사 경로와 통합)
- [ ] 읽음·저장 토글 API + 모두 읽음
- [ ] 검색 응답에 상태 결합
- [ ] `안 읽은 것만` 페이징 정합성 확정 후 구현
- [ ] 프론트 — 읽음 점 · 제목 굵기 · ☆저장 · 카드 클릭 시 읽음 처리
- [ ] `compileJava` · `test` + 실측

### 검증 항목

- 새로고침 후 읽음·저장 상태가 유지되는지
- `안 읽은 것만` + 페이징 조합에서 건수·페이지가 어긋나지 않는지
- 재조회 쿼리가 페이지당 1방인지

---

## 리스크

- **프론트 변경 폭이 크다** — partial 2개 + 컴포넌트 3개를 삭제하고 재편한다.
  `app.js` 의 `validPages` · `partialNames` · `navigateTo` · `cleanupRegistry` 를 모두 손대야 하고,
  `home-side.html` 의 `navigateTo('keywords')` 2곳도 함께 고쳐야 한다(놓치면 홈에서 죽은 링크)
- **재구독 방식의 포트폴리오 부작용** — Phase 4 의 함정. 이름 기반 매칭이라 rename 과 충돌한다
- **`안 읽은 것만` + ES 페이징** — 상태가 DB 에 있고 검색이 ES 라 후처리 필터는 페이징을 깨뜨린다
- **Entity 신규 2개 + 컬럼 1개** — 운영 반영 시 마이그레이션 순서가 필요하다.
  모두 nullable/신규 테이블이라 기존 코드가 깨지지는 않지만, 배포 순서는 commit 단계에서 확정
- **수집 이력 무한 증가** — 키워드 14개 × 매시 = 일 336행. 보존 정책 없으면 1년에 12만 행
- 목업은 고정 데이터(키워드 14개 · 기사 10건) 기준이라 **키워드 0개·100개, 기사 0건 레이아웃이 미정의**.
  Phase 1 에서 빈 상태 3종을 확정했으나 과다 상태(레일 스크롤)는 실데이터로 확인 필요
- 유사 기사 클러스터링을 뺐으므로 **같은 사건 기사가 스트림을 도배**할 수 있다.
  통합 화면은 기존보다 기사를 많이 보여주므로 체감이 커질 수 있다 → 별도 이슈로 후속
