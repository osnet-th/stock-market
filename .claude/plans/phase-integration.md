# Phase 6: 통합 및 최적화 (Week 10)

## 6.1 통합 테스트

### 체크리스트

- [ ] 전체 플로우 검증
- [ ] API 폴백 시나리오 테스트
- [ ] 인증/인가 통합 테스트

### 전체 플로우 검증
- 사용자 로그인 → 주식 조회 → 뉴스 조회 → 게시글 작성 → 관심 등록
- OAuth 로그인 플로우 (카카오, 구글)
- JWT 토큰 생성 및 검증
- 권한 검증 (게시글 수정/삭제)

### API 폴백 시나리오
- 주식 API 폴백: Alpha Vantage 실패 → Finnhub 시도 → 금융위원회 시도
- 뉴스 API 폴백: 딥서치 실패 → NewsAPI.org 시도 → 국제방송교류재단 시도
- Circuit Breaker 동작 확인

### 인증/인가 통합 테스트
- 미인증 사용자의 보호된 API 접근 차단
- 잘못된 JWT 토큰 검증
- 토큰 만료 처리
- 권한 없는 사용자의 게시글 수정/삭제 차단

---

## 6.2 성능 최적화

### 체크리스트

- [ ] 캐싱 적용 (Caffeine)
- [ ] 페이징 처리
- [ ] 비동기 처리 (외부 API 호출)

### 캐싱 적용

**대상:**
- 자주 조회되는 주식 정보
- 뉴스 검색 결과
- 사용자 프로필 정보

**구현:**
```java
@Cacheable(value = "stocks", key = "#stockCode")
public Stock findByCode(String stockCode) {
    // 주식 정보 조회
}

@CacheEvict(value = "stocks", key = "#stockCode")
public void invalidateStockCache(String stockCode) {
    // 캐시 무효화
}
```

**설정:**
- Caffeine Cache 사용
- TTL 설정 (예: 주식 정보 5분, 뉴스 10분)
- 최대 캐시 크기 설정

### 페이징 처리

**대상:**
- 게시글 목록 조회
- 뉴스 목록 조회
- 관심 목록 조회

**구현:**
```java
public Page<Post> findPosts(Pageable pageable) {
    // 페이징 처리된 게시글 조회
}
```

**설정:**
- 기본 페이지 크기: 20
- 최대 페이지 크기: 100

### 비동기 처리

**대상:**
- 외부 API 호출 (주식, 뉴스)
- 대량 데이터 처리

**구현:**
```java
@Async
public CompletableFuture<List<News>> fetchNewsAsync(String stockCode) {
    // 비동기 뉴스 조회
}
```

**설정:**
- ThreadPoolTaskExecutor 설정
- 코어 스레드 수: 5
- 최대 스레드 수: 10

---

## 6.3 문서화

### 체크리스트

- [ ] API 문서 (Swagger/OpenAPI)
- [ ] README 작성
- [ ] 배포 가이드

### API 문서 (Swagger/OpenAPI)

**포함 내용:**
- 모든 REST API 엔드포인트
- 요청/응답 스키마
- 인증 방법 (JWT)
- 에러 코드 및 메시지

**Swagger 설정:**
```java
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("주식 & 뉴스 통합 플랫폼 API")
                .version("1.0.0")
                .description("주식 정보, 뉴스, 커뮤니티 기능을 제공하는 API"));
    }
}
```

### README 작성

**포함 내용:**
1. 프로젝트 개요
2. 기술 스택
3. 빌드 및 실행 방법
4. 환경 변수 설정
5. API 엔드포인트 목록
6. 아키텍처 다이어그램
7. 기여 가이드

### 배포 가이드

**포함 내용:**
1. 환경 요구사항
   - Java 17+
   - PostgreSQL 14+
   - Redis (선택사항)

2. 환경 변수 설정
   ```bash
   # Database
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/stock_market
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=password

   # JWT
   JWT_SECRET_KEY=your-secret-key
   JWT_EXPIRATION_TIME=3600000

   # OAuth
   OAUTH_KAKAO_CLIENT_ID=your-kakao-client-id
   OAUTH_KAKAO_CLIENT_SECRET=your-kakao-client-secret
   OAUTH_GOOGLE_CLIENT_ID=your-google-client-id
   OAUTH_GOOGLE_CLIENT_SECRET=your-google-client-secret

   # API Keys
   ALPHA_VANTAGE_API_KEY=your-api-key
   FINNHUB_API_KEY=your-api-key
   NEWS_API_KEY=your-api-key
   ```

3. 빌드 및 배포
   ```bash
   ./gradlew clean build
   java -jar build/libs/stock-market-0.0.1-SNAPSHOT.jar
   ```

4. Docker 배포 (선택사항)
   ```dockerfile
   FROM openjdk:17-jdk-slim
   COPY build/libs/*.jar app.jar
   ENTRYPOINT ["java", "-jar", "/app.jar"]
   ```

---

## 성능 모니터링

### 모니터링 항목
- API 응답 시간
- 외부 API 호출 성공률
- 캐시 히트율
- 데이터베이스 쿼리 성능
- JVM 메모리 사용량

### 도구
- Spring Boot Actuator
- Micrometer
- Prometheus (선택사항)
- Grafana (선택사항)

---

## 참고 문서

- [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [STOCK-MARKET-PROJECT.md](../../STOCK-MARKET-PROJECT.md)
- [CLAUDE.md](../../CLAUDE.md)

---

**작성일**: 2026-01-19
**작성자**: Claude Code