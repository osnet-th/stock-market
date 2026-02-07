# 뉴스 기능 현황

작성일: 2026-02-07
수정일: 2026-02-07

## 개요

뉴스 도메인의 전체 구현 상태 및 현황을 정리한 문서입니다.

## 구현 완료 기능

### 뉴스 조회

- [완료] 외부 API를 통한 뉴스 검색 (NewsSearchService)
- [완료] 포트 기반 다중 API 지원 구조 (NewsSearchPort)
- [완료] 금일 00시 이후 발행 뉴스만 조회
- [완료] API 실패 시 다음 포트로 폴백

### 뉴스 저장

- [완료] 단건 저장 (NewsSaveService.save)
- [완료] 배치 저장 (NewsSaveService.saveBatch)
- [완료] URL 기반 중복 처리 (INSERT ... ON CONFLICT DO NOTHING)
- [완료] 청크 단위 트랜잭션 처리 (batch_size: 1000)
- [완료] 부분 성공 허용 (성공/무시/실패 건수 반환)

### 저장된 뉴스 조회

- [완료] 저장 목적(KEYWORD, STOCK) 기반 조회 (NewsQueryService.getByPurpose)
- [완료] URL 존재 여부 확인 (NewsQueryService.findExistingUrls)

### 키워드 관리

- [완료] 키워드 등록 (KeywordService.registerKeyword)
- [완료] 사용자별 키워드 조회 (KeywordService.getKeywordsByUserId)
- [완료] 사용자별 활성 키워드 조회 (KeywordService.getActiveKeywordsByUserId)
- [완료] 전체 활성 키워드 조회 (KeywordService.getAllActiveKeywords)
- [완료] 키워드 활성화/비활성화 (KeywordService.activateKeyword/deactivateKeyword)
- [완료] 키워드 삭제 (KeywordService.deleteKeyword)

### 키워드 뉴스 배치

- [완료] 키워드 기반 뉴스 자동 수집 (KeywordNewsBatchService)
- [완료] 활성 키워드별 뉴스 검색
- [완료] 중복 필터링 (신규 뉴스만 저장)
- [완료] 배치 저장
- [완료] 스케줄링 (매 시간 정각 실행)

## 패키지 구조

```
news/
├── domain/
│   ├── model/
│   │   ├── News.java              # 뉴스 도메인 모델
│   │   ├── Keyword.java           # 키워드 도메인 모델
│   │   ├── NewsPurpose.java       # 저장 목적 Enum (KEYWORD, STOCK)
│   │   └── NewsSearchResult.java  # 조회 전용 모델
│   ├── repository/
│   │   ├── NewsRepository.java    # 뉴스 저장소 인터페이스
│   │   └── KeywordRepository.java # 키워드 저장소 인터페이스
│   └── service/
│       └── NewsSearchPort.java    # 뉴스 검색 포트 인터페이스
│
├── application/
│   ├── NewsSearchService.java           # 외부 API 뉴스 조회
│   ├── NewsSaveService.java             # 뉴스 저장
│   ├── NewsQueryService.java            # 저장된 뉴스 조회
│   ├── KeywordService.java              # 키워드 관리 인터페이스
│   ├── KeywordServiceImpl.java          # 키워드 관리 구현
│   ├── KeywordNewsBatchService.java     # 키워드 뉴스 배치 인터페이스
│   ├── KeywordNewsBatchServiceImpl.java # 키워드 뉴스 배치 구현
│   └── dto/
│       ├── NewsResultDto.java           # 외부 API 조회 응답
│       ├── NewsDto.java                 # 저장된 뉴스 응답
│       ├── NewsSaveRequest.java         # 뉴스 저장 요청
│       ├── NewsBatchSaveResult.java     # 배치 저장 결과
│       └── RegisterKeywordRequest.java  # 키워드 등록 요청
│
├── infrastructure/
│   ├── persistence/
│   │   ├── NewsEntity.java            # 뉴스 JPA 엔티티
│   │   ├── NewsJpaRepository.java     # 뉴스 JPA 저장소
│   │   ├── NewsRepositoryImpl.java    # 뉴스 저장소 구현
│   │   ├── KeywordEntity.java         # 키워드 JPA 엔티티
│   │   ├── KeywordJpaRepository.java  # 키워드 JPA 저장소
│   │   ├── KeywordRepositoryImpl.java # 키워드 저장소 구현
│   │   └── mapper/
│   │       ├── NewsMapper.java        # 뉴스 매퍼
│   │       └── KeywordMapper.java     # 키워드 매퍼
│   └── scheduler/
│       ├── KeywordNewsScheduler.java      # (구) 스케줄러
│       └── KeywordNewsBatchScheduler.java # 키워드 뉴스 배치 스케줄러
│
└── presentation/
    └── KeywordController.java # 키워드 관리 API
```

## 주요 컴포넌트

### Domain Layer

**News (도메인 모델)**
- 필드: id, originalUrl, userId, title, content, publishedAt, createdAt, purpose, searchKeyword
- 생성 메서드: `News.create()`
- 검증: originalUrl, userId, title, publishedAt, purpose, searchKeyword 필수 검증

