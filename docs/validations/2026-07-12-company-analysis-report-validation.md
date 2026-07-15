# 기업분석리포트 Validation 기록

gate: docs/gates/2026-07-12-company-analysis-report-gates.md
review: docs/reviews/2026-07-12-company-analysis-report-review.md

## 환경

- 로컬 PostgreSQL: 기존 `local-postgres` 컨테이너(5432, DB `stocks`) 재사용 — 신규 기동 없음
- 앱: worktree에서 `SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun` (.env 로드, `KAKAO_REDIRECT_URI`만 localhost로 런타임 오버라이드 — 파일 미수정)
- 인증: dev JWT 시크릿으로 로컬 검증용 토큰 발급(sub=1/2, role=USER) — 필터는 DB 조회 없이 토큰만 검증함을 코드로 확인

## 실행한 검증과 결과 (2026-07-12, 전부 통과)

| # | 검증 | 결과 |
|---|------|------|
| 1 | 정적 검증: `./gradlew compileJava`, JS 3파일 JXA 문법, 파티션 태그 균형 | 통과 |
| 2 | ddl-auto로 `company_analysis_report` 생성 (jsonb 2컬럼, 인덱스 2종, 등급 CHECK 제약) | `\d` 확인 |
| 3 | `GET /preview?stockCode=005930` (삼성전자) | 200, 74KB. 10개년 컬럼(2017~2025 사업 + 2026 1분기 partial), 기업개황(설립일 19690113), 2024 매출 300.87조·영업이익률 10.88%(실제 일치), 지표 판정(자기자본비율 76.96 good 등), 청산가치 188조(조정자산 318조−부채 130조), DCF 보수 433조/낙관 831조, 최대주주 2016~2025 224행, 5%룰 40건, 위험 시그널 전부 false |
| 4 | `POST /api/company-reports` (메모+등급 A 포함) | 201 `{id:1}` |
| 5 | `GET /api/company-reports` 목록 | 등급 7키·hasBuySignal=true(A 보유)·snapshotAt 정상 |
| 6 | `GET /{id}` 상세 | 스냅샷+파생 가치평가 포함 |
| 7 | `PUT /{id}` (낙관 성장률 15%→25%, 등급 변경) | 204. DCF 낙관 705조→977조 **즉시 재계산**, snapshotAt **불변** — "스냅샷=원시, 가치평가=파생" 설계 검증 |
| 8 | `POST /{id}/refresh` | snapshotAt 갱신, 수동 입력·파라미터 유지 |
| 9 | IDOR: userId=2 토큰으로 `GET /1` | 404 (userId 스코프 강제 확인) |
| 10 | `DELETE /{id}` → 재조회 | 204 → 404 |
| 11 | 잘못된 등급("Z") 생성 | 400 |
| 12 | 엣지 — 우선주(005935) preview | 502 + 명확한 메시지("DART 고유번호를 찾을 수 없습니다") — 프론트 previewError로 표시됨 |
| 13 | 엣지 — 달바글로벌(483650, 2025-05 상장) preview | 200. 3개년만으로 정상, **위험 시그널 실감지**: 매출채권 급증 true + 유상증자 이력 true |
| 14 | 정적 리소스: `/index.html`, `/partials/company-report.html` | 200 |
| 15 | (2026-07-13 범위 추가) 목록 필터 종목명 부분일치 전환 후: `?stockName=삼성` → 1건(삼성전자), `?stockName=없는이름` → 0건, 무필터 → 전체 | 통과 |
| 16 | (2026-07-14 범위 추가) ddl-auto로 `draft`(boolean not null default false)·`draft_step`(int null) 컬럼 생성 | `information_schema` 확인 |
| 17 | (2026-07-14) draft 라이프사이클: 생성(draft=true,step=3) 201 → 목록 draft/draftStep 노출 → 임시저장 갱신(step=5, 스냅샷 유지) → 작성완료(draft=false, step null화, hasBuySignal 정상) → draftStep=9 → 400 | 통과 |
| 18-1 | (2026-07-14 범위 추가) 연도 입력 4자리 숫자 제한: "69년" → 400, "1969" → 201 | 통과 |
| 18-2 | (2026-07-14 범위 추가) 예상 매출/계산기 재료: 2개년(분기+선택 순이익) 저장 왕복 보존, 문자 금액("80만") 400, 잘못된 단위("만") 400 | 통과 |
| 18 | (2026-07-14 범위 추가) 구조화 정성 입력: `manual` jsonb 컬럼 생성, 연혁/판매·매입처/경쟁사/급변/주주이벤트 구조가 생성→상세 왕복에서 그대로 보존, 연혁 101행 → 400 | 통과 |

