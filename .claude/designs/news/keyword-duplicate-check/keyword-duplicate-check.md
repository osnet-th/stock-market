# 키워드 중복 등록 방지 설계

## 작업 리스트

- [x] KeywordRepository에 `existsByUserIdAndKeyword` 메서드 추가
- [x] KeywordJpaRepository에 `existsByUserIdAndKeyword` 메서드 추가
- [x] KeywordRepositoryImpl에 `existsByUserIdAndKeyword` 구현
- [x] KeywordServiceImpl.registerKeyword()에 중복 검사 로직 추가

## 배경

동일 사용자가 같은 키워드를 중복 등록할 수 있는 상태. 사용자별 키워드 단위로 중복 방지 필요.

## 핵심 결정

- **중복 기준**: `userId` + `keyword` 조합 (동일 사용자의 같은 키워드만 차단, 다른 사용자는 허용)
- **검증 위치**: `KeywordServiceImpl.registerKeyword()` (애플리케이션 서비스 레벨)
- **예외 처리**: `IllegalArgumentException`으로 중복 메시지 반환 (기존 패턴 유지)

## 구현

### KeywordRepository (도메인)

위치: `news/domain/repository/KeywordRepository.java`

- `boolean existsByUserIdAndKeyword(Long userId, String keyword)` 추가

### KeywordJpaRepository (인프라)

위치: `news/infrastructure/persistence/KeywordJpaRepository.java`

- `boolean existsByUserIdAndKeyword(Long userId, String keyword)` 추가 (Spring Data JPA 자동 생성)

### KeywordRepositoryImpl (인프라)

위치: `news/infrastructure/persistence/KeywordRepositoryImpl.java`

- `existsByUserIdAndKeyword` 위임 구현

### KeywordServiceImpl (애플리케이션)

위치: `news/application/KeywordServiceImpl.java`

- `registerKeyword()`에서 저장 전 중복 검사

[예시 코드](./examples/keyword-duplicate-check-example.md)