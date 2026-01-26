# Kakao OAuth Client 리팩토링 작업 계획서

**작성일**: 2026-01-27

---

## 1. 작업 개요

### 목표
현재 단일 클래스인 `KakaoOAuthClient`를 단일 책임 원칙에 따라 인터페이스 1개와 구현 클래스 3개로 분리

### 기대 효과
- 단일 책임 원칙(SRP) 준수
- 의존성 역전 원칙(DIP) 적용
- 테스트 용이성 향상
- 다른 OAuth Provider 확장 용이

### 참고 문서
- `.claude/designs/kakao-oauth-class-separation.md`

---

## 2. 작업 범위

### 생성할 파일
- `user/domain/oauth/OidcParser.java` (인터페이스)
- `user/domain/oauth/OidcClaims.java` (record)
- `user/infrastructure/oauth/kakao/KakaoJwksProvider.java`
- `user/infrastructure/oauth/kakao/KakaoOidcParser.java`
- `user/infrastructure/oauth/kakao/KakaoTokenClient.java`

### 수정할 파일
- `user/application/OAuthLoginService.java` (의존성 변경)

### 삭제 예정 파일
- `user/infrastructure/oauth/kakao/KakaoOAuthClient.java` (리팩토링 완료 후)

---

## 3. 작업 단계

### Step 1: 도메인 인터페이스 정의
**목표**: OAuth Provider에 독립적인 OIDC 파싱 계약 정의

**작업 내용**:
- `OidcParser` 인터페이스 작성
  - 패키지: `user.domain.oauth`
  - 메서드: `OidcClaims parseIdToken(String idToken)`

- `OidcClaims` record 작성
  - 패키지: `user.domain.oauth`
  - 필드: `issuer`, `subject`, `email`

**TDD 적용**: 도메인 계층이므로 테스트 작성하지 않음

---

### Step 2: KakaoJwksProvider 분리
**목표**: JWKS 공개키 관리 및 캐싱 로직 분리

**작업 내용**:
- `KakaoJwksProvider` 클래스 작성
  - 패키지: `user.infrastructure.oauth.kakao`
  - 주요 메서드:
    - `getPublicKey(String kid)`: kid로 공개키 조회
    - `refreshKeys()`: JWKS 갱신 (private)
    - `toPublicKey(JwkKey jwk)`: JWK를 RSAPublicKey로 변환 (private)

- 내부 클래스:
  - `JwksResponse` record
  - `JwkKey` record
  - `JwksCache` record

**의존성**:
- `KakaoClient`

**TDD 적용**: 인프라 계층이므로 테스트 작성하지 않음

---

### Step 3: KakaoOidcParser 분리
**목표**: OIDC ID Token 파싱 및 검증 로직 분리

**작업 내용**:
- `KakaoOidcParser` 클래스 작성
  - 패키지: `user.infrastructure.oauth.kakao`
  - 구현: `OidcParser` 인터페이스
  - 주요 메서드:
    - `parseIdToken(String idToken)`: OIDC 클레임 파싱 (public)
    - `extractKid(String idToken)`: JWT 헤더에서 kid 추출 (private)
    - `validateOidcConfig()`: OIDC 설정 검증 (private)

**의존성**:
- `KakaoJwksProvider`
- `KakaoOAuthProperties`
- `ObjectMapper`

**TDD 적용**: 인프라 계층이므로 테스트 작성하지 않음

---

### Step 4: KakaoTokenClient 분리
**목표**: 카카오 토큰 발급 로직 분리

**작업 내용**:
- `KakaoTokenClient` 클래스 작성
  - 패키지: `user.infrastructure.oauth.kakao`
  - 주요 메서드:
    - `issueToken(String code)`: 인증 코드로 토큰 발급

**의존성**:
- `KakaoClient`
- `KakaoOAuthProperties`

**TDD 적용**: 인프라 계층이므로 테스트 작성하지 않음

---

### Step 5: Application 계층 의존성 변경
**목표**: Application 계층이 인터페이스에 의존하도록 변경

**작업 내용**:
- `OAuthLoginService` 수정
  - `KakaoOAuthClient` 의존성 제거
  - `KakaoTokenClient` 주입
  - `OidcParser` 인터페이스 주입 (구현체는 `KakaoOidcParser`)

**변경 전**:
```
private final KakaoOAuthClient kakaoOAuthClient;
```

**변경 후**:
```
private final KakaoTokenClient kakaoTokenClient;
private final OidcParser oidcParser;
```

**TDD 적용**: Application 계층이지만 기존 로직 변경이 아닌 의존성 교체이므로 테스트 수정하지 않음

---

### Step 6: 기존 KakaoOAuthClient 제거
**목표**: 리팩토링 완료 후 기존 클래스 정리

**작업 내용**:
- `KakaoOAuthClient.java` 파일 삭제
- 사용처가 모두 제거되었는지 확인

**확인 사항**:
- 컴파일 에러 없음
- 애플리케이션 정상 구동
- 기존 기능 동작 확인

---

## 4. 작업 순서 요약

1. [Step 1] `OidcParser` 인터페이스 및 `OidcClaims` record 작성
2. [Step 2] `KakaoJwksProvider` 분리
3. [Step 3] `KakaoOidcParser` 분리
4. [Step 4] `KakaoTokenClient` 분리
5. [Step 5] `OAuthLoginService` 의존성 변경
6. [Step 6] 기존 `KakaoOAuthClient` 제거 및 검증

---

## 5. 주의 사항

### 코딩 규칙 준수
- DI는 `@RequiredArgsConstructor` 사용
- 인프라 계층은 TDD 적용하지 않음
- 프레젠테이션 계층은 TDD 적용하지 않음
- 기존 코드 스타일 유지

### 동시성 제어
- `KakaoJwksProvider`의 캐시 갱신은 `synchronized` 블록 사용
- 기존 로직 유지

### 설정 변경 없음
- `application.yml` 변경 없음
- `KakaoOAuthProperties` 필드 변경 없음

### 예외 처리
- 기존 예외 처리 로직 유지
- `KakaoTokenIssueFailed`, `KakaoUserInfoFetchFailed` 등 기존 예외 사용

---

## 6. 검증 방법

### 컴파일 검증
- 빌드 에러 없음
- 의존성 주입 정상 동작

### 기능 검증
- 애플리케이션 정상 구동
- 카카오 로그인 플로우 정상 동작
- OIDC ID Token 파싱 정상 동작

### 코드 리뷰 체크리스트
- 단일 책임 원칙 준수 여부
- 인터페이스 의존 여부
- 기존 기능 보존 여부
- 코드 스타일 일관성

---

## 7. 롤백 계획

문제 발생 시:
1. 생성된 파일 삭제
2. `KakaoOAuthClient` 복원
3. `OAuthLoginService` 의존성 원복
4. Git revert

---

## 8. 완료 기준

- 모든 Step 완료
- 컴파일 에러 없음
- 애플리케이션 정상 구동
- 기존 기능 정상 동작
- 코드 리뷰 통과