# Kakao OAuth Client 클래스 분리 설계

**작성일**: 2026-01-27
**수정일**: 2026-01-27

---

## 1. 현재 문제점

### 현재 구조
`KakaoOAuthClient` 클래스가 **3가지 책임**을 동시에 가지고 있음:

1. **토큰 발급**: 카카오 인증 코드를 통해 Access Token 및 ID Token 발급
2. **OIDC 토큰 파싱**: ID Token을 파싱하고 검증하여 사용자 정보 추출
3. **JWKS 관리**: 공개키 캐싱 및 갱신 로직

### 문제점
- **단일 책임 원칙(SRP) 위반**: 하나의 클래스가 너무 많은 책임을 가짐
- **테스트 복잡도 증가**: JWKS 캐싱 로직과 토큰 발급 로직이 혼재되어 단위 테스트 작성 어려움
- **재사용성 저하**: OIDC 파싱 로직을 다른 OAuth 제공자에서 재사용 불가
- **코드 가독성 저하**: 200줄이 넘는 단일 클래스

---

## 2. 분리 설계

### 2.1 클래스 분리 전략

#### (1) `KakaoTokenClient`
**책임**: 카카오 토큰 발급 전담

**주요 메서드**:
- `issueToken(String code)`: 인증 코드로 토큰 발급

**의존성**:
- `KakaoClient`: RestClient 제공
- `KakaoOAuthProperties`: OAuth 설정 정보

**특징**:
- HTTP 통신만 담당
- 단순하고 명확한 책임
- Mock 테스트 용이

---

#### (2) `OidcParser` (인터페이스)
**책임**: OIDC ID Token 파싱 계약 정의

**주요 메서드**:
```java
public interface OidcParser {
    OidcClaims parseIdToken(String idToken);
}
```

**반환 타입**:
```java
public record OidcClaims(
    String issuer,    // 토큰 발급자
    String subject,   // 사용자 고유 ID
    String email      // 사용자 이메일
) {}
```

**특징**:
- 다양한 OAuth Provider에서 재사용 가능한 표준 인터페이스
- 구현체: `KakaoOidcParser`, `GoogleOidcParser` 등
- 의존성 역전 원칙(DIP) 적용
- 도메인 계층에 위치 (infrastructure에서 구현)

---

#### (3) `KakaoOidcParser` (구현체)
**책임**: 카카오 OIDC ID Token 파싱 및 검증 구현

**주요 메서드**:
- `parseIdToken(String idToken)`: ID Token 파싱하여 사용자 정보 추출

**의존성**:
- `JwksProvider`: 공개키 제공
- `KakaoOAuthProperties`: Issuer, Audience 검증 정보
- `ObjectMapper`: JWT 헤더 파싱

**특징**:
- `OidcParser` 인터페이스 구현
- JWT 검증 로직에만 집중
- JWKS 관리는 외부 의존성으로 분리
- 카카오 OIDC 스펙 준수

---

#### (4) `KakaoJwksProvider`
**책임**: 카카오 JWKS 공개키 관리 및 캐싱

**주요 메서드**:
- `getPublicKey(String kid)`: kid로 공개키 조회 (캐시 우선)
- `refreshKeys()`: JWKS 갱신

**의존성**:
- `KakaoClient`: RestClient 제공

**특징**:
- 캐싱 로직 전담 (TTL 6시간)
- Thread-safe 구현
- 공개키 조회 및 갱신 로직 캡슐화
- 다른 OIDC Provider로 확장 가능

---

### 2.2 클래스 다이어그램

```
┌─────────────────────┐
│ KakaoTokenClient    │
├─────────────────────┤
│ - kakaoClient       │
│ - properties        │
├─────────────────────┤
│ + issueToken(code)  │
└─────────────────────┘

┌─────────────────────┐
│  <<interface>>      │
│   OidcParser        │
├─────────────────────┤
│ + parseIdToken()    │
└─────────────────────┘
           △
           │ 구현
           │
┌─────────────────────┐
│ KakaoOidcParser     │
├─────────────────────┤
│ - jwksProvider      │
│ - properties        │
│ - objectMapper      │
├─────────────────────┤
│ + parseIdToken()    │
│ - extractKid()      │
│ - validateConfig()  │
└─────────────────────┘
           │
           │ 의존
           ↓
┌─────────────────────┐
│ KakaoJwksProvider   │
├─────────────────────┤
│ - kakaoClient       │
│ - jwksCache         │
├─────────────────────┤
│ + getPublicKey(kid) │
│ - refreshKeys()     │
│ - toPublicKey()     │
└─────────────────────┘
```

---

### 2.3 협력 구조

