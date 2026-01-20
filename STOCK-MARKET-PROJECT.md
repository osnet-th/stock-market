# 주식 & 뉴스 통합 플랫폼 작업 계획서

## 📋 1. 프로젝트 개요

### 프로젝트 목표
주식 정보와 관련 뉴스를 조회하고, 사용자 커멘트/게시글을 저장할 수 있는 통합 플랫폼 구축

### 핵심 기능
1. 주식 정보 조회
2. 주식 관련 뉴스 조회
3. 주식/뉴스에 대한 커멘트/게시글 작성
4. JWT 기반 인증 (카카오톡, 구글 OAuth)
5. 주식/뉴스 관심/즐겨찾기 등록
6. 여러 API 활용 및 폴백(Fallback) 처리
7. 주식 거래 내역 관리 (매수/매도 기록, 포트폴리오 조회, 수익률 계산)

---

## 🎯 2. 기능 요구사항

### 2.1 인증 및 사용자 관리
- JWT 토큰 기반 인증
- 카카오톡 OAuth 2.0 로그인
- 구글 OAuth 2.0 로그인
- 사용자 프로필 관리

### 2.2 주식 정보 조회
- 실시간 주식 시세 조회
- 주식 상세 정보 조회
- 주식 검색 기능
- 여러 주식 API를 통한 데이터 조회 및 폴백 처리

### 2.3 뉴스 조회
- 특정 주식 관련 뉴스 조회
- 뉴스 검색 기능
- 여러 뉴스 API를 통한 데이터 조회 및 폴백 처리

### 2.4 커뮤니티 기능
- 주식에 대한 커멘트/게시글 작성
- 커멘트/게시글 조회, 수정, 삭제
- 사용자별 게시글 조회

### 2.5 관심/즐겨찾기
- 주식 관심 등록/해제
- 뉴스 즐겨찾기 등록/해제
- 관심 주식 목록 조회
- 즐겨찾기 뉴스 목록 조회

### 2.6 주식 거래 내역 관리
- 매수 기록 등록 (주식 코드, 매수가, 수량, 매수일)
- 매도 기록 등록 (주식 코드, 매도가, 수량, 매도일)
- 사용자별 거래 내역 조회
- 주식별 거래 내역 조회
- 포트폴리오 현황 조회 (보유 주식, 평균 매수가, 보유 수량)
- 수익률 계산 (현재가 기준 실현/미실현 손익)
- 거래 내역 수정/삭제

---

## 🔌 3. 사용 가능한 무료 API 목록

### 3.1 주식 API

