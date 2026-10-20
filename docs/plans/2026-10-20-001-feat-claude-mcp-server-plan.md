---
title: "feat: Claude MCP 연결 — 기업 리포트·S-RIM·포트폴리오 조회 도구"
type: feat
issue: 144
issue_url: https://github.com/osnet-th/stock-market/issues/144
status: active
date: 2026-10-20
approved: "2026-10-20 태형님 승인('진행해'): plan, DB 스키마(테이블 3개), 확인 1~5 권장안, 단위 테스트 시나리오. 2026-10-20 재승인('권장으로 하고 끝까지 다 구현해 결정사항들 다 권장으로 처리해'): KTD1을 Spring Security 7.1.0 지정(선택지 A)으로 갱신. 이후 체크포인트·리뷰 선택·GAP·검증 결정은 권장안으로 진행. PR 생성·병합은 별도 지시"
workflow_exception: "클라우드 세션에 compound-engineering(/ce:plan·/ce:work·/ce:review)이 없어 planning·briefing·review 게이트 절차를 수동 적용한다 (#113·#136·#138·#140·#142와 동일)"
branch: claude/inspiring-wright-rljbsn
branch_exception: "클라우드 세션은 지정 브랜치에만 push할 수 있어 issue/144-{slug} 대신 세션 브랜치를 쓴다 (#136~#142와 동일). validate-plan.sh의 branch 형식 검사 1건은 이 예외로 실패한다."
worktree: /home/user/stock-market-issue-144
worktree_note: "/home/user/stock-market을 가리키는 심볼릭 링크. checkpoint-guard.sh가 경로에서 이슈 번호를 읽기 때문에 둔다."
brainstorm: docs/brainstorms/2026-10-17-claude-mcp-server-brainstorm.md
test_plan_status: approved
schema_plan_status: approved
allowed_paths:
  - build.gradle
  - src/main/java/com/thlee/stock/market/stockmarket/mcp/**
  - src/main/java/com/thlee/stock/market/stockmarket/user/application/OAuthLoginService.java
  - src/main/java/com/thlee/stock/market/stockmarket/user/application/KakaoOAuthService.java
  - src/main/java/com/thlee/stock/market/stockmarket/user/infrastructure/oauth/kakao/KakaoTokenClient.java
  - src/main/java/com/thlee/stock/market/stockmarket/user/infrastructure/oauth/kakao/config/**
  - src/main/resources/application.yml
  - src/main/resources/application-dev.yml
  - src/main/resources/application-prod.yml
  - src/main/resources/mcp/**
  - src/main/resources/templates/mcp/**
  - src/main/resources/db/migration/mcp_oauth_2026_10_20.sql
  - src/test/java/com/thlee/stock/market/stockmarket/mcp/**
  - src/test/java/com/thlee/stock/market/stockmarket/user/application/**
  - docs/mcp/**
  - docs/plans/2026-10-20-001-feat-claude-mcp-server-plan.md
  - docs/brainstorms/2026-10-17-claude-mcp-server-brainstorm.md
  - .claude/issues/144/**
blocked_paths:
  - src/main/java/com/thlee/stock/market/stockmarket/infrastructure/security/**
  - src/main/java/com/thlee/stock/market/stockmarket/companyreport/**
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/**
  - src/main/java/com/thlee/stock/market/stockmarket/stock/**
  - src/main/java/com/thlee/stock/market/stockmarket/stockevaluation/**
  - src/main/java/com/thlee/stock/market/stockmarket/economics/**
  - src/main/java/com/thlee/stock/market/stockmarket/news/**
  - src/main/java/com/thlee/stock/market/stockmarket/chatbot/**
  - src/main/java/com/thlee/stock/market/stockmarket/user/presentation/**
  - src/main/java/com/thlee/stock/market/stockmarket/user/domain/**
  - src/main/resources/static/**
  - nginx/**
  - cloudflared/**
  - docker-compose.yml
---

# Claude MCP 연결 — 기업 리포트·S-RIM·포트폴리오 조회 도구 (#144)

## 요약
- 앱 안에 MCP 서버(Spring AI 2.0, Streamable HTTP, `/mcp`)를 둔다. Claude(claude.ai 웹·데스크톱·모바일 커스텀 커넥터)와 Claude Code가 연결한다.
- 조회 도구 10개를 제공한다. 각 도구는 기존 application 서비스를 그대로 부르는 얇은 presentation 어댑터다.
- 인증은 OAuth다. 앱 안에 인가 서버를 두고, 연결할 때 카카오 로그인으로 본인을 확인한다. 토큰에 userId를 담고 도구는 그 사용자의 데이터만 조회한다.
- 바뀌지 않는 것: 기존 화면 로그인(카카오 → 앱 JWT), 기존 REST API와 보안 체인, 기존 도메인 로직, 화면

## 작업 리스트
- [x] U1 의존성·MCP 서버 뼈대·`/mcp` 리소스 서버 보안 체인 → 체크포인트 CP1 (2026-10-20, 아래 CP1 결과)
- [x] U2 인가 서버·저장 테이블·카카오 로그인 연결·토큰 userId → 체크포인트 CP2 (2026-10-20, 아래 CP2 결과)
- [x] U3 기업 리포트·S-RIM·채권 금리 도구(+S-RIM 입력 조립) → 체크포인트 CP3 (2026-10-20, 아래 CP3 결과)
- [x] U4 포트폴리오·배당 일정·경제지표·뉴스 도구 → 체크포인트 CP4 (2026-10-20, 아래 CP4 결과)
- [x] U5 설정·연결 안내 문서 (2026-10-20, `docs/mcp/claude-mcp-connection-guide.md`)
- [ ] 리뷰 (태형님이 고른 이슈만 반영) — 2026-10-20 지시("결정사항들 다 권장으로 처리해")로 권장안(P1·P2 전부 + 작은 P3) 반영, 아래 리뷰 결과
  - [x] R1 리뷰 반영(A1·A2·A7·A9, S1·S2·S3·S5·S6) + 테스트 P1~P3·M3·T1~T2 → CP5 (2026-10-20, 아래 CP5 결과)
- [ ] 검증 (태형님 선택 방식)

## 배경 / 현재 상태
- **보안:** 프로필마다 `SecurityFilterChain`이 하나뿐이다(`ProdSecurityConfig`, `DevSecurityConfig`). `@Order`·`securityMatcher` 없이 모든 경로를 받는다. 둘 다 STATELESS, CSRF 비활성, 세션·쿠키를 쓰지 않는다. dev는 전부 permitAll이다.
- **로그인:** 브라우저가 카카오 인가 URL로 보내고(`login.html:40-48`), `GET /oauth/kakao`가 코드를 받아(`AuthController.java:35-46`) `KakaoTokenClient.issueToken`으로 교환한다. 교환 때 쓰는 `redirect_uri`는 설정값 하나(`kakao.oauth.redirect-uri`)다(`KakaoTokenClient.java:21-27`).
  - `OAuthLoginService.login`은 계정이 없으면 `SIGNING_USER`를 새로 만든다(`OAuthLoginService.java:63-78`). 가입을 마치면 `USER`가 된다.
  - 앱 JWT는 HS256, `sub`=userId, 만료 1일이다. refresh 토큰은 브라우저에 주지 않는다.
- **배포:** Cloudflare Tunnel → Nginx(`location /` 하나로 전체 프록시) → 앱. Nginx가 `X-Forwarded-Proto`에 `http`를 넣는다(터널→Nginx 구간이 http). 공개 주소는 `https://hubth.com`이다.
- **도구가 감쌀 기존 기능**
  - 기업 리포트: `CompanyReportReadService.preview(stockCode)`(트랜잭션 없음, DART·KIS 실시간 조회), `findList(userId, ...)`, `findDetail(id, userId)`(readOnly)
  - S-RIM: `SrimService.preview(SrimInputData)`. 입력은 자기자본·주식수·요구수익률(소수)·연도 행(`DIRECT`는 ROE 직접, `CALCULATED`는 예상 순이익으로 계산). 화면은 연도 행을 기본으로 만들지 않고, 채권 금리도 기본 선택이 없다(`company-report.js:113, 176-190, 208-210`).
  - 채권 금리: `BondYieldQueryService.lookup(date)` → 국고채·회사채(AAA~BBB-) 만기별 금리(%)
  - 보유 종목: `PortfolioService.getItems(userId)`에는 현재가가 없다. 화면은 `StockPriceService.getPrices`(`POST /api/stocks/prices`)를 따로 부른다.
  - 포트폴리오 요약: `PortfolioSummaryService.getSummary(userId)` → 평가액·수익률·CAGR·배당·이자(`PortfolioIncomeResponse`)
  - 배당 일정: `StockEvaluationService.getSchedule(stockCode, DIVIDEND, from, to)` → 표(열·행 문자열)
  - 경제지표: `EcosIndicatorService.getIndicatorsByCategory(category)`, `findRecentlyUpdated()`, `GlobalIndicatorQueryService`
  - 뉴스: `NewsSearchApplicationService.search(criteria, userId)` — 사용자가 구독한 키워드 범위에서만 검색한다

## 요구사항 원장

| ID | 요구사항 | 출처 | 이번 범위 | 근거 |
|---|---|---|---|---|
| REQ-1 | 앱 안에 MCP 서버(Streamable HTTP)를 두고 Claude(웹·데스크톱·모바일 커넥터)와 Claude Code에서 연결 | 이슈 작업 내용 1 | 포함 | — |
| REQ-2 | 기업 리포트 요약 도구 | 이슈 작업 내용 2 | 포함 | — |
| REQ-3 | 내 리포트 목록·상세 도구 | 이슈 작업 내용 2 | 포함 | — |
| REQ-4 | S-RIM 적정주가 계산 도구(종목코드만으로 계산, 요구수익률·ROE 가정 선택) | 이슈 작업 내용 2 | 포함 | — |
| REQ-5 | 채권 기준수익률 도구 | 이슈 작업 내용 2 | 포함 | — |
| REQ-6 | 보유 종목 도구 | 이슈 작업 내용 2 | 포함 | — |
| REQ-7 | 포트폴리오 요약 도구(예상 배당·이자 포함) | 이슈 작업 내용 2 | 포함 | — |
| REQ-8 | 종목 배당 일정 도구 | 이슈 작업 내용 2 | 포함 | — |
| REQ-9 | 경제지표 도구 | 이슈 작업 내용 2 | 포함 | — |
| REQ-10 | 뉴스 검색 도구 | 이슈 작업 내용 2 | 포함 | — |
| REQ-11 | OAuth 연결 인증, 카카오 로그인으로 본인 확인, 본인 데이터만 조회 | 이슈 작업 내용 3 | 포함 | — |
| REQ-12 | claude.ai 커스텀 커넥터 + 카카오 로그인으로 연결하면 도구 10개 사용 | 이슈 완료 조건 1 | 포함 | 실제 claude.ai 연결 확인은 배포 후 태형님 확인(검증 절) |
| REQ-13 | Claude Code에 HTTP MCP 서버로 추가해도 같은 도구 사용 | 이슈 완료 조건 2 | 포함 | — |
| REQ-14 | 도구 응답에 기준연도·보고서 종류·기준일·단위 | 이슈 완료 조건 3 | 포함 | — |
| REQ-15 | 다른 사용자의 리포트·포트폴리오는 조회되지 않음 | 이슈 완료 조건 4 | 포함 | — |
| REQ-16 | 인증 없는 MCP 호출 거절(dev 포함) | 이슈 완료 조건 5, brainstorm 확인 5 | 포함 | — |
| REQ-17 | 기존 화면 로그인과 API 동작 불변 | 이슈 완료 조건 6 | 포함 | — |
| REQ-18 | 쓰기 도구(리포트 생성·수정·메모 저장, 포트폴리오 변경) | 이슈 범위 밖 | 제외 | 2단계 |
| REQ-19 | Gemini 챗봇·기존 화면 로그인 방식 변경 | 이슈 범위 밖 | 제외 | — |
| REQ-20 | 해외 종목 S-RIM 자동 계산 | 이슈 범위 밖 | 제외 | `srimBasis`가 국내 연결재무제표만 제공 |
| REQ-21 | 연결 승인 화면에서 어떤 클라이언트가 접근하는지 보여 준다 | 이 plan KTD4 | 포함 (확인 2) | — |

## 핵심 기술 결정
- **KTD1 라이브러리:** Spring AI BOM 2.0.0의 `spring-ai-starter-mcp-server-webmvc`(protocol `STREAMABLE`, 엔드포인트 `/mcp`), `org.springaicommunity:mcp-server-security`·`mcp-authorization-server` 0.1.13. 문서상 Spring AI 2.0.x는 Boot 4.0.x를, MCP Security는 Spring Security 7·Spring AI 2.0.x를 지원한다. U1 빌드에서 의존성 충돌이 나면 멈추고 보고한다.
  - **2026-10-20 갱신(재승인):** MCP Security 0.1.13·0.1.14의 `mcp-authorization-server`는 Spring Security 7.1.0을 요구한다. Boot 4.0.1은 7.0.2를 관리해, 7.0.2로 내려가면 참조 클래스 103개 중 `OAuth2ClientRegistrationAuthenticationValidator`(DCR용) 1개가 없다. 그래서 `build.gradle`에서 `spring-security.version`을 7.1.0으로 지정한다(선택지 A). 기존 로그인·API 회귀는 U1에서 전체 테스트와 `bootRun`으로 먼저 확인하고, 깨지면 멈춘다(대안 B: Boot 4.1.x).
- **KTD2 보안 체인 분리:** 새 체인 3개(리소스 서버, 인가 서버, 로그인 — 로그인 체인은 CP2에서 추가)를 `@Order`와 `securityMatcher`로 앞에 둔다. 기존 체인(`ProdSecurityConfig`, `DevSecurityConfig`)은 손대지 않고 나머지 경로를 그대로 받는다.
  - 인가 서버 체인: `/oauth2/**`, `/.well-known/oauth-authorization-server`, `/.well-known/openid-configuration`(필요 시), `/mcp-auth/**`, `/oauth/kakao/mcp`. 로그인 유지를 위해 이 체인만 세션을 쓴다(IF_REQUIRED).
  - 리소스 서버 체인: `/mcp`, `/.well-known/oauth-protected-resource/**`. STATELESS. 토큰 없으면 `401` + `WWW-Authenticate: Bearer resource_metadata=...`. dev도 같다(REQ-16).
- **KTD3 카카오 로그인 연결:** 인가 서버의 로그인 페이지를 카카오 로그인으로 바꾼다.
  - `/mcp-auth/login` → 카카오 인가 URL로 보낸다(`redirect_uri`=`{issuer}/oauth/kakao/mcp`, `state` 검증).
  - `/oauth/kakao/mcp` → `KakaoTokenClient`로 코드를 교환한다. 이때 `redirect_uri`를 인자로 받는 메서드를 추가한다(기존 메서드 동작 불변).
  - 기존 OAuth 계정이 있고 `USER`·`ACTIVE`인 사용자만 로그인시킨다. 계정이 없거나 가입 미완료(`SIGNING_USER`)면 사용자를 만들지 않고 "앱에서 먼저 가입하세요" 화면을 보인다. 이를 위해 `OAuthLoginService`에 조회 전용 메서드를 추가한다(기존 `login` 동작 불변).
  - 로그인 성공 시 세션에 인증(principal 이름 = userId)을 저장하고 원래 `/oauth2/authorize` 요청으로 돌아간다.
  - 카카오 개발자 콘솔에 `https://hubth.com/oauth/kakao/mcp` Redirect URI 등록이 필요하다(태형님 작업).
- **KTD4 클라이언트 등록·승인:** CIMD와 DCR을 켠다(claude.ai는 CIMD를 우선 쓰고 없으면 DCR). 어떤 클라이언트가 접근하는지 보이는 승인 확인을 둔다(확인 2). CP2에서 MCP 로그인 화면이 이 역할을 하도록 조정했다(CP2 결과 참고). 허용 리다이렉트는 `https://claude.ai/api/mcp/auth_callback`과 loopback(`localhost`/`127.0.0.1`, Claude Code)로 제한한다. 라이브러리가 제한을 지원하지 않으면 CP2에서 보고한다.
- **KTD5 토큰:** access token은 JWT(RS256), 1시간. `sub`=userId. refresh token은 30일이고 쓸 때마다 교체한다(Claude 요구: 공개 클라이언트 교체). 서명 키는 환경변수(PEM)로 받아 재배포 후에도 토큰이 유지된다. dev에서 키가 없으면 기동 시 생성한다.
- **KTD6 issuer:** 터널 구간이 http라 요청에서 issuer를 추론하지 않고 환경변수 `MCP_OAUTH_ISSUER`(운영 `https://hubth.com`)로 고정한다. 리소스 식별자는 `{issuer}/mcp`다. MCP 라이브러리가 메타데이터·`WWW-Authenticate` 주소를 요청 주소로 만들기 때문에, MCP·OAuth 경로에서만 요청의 scheme·host·port를 issuer로 바꾸는 필터(`McpPublicOriginFilter`)를 보안 필터 앞에 둔다(CP1에서 추가).
- **KTD7 저장:** 등록 클라이언트·인가(코드·토큰)·승인을 DB에 저장한다(Spring Security 제공 JDBC 구현). 재배포 때마다 다시 연결하지 않게 하기 위해서다(main 병합마다 자동 배포). 표는 아래 DB 스키마 리뷰를 따른다.
- **KTD8 도구 구조:** 새 `mcp` 패키지.
  - presentation: `@McpTool` 클래스(도메인별 3~4개)와 응답 DTO. 토큰의 `sub`에서 userId를 꺼내는 헬퍼.
  - application: 여러 도메인을 묶는 조회(`McpQueryService`: 보유 종목+현재가, S-RIM 입력 조립)와 요약 DTO 변환. 트랜잭션은 기존 서비스의 것을 그대로 쓴다.
  - infrastructure: 보안 체인 2개, JWK·issuer 설정, 저장 테이블 초기화.
- **KTD9 응답 요약:** Claude 맥락을 아끼려고 요약 DTO를 돌려준다. 기업 리포트 요약은 주가지표·최근 5년 비율·위험 신호·청산가치·DCF·기준연도·주가 기준일·통화를 담고, 10년 원본 재무 행은 뺀다. 금액 단위(원/달러)와 비율 단위(%)를 필드 이름이나 별도 필드로 밝힌다(REQ-14).
- **KTD10 S-RIM 도구(확인 3):** 인자는 `stockCode`(필수), `requiredReturnPercent`(선택), `roePercent`(선택).
  - 자기자본·주식수·기준일은 `srimBasis`, 기준 주가는 `priceMetrics.referencePrice`로 채운다. `srimBasis`가 없으면 계산하지 않고 사유를 돌려준다.
  - 요구수익률이 없으면 오늘 기준 회사채 BBB- 5년 금리를 쓰고, 쓴 금리 항목·기준일을 응답에 담는다.
  - ROE가 없으면 리포트 비율의 최근 연도 ROE를 쓴다. 연도 행은 올해 1개를 `DIRECT`로 만든다.
  - 결과는 영구·10% 감소·20% 감소 3가지 적정주가와 현재가 대비 차이다.

## API 계약
- 새 HTTP 진입점(기존 REST 변경 없음)
  - `POST/GET /mcp` — MCP Streamable HTTP. Bearer 토큰 필수
  - `/.well-known/oauth-protected-resource/mcp`, `/.well-known/oauth-authorization-server` — 메타데이터(공개)
  - `/oauth2/authorize`, `/oauth2/token`, `/oauth2/register`(DCR), `/oauth2/jwks` 등 인가 서버 표준 경로
  - `/mcp-auth/login`, `/oauth/kakao/mcp` — 카카오 로그인 연결, 가입 안내 화면
- MCP 도구 10개(이름·인자·응답 요약)
  - `get_company_report_summary(stockCode)`, `list_my_company_reports(stockName?, page?, size?)`, `get_my_company_report(reportId)`
  - `calculate_srim(stockCode, requiredReturnPercent?, roePercent?)`, `get_bond_yields(date?)`
  - `get_my_holdings()`, `get_my_portfolio_summary()`, `get_dividend_schedule(stockCode, fromDate?, toDate?)`
  - `get_economic_indicators(category?)` (없으면 최근 갱신 지표), `search_news(query?, page?, size?)`
- 에러: 도구 안의 예외(종목 없음, 리포트 없음/다른 사용자, 외부 API 실패)는 MCP 도구 오류로 돌려주고 메시지에 이유를 담는다.

## DB 스키마 리뷰 (2026-10-20 승인)
Spring Security 7.0.2 jar의 표준 스키마(`oauth2-registered-client-schema.sql`, `oauth2-authorization-schema.sql`, `oauth2-authorization-consent-schema.sql`)를 PostgreSQL 안내대로 바꿔 쓴다(`blob`→`text`, `timestamp`→`timestamptz`). CIMD 클라이언트 ID가 URL이라 ID 열 길이를 늘린다.

| 항목 | 설계 |
|---|---|
| table | `oauth2_registered_client`(등록 클라이언트), `oauth2_authorization`(인가 코드·access/refresh 토큰), `oauth2_authorization_consent`(승인 기록). 모두 신규 |
| `oauth2_registered_client` | `id varchar(500) PK`, `client_id varchar(500) NOT NULL`, `client_id_issued_at timestamptz NOT NULL DEFAULT now()`, `client_secret varchar(200)`, `client_secret_expires_at timestamptz`, `client_name varchar(200) NOT NULL`, `client_authentication_methods varchar(1000) NOT NULL`, `authorization_grant_types varchar(1000) NOT NULL`, `redirect_uris varchar(1000)`, `post_logout_redirect_uris varchar(1000)`, `scopes varchar(1000) NOT NULL`, `client_settings varchar(2000) NOT NULL`, `token_settings varchar(2000) NOT NULL` |
| `oauth2_authorization` | `id varchar(100) PK`, `registered_client_id varchar(500) NOT NULL`, `principal_name varchar(200) NOT NULL`, `authorization_grant_type varchar(100) NOT NULL`, `authorized_scopes varchar(1000)`, `attributes text`, `state varchar(500)`, 코드·access·oidc id·refresh·user code·device code 각각 `*_value text`, `*_issued_at timestamptz`, `*_expires_at timestamptz`, `*_metadata text`, `access_token_type varchar(100)`, `access_token_scopes varchar(1000)` (표준 열 그대로) |
| `oauth2_authorization_consent` | `registered_client_id varchar(500)`, `principal_name varchar(200)`, `authorities varchar(1000) NOT NULL`, PK `(registered_client_id, principal_name)` |
| PK·UK·FK·index | 표준 PK만 둔다. FK 없음(표준과 같음). `client_id`는 표준처럼 UK 없이 조회한다. 사용자 1명 규모라 추가 인덱스 없음 |
| Entity 매핑 | JPA Entity를 만들지 않는다. Spring Security의 `JdbcRegisteredClientRepository`·`JdbcOAuth2AuthorizationService`·`JdbcOAuth2AuthorizationConsentService`가 `JdbcTemplate`으로 읽고 쓴다. 연관관계 없음 |
| 반영 방식 | Entity가 아니라 `ddl-auto`로 생기지 않는다. 앱 기동 시 `classpath:mcp/mcp-oauth-schema.sql`(`CREATE TABLE IF NOT EXISTS`)을 실행해 만든다. 운영 수동 적용·롤백 백업용으로 `db/migration/mcp_oauth_2026_10_20.sql`을 남긴다 |
| 기존 데이터 영향 | 없음. 기존 테이블을 바꾸지 않는다 |
| 롤백 | 코드만 되돌려도 된다(남은 표는 쓰지 않는다). 표를 지우려면 SQL 파일의 `DROP TABLE IF EXISTS` 3개를 쓴다. 이때 Claude 연결이 끊겨 다시 연결해야 한다 |
| 정리 | 만료된 인가 행은 표준 구현이 지우지 않는다. 행 수가 작아 1단계에서는 정리 배치를 두지 않는다(확인 4) |

## 구현 단위
### U1 의존성·MCP 서버 뼈대·리소스 서버 체인 (KTD1·KTD2·KTD6)
- `build.gradle`: Spring AI BOM, MCP 서버 스타터, MCP Security 2개
- `application*.yml`: `spring.ai.mcp.server.*`(name, version, protocol STREAMABLE, instructions), `mcp.oauth.issuer`
- `mcp/infrastructure/security`: 리소스 서버 체인(`McpServerOAuth2Configurer`, `@Order`, `securityMatcher`)
- 도구 1개(채권 금리)를 먼저 붙여 연결을 확인한다
- CP1: `/mcp` 무토큰 401 + `WWW-Authenticate`, 보호 리소스 메타데이터 응답, 기존 `/api/**`·화면·`/oauth/kakao` 응답 불변, 전체 `./gradlew test`

### U2 인가 서버·저장·카카오 로그인 연결 (KTD3~KTD7, 선행 U1)
- 인가 서버 체인(`McpAuthorizationServerConfigurer`, CIMD·DCR, 승인 확인(CP2에서 로그인 화면으로 조정), 리다이렉트 제한), JWK(환경변수 PEM), 토큰 설정·`sub`=userId
- 저장 테이블 SQL과 기동 시 초기화, JDBC 저장소 3종
- `KakaoTokenClient`·`KakaoOAuthService`에 `redirect_uri` 인자 메서드, `OAuthLoginService`에 조회 전용 메서드
- `/mcp-auth/login`, `/oauth/kakao/mcp` 컨트롤러와 가입 안내 화면
- CP2: 인가 서버 메타데이터, DCR 등록, `/oauth2/authorize` → 카카오 로그인 화면 이동, 토큰 발급(카카오 대신 테스트 계정 경로는 쓰지 않고, 로컬에서는 카카오 단계 전까지와 토큰 엔드포인트 form 요청까지 확인), 표 생성 확인

### U3 기업 리포트·S-RIM·채권 금리 도구 (KTD8~KTD10, 선행 U1)
- 리포트 요약·목록·상세 도구, S-RIM 입력 조립과 도구, 요약 DTO
- CP3: 도구 목록, 각 도구 응답(dev 키 없이 가능한 범위), 다른 userId의 리포트 상세 거절

### U4 포트폴리오·배당 일정·경제지표·뉴스 도구 (선행 U1)
- 보유 종목+현재가 조합, 포트폴리오 요약, 배당 일정, 경제지표, 뉴스 검색 도구
- CP4: 각 도구 응답, 본인 데이터만 나오는지

### U5 설정·연결 안내
- `docs/mcp/`에 연결 안내(카카오 Redirect URI 등록, 운영 환경변수 목록·키 생성 방법, claude.ai 커넥터 추가, Claude Code `claude mcp add --transport http`)
- `.env` 실제 값은 쓰지 않는다

## 시스템 전반 영향
- 기존 화면·REST·앱 JWT: 새 체인이 자기 경로만 받으므로 바뀌지 않는다. 기존 보안 설정 파일은 수정하지 않는다(blocked).
- 세션: 인가 서버 경로에서만 `JSESSIONID`가 생긴다. 기존 화면은 localStorage 토큰 그대로다.
- 외부 호출: 도구가 기존 서비스를 부르므로 DART·KIS 호출량이 Claude 사용만큼 늘 수 있다. 기업 리포트 요약은 화면과 같은 미리보기 비용이다.
- 배포: 운영 `.env`에 새 환경변수(issuer, JWK PEM, 카카오 MCP redirect URI)가 필요하다. Nginx·Cloudflare 설정은 바꾸지 않는다(이미 전체 경로 프록시).

## 위험과 완화
- **라이브러리 호환(높음):** Spring AI 2.0 + MCP Security 0.1.13 + Boot 4.0.1 + Spring Security 7.1.0 지정. U1에서 빌드·기동과 기존 로그인·API 회귀부터 확인하고 실패하면 멈춘다.
- **CIMD 클라이언트와 JDBC 인가 저장:** 인가 저장소가 클라이언트를 다시 찾을 때 CIMD 클라이언트가 조회되는지 U2에서 확인한다. 안 되면 보고하고 대안(DCR만 사용 등)을 제안한다.
- **issuer·https:** 프록시 뒤라 메타데이터 URL이 http로 나오면 Claude가 거절한다. issuer를 설정값으로 고정하고 CP2에서 메타데이터의 모든 URL이 https인지 본다.
- **세션과 STATELESS 혼재:** 체인별 세션 정책이 섞이지 않도록 `securityMatcher`를 좁게 둔다. CP1·CP2에서 기존 API 응답에 `Set-Cookie`가 없는지 본다.
- **토큰 탈취 범위:** 도구가 조회 전용이라 피해는 조회로 한정된다. access token 1시간, refresh 교체로 줄인다.
- **응답 크기·지연:** 기업 리포트 미리보기는 DART·KIS를 여러 번 부른다. 요약 DTO로 크기를 줄이고, 지연은 화면과 같다.
- **실제 claude.ai 연결:** 운영 배포와 카카오 Redirect URI 등록 후에만 확인할 수 있다.

## 태형님 확인 결과 (2026-10-20, "진행해" — 1~5 모두 권장안)
1. **DB 스키마:** 위 표 3개 신규 생성(기동 시 자동 생성 + 백업 SQL). 권장: 승인. 대안: 메모리 저장(테이블 없음, 대신 배포마다 Claude 재연결 필요)
2. **승인 화면:** 권장: 켠다(연결 때 "어떤 클라이언트가 접근하는지" 한 번 확인). 대안: 끈다(카카오 로그인 후 바로 연결)
3. **S-RIM 기본값:** 권장: 요구수익률 = 회사채 BBB- 5년, ROE = 리포트 최근 연도 ROE, 연도 행 = 올해 1개 `DIRECT`. 대안: 둘 다 필수 인자로 받는다
4. **만료 토큰 정리:** 권장: 1단계는 정리하지 않는다. 대안: 매일 만료 행 삭제 배치 추가
5. **카카오 MCP 전용 Redirect URI:** 권장: `https://hubth.com/oauth/kakao/mcp`를 새로 등록(기존 `/oauth/kakao` 흐름 불변). 대안: 기존 콜백을 `state`로 나눠 함께 쓴다(기존 컨트롤러 수정 필요)

## 단위 테스트 계획 (2026-10-20 승인)
상태 기반 검증만 한다. 외부 의존(서비스·포트)은 Mock으로 대체한다. 검증 명령: `./gradlew test`
- **T-S S-RIM 입력 조립** (U3)
  - S1: srimBasis가 있고 요구수익률·ROE를 주면 자기자본·주식수·기준일·기준 주가가 채워지고, 요구수익률은 소수, 연도 행은 올해 1개 `DIRECT`다
  - S2: 요구수익률이 없으면 회사채 BBB- 5년 금리를 쓰고 쓴 항목·기준일을 담는다
  - S3: ROE가 없으면 리포트 최근 연도 ROE를 쓴다
  - S4: srimBasis가 없으면 계산 불가와 사유를 돌려준다
  - S5: BBB- 5년 금리가 없으면 요구수익률 입력을 요구하는 사유를 돌려준다
  - S6: ROE 자료가 없으면 ROE 입력을 요구하는 사유를 돌려준다
- **T-M 요약 변환** (U3·U4)
  - M1: 기업 리포트 요약은 최근 5년 비율만 담고 기준연도·주가 기준일·통화·단위를 담는다
  - M2: 보유 종목은 수량×현재가로 평가액을 만들고, 현재가가 없으면 평가액이 비고 사유가 표시된다
- **T-U 토큰 사용자 확인** (U1)
  - U1: `sub`가 숫자면 그 userId를 쓴다
  - U2: 토큰이 없거나 `sub`가 숫자가 아니면 인증 오류다
- **T-L 카카오 MCP 로그인 사용자 확인** (U2, `OAuthLoginService` 조회 메서드)
  - L1: 가입을 마친 활성 사용자면 userId를 돌려준다
  - L2: 계정이 없거나 가입 미완료면 "가입 필요"를 돌려주고 사용자를 만들지 않는다
- 제외: 보안 체인·OAuth 전체 흐름(→ bootRun + curl 검증), 실제 카카오·claude.ai 연결(→ 배포 후 확인)

## 작업 진행 방식 (워크플로우 예외)
- `/ce:plan`·`/ce:work`·`/ce:review`가 없어 게이트 절차를 수동으로 적용한다(#142와 동일).
- 각 CP에서 `scripts/checkpoint-guard.sh /home/user/stock-market-issue-144`로 경로를 검사하고, read-only 가드 서브에이전트로 의미 검사를 한다.

## 리뷰 결과 (2026-10-20)
리뷰어 2명(아키텍처·정확성 / OAuth·로그인 보안)이 브랜치 전체 변경(45개 파일)을 읽기 전용으로 검토했다.

| No | Severity | Issue | 관련 요구사항 | 위치 | 처리 |
|---|---|---|---|---|---|
| A1 | P1 | presentation이 infrastructure 설정(`McpOAuthProperties`·`McpPaths`)을 import | 공통 아키텍처 기준 | `McpLoginController.java:3-4` | 반영: 두 클래스를 계층 중립 `mcp/config`로 옮긴다 |
| A2 | P2 | 해외 종목에 원화 원금이 없으면 외화 원금을 원화로 씀 | REQ-6, REQ-14 | `McpHoldingsMapper.java:35` | 반영: 시세의 환율로 환산, 환율도 없으면 원금·손익을 비우고 사유 표시 |
| A3 | P3 | application이 infrastructure 설정(`KakaoOAuthProperties`) import | 공통 아키텍처 기준 | `KakaoOAuthService.java:7` | 보류: 같은 클래스에 `KakaoTokenClient` import 선례가 있다. 구조 변경이라 별도 작업으로 둔다 |
| A4 | P3 | 보유 종목 현재가를 종목마다 따로 조회 | REQ-6 / 성능 | `McpPortfolioQueryService.java:39-44` | 보류: 기존 시세 캐시를 쓰고 보유 종목 수가 적다. 일괄 조회는 기존 DTO 변경이 필요하다 |
| A5 | P3 | S-RIM이 미리보기 전체를 다시 계산 | REQ-4 / 성능 | `McpCompanyReportQueryService.java:57` | 보류: 화면과 같은 비용. 위험으로 기록 |
| A6 | P3 | 긴 메서드 | 공통 기준 | `McpLoginController`·`McpSrimInputAssembler`·`EconomicsMcpTools` | 일부 반영: 로그인 콜백은 S1 수정과 함께 나눈다. 나머지 보류 |
| A7 | P3 | 상수·Lombok 미사용 | 공통 기준 | `McpClientPolicyRepository` 수동 생성자, `"KRW"`·`"%"`·`"입력값"`·`"ROLE_USER"` 리터럴 | 반영 |
| A8 | P3 | 화면 HTML을 Java 문자열로 만듦 | U2 | `McpLoginController.java:123-132` | 보류: 화면 2개뿐이고 값은 모두 escape한다 |
| A9 | P3 | 원화 현재가 `0` 분기 테스트 없음 | T-M M2 | `McpHoldingsMapperTest` | 반영: M3 추가 |
| S1 | P2 | 로그인 세션이 살아 있으면 화면 확인 없이 코드가 발급됨(인가 요청 위조: 공격자가 만든 인가 URL을 피해자가 열면 공격자의 claude.ai가 피해자 토큰을 받음) | REQ-11 | `McpClientPolicyRepository.java:66`, `McpLoginController.java:97-102` | 반영: 코드를 발급한 직후 세션을 끝낸다(1회용 로그인). 매 연결마다 연결 확인 화면과 카카오 로그인을 거친다 |
| S2 | P2 | 탈퇴·비활성 사용자의 refresh가 계속 동작 | REQ-11 | `McpAuthorizationServerSecurityConfig` 토큰 발급 | 반영: 토큰 정책 클래스(`McpTokenCustomizer`)가 토큰을 발급할 때마다(코드 교환·refresh) 활성 사용자인지 다시 보고 아니면 `invalid_grant`. 사용자 확인은 기존 보안 설정 선례대로 user 도메인(`UserRepository`, `User.canAccessResource`)을 쓴다. 이미 발급된 access token은 최대 1시간 남는다 |
| S3 | P2 | CIMD 문서 조회 SSRF(사설 IP 차단 없음) | 공통 보안 기준 | `McpAuthorizationServerSecurityConfig.java:92-94` | 반영: URL 형태 client_id는 `claude.ai`·`claude.com`·`anthropic.com`(하위 도메인 포함) https만 조회한다 |
| S4 | P3 | 열린 DCR·CIMD 캐시 상한 없음 | 공통 보안 기준 | `McpAuthorizationServerSecurityConfig.java:55-56` | 보류: rate limit은 인프라 작업. S3로 CIMD 대상이 제한된다 |
| S5 | P3 | 리소스 서버가 `iss`를 검증하지 않음 | REQ-16 | `McpJwkConfig.java:43` | 반영: issuer 검증 추가 |
| S6 | P2(리뷰는 P3) | CIMD 클라이언트가 캐시 만료(기본 300초) 뒤 `findById`에서 사라져 refresh가 실패 | REQ-12 | `McpClientPolicyRepository.findById` | 반영: id가 CIMD URL이면 `findByClientId`로 다시 조회 |

추가 테스트(R1, 상태 기반)
- P1: URL 형태 client_id는 허용 호스트(https)만 조회하고, 다른 호스트·http·사설 주소 형태는 없는 클라이언트로 본다
- P2: CIMD 클라이언트가 캐시에서 빠져도 `findById`가 문서 URL로 다시 찾는다
- P3: DCR 클라이언트(UUID id)는 그대로 조회된다
- M3: 원화 현재가 `0`이면 현재가 없음, 해외 종목 원화 원금이 없으면 환율로 환산하고 환율도 없으면 원금·손익을 비운다
- T1: 활성 사용자면 access token aud를 MCP 리소스로 고정한다
- T2: 탈퇴·가입 미완료·없는 사용자·숫자가 아닌 sub면 `invalid_grant`로 발급하지 않는다

## 체크포인트
- CP1~CP4는 구현 단위 끝에서 수행한다. 2026-10-20 지시("끝까지 다 구현해 결정사항들 다 권장으로 처리해")로 체크포인트마다 멈추지 않고 결과를 기록한 뒤 다음 단위로 간다.

### CP5 결과 (2026-10-20, 리뷰 반영 R1)
- 바뀐 흐름
  - 인가 코드를 보낸 직후 로그인 세션을 끝낸다(`McpOneTimeAuthorizationResponseHandler`, 응답 형식은 기본과 같은 code·state). 연결마다 연결 확인 화면과 카카오 로그인을 거친다
  - 토큰을 발급할 때마다 활성 사용자인지 본다(`McpTokenCustomizer`, aud 고정도 이 클래스로 옮김)
  - URL 형태 client_id는 `claude.ai`·`claude.com`·`anthropic.com`(하위 도메인 포함) https만 조회한다. CIMD 클라이언트가 id 조회 캐시에서 빠지면 문서 URL로 다시 찾는다
  - 리소스 서버가 `iss`를 검증한다
  - 보유 종목: 해외 종목 원화 원금이 없으면 현재 환율로 환산하고, 환율도 없으면 원금·손익을 비우고 사유를 쓴다
  - `McpOAuthProperties`·`McpPaths`를 계층 중립 `mcp/config`로 옮겼다(presentation → infrastructure 참조 제거). 상수·Lombok 정리, 로그인 콜백의 로그인 처리를 메서드로 나눴다
- 확인(dev `bootRun`):
  - 정상 토큰 `initialize` 200. `iss`가 다른 토큰 401(`The iss claim is not valid`)
  - `client_id=https://evil.example/meta` 인가 요청 400, 서버가 그 주소로 요청하지 않음(로그에 호스트 없음)
  - DCR 등록 → 인가 요청 302 로그인 화면 → 클라이언트 이름 표시, 카카오 콜백 주소 그대로. 토큰 엔드포인트 가짜 코드 `invalid_grant`
  - 기존 `/api/economics/indicators/categories` 200, `/login.html` `Set-Cookie` 없음
  - 세션 종료(1회용 로그인)·활성 사용자 재확인·CIMD 캐시 만료 뒤 refresh는 실제 카카오 로그인이 필요해 배포 후 확인한다(단위 테스트 P2·T1·T2로 판단 로직은 확인)
- 테스트: 전체 293건 통과(P1~P3·T1~T2·M3 6건 추가)

### CP4 결과 (2026-10-20)
- 바뀐 흐름: 도구 5개(`get_my_holdings`, `get_my_portfolio_summary`, `get_dividend_schedule`, `get_economic_indicators`, `search_news`)를 더해 도구가 10개가 됐다. 모두 조회 전용이고, 보유 종목·포트폴리오 요약·뉴스는 토큰의 userId 범위만 조회한다.
- 주요 파일: `mcp/application/{McpPortfolioQueryService, McpHoldingsMapper}`, `mcp/application/dto/McpHoldings`, `mcp/presentation/tool/{PortfolioMcpTools, NewsMcpTools, EconomicsMcpTools}`, `mcp/presentation/dto/{McpPortfolioSummary, McpDividendSchedule, McpEconomicIndicators, McpNewsPage}`
- plan 대비 조정(권장안으로 처리, 2026-10-20 지시)
  - KTD8의 `McpQueryService`는 `McpPortfolioQueryService`로 두었다(이름만 다름). 여러 도메인을 묶는 보유 종목+현재가만 application에 두고, 한 도메인만 쓰는 도구(요약·배당·경제지표·뉴스)는 기존 서비스를 바로 부른다
  - 보유 종목은 주식 항목 중 전량 매도(`CLOSED`)를 뺀다. 현재가는 종목마다 `StockPriceService.getPrice`(캐시)로 가져온다. `getPrices`는 요청 DTO가 presentation에 있고 생성자가 없어 쓰지 않았다. 조회 실패나 원화 현재가 `0`은 현재가 없음으로 보고 평가액·손익을 비운 뒤 `priceNote`에 이유를 쓴다. 주식 외 자산과 합계는 포트폴리오 요약 도구가 맡는다
  - 배당 일정 기본 기간은 화면과 같은 6개월 전 ~ 3개월 후다. 기간 길이 제한은 두지 않는다
  - 경제지표 `category`는 ECOS·해외 카테고리 코드나 이름을 받는다. 없으면 최근 7일 갱신 지표(ECOS+해외)를 준다. 최근 갱신 목록의 ECOS 항목은 저장값에 단위가 없어 `unit`이 비어 있다
  - 뉴스는 화면과 같이 구독 키워드 전체를 범위로, 최신순, 본문 앞 300자, 검색어 200자 이내, `size` 1~30으로 맞춘다
- 확인(dev `bootRun`, 테스트 토큰, 사용자 1·2 검증 데이터는 확인 후 지웠다):
  - `tools/list` 10개, 모두 `readOnlyHint=true`
  - 보유 종목: 사용자마다 자기 종목만 나오고 매도 완료 항목은 빠진다. dev에 KIS 키가 없어 현재가 없음 안내가 나온다
  - 포트폴리오 요약: 사용자 1은 700,000원, 사용자 2는 450,000원으로 각자 값이 나온다. `currency=KRW`, `rateUnit=%`
  - 뉴스: 사용자 1은 키워드 A 기사만, 사용자 2는 키워드 B 기사만 보인다. 본문은 300자로 잘린다
  - 배당 일정: KIS 키가 없어 도구 오류. 6자리가 아닌 코드는 거절된다
  - 경제지표: 최근 갱신·해외 카테고리는 값이 나온다. ECOS 카테고리는 키가 dev용 값이라 비어 있다. 모르는 카테고리는 사용 가능한 코드 목록과 함께 도구 오류가 난다
- 테스트: 전체 287건 통과(M2 1건 추가)
- 가드: 경로 검사 통과(11개 파일). 의미 검사 PASS. 선택 권장 중 현재가 `0` 처리는 반영했고, 나머지는 위 기록으로 처리
- 남은 위험: 배당 일정·ECOS·현재가의 실제 응답은 운영 키가 필요해 배포 후 확인한다

### CP3 결과 (2026-10-20)
- 바뀐 흐름: 도구 4개(`get_company_report_summary`, `list_my_company_reports`, `get_my_company_report`, `calculate_srim`)를 더해 도구가 5개가 됐다. 모두 조회 전용(`readOnlyHint=true`)이고 토큰의 userId로 사용자를 확인한다. 목록·상세는 그 userId로만 조회한다.
- 주요 파일: `mcp/application/{McpCompanyReportQueryService, McpSrimInputAssembler, McpCompanyReportMapper}`, `mcp/application/dto/{McpCompanyReportSummary, McpCompanyReportDetail, McpSrimResult}`, `mcp/presentation/tool/CompanyReportMcpTools`
- plan 대비 조정(권장안으로 처리, 2026-10-20 지시)
  - KTD8의 `McpQueryService`는 도메인별로 나눠 기업 리포트 쪽을 `McpCompanyReportQueryService`로 두었다(이름만 다름)
  - 요약의 "최근 5년"은 최근 5개 연도 열이다. 올해 분기·반기 열이 들어갈 수 있고 열마다 `partial`로 표시한다. 기준연도(`baseYear`)는 부분 연도를 뺀 최근 연도다
  - S-RIM 결과에 자기자본·주식수 근거 보고서 이름(`equityReport`, `sharesReport`)을 넣었다(REQ-14, 가드 권장)
  - 금리 출처 통신 실패는 계산 불가로 돌리고 `requiredReturnPercent` 입력을 요청한다
  - 목록 `size`는 1~50, `page`는 0 이상으로 맞춘다
- 확인(dev `bootRun`, 테스트 토큰):
  - `tools/list` 5개, 모두 `readOnlyHint=true`, 필수 인자가 API 계약과 같다
  - 사용자 2의 리포트: 사용자 1 목록 0건, 상세는 도구 오류("찾을 수 없습니다"). 사용자 2 목록 1건, 상세 성공(검증 행은 확인 후 지웠다)
  - 요약·S-RIM 도구는 dev에 DART 키가 없어 DART 조회에서 도구 오류로 끝난다(예상 범위). `stockCode`가 비면 도구 오류
  - 조립한 입력으로 `SrimService.preview`를 부른 결과(BPS 20,000, ROE 15%, 요구수익률 8%)가 손계산과 같다: 영구 37,500 / 10% 감소 27,000 / 20% 감소 24,000
  - 실시간 채권 금리에 회사채 BBB- 60개월 금리가 있다. 무토큰 `/mcp` 401
- 테스트: 전체 286건 통과(S1~S6·M1 7건 추가)
- 가드: 경로 검사 통과(10개 파일). 의미 검사 PASS. 선택 권장 2건 중 근거 보고서 이름은 반영, 열 수는 위 기록으로 처리
- 남은 위험: 요약·S-RIM의 실제 DART·KIS 응답은 운영 키가 필요해 배포 후 확인한다

### CP2 결과 (2026-10-20)
- 바뀐 흐름: `/oauth2/authorize`에 로그인이 없으면 `{issuer}/mcp-auth/login`으로 보낸다. 이 화면이 연결을 요청한 클라이언트 이름과 조회 범위를 보이고 카카오 로그인으로 보낸다(`state` 저장). `/oauth/kakao/mcp`가 `state`를 확인하고 코드를 교환한 뒤, 가입을 마친 활성 사용자만 세션에 로그인(principal=userId)시키고 원래 인가 요청으로 돌려보낸다. 계정 없음·가입 미완료·카카오 실패는 안내 화면만 보이고 사용자를 만들지 않는다.
- 주요 파일: `mcp/infrastructure/security/{McpAuthorizationServerSecurityConfig, McpClientPolicyRepository, McpOAuthSchemaInitializer}`, `mcp/presentation/McpLoginController`, `user/application/{OAuthLoginService.findActiveUserId, KakaoOAuthService.authorizeUrl·loginWithKakao(code, redirectUri)}`, `KakaoTokenClient.issueToken(code, redirectUri)`, 스키마 SQL 2개
- plan 대비 조정(권장안으로 처리, 2026-10-20 지시)
  - **승인 화면(KTD4·확인 2):** 인가 서버 기본 승인 화면은 scope를 요청할 때만 뜨고, MCP 라이브러리는 scope가 없으면 승인을 건너뛴다(`McpNoScopeClientConsentNotRequired`). 그래서 MCP 로그인 화면에 "어떤 클라이언트가 무엇을 조회하려 하는지"를 보이고 "카카오로 로그인하고 연결 허용"을 눌러야 진행하게 해 승인 확인을 대신했다. 기본 승인 화면은 끈다(`requireAuthorizationConsent(false)`). 승인 테이블은 표준 구현이 쓰도록 그대로 둔다
  - **로그인 경로 체인:** 인가 서버 설정이 경로를 좁혀도 `/mcp-auth/**`·`/oauth/kakao/mcp`가 기존 체인(운영은 인증 필요)으로 넘어가지 않게 로그인 전용 체인(`@Order(3)`)을 더 두었다
  - **공개 주소 리다이렉트:** 톰캣이 상대 리다이렉트를 내부 주소로 바꿔서 로그인 이동 주소를 issuer 기준 절대 주소로 만든다
  - **메타데이터:** PKCE 공개 클라이언트용 `none`을 토큰 엔드포인트 인증 방식에 추가했다(claude.ai CIMD 조건)
  - **토큰 aud:** 클라이언트가 `resource`를 보내지 않아도 리소스 서버 aud 검증을 통과하도록 access token의 aud를 `{issuer}/mcp`로 고정했다
- 확인(dev `bootRun`, issuer `https://hubth.test`):
  - 인가 서버 메타데이터의 모든 주소가 https issuer 기준, `registration_endpoint`, `client_id_metadata_document_supported=true`, 인증 방식에 `none`
  - DCR: claude.ai 콜백·loopback(`http://localhost:53100/callback`) 등록 성공, 다른 주소(`https://evil.example/cb`) 400
  - 인가 요청 → 302 `https://hubth.test/mcp-auth/login`(세션 쿠키 발급) → 로그인 화면에 클라이언트 이름("Claude")과 카카오 인가 주소(`redirect_uri=https://hubth.test/oauth/kakao/mcp`, `state`)
  - 콜백: `state` 불일치 → 연결 실패 화면, 맞는 `state`+가짜 코드 → 카카오 교환 실패 → 연결 실패 화면(500 아님)
  - 토큰 엔드포인트 form 요청 → `{"error":"invalid_grant"}`
  - 테이블 3개 기동 시 생성, 등록 클라이언트 2건 저장. 기존 API 응답에 `Set-Cookie` 없음
  - 카카오 실제 로그인 이후 단계(토큰 발급·refresh·CIMD 문서 조회)는 실제 카카오 계정이 필요해 운영 배포 후 확인한다
- 테스트: 전체 279건 통과(L1·L2 4건 추가)
- 가드: 경로 검사 통과(12개 파일). 의미 검사 PLAN_UPDATE_NEEDED(문구) → KTD2 체인 수·U2 승인 문구를 고쳤다. 보안 점검(오픈 리다이렉트·state·세션 고정·사용자 미생성·기존 로그인 불변) 이상 없음
- 남은 위험(기록, 진행 막지 않음): ①로그인 세션이 살아 있는 동안 다른 DCR 클라이언트도 화면 확인 없이 코드를 받을 수 있다(리다이렉트가 claude.ai·loopback뿐이고 PKCE 필수라 영향 작음) ②DCR 클라이언트 이름은 클라이언트가 정한다 ③CIMD 문서 조회의 SSRF 방어는 라이브러리 검증기에 맡긴다

### CP1 결과 (2026-10-20)
- 바뀐 흐름: `/mcp`·보호 리소스 메타데이터를 받는 리소스 서버 체인(`@Order(1)`)이 기존 체인보다 먼저 MCP 경로만 받는다. 공개 주소 필터가 MCP·OAuth 경로의 요청 주소를 issuer 기준으로 바꾼다. 서명 키(JWK)와 JWT 검증기를 둔다.
- 주요 파일: `build.gradle`(Spring AI BOM 2.0.0, MCP 서버 스타터, MCP Security 0.1.13, 리소스 서버·JDBC 스타터, Spring Security 7.1.0 지정), `application.yml`(`spring.ai.mcp.server.*`, `mcp.oauth.*`), `mcp/infrastructure/{config,security}`, `mcp/presentation/{McpUserContext, tool/EconomicsMcpTools, dto/McpBondYields}`
- 확인(dev `bootRun`, issuer `https://hubth.test`, 테스트 키):
  - 무토큰 `POST /mcp` → 401 + `WWW-Authenticate: Bearer resource_metadata=https://hubth.test/.well-known/oauth-protected-resource/mcp`
  - 메타데이터: `resource=https://hubth.test/mcp`, `authorization_servers=[https://hubth.test]`
  - 테스트 토큰(sub=1, aud=리소스): `initialize`·`tools/list`(1개)·`get_bond_yields` 호출 성공. 도구 안에서 인증 정보를 읽는다
  - aud가 다른 토큰 → 401 `invalid_token`
  - 기존 `/api/economics/indicators/categories`·`/login.html` 200, `Set-Cookie` 없음
- 테스트: 전체 `./gradlew test` 275건 통과(기존 272 + U1·U2 3건). Spring Security 7.1.0 지정 후 기존 272건 회귀 통과
- 가드: 경로 검사 통과(11개 파일). 의미 검사 SCOPE_CREEP(경미) 2건 → 권장안대로 처리(2026-10-20 지시)
  - 공개 주소 필터(`McpPublicOriginFilter`)가 plan 항목에 이름이 없었다. KTD6(issuer 고정)을 지키려면 필요하다(메타데이터·`WWW-Authenticate` 주소가 요청 주소로 만들어진다). KTD6에 포함해 유지하고, 필터 대상 경로를 MCP 메타데이터 3종으로 좁혔다(`/.well-known/` 전체 → 3개)
  - U2 설정 일부(JDBC 스타터, 인가 서버 경로 목록, 카카오 MCP 콜백 설정)가 U1에 먼저 들어갔다. U2에서 쓴다

## 검증
- `./gradlew test` 전체
- `bootRun`(dev) + curl: `/mcp` 무토큰 401·헤더, 메타데이터, DCR 등록, 토큰 엔드포인트, 기존 API 회귀
- MCP 도구 호출: 테스트용 토큰으로 `tools/list`, 각 도구 호출(외부 키가 없는 도구는 오류 메시지 확인)
- 운영: 배포 후 태형님이 claude.ai 커넥터·Claude Code로 실제 연결(카카오 로그인 포함)

## 수정 범위
- 수정 가능: frontmatter `allowed_paths`
- 수정 금지: frontmatter `blocked_paths`. 특히 기존 보안 설정, 다른 도메인 코드, 화면, 배포 설정

## 완료 정의
- REQ-1~17, REQ-21 충족, 리뷰·검증 완료, PR 병합, 운영 연결 확인