## 특이사항 (버그 아님, 기록)

- **PER null(삼성전자)**: 기존 `ValuationMetricService`가 "당기순이익 값을 찾지 못했습니다" 경고와 함께 EPS를 못 구한 것이 스냅샷에 표면화. 신규 코드 무관 — 필요 시 별도 이슈.
- **EV/EBITDA null(삼성전자)**: 연결 CF 전문에 "상각" 조정 행이 없어 EBITDA 미산출 → 설계된 null("—") 처리.
- ES 미기동 경고 로그: 로컬에 Elasticsearch 없음(기존 동작, 무관).

## 브라우저 검증 (2026-07-14, Chrome 확장으로 직접 수행 — 전부 통과)

| # | 검증 | 결과 |
|---|------|------|
| B1 | 목록: 사이드바 "기업 리포트" 메뉴, 테이블(등급 칩 7·매수재료·기준일), 종목명 검색 UI | 정상 렌더 |
| B2 | 위저드 1단계: 종목 검색(삼성전자) → 선택 → 자동 산출 로딩(~10초) | 정상 |
| B3 | 2단계: 기업개황 자동 카드 + 연혁 행 3개 + 경영이념 + 판매/매입처 편집기 + 분산 평가 select | 정상 렌더 |
| B4 | 3단계: 10개년 실적 차트에 **2027E 예상 매출 막대 이어 그려짐**(labels [...,2026*,2027E]), 분기 매출/순이익 입력 시 연간 합계 자동(3,570,000/540,000) | 정상 |
| B5 | 5단계 계산기: 주가 300,000 입력 → 시총 1748.3조 즉시 재계산, PBR 4.01/PSR 5.24/PCFR 20.49, 예상 PSR 4.9배·예상 PER 32.38배(분기 순이익 합 기반) | 정상 |
| B6 | 7단계 등급(A/B) + 임시저장 → id 발급·"임시저장되었습니다" 안내 → 목록 "작성중 · 7단계" 배지 + 매수재료 배지 + A/B 칩 | 정상 |
| B7 | 작성중 클릭 재개: 7단계 복원, 연혁·분기 순이익·계산기 주가·등급 전부 복원, 스냅샷 재사용(재조회 없음) | 정상 |
| B8 | 작성 완료 → 상세: 투자판단 카드(A 하이라이트), 연혁 타임라인 표, 판매/매입처 표+분산 배지, 예상 실적 표(매출/순이익 2행), 주가지표 자동+**내 계산 블록**, 청산가치/DCF, 주주 동향(지분 추이·5%룰·배당·자기주식), 위험 시그널 6종 | 정상 렌더 |
| B9 | 회귀: 종목평가·포트폴리오 이동 후 기업 리포트 재진입 — 목록 정상, 신규 기능발 콘솔 에러 0 | 통과 |
| B10 | 정리: 검증용 리포트 삭제, 태형님 기존 리포트(id 6)만 유지 | 완료 |

### 브라우저 검증 중 발견된 기존 이슈 (본 기능 무관, 후속 후보)

- `realestate.html:239` — `realestate.tab`이 null일 때 `nextScheduledAt` 접근 콘솔 에러 (부동산 파티션 기존 버그)
- `_chat.html` — `getEcosCategoryLabel` 미정의 참조 콘솔 에러 (챗봇 파티션 기존 버그)

## 추가 수정·재검증 (2026-07-15, 주가지표 "?" 툴팁 실표시)

- **문제(태형님 지적)**: 주가지표 카드 "?"가 HTML `title` 네이티브 툴팁이라 계산식이 화면에 드러나지 않음(hover 지연·무스타일 → "물음표만 있고 설명이 안 보임").
- **수정**:
  - `js/components/company-report.js`: `crHint` 상태 + `crHintShow`/`crHintHide`/`crHintToggle`/`crHintClose` 헬퍼 추가. 토글은 같은 배지 재클릭 시 닫기, 다른 배지 클릭 시 교체(`crHint.text` 비교).
  - `partials/company-report.html`: 공유 팝오버 1개(짙은 배경·계산식 줄바꿈·"클릭 고정" 안내) 추가, 배지 28곳(작성 5단계 11 + 상세 자동 11 + 상세 계산 6)을 `:title` → `@mouseenter`/`@mouseleave`/`@click.stop` 트리거로 치환. `@click.stop`으로 여는 클릭이 `@click.outside`에 잡혀 즉시 닫히던 버그 차단.
- **검증(2026-07-15, Chrome 확장 실동작, 삼성전자 preview 5단계)** — 전부 통과:

| # | 검증 | 결과 |
|---|------|------|
| T1 | 서버 반영: `curl`로 8080이 신버전(hover 트리거 28곳) 서빙, devtools 자동 반영 | 통과 |
| T2 | 새로고침 후 Alpine `crHint` 정상 초기화(초기값 {show:false,…}) | 통과 |
| T3 | hover → 팝오버 렌더("PSR = 시가총액 ÷ 매출액…") / mouseleave → 닫힘 | 통과 |
| T4 | click → 고정(pinned) / 다른 배지 클릭 → 텍스트 교체 / 같은 배지 재클릭 → 닫힘 | 통과 |
| T5 | 바깥 클릭 → 닫힘(`@click.outside`) | 통과 |
| T6 | 팝오버 정상 동작 = Alpine 부팅 정상 → 신규 콘솔 에러 없음 | 통과 |

## 추가 구현·검증 (2026-07-15, 주가지표 계산 근거값(대입값) 노출)

- **범위(태형님 승인 옵션 B)**: 툴팁에 공식뿐 아니라 실제 대입값을 함께 표시. 스냅샷 응답 구조 확장(Approval Gate).
- **백엔드**: `ReportSnapshot.PriceMetrics`에 `breakdowns`(`Map<지표key, MetricBreakdown{terms, result, extras}>`) 추가. `SnapshotFinancialExtractor.buildBreakdowns`가 계산 중간값(현재가·유통주식수·시총·매출·영업CF·순이익·자본총계·자산총계·영업이익·상각비·차입금·현금성·실효세율)을 수집. 근거를 완전히 구성 못하는 지표는 생략(프론트는 공식만).
- **프론트**: `crBreakdownText`/`crMetricUnit`/`_crBdVal`로 공식 아래 "= 항식 = 결과 (중간값)"을 동적 조립. `companyReport.view`로 preview/detail 스냅샷 판별. 상세 "내 계산" 6카드는 자동 근거가 아니므로 `showBreakdown=false`.
- **하위호환**: record 필드 추가 + `FAIL_ON_UNKNOWN_PROPERTIES=false` → 옛 스냅샷(breakdowns 없음)은 공식만 표시. "데이터 새로고침" 시 채워짐.

| # | 검증 | 결과 |
|---|------|------|
| T7 | 백엔드 `compileJava` BUILD SUCCESSFUL, devtools 재시작 반영 | 통과 |
| T8 | 새 preview 응답에 `breakdowns` 7개(marketCap·bps·pbr·psr·pcfr·roic·accrual). 삼성전자 EPS/PER/EV-EBITDA는 원천 결측이라 생략(정상) | 통과 |
| T9 | PSR hover → "= 시가총액 1532.7조 ÷ 매출액 333.6조 = 4.59배" | 통과 |
| T10 | ROIC hover → "= NOPAT 39.8조 ÷ 투하자본 334.5조 = 11.91%" + extras(영업이익 43.6조·실효세율 8.64%·자본총계·차입금·현금성) | 통과 |
| T11 | 결측 EPS hover → 공식만(근거 라인 없음) | 통과 |
| T12 | PSR 클릭 → 고정 + 근거 표시 유지, "클릭 고정됨" 안내 | 통과 |
| T13 | 재무제표 요약을 재무상태표(BS)·손익계산서(IS)·현금흐름표(CF) 3구역 소제목으로 분리(`statements` key 그룹핑, 백엔드 무변경) — 작성 preview 3구역 렌더 확인 | 통과 |
| T14 | EPS/PER 폴백: 기존 `ValuationMetricService`가 계정명 정확일치 실패로 EPS 결측일 때 timeline 당기순이익(account_id 매칭) ÷ 유통주식수로 보정. 삼성전자 **EPS 7,757 · PER 36.03 · PER×PBR 134.51** 산출 + 근거("당기순이익 45.2조 ÷ 유통주식수 58.3억주 = 7,757원") 표시 확인 | 통과 |

- **알려진 한계**: EPS·BPS 결과값은 기존 `ValuationMetricService` 산출값 우선(단 **EPS 결측 시 timeline 당기순이익 ÷ 유통주식수로 폴백**, T14). 근거는 스냅샷 재무 기준이라 참고 성격. EV/EBITDA·ROIC는 근사식이라 중간값(상각비·실효세율)을 extras로 노출.

## 미검증 항목

- 프로덕션 프로필(HTTPS/실 사용자) 환경 동작 — 배포 후 확인.
- 참고(2026-07-13~14): 테스트 중 구버전 좀비 인스턴스가 8080을 점유해 신규 기동이 조용히 실패하는 사고가 있었음 — 좀비 kill 후 클린 재기동으로 해결. 재기동 시 포트 점유 확인 필요.
