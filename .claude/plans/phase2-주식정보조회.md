# Phase 2: 주식 정보 조회 (Week 3-4)

## 2.1 주식 도메인 모델 (TDD)

### 테스트 → 구현 순서

1. [ ] Stock 도메인 모델 테스트 작성
2. [ ] Stock 도메인 모델 최소 구현
3. [ ] 주식 검색 도메인 서비스 테스트 작성
4. [ ] 주식 검색 도메인 서비스 최소 구현

### 대상 파일
- `stock/domain/model/Stock.java` (테스트 먼저)
- `stock/domain/service/StockDomainService.java` (테스트 먼저)

---

## 2.2 주식 API 클라이언트 (TDD)

### 테스트 → 구현 순서

1. [ ] Alpha Vantage API 클라이언트 테스트 작성
2. [ ] Alpha Vantage API 클라이언트 최소 구현
3. [ ] Finnhub API 클라이언트 테스트 작성
4. [ ] Finnhub API 클라이언트 최소 구현
5. [ ] 금융위원회 API 클라이언트 테스트 작성
6. [ ] 금융위원회 API 클라이언트 최소 구현

### 대상 파일
- `stock/infrastructure/api/alphavantage/AlphaVantageClient.java` (테스트 먼저)
- `stock/infrastructure/api/finnhub/FinnhubClient.java` (테스트 먼저)
- `stock/infrastructure/api/fss/FssStockClient.java` (테스트 먼저)

---

## 2.3 주식 API 폴백 전략 (TDD)

### 테스트 → 구현 순서

1. [ ] API 폴백 서비스 테스트 작성 (첫 번째 API 실패 시 두 번째 호출)
2. [ ] API 폴백 서비스 최소 구현
3. [ ] API 사용량 추적 테스트 작성
4. [ ] API 사용량 추적 최소 구현

### 대상 파일
- `stock/infrastructure/api/fallback/StockApiFallbackService.java` (테스트 먼저)

---

## 2.4 주식 조회 유스케이스 (TDD)

### 테스트 → 구현 순서

1. [ ] 주식 조회 유스케이스 테스트 작성
2. [ ] 주식 조회 유스케이스 최소 구현
3. [ ] 주식 검색 유스케이스 테스트 작성
4. [ ] 주식 검색 유스케이스 최소 구현

### 대상 파일
- `stock/application/StockQueryService.java` (테스트 먼저)
- `stock/presentation/StockController.java` (테스트 먼저)

---

## API 제약 사항

### 무료 API 사용량 제한
- **Alpha Vantage**: 일 25건, 분당 5건
- **Finnhub**: 분당 60건
- **금융위원회**: 일 1,000건

### 폴백 전략
- 여러 API를 순차적으로 시도
- 무료 사용량 초과 시 다음 API로 자동 전환
- Circuit Breaker 패턴 적용 고려

---

## 제약 사항

- **DDD 계층형 구조 준수 필수**
- **Entity 연관관계 사용 금지** (ID 기반 참조만)
- **domain 계층에 Spring/JPA 의존성 금지**
- **@Transactional은 application 계층에서만 사용**
- **테스트 실패 → 최소 구현 → 테스트 성공 순서 준수**
- **Mock은 테스트 대상의 경계에서만 사용**
- **API 키 관리 보안 (환경 변수 사용)**

---

## 참고 문서

- [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [STOCK-MARKET-PROJECT.md](../../STOCK-MARKET-PROJECT.md)
- [CLAUDE.md](../../CLAUDE.md)

---

**작성일**: 2026-01-19
**작성자**: Claude Code