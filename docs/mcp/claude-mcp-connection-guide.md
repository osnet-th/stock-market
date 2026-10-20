# Claude MCP 연결 안내

앱 안의 MCP 서버(`/mcp`)를 Claude(웹·데스크톱·모바일)와 Claude Code에 연결하는 방법이다. 연결하면 Claude가 내 데이터를 조회 전용 도구 10개로 가져온다. 로그인은 카카오 계정으로 하고, 가입을 마친 계정만 연결된다.

## 도구 목록

| 도구 | 내용 |
|---|---|
| `get_company_report_summary` | 종목코드로 기업 리포트 요약 (주가지표, 최근 5개 연도 비율, 위험 신호, 청산가치·DCF) |
| `list_my_company_reports` | 내가 저장한 기업 리포트 목록 |
| `get_my_company_report` | 내가 저장한 기업 리포트 상세 |
| `calculate_srim` | S-RIM 적정주가 (요구수익률·ROE 생략 시 회사채 BBB- 5년 금리·최근 연간 ROE) |
| `get_bond_yields` | 채권 기준수익률 |
| `get_my_holdings` | 내 보유 주식과 현재가·평가액 |
| `get_my_portfolio_summary` | 내 포트폴리오 평가액·수익률·예상 배당·이자 |
| `get_dividend_schedule` | 국내 종목 배당 일정 |
| `get_economic_indicators` | 국내(ECOS)·해외 경제지표 |
| `search_news` | 내 구독 키워드 뉴스 검색 |

## 1. 운영 준비 (최초 1회)

### 1-1. 카카오 Redirect URI 등록

카카오 디벨로퍼스에서 기존 `https://hubth.com/oauth/kakao`를 등록한 카카오 로그인 Redirect URI 설정에 아래 주소를 추가한다. 기존 주소는 그대로 둔다.

```
https://hubth.com/oauth/kakao/mcp
```

### 1-2. 서명 키 만들기

MCP 토큰 서명용 RSA 키를 만든다. 키 파일은 서버 밖에 두지 말고, 저장소에 올리지 않는다.

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out mcp-jwk.pem
# .env에 넣을 한 줄 값 (머리·꼬리 줄과 줄바꿈 제거)
grep -v -- '-----' mcp-jwk.pem | tr -d '\n'
```

- `.env`는 여러 줄 값을 넣기 어려워 본문만 한 줄로 넣는다. 앱은 머리·꼬리 줄과 공백을 지우고 읽는다.
- 키를 비워 두면 앱이 기동할 때마다 새 키를 만든다. 이때 재배포하면 이미 연결한 Claude가 다시 로그인해야 한다.
- 키를 바꾸면 발급된 토큰이 모두 무효가 되어 다시 연결해야 한다.

### 1-3. 운영 `.env` 환경변수

| 변수 | 값 | 설명 |
|---|---|---|
| `MCP_OAUTH_ISSUER` | `https://hubth.com` | 공개 주소. 끝에 `/`를 붙이지 않는다. 메타데이터·토큰의 모든 주소가 이 값 기준이다 |
| `MCP_OAUTH_JWK_PRIVATE_KEY` | 1-2의 한 줄 값 | 토큰 서명 키 |
| `MCP_OAUTH_JWK_KEY_ID` | 예: `stock-market-mcp-1` | 키 식별자. 생략하면 `stock-market-mcp` |
| `KAKAO_MCP_REDIRECT_URI` | `https://hubth.com/oauth/kakao/mcp` | 1-1에 등록한 주소와 같아야 한다 |

기존 `KAKAO_CLIENT_ID`·`KAKAO_CLIENT_SECRET`을 함께 쓴다. Nginx·Cloudflare 설정은 바꾸지 않는다.

### 1-4. 배포 후 확인

```bash
# 401과 WWW-Authenticate(resource_metadata=https://hubth.com/...)가 오면 정상
curl -si -X POST https://hubth.com/mcp | grep -i "HTTP/\|www-authenticate"
# 인가 서버 메타데이터: 모든 주소가 https://hubth.com 으로 시작해야 한다
curl -s https://hubth.com/.well-known/oauth-authorization-server
```

