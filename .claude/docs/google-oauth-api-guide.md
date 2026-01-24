작성일: 2026-01-24


# Google oauth 설정 값

환경 변수로 설정 필요:
- `GOOGLE_CLIENT_ID`: Google OAuth Client ID
- `GOOGLE_CLIENT_SECRET`: Google OAuth Client Secret
- `GOOGLE_REDIRECT_URI`: Redirect URI (기본값: http://localhost:8080/oauth/google)

# Google OAuth API Guide

본 문서는 Google OAuth 2.0 (Authorization Code + PKCE 권장) 기반 로그인 연동을 위한 최소 정보와 예시를 제공합니다.

## 1. 설정 절차

1) Google Cloud Console에서 프로젝트 생성  
2) OAuth 동의 화면 구성  
- 앱 이름, 지원 이메일, 개인정보처리방침 URL 등 설정  
3) OAuth 클라이언트 ID 생성  
- 유형: 웹 애플리케이션  
- 승인된 리디렉션 URI 등록  
  예) `https://api.example.com/auth/google/callback`  
4) 클라이언트 ID / 클라이언트 시크릿 보관  
- 서버 환경 변수로 저장  
  예) `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`

## 2. 승인 URL (Authorization Endpoint)

요청 URL: `https://accounts.google.com/o/oauth2/v2/auth`

필수 파라미터(권장):
- `client_id`: 발급받은 클라이언트 ID
- `redirect_uri`: 승인된 리디렉션 URI
- `response_type`: `code`
- `scope`: `openid email profile`
- `access_type`: `offline` (리프레시 토큰 필요 시)
- `prompt`: `consent` (리프레시 토큰 보장 필요 시)
- `code_challenge`: PKCE 사용 시
- `code_challenge_method`: `S256`
- `state`: CSRF 방지용 난수

예시:
```
GET https://accounts.google.com/o/oauth2/v2/auth
  ?client_id=YOUR_CLIENT_ID
  &redirect_uri=https%3A%2F%2Fapi.example.com%2Fauth%2Fgoogle%2Fcallback
  &response_type=code
  &scope=openid%20email%20profile
  &access_type=offline
  &prompt=consent
  &state=RANDOM_STATE
  &code_challenge=BASE64URL_SHA256
  &code_challenge_method=S256
```

## 3. 토큰 교환 (Token Endpoint)

요청 URL: `https://oauth2.googleapis.com/token`

필수 파라미터(Authorization Code):
- `client_id`
- `client_secret` (PKCE를 쓰더라도 서버에서는 일반적으로 필요)
- `code`
- `redirect_uri`
- `grant_type`: `authorization_code`
- `code_verifier` (PKCE 사용 시)

응답 예시 주요 필드:
- `access_token`
- `expires_in`
- `refresh_token` (옵션)
- `id_token`
- `token_type`

## 4. 사용자 정보 조회

방법 A) OpenID Connect UserInfo 사용  
요청 URL: `https://openidconnect.googleapis.com/v1/userinfo`  
헤더: `Authorization: Bearer ACCESS_TOKEN`

주요 필드:
- `sub` (사용자 고유 ID)
- `email`
- `email_verified`
- `name`
- `picture`

방법 B) ID Token 디코딩  
ID Token(JWT)을 검증 후 클레임에서 사용자 정보 추출  
주의: 반드시 서명 검증 및 issuer/audience 확인

## 5. 구현 코드 (Spring 예시)

### 5.1. 설정 클래스
```
@Configuration
@ConfigurationProperties(prefix = "oauth.google")
public class GoogleOAuthProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
}
```

### 5.2. 승인 URL 생성
```
public String buildAuthUrl(String state, String codeChallenge) {
    return UriComponentsBuilder
        .fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
        .queryParam("client_id", clientId)
        .queryParam("redirect_uri", redirectUri)
        .queryParam("response_type", "code")
        .queryParam("scope", "openid email profile")
        .queryParam("access_type", "offline")
        .queryParam("prompt", "consent")
        .queryParam("state", state)
        .queryParam("code_challenge", codeChallenge)
        .queryParam("code_challenge_method", "S256")
        .build()
        .toUriString();
}
```

### 5.3. 토큰 교환 요청
```
public GoogleTokenResponse exchangeToken(String code, String codeVerifier) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    form.add("redirect_uri", redirectUri);
    form.add("code", code);
    form.add("grant_type", "authorization_code");
    form.add("code_verifier", codeVerifier);

    return restTemplate.postForObject(
        "https://oauth2.googleapis.com/token",
        new HttpEntity<>(form, defaultHeaders()),
        GoogleTokenResponse.class
    );
}
```

### 5.4. 사용자 정보 조회
```
public GoogleUserInfo getUserInfo(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<GoogleUserInfo> response =
        restTemplate.exchange(
            "https://openidconnect.googleapis.com/v1/userinfo",
            HttpMethod.GET,
            entity,
            GoogleUserInfo.class
        );
    return response.getBody();
}
```

## 6. 보안 및 주의사항

- `state`는 반드시 랜덤 값으로 생성하고 서버에서 검증  
- PKCE 권장 (code_challenge / code_verifier)  
- `redirect_uri` 정확히 일치 필요  
- `id_token` 사용 시 issuer/audience 검증 필수  
- 리프레시 토큰 필요 시 `access_type=offline`, `prompt=consent` 설정
