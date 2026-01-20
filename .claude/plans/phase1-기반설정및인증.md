# Phase 1: 기반 설정 및 인증 (Week 1-2)

## 1.1 프로젝트 초기 설정

### 체크리스트
- [완료] Spring Boot 프로젝트 생성
- [ ] 기본 의존성 추가 (Spring Web, JPA, Security, etc.)
- [ ] 패키지 구조 생성
- [ ] 데이터베이스 설정 (H2 개발용, PostgreSQL 운영용)

---

## 1.2 JWT 인증 구현 (TDD)

### 테스트 → 구현 순서

1. [ ] JWT 토큰 생성 테스트 작성
2. [ ] JWT 토큰 생성 최소 구현
3. [ ] JWT 토큰 검증 테스트 작성
4. [ ] JWT 토큰 검증 최소 구현
5. [ ] JWT 토큰 갱신 테스트 작성
6. [ ] JWT 토큰 갱신 최소 구현

### 대상 파일
- `user/domain/model/User.java`
- `user/domain/service/JwtTokenService.java` (테스트 먼저)
- `user/application/AuthService.java` (테스트 먼저)

---

## 1.3 OAuth 통합 (TDD)

### 테스트 → 구현 순서

1. [ ] 카카오 OAuth 클라이언트 테스트 작성 (Mock)
2. [ ] 카카오 OAuth 클라이언트 최소 구현
3. [ ] 구글 OAuth 클라이언트 테스트 작성 (Mock)
4. [ ] 구글 OAuth 클라이언트 최소 구현
5. [ ] OAuth 로그인 유스케이스 테스트 작성
6. [ ] OAuth 로그인 유스케이스 최소 구현

### 대상 파일
- `user/infrastructure/oauth/kakao/KakaoOAuthClient.java` (테스트 먼저)
- `user/infrastructure/oauth/google/GoogleOAuthClient.java` (테스트 먼저)
- `user/application/OAuthLoginService.java` (테스트 먼저)
- `user/presentation/AuthController.java` (테스트 먼저)

---

## 제약 사항

- **DDD 계층형 구조 준수 필수**
- **Entity 연관관계 사용 금지** (ID 기반 참조만)
- **domain 계층에 Spring/JPA 의존성 금지**
- **@Transactional은 application 계층에서만 사용**
- **테스트 실패 → 최소 구현 → 테스트 성공 순서 준수**
- **Mock은 테스트 대상의 경계에서만 사용**
- **domain 계층은 Mock 없이 순수 테스트**

---

## 참고 문서

- [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [STOCK-MARKET-PROJECT.md](../../STOCK-MARKET-PROJECT.md)
- [CLAUDE.md](../../CLAUDE.md)

---

**작성일**: 2026-01-19
**작성자**: 이태형