#### 국내 API
1. **금융위원회 주식시세정보** ([공공데이터포털](https://www.data.go.kr/data/15094808/openapi.do))
   - 한국거래소 실시간 주식 시세
   - 시가, 종가, 고가, 저가, 거래량 제공
   - 무료 제공량: 일 1,000건

2. **Kiwoom Open API** ([키움증권](https://www.kiwoom.com/h/customer/download/VOpenApiInfoView))
   - 국내 증권사 API
   - 실시간 시세 및 거래 정보
   - 개발자 계정 필요

#### 해외 API
3. **Alpha Vantage** ([alphavantage.co](https://www.alphavantage.co/))
   - 실시간 및 과거 주식 데이터
   - JSON 및 CSV 형식 지원
   - 무료 제공량: 일 25건 (분당 5건)

4. **Finnhub** ([finnhub.io](https://finnhub.io/))
   - 실시간 주식 시세
   - 무료 제공량: 분당 60건

5. **Marketstack** ([marketstack.com](https://marketstack.com/))
   - 실시간 및 과거 시장 데이터
   - 무료 제공량: 월 100건

6. **Yahoo Finance API**
   - 인기 있는 금융 데이터 소스
   - 무료 티어 제한 있음

7. **Massive** ([massive.com](https://massive.com/))
   - 실시간 및 과거 틱 데이터
   - REST 및 WebSocket 지원
   - 무료 티어 제공

### 3.2 뉴스 API

#### 국내 API
1. **딥서치 뉴스 API** ([news.deepsearch.com](https://news.deepsearch.com/))
   - 국내 150개, 해외 50개 언론사 뉴스
   - 일 30,000건 뉴스 수집
   - 검색, 실시간 이슈, Q&A 기능
   - 무료 API 키 제공

2. **국제방송교류재단 뉴스기사API** ([공공데이터포털](https://www.data.go.kr/data/15108015/openapi.do))
   - 아리랑 국제방송 뉴스
   - 기사 제목, 본문, 스크립트, URL, 썸네일, 방송일 제공
   - REST API

3. **한국언론진흥재단**
   - 뉴스 분류 및 검색 기능

#### 해외 API
4. **NewsAPI.org** ([newsapi.org](https://newsapi.org/))
   - 전 세계 150,000개 이상 뉴스 소스
   - JSON 형식 검색 결과
   - 한국 뉴스 포함
   - 무료 티어: localhost만 사용 가능 (배포 불가)
   - 개발용으로만 활용

---

## 🏗️ 4. 아키텍처 설계

본 프로젝트의 아키텍처 설계 상세 내용은 **[ARCHITECTURE.md](ARCHITECTURE.md)** 문서를 참고하세요.

주요 내용:
- 계층 구조 (DDD 스타일)
- 도메인 모델 식별
- 패키지 구조 (도메인별 분리)
- 주요 설계 원칙 (Entity 연관관계 금지, ID 기반 참조, N+1 문제 해결 등)
- 레이어별 책임 정의
- DTO / Entity / Domain 경계 규칙
- 트랜잭션 경계 규칙

---

## 📝 5. 구현 계획 (TDD 순서)

각 Phase별 상세 구현 계획은 아래 문서를 참고하세요:

- **[기반 설정 및 인증](.claude/plans/phase-auth-setup.md)**
  - 프로젝트 초기 설정
  - JWT 인증 구현
  - OAuth 통합 (카카오, 구글)

- **[주식 정보 조회](.claude/plans/phase-stock-info.md)**
  - 주식 도메인 모델
  - 주식 API 클라이언트 (Alpha Vantage, Finnhub, 금융위원회)
  - API 폴백 전략
  - 주식 조회 유스케이스

- **[뉴스 조회](.claude/plans/phase-news.md)**
  - 뉴스 도메인 모델
  - 뉴스 API 클라이언트 (딥서치, NewsAPI.org, 국제방송교류재단)
  - API 폴백 전략
  - 뉴스 조회 유스케이스

- **[커뮤니티 기능](.claude/plans/phase-community.md)**
  - 게시글 도메인 모델
  - 게시글 저장소 구현
  - 게시글 관리 유스케이스 (작성, 수정, 삭제, 조회)

- **[관심/즐겨찾기](.claude/plans/phase-favorites.md)**
  - 관심/즐겨찾기 도메인 모델
  - 저장소 구현
  - 관심/즐겨찾기 유스케이스 (등록, 해제, 조회)

- **[주식 거래 내역 관리](.claude/plans/phase-trade-history.md)**
  - 거래 내역 도메인 모델
  - 매수/매도 기록 저장소 구현
  - 포트폴리오 조회 유스케이스
  - 수익률 계산 유스케이스

- **[통합 및 최적화](.claude/plans/phase-integration.md)**
  - 통합 테스트
  - 성능 최적화 (캐싱, 페이징, 비동기)
  - 문서화 (API 문서, README, 배포 가이드)

---

## ⚠️ 6. 제약 사항 및 주의 사항

### 6.1 아키텍처 제약
- **DDD 계층형 구조 준수 필수**
- **Entity 연관관계 사용 금지** (ID 기반 참조만)
- **domain 계층에 Spring/JPA 의존성 금지**
- **@Transactional은 application 계층에서만 사용**

### 6.2 TDD 제약
- **구현 코드보다 테스트 코드를 먼저 작성**
- **테스트 실패 → 최소 구현 → 테스트 성공 순서 준수**
- **Mock은 테스트 대상의 경계에서만 사용**
- **domain 계층은 Mock 없이 순수 테스트**

### 6.3 API 제약
- **무료 API 사용량 제한 고려**
  - Alpha Vantage: 일 25건, 분당 5건
  - NewsAPI.org: localhost만 사용 가능 (배포 불가)
- **API 키 관리 보안 (환경 변수 사용)**
- **폴백 전략 필수 구현**

### 6.4 Entity 작성 제약
- **Entity 작성 전 사전 승인 필요**
- **연관관계 매핑 금지**

### 6.5 코드 작성 제약
- **요청받지 않은 대규모 리팩토링 금지**
- **구조 변경 시 사전 허락 필수**
- **공개 API 변경 최소화**

---

## 📚 8. 기술 스택

### Backend
- Java 17+
- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- JWT (jjwt)

### Database
- H2 (개발)
- PostgreSQL (운영)

### External APIs
- 주식: Alpha Vantage, Finnhub, 금융위원회 등
- 뉴스: 딥서치, NewsAPI.org, 국제방송교류재단 등
- OAuth: 카카오, 구글

### Testing
- JUnit 5
- Mockito
- AssertJ

### Others
- Caffeine Cache
- Lombok
- Swagger/OpenAPI

---

## 🚀 9. 다음 단계

1. **태형님의 검토 및 승인 대기**
2. 승인 후 Phase 1 시작
3. 각 Phase별 TDD 방식으로 구현
4. 구현 중 변경사항 발생 시 재검토 요청

---

## 📖 참고 자료

### 주식 API
- [금융위원회 주식시세정보](https://www.data.go.kr/data/15094808/openapi.do)
- [Alpha Vantage](https://www.alphavantage.co/)
- [Finnhub](https://finnhub.io/)
- [Marketstack](https://marketstack.com/)
- [Massive](https://massive.com/)
- [키움증권 Open API](https://www.kiwoom.com/h/customer/download/VOpenApiInfoView)

### 뉴스 API
- [딥서치 뉴스 API](https://news.deepsearch.com/)
- [국제방송교류재단 뉴스기사API](https://www.data.go.kr/data/15108015/openapi.do)
- [NewsAPI.org](https://newsapi.org/)
- [한국 오픈 APIs 모음](https://github.com/dl0312/open-apis-korea)

---

**작성일**: 2026-01-19