```
Application Layer
       │
       ├──→ KakaoTokenClient    ──→  카카오 토큰 발급
       │
       └──→ OidcParser (interface)
                  │
                  │ 구현
                  ↓
            KakaoOidcParser    ──→  ID Token 파싱/검증
                  │
                  │ 의존
                  ↓
            KakaoJwksProvider  ──→  공개키 제공 (캐싱)
```

**의존성 주입 예시**:
```java
@Service
public class OAuthLoginService {
    private final KakaoTokenClient tokenClient;
    private final OidcParser oidcParser;  // 인터페이스에 의존

    public UserInfo login(String code) {
        KakaoTokenResponse token = tokenClient.issueToken(code);
        OidcClaims claims = oidcParser.parseIdToken(token.idToken());
        // ...
    }
}
```

---

## 3. 기대 효과

### 3.1 단일 책임 원칙 준수
- 각 클래스가 명확한 하나의 책임만 가짐
- 변경 이유가 단일화됨

### 3.2 테스트 용이성 향상
- 각 클래스를 독립적으로 테스트 가능
- Mock 의존성이 명확해짐
- JWKS 캐싱 로직을 독립적으로 테스트 가능

### 3.3 재사용성 증가
- **`OidcParser` 인터페이스**를 통해 다른 OAuth Provider(Google, Naver 등)에서 재사용 가능
- Application Layer는 구체 구현체가 아닌 인터페이스에 의존하여 Provider 교체 용이
- `JwksProvider`도 향후 인터페이스로 추상화 가능

### 3.4 확장성 개선
- 새로운 OAuth Provider 추가 시 기존 코드 영향 최소화
- JWKS 캐싱 전략 변경 시 `KakaoJwksProvider`만 수정

### 3.5 코드 가독성 향상
- 각 클래스가 50~70줄 내외로 간결해짐
- 책임이 명확하여 이해하기 쉬움

---

## 4. 마이그레이션 전략

### 4.1 단계별 진행

1. **`OidcParser` 인터페이스 정의**
   - 도메인 계층에 인터페이스 작성
   - 반환 타입 `OidcClaims` 정의

2. **`KakaoJwksProvider` 추출**
   - JWKS 관련 로직 먼저 분리
   - 테스트 작성

3. **`KakaoOidcParser` 추출**
   - `OidcParser` 인터페이스 구현
   - ID Token 파싱 로직 분리
   - `KakaoJwksProvider` 의존성 주입
   - 테스트 작성

4. **`KakaoTokenClient` 추출**
   - 토큰 발급 로직 분리
   - 테스트 작성

5. **기존 `KakaoOAuthClient` 제거 또는 Facade로 전환**
   - 기존 사용처 리팩토링 (`OidcParser` 인터페이스 의존으로 변경)
   - 또는 Facade 패턴으로 래핑하여 호환성 유지

### 4.2 TDD 순서

각 클래스 추출 시:
1. 테스트 작성 (실패)
2. 최소 구현
3. 테스트 통과
4. 리팩토링

---

## 5. 주의 사항

- **Thread-safety**: `KakaoJwksProvider`의 캐시 갱신 로직은 동시성 제어 필요
- **캐시 TTL**: 현재 6시간 유지
- **에러 처리**: 각 클래스에서 적절한 예외 처리
- **기존 코드 호환성**: 점진적 마이그레이션으로 기존 코드 영향 최소화

---

## 6. 향후 확장 가능성

### 6.1 다양한 OAuth Provider 지원

이미 설계에 포함된 `OidcParser` 인터페이스를 활용하여 다른 Provider 추가 가능:

```
OidcParser (interface) ✓ 초기 설계에 포함
  ├── KakaoOidcParser    ✓ 초기 구현
  ├── GoogleOidcParser   (향후 추가)
  └── NaverOidcParser    (향후 추가)
```

추가 확장:
```
JwksProvider (interface)  (향후 추상화)
  ├── KakaoJwksProvider
  ├── GoogleJwksProvider
  └── NaverJwksProvider
```

### 6.2 범용 OIDC 라이브러리 전환
- Spring Security OAuth2 Client 고려
- Nimbus JOSE + JWT 라이브러리 활용

---

## 7. 결론

현재 `KakaoOAuthClient`를 **인터페이스 1개 + 구현 클래스 3개로 분리**하여:

**구조**:
- `OidcParser` (인터페이스)
- `KakaoTokenClient` (토큰 발급)
- `KakaoOidcParser` (OIDC 파싱 구현체)
- `KakaoJwksProvider` (JWKS 관리)

**달성 목표**:
- **단일 책임 원칙** 준수
- **의존성 역전 원칙** 적용 (인터페이스 의존)
- **테스트 용이성** 향상
- **재사용성 및 확장성** 개선

TDD 방식으로 점진적 리팩토링을 진행하여 안전하게 구조 개선을 수행합니다.