# ProdSecurityConfig 수정 예시

**위치**: `infrastructure/security/config/ProdSecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@Profile("prod")
@RequiredArgsConstructor
public class ProdSecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // 인증 실패 시 401 응답
            .exceptionHandling(exception ->
                exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/login.html", "/signup.html").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            // JWT 필터 등록
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    // corsConfigurationSource()는 기존 코드 유지
}
```

## 변경 포인트

- `JwtTokenProvider`, `JwtAuthenticationEntryPoint` 주입
- `exceptionHandling`에 `JwtAuthenticationEntryPoint` 등록
- `addFilterBefore`로 JWT 필터 등록
- 기존 TODO 주석 제거