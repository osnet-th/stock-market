# JWT Authentication Filter 설계

## 배경

현재 JWT 토큰 발급/검증 기능은 구현되어 있으나, Security Filter Chain에 JWT 필터가 등록되지 않아
인증이 필요한 API에 접근할 수 없는 상태이다.

## 핵심 결정

- `OncePerRequestFilter`를 상속하여 JWT 필터 구현
- `UsernamePasswordAuthenticationFilter` 앞에 필터 배치
- 토큰 검증 실패 시 SecurityContext를 설정하지 않고 다음 필터로 위임 (Security가 403 처리)
- 만료/무효 토큰은 401 응답을 위한 `AuthenticationEntryPoint` 구현

## 구현 컴포넌트

### 1. JwtAuthenticationFilter

- `Authorization: Bearer {token}` 헤더에서 토큰 추출
- `JwtTokenProvider.validateToken()`으로 검증
- 유효한 경우 userId, role을 SecurityContext에 설정
- permitAll 경로는 필터 통과 (토큰 없어도 OK)

### 2. JwtAuthenticationEntryPoint

- 인증 실패 시 401 JSON 응답 반환

### 3. ProdSecurityConfig 수정

- JWT 필터 등록
- AuthenticationEntryPoint 등록

## 주의사항

- SIGNING_USER 권한은 `/api/auth/signup` 등 제한된 엔드포인트만 허용
- Access Token만 필터에서 검증 (Refresh Token은 `/api/auth/refresh`에서 별도 처리)
- DB 조회 없이 토큰 claims만으로 인증 처리 (성능)

## 작업 리스트

- [x] `JwtAuthenticationFilter` 구현 → [예시](examples/JwtAuthenticationFilter-example.md)
- [x] `JwtAuthenticationEntryPoint` 구현 → [예시](examples/JwtAuthenticationEntryPoint-example.md)
- [x] `ProdSecurityConfig` 수정 (필터 + EntryPoint 등록) → [예시](examples/ProdSecurityConfig-example.md)