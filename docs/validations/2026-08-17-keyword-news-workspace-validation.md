# 키워드 뉴스 워크스페이스 통합 Validation

gate: docs/gates/2026-08-17-keyword-news-workspace-gates.md
plan: docs/plans/2026-08-17-002-feat-keyword-news-workspace-plan.md
review: docs/reviews/2026-08-17-keyword-news-workspace-review.md
issue: https://github.com/osnet-th/stock-market/issues/115

## 결론

리뷰에서 "실 DB 미검증"으로 분류한 항목을 실제 PostgreSQL · Elasticsearch 에서 실행해
**버그 2건을 발견·수정**했다.

- **V3** `findLatestByScope` 파라미터 타입 추론 실패 — 이번 작업이 만든 버그
- **E-1** 뉴스 인덱스 생성 실패 — **기존 버그**(이번 작업과 무관). 태형님 승인 후 함께 수정

## 환경

- PostgreSQL: `local-postgres` 컨테이너 (localhost:5432/stocks), 기동 중
- Elasticsearch: 검증 전용 컨테이너로 기동. 운영과 **같은 이미지**(`Dockerfile.elasticsearch`
  = ES 9.0.0 + `analysis-nori`)를 쓰되 메모리만 낮췄다 — 힙 512m / 컨테이너 1GB (운영은 2g/3g).
  실사용 928MB/1GB 로 빠듯했으며 500MB 로는 기동 자체가 불가능했을 것이다.
  `docker-compose.yml` 은 수정하지 않았고, 검증 후 컨테이너는 제거했다(이미지는 보존).
- 검증 방식: `@SpringBootTest` + `@Transactional` 임시 테스트로 리포지토리를 실제 호출.
  트랜잭션 롤백으로 dev DB 에 잔여 데이터를 남기지 않았다(검증 후 전 테이블 0행 확인).
  CLAUDE.md 의 "통합테스트 미작성" 정책에 따라 검증 종료 후 테스트 파일은 삭제했다.

## 스키마 검증 (ddl-auto: update)

세 가지 모두 Entity 정의대로 생성됐다.

| 대상 | 결과 |
|---|---|
| `keyword_collection_history` | 7컬럼 · PK · 인덱스 2종(`idx_kch_keyword_attempted`, `idx_kch_attempted`) · status CHECK(SUCCESS/FAILURE) |
| `user_news_state` | 6컬럼 · PK · 유니크 `uk_user_news_state(user_id, news_id)` · 인덱스 2종 |
| `news.source` | `character varying(100)` nullable — 기존 행 영향 없음 |

## 실행 검증 결과

| ID | 대상 | 결과 |
|---|---|---|
| V1 | `CAST(n.createdAt AS LocalDate)` 일자 그룹핑 | PASS — 일자별 집계 1행, 오늘 3건 |
| V2 | 연속 실패 네이티브 CTE (Postgres `FILTER` + 상관 서브쿼리) | PASS — 실패 키워드 streak=2 · 마지막 성공 시각 정상, 정상 키워드 streak=0 |
| V3 | `findLatestByScope` 선택 조건 + `안 읽은 것만` | **최초 FAIL → 수정 후 PASS** (아래) |
| V4 | 읽은 기사 URL 조회 (ES 제외 목록) | PASS — 읽은 1건만 반환 |
| V5 | 기사 이관 + 총계 집계 | PASS — 2건 이관, 대상 키워드 총계 2 |

전체 테스트 142건 전건 PASS.

## V3 — 발견한 제품 버그

### 증상

```
ERROR: could not determine data type of parameter $2
```

생성된 SQL 이 `(? is null or ne1_0.published_at>=?)` 인데, Postgres 는 `? is null` 위치의
파라미터 타입을 추론할 수 없어 쿼리 자체가 실패했다.

### 영향

`findLatestByScope` 는 **검색어가 없을 때의 기본 경로**다. 즉 통합 화면에 들어가면
뉴스 스트림이 통째로 실패한다. 화면의 가장 기본 동작이 막히는 결함이었다.

### 왜 이전 단계에서 못 잡았나

- 단위 테스트는 리포지토리를 목으로 대체해 SQL 이 실행되지 않는다
- 컨텍스트 로딩 테스트는 `@Query` 의 **파싱**만 검증하고 실행하지 않는다
- 그래서 리뷰에서 이 항목을 "실 DB 미검증"으로 분류해 두었고, 실제로 여기서 드러났다

### 조치

선택 조건 파라미터에 `CAST` 를 씌워 타입 힌트를 줬다.

```
AND (CAST(:startAt AS LocalDateTime) IS NULL OR n.publishedAt >= :startAt)
AND (CAST(:endAt   AS LocalDateTime) IS NULL OR n.publishedAt <= :endAt)
AND (CAST(:region  AS String)        IS NULL OR n.region = :region)
AND (CAST(:unreadOnly AS Boolean) = false OR NOT EXISTS (...))
```

