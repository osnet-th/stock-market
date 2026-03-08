# JwtAuthenticationEntryPoint 예시

**위치**: `infrastructure/security/jwt/JwtAuthenticationEntryPoint.java`

```java
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\"}");
    }
}
```

## 설계 포인트

- 인증 실패 시 403 대신 401 JSON 응답 반환
- 클라이언트가 토큰 갱신 또는 재로그인 필요 여부를 판단할 수 있도록 함