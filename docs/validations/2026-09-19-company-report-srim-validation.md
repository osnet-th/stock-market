# 기업 리포트 S-RIM 검증

gate: docs/gates/2026-09-19-company-report-srim-gates.md
plan: docs/plans/2026-09-19-001-feat-company-report-srim-plan.md
work: docs/works/2026-09-19-company-report-srim-work.md
review: docs/reviews/2026-09-19-company-report-srim-review.md
issue: https://github.com/osnet-th/stock-market/issues/122

승인: 태형님 "검증 진행해" (2026-09-20). 2차 리뷰 findings 7건 수정 이후 상태를 검증한다.

## 환경
- DB: `jdbc:postgresql://host.docker.internal:5432/stocks` (로컬 Docker PostgreSQL). 운영 아님.
- Spring profile: `dev` (`ddl-auto: update`).
- 이 환경에 `node`와 `psql`이 없다. JS는 macOS JavaScriptCore(`osascript -l JavaScript`)로, DB는 JDBC/Spring 컨텍스트로 실행했다.

## 실행한 명령과 결과

| # | 명령 | 결과 |
|---|---|---|
| 1 | `./gradlew compileJava --console=plain` | 성공 |
| 2 | `./gradlew clean test --console=plain` | **성공 — 클래스 21개 / 테스트 117건 / 실패 0 / 오류 0** |
| 3 | `./gradlew test --tests '*TempSrimDbValidationTest*'` (임시) | **성공 — 5건 전부 통과** (아래 상세) |
| 4 | Java 하네스 `SrimFixSmoke` (scratchpad) | 성공 — 19건 |
| 5 | JS 하네스 (JavaScriptCore) | 성공 — 37건 |
| 6 | HTML 바인딩 평가 하네스 (Alpine `with(scope)` 모사) | 성공 — 5건 |
| 7 | `git diff --check` | 성공 |

### 3. 실제 PostgreSQL 검증 (2차 리뷰 F3/F4의 핵심 미검증 항목)
`@SpringBootTest @Transactional` 임시 테스트로 실행하고 **검증 후 파일을 삭제**했다. 트랜잭션 롤백이라 DB에 흔적을 남기지 않는다.

- `srim` 컬럼이 실제로 존재한다 — 타입 `jsonb` 확인.
- JSONB 저장·재조회가 값을 보존한다 — w=1/0.9/0.8 = 15000.000000 / 12000.000000 / 11142.857143, 입력 근거도 왕복 후 동일.
- **재무 새로고침이 S-RIM을 보존한다** — `updateSnapshot` JPQL이 실제로 실행됐고, snapshot만 바뀌고 srim은 그대로였다. 이 기능의 핵심 요구를 DB에서 직접 확인한 것이다.
- 빈 종목명은 기존 이름을 유지한다 — 도메인 `resolveRefreshedStockName` 경유.
- 다른 userId로는 갱신되지 않는다 — 소유권 스코프 동작.

**F3 해소**: `coalesce` 제거 후의 `updateSnapshot`이 PostgreSQL에서 정상 실행됐다. 파라미터 타입 추론 실패는 발생하지 않았다. (정적 검토로는 판정 불가였던 항목이다.)
**F4 해소**: 같은 트랜잭션 안에서 bulk update 직후 재조회가 최신 값을 반환했다. `clearAutomatically`가 동작한다.

검증 중 단정 오류 1건이 있었다. `{"refreshed":true}`와 `{"refreshed": true}`를 직접 비교했는데 PostgreSQL `jsonb`가 공백을 정규화한 것이었다. 제품 결함이 아니라 테스트 단정 문제였고, 내용 비교로 고쳐 통과시켰다.

### 6. HTML 바인딩
2차 리뷰에서 교체한 4개 바인딩을 HTML에서 그대로 추출해 Alpine과 동일한 `with(scope)` 방식으로 평가했다.
- `crSrimCurrencyLabel()` → `원(KRW)`
- 작성 화면 ROE → `12%`
- 상세 요구수익률 → `8%`
- 상세 ROE → `12%`
- 결과 미계산 상태 → `—`