기동할 때 OAuth 저장 테이블 3개(`oauth2_registered_client`, `oauth2_authorization`, `oauth2_authorization_consent`)가 없으면 만든다. 수동 적용·삭제용 SQL은 `src/main/resources/db/migration/mcp_oauth_2026_10_20.sql`에 있다.

## 2. Claude(웹·데스크톱·모바일)에 연결

claude.ai에서 한 번 추가하면 같은 계정의 Claude 데스크톱·모바일에서도 쓴다(인증 기반이 같다). Free 요금제는 커스텀 커넥터를 1개까지 추가할 수 있다.

1. claude.ai에서 **Customize > Connectors**(`https://claude.ai/customize/connectors`) > **Add custom connector**를 누른다. Team/Enterprise 조직은 Owner가 **Organization settings > Connectors > Add > Custom**에서 추가한다.
2. 이름(예: `hubth`)과 MCP 서버 URL `https://hubth.com/mcp`를 넣는다.
3. 인증은 OAuth 로그인을 고른다. OAuth 클라이언트는 기본값(Claude 공개 식별자 또는 자동 등록)을 쓴다. 이 서버는 둘 다 받는다. 직접 만든 Client ID·Secret은 넣지 않는다.
4. 로그인 창에서 연결 요청 화면을 확인하고 **카카오로 로그인하고 연결 허용**을 누른 뒤 카카오 로그인을 마친다.
5. 대화에서 **+ > Connectors**를 열고 이 커넥터를 켠다. 대화마다 켜고 끌 수 있다.

예시 질문: "내 보유 종목 평가액 알려줘", "005930 S-RIM 적정주가 계산해줘", "최근 바뀐 경제지표 정리해줘".

## 3. Claude Code에 연결

```bash
# 모든 프로젝트에서 쓰려면 --scope user (기본은 local: 현재 프로젝트만)
claude mcp add --transport http --scope user hubth https://hubth.com/mcp
```

1. Claude Code를 실행하고 `/mcp`에서 `hubth`를 골라 인증한다. 브라우저에서 카카오 로그인을 마친다(셸에서는 `claude mcp login hubth`).
2. Claude Code는 내 컴퓨터의 임의 포트로 콜백을 받는다(`http://localhost:<port>/callback`). 서버가 loopback 주소를 허용하므로 따로 설정할 것이 없다. 브라우저 이동이 실패하면 주소창의 콜백 URL 전체를 Claude Code 입력창에 붙여 넣는다.
3. 토큰은 Claude Code가 저장하고 갱신한다. 연결을 끊으려면 `/mcp`에서 인증 지우기 또는 `claude mcp logout hubth`.

## 4. 연결 동작과 제한

- 연결할 때 카카오 로그인 전 화면에 연결을 요청한 클라이언트 이름과 조회 범위가 나온다. 카카오 로그인까지 마쳐야 연결된다.
- 계정이 없거나 가입을 마치지 않은 카카오 계정은 연결되지 않고 안내 화면만 보인다. 앱에서 가입을 먼저 마친다.
- 허용하는 콜백 주소는 claude.ai(`https://claude.ai/api/mcp/auth_callback`)와 내 컴퓨터(loopback, Claude Code)뿐이다.
- access token은 1시간, refresh token은 30일이며 쓸 때마다 새로 바뀐다. 30일 동안 쓰지 않으면 다시 연결한다.
- 모든 도구는 조회 전용이다. 데이터를 바꾸거나 지우는 도구는 없다.
- 기업 리포트 요약·S-RIM·현재가·배당 일정은 화면과 같이 DART·KIS를 실시간으로 부른다. 응답이 몇 초 걸릴 수 있다.

## 5. 연결 끊기

- Claude 쪽: 커넥터(또는 Claude Code의 MCP 서버)를 지운다.
- 서버 쪽에서 모든 연결을 끊으려면 서명 키(`MCP_OAUTH_JWK_PRIVATE_KEY`)를 새로 만들어 바꾸고 재배포한다.
