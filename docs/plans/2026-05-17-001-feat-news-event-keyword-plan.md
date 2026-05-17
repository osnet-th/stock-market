# feat: 뉴스 기록 키워드 입력

> 원천 이슈: #46
> 브레인스토밍: [docs/brainstorms/2026-05-17-news-event-keyword-brainstorm.md](../brainstorms/2026-05-17-news-event-keyword-brainstorm.md)

## 배경

뉴스 기록(`newsjournal`) 사건에 해시태그형 키워드를 여러 개 기입할 수 있도록 한다.
키워드는 선택 입력이며, 관련 기사 링크와 동일하게 자식 테이블 + replace-all 정책으로
관리한다.

## 범위

**포함**: 키워드 입력·저장·수정·삭제·표시, 모달 입력 UI, 타임라인 카드 칩 표시.
**제외**: 키워드 클릭 필터링, 키워드 검색, 키워드 자동완성/마스터 테이블.

## 핵심 설계

| 항목 | 결정 |
|---|---|
| 저장 | 자식 테이블 `news_event_keyword` (`NewsEventLink` 패턴 복제) |
| 도메인 모델 | `NewsEventKeyword(id, eventId, keyword, displayOrder)` |
| Command | 별도 record 없이 `List<String> keywords` 사용 |
| 상한 | 키워드당 50자, 사건당 최대 20개 |
| 정규화 | 앞쪽 `#` 제거 + trim, 저장값에 `#` 미포함 |
| 갱신 | replace-all (링크와 동일) |
| DB | `ddl-auto: update` 자동 생성, 마이그레이션 SQL 불필요 |

## 작업 체크리스트

### 백엔드 — 도메인 / 영속화
- [x] `NewsEventKeyword` 도메인 모델 (`create()` 팩토리 + `assignId()`)
- [x] `NewsEventKeywordEntity` (table `news_event_keyword`, index `idx_news_event_keyword_event`)
- [x] `NewsEventKeywordJpaRepository` (`findByEventIdOrderBy...`, `findByEventIdIn...`, `deleteByEventId`)
- [x] `NewsEventKeywordRepository` 포트 (`findByEventId`, `findAllByEventIds`, `replaceAll`, `deleteByEventId`)
- [x] `NewsEventKeywordRepositoryImpl` 어댑터
- [x] `NewsEventMapper`에 `toEntity`/`toDomain`(`NewsEventKeyword`) 추가

### 백엔드 — 애플리케이션
- [x] `CreateNewsEventCommand` / `UpdateNewsEventCommand`에 `List<String> keywords` 추가
- [x] `NewsEventDetailResult` / `NewsEventListItemResult`에 `List<NewsEventKeyword> keywords` 추가
- [x] `NewsEventWriteService`: `keywordRepository` 주입, create/update `replaceAll`, delete `deleteByEventId`
- [x] `NewsEventReadService`: `keywordRepository` 주입, `findById`/`findList`에 keywords 동봉 (N+1 회피)

### 백엔드 — 표현
- [x] `CreateNewsEventRequest` / `UpdateNewsEventRequest`에 `keywords` 필드 + 검증 추가
- [x] `NewsEventDetailResponse` / `NewsEventListResponse.ItemDto`에 `List<String> keywords` 추가

### 프론트엔드
- [x] `news-journal.js`: 폼 `keywords` + `keywordDraft` 상태, add/remove 핸들러, save body 포함
- [x] `news-journal.html`: 모달 키워드 입력 섹션(칩 + Enter 입력), 타임라인 카드 키워드 칩 표시

### 검증
- [x] `./gradlew compileJava` 통과
- [x] 앱 기동 → `news_event_keyword` 테이블 자동 생성 확인
- [x] 키워드 포함 생성/수정/삭제/조회 골든패스 확인 (UI 확인 불가 시 명시)

## 검증 결과

- `./gradlew compileJava` 통과, `node --check` 프론트 JS 통과.
- 앱 기동(dev) → `news_event_keyword` 테이블 + `idx_news_event_keyword_event` 인덱스 자동 생성 확인.
- REST 라운드트립 (JWT 인증):
  - POST 생성 → 키워드 저장 (201).
  - GET 상세 / GET 목록 → `keywords` 응답 포함.
  - PUT 수정 → replace-all 동작 확인 (기존 행 삭제 후 신규 행 재생성, displayOrder 0..n).
  - DELETE → 키워드 자식 cascade 삭제 확인 (잔여 0건).
  - 빈 키워드 요청 → 400 `keywords[1]: must not be blank` (container-element 검증 동작).
- 프론트 UI는 본 샌드박스에서 브라우저 검증 불가 — JS 문법 검사만 수행.

## 리스크 / 주의

- `ddl-auto: update`가 새 테이블을 생성 (운영 무중단, 백필 불필요).
- request/response 스키마는 옵셔널 필드 추가 → 기존 클라이언트 하위호환.
- 키워드 자식은 사건 삭제 시 `deleteByEventId`로 명시적 제거 (Entity 연관관계 미사용).