## 추가 검증 (2026-09-20, 태형님 "나도 테스트해보게" 요청으로 앱 기동 후)
`./gradlew bootRun`으로 로컬 기동(profile dev, 포트 8080)하고 확인했다.

- `GET /` → 200. 앱 정상 기동.
- `GET /api/company-reports` → 401.
- **`POST /api/company-reports/srim/calculate` → 401** — 신규 엔드포인트가 매핑됐고 미인증 요청을 거부한다. 기존 "인증 응답 코드 미검증" 항목의 일부가 해소됐다.
- dev 프로파일은 `DevSecurityConfig`가 `anyRequest().permitAll()`이지만, 컨트롤러의 `CompanyReportSecurityContext.currentUserId()`가 Long principal을 요구해 401이 난다. 설계대로 동작한다.
- 수정한 정적 자산이 실제로 서빙된다 — `/partials/company-report.html`에 `crSrimPercent` 3건·`crSrimCurrencyLabel` 1건, 구버전 `Number(x)*100` 0건. `/js/components/company-report.js`에 `_crSrimRoundHalfUp` 존재. `static-locations: file:src/main/resources/static/` 설정이라 재빌드 없이 소스가 서빙된다.
- Elasticsearch(9200) 미실행으로 로그 인덱스 템플릿 등록 경고가 나지만 기업 리포트 기능과 무관하다.

## 미검증 항목
- **브라우저 실동작 E2E**: 로그인 세션이 필요하고 프리뷰 샌드박스가 worktree 경로를 막는 기존 제약이 그대로다. 위저드 임시저장 → 재개 → 완료 저장의 화면 조작 흐름은 확인하지 못했다. work 단계의 격리 브라우저 확인(고정 응답)과 이번 함수/바인딩 단위 검증으로 대체했다.
- **인증/소유권의 HTTP 응답 코드**: 미인증 401은 위에서 확인했다. 로그인 상태에서의 남의 리포트 접근(404) 등 소유권 응답 매핑은 로그인 수단이 없어 확인하지 못했다.
- **로컬 로그인 차단**: worktree `.env`의 `KAKAO_REDIRECT_URI`가 prod(`https://hubth.com/...`)를 가리켜 로컬에서 카카오 로그인이 완료되지 않는다. 로컬 테스트를 하려면 `http://localhost:8080/oauth/kakao`로 바꾸고 해당 URI가 카카오 개발자 콘솔에 등록돼 있어야 한다. 이 변경은 태형님 승인 대상이라 수행하지 않았다.
- **외부 재무 API를 포함한 통합 흐름**: DART/KIS 실호출 경로는 이번 변경 범위 밖이며 실행하지 않았다.
- **마이그레이션 SQL 수동 적용**: `company_report_srim_2026_09_19.sql`은 어떤 DB에도 수동 실행하지 않았다.

## 검증 중 발생한 환경 변경 (보고 필요)
- 테스트가 profile `dev`(`ddl-auto: update`)로 기동되면서 **Hibernate가 로컬 dev DB `stocks`에 `srim jsonb` 컬럼을 자동 생성**했다. 마이그레이션 SQL과 동일한 nullable 컬럼 추가이며 기존 데이터에 영향이 없다. 확인 시점의 dev DB는 `company_analysis_report` 전체 행이 0건인 빈 상태였다.
- 임시 테스트 파일 `src/test/.../TempSrimDbValidationTest.java`를 만들었다가 삭제했다. 저장소에 남지 않았음을 `find src/test -name 'Temp*'` 0건으로 확인했고, 삭제 후 `./gradlew clean test`를 다시 돌려 117건 통과를 재확인했다.
- 검증용 임시 행 잔여 0건을 DB 조회로 확인했다.

## 판정
- 2차 리뷰 findings 7건의 수정이 기존 테스트를 깨지 않았다 (117/117).
- 정적으로 판정 불가였던 F3과 잠복 위험이던 F4를 실제 PostgreSQL에서 해소했다.
- 브라우저 E2E와 HTTP 인증 응답은 환경 제약으로 남아 있다. 이 상태로 commit 단계에 진입할지는 태형님 확인이 필요하다.
