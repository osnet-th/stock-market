# 한국자산평가 채권금리 조회·캐싱 단위 테스트 계획 (#131)

> 2026-09-29 태형님 승인 시나리오. 정상 13건, 예외 9건 (한도 초과 S4를 예외 흐름으로 셈).

## 테스트 대상
**대상 클래스/메서드**
- `KoreaApBondRateParser`: 응답 본문 → 도메인 스냅샷 (infrastructure, 순수 로직)
- `BondYieldQueryService.lookup(요청일)`: 날짜 폴백, 금리 캐시와 연결 캐시, 벽시계 상한 (application)

**대상 유스케이스**
- 기준일 하루치 금리 해석: 국고채·공모 무보증 회사채 분류, 결측 정규화, 빈 날짜와 파싱 오류 구분
- 요청일 금리 조회: 폴백, 한도 초과, 오류 전파, 캐시 재사용·만료, 동시 요청 합치기, 날짜 규칙

## 테스트 범위
**포함**
- 위 두 클래스의 결과 값 검증(상태 기반)

**제외**
| 대상 | 이유 |
|---|---|
| `KoreaApBondRateClient`(HTTP 호출), 어댑터 조합 | 외부 연동이라 리뷰에서 확인 |
| `BondYieldController` / `BondYieldResponse` / `GlobalExceptionHandler` 매핑 | 하네스 기본 제외(Controller) |
| 도메인 record 자체 | 로직 없음 |
| 프론트(`company-report.js`, `stock-eval.js`, partial) | 테스트 도구 없음, 수동 확인 |
| 호출 여부·호출 횟수 검증 | 작성하지 않는다(CLAUDE.md) |

## Mock 대상
- **Repository:** 없음
- **외부 API:** 서비스 테스트는 `BondYieldPort`를 테스트용 가짜 구현(stub)으로 대체한다.
  - 날짜별로 스냅샷·빈 스냅샷·예외를 돌려준다.
  - 호출할 때마다 다른 금리를 돌려줄 수 있다.
  - 호출 시 가짜 시간을 진행시킬 수 있다.
- **파서 테스트:** 외부 호출 없음. M0 실측 응답 원문을 fixture로 쓴다.
  - 경로: `src/test/resources/koreaap/`
  - P4~P7은 실측 구조를 변형한 본문을 테스트 안에서 만든다.
- **기타 외부 의존성:** 시간. Caffeine `Ticker`와 `Clock`을 가짜로 주입한다. 오늘은 2026-09-29 KST로 고정한다.

## 테스트 케이스

### KoreaApBondRateParserTest
| Case | Given | When | Then |
|---|---|---|---|
| P1 정상 매핑 | 평일 실측 응답(국고채, 공모 무보증 10등급, 금융채·사모채 행 포함) | 파싱 | 국고채 6개 만기(12·36·60·120·240·360개월)와 공모 무보증 10등급의 제공 만기 금리가 매핑되고, 공모 무보증 BBB- 3·5년이 실측값과 같으며, 금융채·사모채 값은 스냅샷에 없다 |
| P2 결측 정규화 | P1 응답에서 일부 칸을 `-`, 공백, `0`으로 바꾼 본문 | 파싱 | `-`·공백 칸은 rate null(미제공), `0` 칸은 0 |
| P3 빈 날짜 | 휴일 실측 응답 | 파싱 | 빈 스냅샷, 예외 없음 |
| P4 형식 깨짐 (예외) | JSON이 아닌 본문(차단 페이지 등) | 파싱 | `BondYieldParseException` |
| P5 대상 없음 (예외) | 행은 있으나 국고채·공모 무보증 행이 없는 본문 | 파싱 | `BondYieldParseException` |
| P6 대상 중복 (예외) | 같은 등급의 공모 무보증 행이 2개인 본문 | 파싱 | `BondYieldParseException` |
| P7 숫자 아님 (예외) | 수익률 칸이 `abc`인 본문 | 파싱 | `BondYieldParseException` |

### BondYieldQueryServiceTest
| Case | Given | When | Then |
|---|---|---|---|
| S1 요청일 데이터 | 요청일에 데이터 있음 | 조회 | FOUND, 기준일 = 요청일, 출처 이름 = 포트 값 |
| S2 폴백 | 요청일(일)·토는 빈 날짜, 금은 데이터 | 조회 | FALLBACK, 요청일 = 일요일, 기준일 = 금요일 |
| S3 10일 경계 | D~D-9 빈 날짜, D-10 데이터 | 조회 | FALLBACK, 기준일 = D-10 |
| S4 한도 초과 (예외 흐름) | D~D-10 빈 날짜, D-11 데이터 | 조회 | NOT_FOUND, 기준일 null (D-11 미사용) |
| S5 부분 결측 | 요청일 데이터 중 일부 rate null | 조회 | FOUND(폴백 없음), 해당 항목 rate null |
| S6 통신 오류 (예외) | 요청일 조회가 `BondYieldFetchException` | 조회 | `BondYieldFetchException` 전파 (NOT_FOUND·폴백 아님) |
| S7 폴백 중 파싱 오류 (예외) | D 빈 날짜, D-1 `BondYieldParseException`, D-2 데이터 | 조회 | `BondYieldParseException` 전파 (D-2로 건너뛰지 않음) |
| S8 오류 비캐시 | 첫 조회 `BondYieldFetchException`, 이후 정상 데이터 | 같은 날짜 2회 | 두 번째 결과 FOUND |
| S9 캐시 재사용 | 포트가 호출마다 다른 금리를 줌, 과거 기준일 | 같은 날짜 2회 | 두 결과의 금리가 같다(첫 값) |
| S10 만료 후 재조회 | S9 상태 | 24시간 경과 후 재조회 | 새 금리 |
| S11 당일·폴백 짧은 TTL | 오늘 빈 날짜 → 어제로 폴백, 이후 포트가 오늘 데이터를 줌 | 9분 뒤, 11분 뒤 재조회 | 9분 뒤 FALLBACK(기준일 어제), 11분 뒤 FOUND(기준일 오늘) |
| S12 미래 날짜 (예외) | 오늘+1일 | 조회 | `IllegalArgumentException` |
| S13 날짜 생략 | 요청일 null | 조회 | 요청일 = 오늘(KST) |
| S14 벽시계 상한 (예외) | 포트 호출마다 가짜 시간 8초 경과, 모든 날짜 빈 날짜 | 조회 | `BondYieldFetchException`(시간 초과), NOT_FOUND 아님 |
| S15 동시 요청 합치기 | 첫 조회의 포트 호출이 끝나기 전에 같은 날짜 두 번째 요청이 대기, 포트는 호출마다 다른 금리 | 동시 2회 | 두 결과의 금리가 같다 |

**작성 메모**
- S15: 첫 스레드가 포트 안에서 멈춘 상태를 만들고, 두 번째 스레드가 대기(WAITING/BLOCKED)에 들어간 것을 확인한 뒤 풀어 준다. 결과 값만 비교한다.
- S9·S10·S15: 호출 횟수를 세지 않는다. 결과 금리가 같은지 다른지로 캐시·합치기를 확인한다.

## 작성 순서
1. 서비스 테스트(S1~S15)는 M0 없이 먼저 작성한다. 테스트 → 실패 확인 → U1·U3 구현 → 통과 순서다.
2. 파서 테스트(P1~P7)는 M0 fixture를 확보한 뒤 작성한다. 테스트 → 실패 확인 → U2 구현 → 통과 순서다.

## 검증 명령
- `./gradlew test --tests "*KoreaApBondRateParserTest" --tests "*BondYieldQueryServiceTest"`