수정 후 전체 3건 / 안 읽음 2건으로 정상 동작하며, `totalPages` 도 필터 후 건수 기준으로 맞았다
(후처리 필터를 쓰지 않아 페이징이 어긋나지 않는다는 설계 의도 확인).

## E-1 — 발견한 기존 버그 (이번 작업과 무관)

### 증상

```
[es/indices.create] failed: [illegal_argument_exception]
No enum constant org.apache.lucene.analysis.ko.POS.Tag.E
```

`news-settings.json` 의 `nori_posfilter.stoptags` 에 있던 우산 태그 `E`(어미)·`J`(조사) 가
Lucene 10(ES 9.0.0)에서 제거돼 **뉴스 인덱스 생성 자체가 실패**한다.
34개 태그를 개별 확인한 결과 무효인 것은 이 둘뿐이었다.

### 이번 작업과 무관함

`news-settings.json` 은 최초 ES 기능 커밋(`22b7547`) 이후 수정된 적이 없고,
이 브랜치는 해당 파일도 `Dockerfile.elasticsearch` 도 변경하지 않았다.

### 운영 영향 (미확인)

`Dockerfile.elasticsearch` 가 `elasticsearch:9.0.0` 으로 고정돼 있다.
운영 ES 가 **빈 볼륨에서 9.0.0 으로 기동**됐다면 인덱스 생성이 실패했을 것이고,
`NewsElasticsearchSearcher` 가 예외를 흡수해 빈 결과를 돌려주므로
오류가 아니라 "검색 결과 없음"으로 보인다 — **조용히 죽어 있을 수 있다.**
반대로 ES 8 시절 인덱스가 볼륨에 남아 있다면 현재도 동작한다.
운영 상태는 확인하지 못했다. `GET /news/_settings` 로 판별 가능하다.

### 조치 (태형님 승인 후 이번 작업에 포함)

```
E -> EP EF EC ETN ETM
J -> JKS JKC JKG JKO JKB JKV JKQ JX JC
```

수정 과정에서 설명을 `_comment` 키로 넣었더니 ES 클라이언트가
`Unknown field '_comment'` 로 거부해 제거했다 — 인덱스 설정 JSON 에는 주석을 넣을 수 없다.

## ES 경로 실행 검증 결과

| ID | 대상 | 결과 |
|---|---|---|
| E0 | nori 설정으로 인덱스 생성 + 색인 | PASS — 3건 색인 (설정 적용 확인) |
| E1 | `keywordId` terms 스코프 필터 | PASS — A+B 2건 / B만 0건 |
| E2 | `제목만` vs `제목+본문` 필드 분기 | PASS — 1건 / 2건. 본문 매칭이 nori 로 동작 |
| E3 | `안 읽은 것만` `_id` must_not 제외 | PASS — 2건 → 1건, `totalHits` 에 반영 |
| E4 | `publishedAt` 최신순 정렬 | PASS — 발행일 내림차순 |

E2 는 nori 가 실제로 동작함을 같이 보여준다. 별도로 `_analyze` 로 확인한 결과
`삼성전자 HBM4 양산 라인 증설` → `[삼성, 전자, HBM, 4, 양산, 라인, 증설]` 로 분해된다.

## 미검증으로 남는 항목

### 인증 경유 E2E

카카오 로그인이 필요해 실제 HTTP 요청 단위 검증(403 소유권 차단, `/api/news/search` 인증 필수 전환,
H1 조치 동작)은 수행하지 못했다. 소유권 로직 자체는 정적 검토와 단위 테스트로 확인했다.

### 포트폴리오 연동

키워드 수정 시 같은 이름의 포트폴리오 항목 `newsEnabled` 가 꺼지는 동작은
실데이터로 확인하지 못했다. 기존 모델의 의도된 역방향 동기화로 판단했으나 체감 확인이 남는다.

## 후속 제안

- **회귀 가드 부재**: V3 같은 결함은 목 기반 단위 테스트로 잡히지 않는다.
  프로젝트 정책상 통합 테스트를 두지 않아 이번 검증 테스트는 삭제했으나,
  실 DB 를 쓰는 쿼리 스모크 테스트를 예외적으로 허용할지는 태형님 판단이 필요하다.
- **운영 ES 인덱스 상태 확인** — `GET /news/_settings` 로 인덱스가 존재하는지,
  존재한다면 옛 stoptags 로 만들어졌는지 확인이 필요하다.
  옛 설정으로 만들어진 인덱스는 이번 수정만으로 바뀌지 않으므로 재색인이 필요할 수 있다
- `news.source` 기존 데이터 백필 (현재 URL 도메인 폴백으로 표시 중)
- 키워드 수정 후 재색인 경로는 단위 수준에서만 확인 — 실제 rename 시나리오 E2E 는 남아 있다
