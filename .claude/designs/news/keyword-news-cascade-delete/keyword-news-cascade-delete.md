# 키워드 삭제 시 관련 뉴스 연쇄 삭제 설계

## 작업 리스트

- [ ] NewsRepository에 `deleteByUserIdAndSearchKeyword` 메서드 추가
- [ ] NewsJpaRepository에 `deleteByUserIdAndSearchKeyword` 메서드 추가
- [ ] NewsRepositoryImpl에 `deleteByUserIdAndSearchKeyword` 구현
- [ ] KeywordServiceImpl.deleteKeyword()에서 관련 뉴스 삭제 로직 추가

## 배경

키워드 삭제 시 해당 키워드로 수집된 뉴스가 그대로 남음. 키워드 삭제와 함께 관련 뉴스도 정리 필요.

## 핵심 결정

- **삭제 기준**: `userId` + `searchKeyword` 조합 (다른 사용자의 뉴스는 보존)
- **삭제 위치**: `KeywordServiceImpl.deleteKeyword()` 내에서 뉴스 먼저 삭제 → 키워드 삭제 (같은 트랜잭션)
- **의존성**: `KeywordServiceImpl`에 `NewsRepository` 주입 (동일 모듈 내 연관이므로 허용)

## 구현

### NewsRepository (도메인)

위치: `news/domain/repository/NewsRepository.java`

- `void deleteByUserIdAndSearchKeyword(Long userId, String searchKeyword)` 추가

### NewsJpaRepository (인프라)

위치: `news/infrastructure/persistence/NewsJpaRepository.java`

- `void deleteByUserIdAndSearchKeyword(Long userId, String searchKeyword)` 추가 (Spring Data JPA 자동 생성)

### NewsRepositoryImpl (인프라)

위치: `news/infrastructure/persistence/NewsRepositoryImpl.java`

- 위임 구현

### KeywordServiceImpl (애플리케이션)

위치: `news/application/KeywordServiceImpl.java`

- `NewsRepository` 의존성 추가
- `deleteKeyword()`에서 뉴스 삭제 후 키워드 삭제

[예시 코드](./examples/cascade-delete-example.md)

## 주의사항

- 뉴스 삭제 → 키워드 삭제 순서 (FK 제약 없지만 논리적 순서 유지)
- 동일 트랜잭션 내 처리 (`@Transactional` 기존 적용)
