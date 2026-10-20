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
- [ ] U1 의존성·MCP 서버 뼈대·`/mcp` 리소스 서버 보안 체인 → 체크포인트 CP1
- [ ] U2 인가 서버·저장 테이블·카카오 로그인 연결·토큰 userId → 체크포인트 CP2
- [ ] U3 기업 리포트·S-RIM·채권 금리 도구(+S-RIM 입력 조립) → 체크포인트 CP3
- [ ] U4 포트폴리오·배당 일정·경제지표·뉴스 도구 → 체크포인트 CP4
- [ ] U5 설정·연결 안내 문서
- [ ] 리뷰 (태형님이 고른 이슈만 반영)
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
- **KTD2 보안 체인 분리:** 새 체인 2개를 `@Order`와 `securityMatcher`로 앞에 둔다. 기존 체인(`ProdSecurityConfig`, `DevSecurityConfig`)은 손대지 않고 나머지 경로를 그대로 받는다.
  - 인가 서버 체인: `/oauth2/**`, `/.well-known/oauth-authorization-server`, `/.well-known/openid-configuration`(필요 시), `/mcp-auth/**`, `/oauth/kakao/mcp`. 로그인 유지를 위해 이 체인만 세션을 쓴다(IF_REQUIRED).
  - 리소스 서버 체인: `/mcp`, `/.well-known/oauth-protected-resource/**`. STATELESS. 토큰 없으면 `401` + `WWW-Authenticate: Bearer resource_metadata=...`. dev도 같다(REQ-16).
- **KTD3 카카오 로그인 연결:** 인가 서버의 로그인 페이지를 카카오 로그인으로 바꾼다.
  - `/mcp-auth/login` → 카카오 인가 URL로 보낸다(`redirect_uri`=`{issuer}/oauth/kakao/mcp`, `state` 검증).
  - `/oauth/kakao/mcp` → `KakaoTokenClient`로 코드를 교환한다. 이때 `redirect_uri`를 인자로 받는 메서드를 추가한다(기존 메서드 동작 불변).
  - 기존 OAuth 계정이 있고 `USER`·`ACTIVE`인 사용자만 로그인시킨다. 계정이 없거나 가입 미완료(`SIGNING_USER`)면 사용자를 만들지 않고 "앱에서 먼저 가입하세요" 화면을 보인다. 이를 위해 `OAuthLoginService`에 조회 전용 메서드를 추가한다(기존 `login` 동작 불변).
  - 로그인 성공 시 세션에 인증(principal 이름 = userId)을 저장하고 원래 `/oauth2/authorize` 요청으로 돌아간다.
  - 카카오 개발자 콘솔에 `https://hubth.com/oauth/kakao/mcp` Redirect URI 등록이 필요하다(태형님 작업).
- **KTD4 클라이언트 등록·승인:** CIMD와 DCR을 켠다(claude.ai는 CIMD를 우선 쓰고 없으면 DCR). 승인(consent) 화면을 켜서 어떤 클라이언트가 접근하는지 보인다(확인 2). 허용 리다이렉트는 `https://claude.ai/api/mcp/auth_callback`과 loopback(`localhost`/`127.0.0.1`, Claude Code)로 제한한다. 라이브러리가 제한을 지원하지 않으면 CP2에서 보고한다.
- **KTD5 토큰:** access token은 JWT(RS256), 1시간. `sub`=userId. refresh token은 30일이고 쓸 때마다 교체한다(Claude 요구: 공개 클라이언트 교체). 서명 키는 환경변수(PEM)로 받아 재배포 후에도 토큰이 유지된다. dev에서 키가 없으면 기동 시 생성한다.
- **KTD6 issuer:** 터널 구간이 http라 요청에서 issuer를 추론하지 않고 환경변수 `MCP_OAUTH_ISSUER`(운영 `https://hubth.com`)로 고정한다. 리소스 식별자는 `{issuer}/mcp`다.
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
- 인가 서버 체인(`McpAuthorizationServerConfigurer`, CIMD·DCR, consent, 리다이렉트 제한), JWK(환경변수 PEM), 토큰 설정·`sub`=userId
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

## 체크포인트
- CP1~CP4는 구현 단위 끝에서 수행하고 태형님 확인을 받는다.

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
