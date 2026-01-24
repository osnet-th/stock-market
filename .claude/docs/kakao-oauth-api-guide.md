## 카카오톡 oauth 로그인 연계

## REST API 연결 방식

환경 변수로 설정 필요:
- `KAKAO_CLIENT_ID`: Kakao REST API Key
- `KAKAO_CLIENT_SECRET`: Kakao Client Secret Key
- `KAKAO_REDIRECT_URI`: Redirect URI (기본값: http://localhost:8080/oauth/kakao)

---

## Spring Boot 구현 방법 (설명형)

### 작성일
2026-01-24

### 구현 범위
- Kakao OAuth 인가 코드 발급, 토큰 발급, 사용자 정보 조회 흐름 정리
- Spring Boot 애플리케이션에서 필요한 구성 요소와 책임 분리 안내
- 보안 및 운영 주의사항

### 전체 흐름 요약
- 클라이언트가 카카오 인가 페이지로 이동해 인가 코드를 받는다
- 백엔드는 인가 코드를 이용해 카카오 토큰을 발급받는다
- 발급받은 액세스 토큰으로 사용자 정보를 조회한다
- 조회 결과로 내부 사용자 식별, 가입/로그인 처리, 세션/JWT 발급을 진행한다

### 필요한 구성 요소
- 인증 엔드포인트: 인가 코드 수신 및 처리 시작
- OAuth 전용 서비스: 토큰 발급과 사용자 정보 조회 책임 분리
- 사용자 도메인 서비스: 회원 존재 여부 확인, 신규 가입 처리
- 토큰/세션 발급 모듈: 내부 인증 수단(JWT 등) 발급
- 외부 API 클라이언트: 카카오 REST API 호출 전담

### 인가 코드 발급 단계
- 카카오 인가 URL에 클라이언트 ID와 Redirect URL을 전달한다
- 사용자 동의 완료 후 Redirect URL로 인가 코드가 전달된다
- 전달된 인가 코드는 일회성으로 사용되므로 즉시 토큰 발급에 사용한다

### 토큰 발급 단계
- 카카오 토큰 발급 API에 인가 코드, 클라이언트 ID, 클라이언트 Secret, Redirect URL을 전달한다
- 응답으로 액세스 토큰과 리프레시 토큰을 받는다
- 토큰 유효기간과 갱신 정책을 내부 정책과 맞춘다

### 사용자 정보 조회 단계
- 액세스 토큰으로 사용자 정보 조회 API를 호출한다
- 카카오 고유 사용자 식별자와 프로필/이메일 정보를 확인한다
- 내부 사용자 매핑 규칙을 적용한다

### 내부 로그인/가입 처리
- 카카오 사용자 식별자 기반으로 기존 회원 여부를 판단한다
- 신규 회원이면 내부 가입 절차를 수행한다
- 로그인 성공 시 내부 인증 토큰(JWT 등)을 발급한다

### 예외 처리 기준
- 인가 코드 만료/중복 사용 시 재시도 안내
- 토큰 발급 실패 시 상태 코드와 에러 메시지 기록
- 사용자 정보 조회 실패 시 토큰 재발급 또는 로그인 실패 처리
- 카카오 응답 지연/오류 대비 타임아웃 및 재시도 정책 고려

### 보안 및 운영 주의사항
- 클라이언트 Secret은 서버에서만 사용한다
- Redirect URL은 카카오 개발자 콘솔 설정과 정확히 일치해야 한다
- 로그에 인가 코드, 액세스 토큰, 리프레시 토큰을 남기지 않는다
- 외부 API 호출 실패에 대비한 모니터링과 알림을 준비한다

### 테스트 방향
- OAuth 서비스의 토큰 발급/사용자 조회를 단위 테스트로 검증한다
- 외부 API 호출은 Mock 처리로 경계만 검증한다
- 도메인 규칙(신규 가입/기존 로그인 분기)을 단위 테스트로 분리한다

---

## 코드 예시 (Java Spring Boot)

### application.yml 예시

```yaml
kakao:
  oauth:
    client-id: 9e30f4bba41efa94c5256b8f981c2da9
    client-secret: OPXDUa5oZsvKzWdk1hCeBc2ju9Gc8OiG
    redirect-uri: http://localhost:8080/oauth/kakao
    authorize-uri: https://kauth.kakao.com/oauth/authorize
    token-uri: https://kauth.kakao.com/oauth/token
    user-info-uri: https://kapi.kakao.com/v2/user/me
```

### 설정 바인딩

```java
package com.stockmarket.auth.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao.oauth")
public record KakaoOAuthProperties(
    String clientId,
    String clientSecret,
    String redirectUri,
    String authorizeUri,
    String tokenUri,
    String userInfoUri
) {}
```

### 컨트롤러

```java
package com.stockmarket.auth.kakao;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth")
public class OAuthController {
    private final KakaoOAuthService kakaoOAuthService;

    public KakaoOAuthController(KakaoOAuthService kakaoOAuthService) {
        this.kakaoOAuthService = kakaoOAuthService;
    }

    @GetMapping("/kakao")
    public ResponseEntity<LoginResult> kakaoCallback(@RequestParam("code") String code) {
        LoginResult result = kakaoOAuthService.loginWithKakao(code);
        return ResponseEntity.ok(result);
    }
    
//    Oauth Google 구현 예정
//    @GetMapping("/google")
//    public ResponseEntity<LoginResult> kakaoCallback(@RequestParam("code") String code) {
//        LoginResult result = kakaoOAuthService.loginWithKakao(code);
//        return ResponseEntity.ok(result);
//    }
}
```

### 서비스

```java
package com.stockmarket.auth.kakao;

import org.springframework.stereotype.Service;

@Service
public class KakaoOAuthService {
    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserAuthService userAuthService;
    private final TokenIssuer tokenIssuer;

    public KakaoOAuthService(
        KakaoOAuthClient kakaoOAuthClient,
        UserAuthService userAuthService,
        TokenIssuer tokenIssuer
    ) {
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.userAuthService = userAuthService;
        this.tokenIssuer = tokenIssuer;
    }

    public LoginResult loginWithKakao(String code) {
        KakaoTokenResponse token = kakaoOAuthClient.issueToken(code);
        KakaoUserResponse user = kakaoOAuthClient.getUserInfo(token.accessToken());

        UserAccount account = userAuthService.resolveKakaoUser(user.id(), user.email(), user.nickname());
        String jwt = tokenIssuer.issue(account.id());

        return new LoginResult(jwt, account.id(), account.isNew());
    }
}
```

### 외부 API 클라이언트

```java
package com.stockmarket.auth.kakao;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoOAuthClient {
    private final RestClient restClient;
    private final KakaoOAuthProperties properties;

    public KakaoOAuthClient(RestClient.Builder builder, KakaoOAuthProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    public KakaoTokenResponse issueToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());
        body.add("redirect_uri", properties.redirectUri());
        body.add("code", code);

        return restClient.post()
            .uri(properties.tokenUri())
            .headers(h -> h.addAll(headers))
            .body(new HttpEntity<>(body, headers))
            .retrieve()
            .body(KakaoTokenResponse.class);
    }

    public KakaoUserResponse getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        return restClient.post()
            .uri(properties.userInfoUri())
            .headers(h -> h.addAll(headers))
            .retrieve()
            .body(KakaoUserResponse.class);
    }
}
```

### 응답 DTO

```java
package com.stockmarket.auth.kakao;

public record KakaoTokenResponse(
    String accessToken,
    String tokenType,
    String refreshToken,
    Long expiresIn,
    Long refreshTokenExpiresIn
) {}
```

```java
package com.stockmarket.auth.kakao;

public record KakaoUserResponse(
    Long id,
    String email,
    String nickname
) {}
```

### 내부 인증 결과 모델 예시

```java
package com.stockmarket.auth.kakao;

public record LoginResult(
    String accessToken,
    Long userId,
    boolean isNew
) {}
```

### 내부 사용자 인증 서비스/토큰 발급기 예시

```java
package com.stockmarket.auth.kakao;

public interface UserAuthService {
    UserAccount resolveKakaoUser(Long kakaoId, String email, String nickname);
}
```

```java
package com.stockmarket.auth.kakao;

public interface TokenIssuer {
    String issue(Long userId);
}
```

```java
package com.stockmarket.auth.kakao;

public record UserAccount(Long id, boolean isNew) {}
```

