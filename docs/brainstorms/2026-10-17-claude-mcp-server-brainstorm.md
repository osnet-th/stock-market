# Claude MCP 서버 (기업 리포트·S-RIM·포트폴리오 조회) Harness Brainstorm

## 입력
- issue: #144 [enhancement] Claude MCP 연결 — 기업 리포트·S-RIM·포트폴리오 조회 도구
  - 비 이슈로 시작해 이슈 등록 Gate에서 태형님 승인("진행해", 2026-10-20)을 받아 등록했다.
- worktree: 클라우드 세션 브랜치 `claude/inspiring-wright-rljbsn` (최신 main 기준). `/home/user/stock-market-issue-144`는 이 저장소를 가리키는 심볼릭 링크다(#142와 같은 방식).
- source: 태형님 요청(2026-10-17)
  - "stocks 에다가 기업 리포트나 적정주가 계산 이런거 정보 mcp 서버로 등록해서 클로드로 요청하면 가져오도록 하고싶은데"
  - "MCP 로 넣었을때 어떤식으로 활용할 수 있을지 알려줘" → 활용 예시 5가지 제시
  - "해당 내용 다 포함되게 1차로 열자"
  - "클로드 코드말고 그냥 클로드에도 연결하고싶은데" → OAuth 방식 안내 후 "진행해"(2026-10-18)
- 참고 문서: Spring AI 2.0 MCP Server·MCP Security 문서(docs.spring.io/spring-ai), Claude Code MCP 문서(code.claude.com/docs/en/mcp), Claude 커넥터 문서(claude.com/docs/connectors/custom/remote-mcp, claude.com/docs/connectors/building/authentication)

## 요청 이해
앱 안에 MCP 서버를 띄워, Claude(Claude Code 등)에 말로 요청하면 앱의 기업 리포트·S-RIM 적정주가·포트폴리오·배당·경제지표·뉴스 데이터를 가져와 분석에 쓰게 한다. 1차 범위는 앞서 제시한 활용 예시 5가지를 모두 할 수 있는 도구 묶음이다.

1. 종목 하나 깊게 보기: 기업 리포트 지표 + S-RIM 적정주가와 현재가 비교, 요구수익률 바꿔 다시 계산
2. 여러 종목 비교: 저장한 리포트 중 조건 필터, 두 종목 재무 추이 비교
3. 리포트 작성 보조: 위험 신호·재무 추이를 읽고 정리
4. 포트폴리오 연계: 보유 종목별 적정주가 대비 현재가, 배당 일정·이번 달 예상 배당
5. 다른 정보와 함께 보기: 경제지표·뉴스와 합쳐 설명

## 문제 정의
- 기업 리포트, S-RIM, 포트폴리오 정보가 화면에만 있어서 여러 종목을 비교하거나 조건을 바꿔 다시 계산하려면 화면을 여러 번 오가야 한다.
- 지금 챗봇(Gemini)은 앱 안에서만 쓰고, Claude에서는 앱 데이터를 볼 수 없다.

## 목표
- Claude(claude.ai 웹·데스크톱·모바일)에 커스텀 커넥터로, Claude Code에 HTTP MCP 서버로 등록하면 위 5가지 요청을 대화로 처리할 수 있다.
- 연결할 때 앱 로그인(카카오)으로 본인을 확인한다.
- 각 도구는 기존 application 서비스를 그대로 써서, 화면과 같은 값을 돌려준다.
- 도구 응답에는 기준연도·보고서 종류·주가 기준일·단위가 들어 있어 Claude가 "언제 기준 값인지" 알 수 있다.
- 요청한 사용자 본인의 데이터(리포트, 포트폴리오)만 볼 수 있다.

## 범위
- MCP 서버 엔드포인트(Streamable HTTP)와 도구(아래 후보 도구 목록)
- MCP 전용 OAuth 인증(인가 서버 + 카카오 로그인 연결 + `/mcp` 토큰 검증)
- 도구 응답용 요약 DTO
- Claude·Claude Code 등록 방법 안내

## 제외 범위
- 쓰기 도구(리포트 생성·수정·메모 저장, 포트폴리오 변경) — 2단계
- 기존 Gemini 챗봇 변경
- 기존 화면용 JWT 인증 방식 변경
- 해외 종목 S-RIM 자동 채움(현재 국내 연결재무제표만 `srimBasis` 제공)

## 현재 코드 확인 결과
- **빌드:** Spring Boot 4.0.1, Spring MVC + WebFlux, Spring Security(JWT Stateless). Spring AI 의존성 없음.
  - Spring AI 문서: "Spring AI 2.0.x supports Spring Boot 4.0.x and 4.1.x". `spring-ai-starter-mcp-server-webmvc` + `spring.ai.mcp.server.protocol=STREAMABLE`, 기본 엔드포인트 `/mcp`. SSE는 2.0.0부터 deprecated.
  - 도구는 `@McpTool`/`@McpToolParam`으로 정의하고 JSON 스키마가 자동 생성된다.
- **인증:** `JwtAuthenticationFilter`가 `Authorization: Bearer`의 access token(만료 1일)으로 principal(Long userId)을 넣는다. 프로덕션 `ProdSecurityConfig`가 경로별 권한, Dev는 전부 permitAll.
  - 사용자별 API 키 개념은 없다. 관리자 판단은 환경변수 `ADMIN_USER_IDS` 화이트리스트를 쓴다.
- **기업 리포트:** `CompanyReportController`(`/api/company-reports`)
  - `preview(stockCode)`: 저장 없이 스냅샷 조립(10년 재무, 비율, 주가지표 EPS·BPS·PER·PBR·PSR·PCR·EV/EBITDA, 청산가치·DCF, 위험 신호, 배당, `srimBasis`). 응답이 크다.
  - `findList(userId, stockName, page, size)`, `findDetail(id, userId)`: 사용자별 저장 리포트.
  - userId는 `CompanyReportSecurityContext.currentUserId()`로 SecurityContext에서 꺼낸다.
- **S-RIM:** `SrimService.preview(SrimInputData)`는 자기자본·주식수·요구수익률·연도별 ROE 가정 등 입력을 모두 받아 계산만 한다.
  - 화면은 `srimBasis`(자기자본·주식수 근거)로 입력을 채우고, 요구수익률은 `/api/economics/bond-yields`(채권 기준수익률)에서 골라 넣는다. 서버에 "종목코드만으로 S-RIM 입력을 조립"하는 기능은 없다.
- **포트폴리오:** `PortfolioController`(`/api/portfolio`) — `items`(보유 항목·현재가), `summary`(평가액·수익률·배당/이자 예상 포함, `PortfolioIncomeService`), `allocation` 등. `userId` 쿼리 파라미터를 받고 JWT 사용자와 같은지 검사한다(`assertUserMatches`).
- **배당 일정:** `/api/stock-evaluation/{stockCode}/schedules/dividend`(KIS 예탁원).
- **경제지표:** ECOS(`/api/economics/indicators`, 카테고리별·이력·최근 갱신), 글로벌 지표, 채권금리.
- **뉴스:** `/api/news/search`(사용자 키워드 기반), 뉴스 저널.
- **배포:** Docker + Nginx + Cloudflare Tunnel(HTTPS 공개).
- **로그인:** 카카오 OAuth로 로그인하고 앱이 자체 JWT를 발급한다. OAuth 인가 서버 기능은 없다.

## Claude 커넥터 요구사항 (문서 확인)
- HTTPS로 공개된 MCP URL. 커넥터는 Claude 계정에 묶여 웹·데스크톱·모바일에 공통 적용된다(Free는 1개까지).
- 인증은 OAuth 또는 "로그인 없음". 로그인 없음은 URL을 아는 누구나 쓸 수 있어 개인 데이터에는 쓰지 않는다. 고정 헤더 방식은 베타(일부 조직)다.
- OAuth 요구:
  - 토큰 없는 요청에 `401` + `WWW-Authenticate: Bearer resource_metadata=...`, 보호 리소스 메타데이터와 인가 서버 메타데이터 공개
  - 클라이언트 등록: Client ID Metadata Document 또는 동적 클라이언트 등록(DCR, `registration_endpoint`)
  - 리다이렉트 URI `https://claude.ai/api/mcp/auth_callback` 허용(웹·데스크톱·모바일 공통). Claude Code는 loopback 리다이렉트를 쓴다.
  - 토큰 엔드포인트는 form-urlencoded 요청을 받고, 10초(갱신 30초) 안에 응답한다. 공개 클라이언트의 refresh token은 교체(rotation)한다.
- Spring AI 문서: `mcp-server-security`의 `McpServerOAuth2Configurer`로 `/mcp`를 OAuth2 리소스 서버로 보호하고, `mcp-authorization-server` 모듈(Spring Authorization Server + DCR + Resource Indicator)로 인가 서버를 둘 수 있다. 예시 설정에 claude.ai 리다이렉트 URI가 들어 있다.

## 후보 도구 (1차)
| 도구 | 활용 예시 | 감쌀 기존 기능 |
|---|---|---|
| `get_company_report_summary` | 1, 2, 3 | 기업 리포트 preview → 요약 DTO(주가지표, 핵심 비율 연도별, 위험 신호, 청산가치·DCF, 기준연도/기준일) |
| `list_my_company_reports` | 2 | 리포트 목록 |
| `get_my_company_report` | 2, 3 | 리포트 상세(메모·등급·스냅샷 요약) |
| `calculate_srim` | 1, 4 | 종목코드 + 요구수익률(+ROE 가정) → `srimBasis`로 입력 조립 → `SrimService` |
| `get_bond_yields` | 1 | 채권 기준수익률(요구수익률 후보) |
| `get_my_holdings` | 4 | 포트폴리오 보유 항목 + 현재가 |
| `get_my_portfolio_summary` | 4 | 평가액·수익률·이번 달/연 예상 배당·이자 |
| `get_dividend_schedule` | 4 | 종목 배당 일정 |
| `get_economic_indicators` | 5 | ECOS 카테고리별 최신값/최근 갱신, 글로벌 지표 |
| `search_news` | 5 | 뉴스 검색 |

## 후보 접근
- **A. 앱 안에 MCP 서버를 둔다(권장).** `spring-ai-starter-mcp-server-webmvc`를 추가하고, 새 presentation 패키지(예: `mcp`)의 `@McpTool` 클래스가 기존 application 서비스를 부른다.
  - 장점: 로직 재사용, 같은 배포 경로, 같은 트랜잭션·캐시.
  - 단점: Spring AI 의존성이 앱에 들어온다. 보안 체인을 경로별로 나눠야 한다.
- **B. 별도 MCP 서버 프로세스가 기존 REST API를 호출한다.**
  - 장점: 앱 변경이 없다.
  - 단점: 프로세스·배포가 하나 더 생기고, JWT 발급·갱신을 MCP 서버가 해야 한다. 요약 DTO도 MCP 서버 쪽에 다시 만들어야 한다.

## 권장 접근
A. MCP 도구는 presentation 계층의 얇은 어댑터로 두고, 도메인·application 로직은 바꾸지 않는다. 새로 생기는 로직은 "S-RIM 입력 조립"과 "요약 DTO 변환" 두 가지다.

## 태형님 확인 필요
1. **사용할 Claude 클라이언트**
   - 권장: 1차는 Claude Code(`claude mcp add --transport http ... --header "Authorization: Bearer ..."`)만 지원한다.
   - 대안: claude.ai/Claude Desktop 커넥터까지 지원한다. 문서상 고정 헤더 방식은 베타(일부 조직만)라 사실상 OAuth가 필요하고, 범위가 크게 늘어난다.
2. **MCP 인증 방식**
   - 권장: 환경변수로 MCP API 키와 사용자 ID를 묶는다(예: `MCP_API_KEYS=userId:키` 형식, `ADMIN_USER_IDS`와 같은 방식). `/mcp` 전용 SecurityFilterChain이 키를 확인해 principal(userId)을 넣는다. DB·Entity 변경이 없다.
   - 대안 a: 사용자별 API 키를 DB에 저장하고 화면에서 발급한다(Entity 신규, DB Schema Review 필요).
   - 대안 b: 기존 JWT access token을 그대로 쓴다(만료 1일이라 매일 교체해야 한다).
3. **쓰기 도구**
   - 권장: 1차는 조회 전용으로 한다. 활용 예시 3의 "리포트 메모 저장"은 2차로 미룬다.
   - 대안: 리포트 메모 저장 도구를 1차에 포함한다.
4. **S-RIM 계산 도구 입력**
   - 권장: `stockCode`(필수), `requiredReturn`(%, 선택 — 없으면 채권 기준수익률 중 화면 기본값과 같은 항목), `roeMode`(선택 — 화면의 ROE 가정 방식 중 기본값). 자기자본·주식수는 `srimBasis`로 채운다. `srimBasis`가 없는 종목(해외·개별재무제표)은 "자동 계산 불가"를 돌려준다.
   - 대안: 화면처럼 모든 입력을 도구 인자로 받는다(Claude가 값을 직접 채워야 해서 오류가 많아진다).
   - 기본 금리 항목과 ROE 가정 기본값은 plan 단계에서 화면 코드 기준으로 확정한다.
5. **dev 프로필 보안**
   - 권장: dev도 `/mcp`는 API 키를 요구한다(dev 전체 permitAll과 다르게 둔다).

## 태형님 확인 결과 (2026-10-18, "진행해")
1. **사용할 Claude:** Claude(claude.ai 웹·데스크톱·모바일)와 Claude Code 모두 지원한다.
2. **인증:** OAuth로 한다. 앱 안에 인가 서버(`mcp-authorization-server`)를 두고, 승인 화면의 사용자 확인은 기존 카카오 로그인으로 한다. 발급 토큰에는 userId를 담고 `/mcp`는 이 토큰만 받는다. 기존 화면용 JWT 체인은 그대로 둔다.
3. **쓰기 도구:** 1차는 조회 전용이다. 쓰기 도구는 2단계로 둔다.
4. **S-RIM 도구 입력:** 권장안대로 `stockCode` 필수, `requiredReturn`·`roeMode`는 선택(기본값은 화면 기본값), 자기자본·주식수는 `srimBasis`로 채운다. `srimBasis`가 없으면 "자동 계산 불가"를 돌려준다.
5. **dev 보안:** dev도 `/mcp`는 인증을 요구한다.

## plan에서 정할 것
- 인가 서버 저장소: 등록 클라이언트·인가·토큰을 메모리에 둘지 DB에 둘지. DB면 테이블이 생기므로 DB Schema Review Gate를 거친다.
- 카카오 로그인과 인가 서버 로그인 연결 방식(세션 사용 범위 포함)
- Cloudflare Tunnel·Nginx에서 `/mcp`, `/.well-known/*`, 인가 서버 경로 공개
- S-RIM 기본 금리 항목과 ROE 가정 기본값(화면 코드 기준)
- 도구별 요약 DTO 필드

## /ce:plan 입력 요약
- **결정 사항**
  - 앱 안에 Spring AI 2.0 MCP Server(Streamable HTTP, `/mcp`)를 둔다. `@McpTool` 클래스는 presentation 계층의 얇은 어댑터로 기존 application 서비스를 부른다.
  - 1차 도구 10개(조회 전용): 기업 리포트 요약, 내 리포트 목록·상세, S-RIM 계산, 채권 기준수익률, 보유 종목, 포트폴리오 요약, 배당 일정, 경제지표, 뉴스 검색
  - 인증은 OAuth다. 앱 안의 인가 서버가 카카오 로그인으로 사용자를 확인해 userId가 담긴 토큰을 발급하고, `/mcp`는 그 토큰을 검증한다. dev도 같다.
  - 도구 응답에는 기준연도·보고서 종류·기준일·단위를 담는다. 사용자 본인의 데이터만 돌려준다.
- **범위:** MCP 서버·도구, 요약 DTO, S-RIM 입력 조립, OAuth 인가 서버·리소스 서버 보안 체인, 공개 경로 설정, 등록 안내
- **제외 범위:** 쓰기 도구, Gemini 챗봇, 기존 화면용 JWT 인증 변경, 해외 종목 S-RIM 자동 채움
- **확인 완료:** 1~5. 인가 서버 저장소와 로그인 연결 방식 등은 plan에서 정하고, DB가 필요하면 DB Schema Review로 승인받는다.