**Keyword (도메인 모델)**
- 필드: id, keyword, userId, active, createdAt
- 생성 메서드: `Keyword.create()`
- 행위 메서드: `activate()`, `deactivate()`
- 검증: keyword, userId 필수 검증

**NewsPurpose (Enum)**
- KEYWORD: 키워드 기반 저장
- STOCK: 주식 기반 저장

**NewsSearchPort (포트 인터페이스)**
- 메서드: `search(keyword, fromDateTime)`
- 외부 뉴스 API 구현체 확장 가능

### Application Layer

**NewsSearchService**
- 책임: 외부 API를 통한 뉴스 검색
- 의존성: `List<NewsSearchPort>`
- 전략: 순차 폴백 (첫 성공 포트 사용)

**NewsSaveService**
- 책임: 뉴스 저장 (단건/배치)
- 의존성: `NewsRepository`, `PlatformTransactionManager`
- 트랜잭션: 단건은 `@Transactional`, 배치는 청크별 REQUIRES_NEW
- 중복 처리: `insertIgnoreDuplicate()` 사용

**NewsQueryService**
- 책임: 저장된 뉴스 조회
- 의존성: `NewsRepository`
- 주요 메서드:
  - `getByPurpose(purpose)`: 저장 목적별 조회
  - `findExistingUrls(urls)`: URL 존재 여부 확인 (배치용)

**KeywordService / KeywordServiceImpl**
- 책임: 키워드 생명주기 관리
- 의존성: `KeywordRepository`
- 주요 메서드:
  - `registerKeyword()`: 키워드 등록
  - `getActiveKeywordsByUserId()`: 사용자별 활성 키워드
  - `getAllActiveKeywords()`: 전체 활성 키워드 (배치용)
  - `activateKeyword()`, `deactivateKeyword()`: 활성화 제어
  - `deleteKeyword()`: 키워드 삭제

**KeywordNewsBatchService / KeywordNewsBatchServiceImpl**
- 책임: 활성 키워드 기반 뉴스 자동 수집
- 의존성: `KeywordService`, `NewsSearchService`, `NewsSaveService`, `NewsQueryService`, `Notification`
- 처리 흐름:
  1. 활성 키워드 조회 (`getAllActiveKeywords()`)
  2. 키워드별 뉴스 검색 (`NewsSearchService.search()`)
  3. 중복 필터링 (`filterNewNews()` - URL 기반)
  4. 배치 저장 (`NewsSaveService.saveBatch()`)
  5. 알림 발송 (Notification - 미구현)
- 반환: 저장된 신규 뉴스 건수

### Infrastructure Layer

**NewsEntity / NewsJpaRepository / NewsRepositoryImpl**
- NewsEntity: JPA 엔티티 (DB 매핑)
- NewsJpaRepository: Spring Data JPA 인터페이스
  - Native Query: `INSERT ... ON CONFLICT (original_url) DO NOTHING`
- NewsRepositoryImpl: NewsRepository 구현
  - `save()`, `insertIgnoreDuplicate()`, `findByOriginalUrl()`, `findByPurpose()`

**KeywordEntity / KeywordJpaRepository / KeywordRepositoryImpl**
- KeywordEntity: JPA 엔티티 (DB 매핑)
- KeywordJpaRepository: Spring Data JPA 인터페이스
- KeywordRepositoryImpl: KeywordRepository 구현
  - `save()`, `findByUserId()`, `findActiveByUserId()`, `findAllActive()`, `findById()`, `delete()`

**KeywordNewsBatchScheduler**
- 스케줄: `@Scheduled(cron = "0 0 * * * *")` (매 시간 정각)
- 실행: `KeywordNewsBatchService.executeKeywordNewsBatch()`

### Presentation Layer

**KeywordController**
- API 엔드포인트: 키워드 관리
- 의존성: `KeywordService`

## 테스트 현황

- [완료] NewsTest (도메인 모델)
- [완료] KeywordTest (도메인 모델)
- [완료] NewsSearchServiceTest
- [완료] NewsSaveServiceTest
- [완료] NewsQueryServiceTest
- [완료] KeywordServiceTest
- [완료] KeywordNewsBatchServiceImplTest

## 미구현 사항

### 뉴스 검색 포트 구현체

- NewsSearchPort 인터페이스만 정의됨
- 실제 외부 API 연동 구현 필요 (추후 추가)
- 후보: Naver 뉴스 API, Google News API 등

### 알림 (Notification)

- Notification 인터페이스 미정의
- 배치 완료 시 알림 발송 기능 미구현
- 구현 예정: 이메일, Slack, SMS 등

### 뉴스 API

- 뉴스 조회 API 미구현
- 뉴스 저장 API 미구현
- Controller/Presentation 계층 미추가

### 주식 기반 뉴스 조회

- 현재 키워드 기반만 구현
- 주식 기반 뉴스 배치 미구현
- NewsPurpose.STOCK 활용 예정

## 설계 문서 참고

- [claude-keyword-news-batch-design.md](.claude/designs/claude-keyword-news-batch-design.md)
- [news-design.md](.claude/designs/news-design.md)

## 기술 스택

- Spring Framework 6.1.x
- Spring Data JPA
- QueryDSL
- PostgreSQL
- JUnit 5
- Mockito