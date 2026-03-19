# DART API 에러 코드 세분화 처리 설계

## 작업 리스트

- [x] `DartStatusCode` enum 생성 (DART API 상태 코드 정의)
- [x] `DartApiResponse.isNoData()` 메서드 추가
- [x] `DartApiClient.callApi()`에서 013(데이터 없음) 시 빈 응답 반환하도록 변경
- [x] `DartApiException`에 상태 코드 필드 추가
- [x] `GlobalExceptionHandler`에서 DART 에러 코드별 HTTP 상태 매핑

## 배경

DART API는 status `"013"`(조회된 데이터가 없음)을 정상 응답으로 사용하지만, 현재 `DartApiClient.callApi()`에서 `"000"` 외 모든 응답을 `DartApiException`으로 처리 → 프론트에 502 에러 전파.

소송이 없는 종목 조회 시 빈 리스트를 반환해야 하는데 에러가 발생하는 문제.

## 핵심 결정

- **013은 에러가 아닌 정상 응답**: `callApi()`에서 빈 리스트를 가진 응답 반환
- **에러 코드를 enum으로 관리**: 하드코딩된 `"000"` 제거, 코드별 의미 문서화
- **DartApiException에 코드 보존**: 에러 종류별 HTTP 상태 매핑 가능하도록

## DART API 상태 코드 체계

| 코드 | 의미 | 분류 | HTTP 매핑 |
|------|------|------|-----------|
| `000` | 정상 | 성공 | - |
| `010` | 등록되지 않은 인증키 | 인증 에러 | 502 (서버 설정 문제) |
| `011` | 사용할 수 없는 인증키 | 인증 에러 | 502 |
| `013` | 조회된 데이터가 없음 | **정상 (빈 결과)** | 200 (빈 리스트) |
| `020` | 요청 제한 초과 | Rate Limit | 429 (Too Many Requests) |
| `100` | 필드 오류 | 요청 에러 | 400 (Bad Request) |
| `800` | 시스템 점검 | 서버 에러 | 503 (Service Unavailable) |
| `900` | 정의되지 않은 오류 | 서버 에러 | 502 |

## 구현

### DartStatusCode enum
위치: `stock/infrastructure/stock/dart/dto/DartStatusCode.java`

[예시 코드](./examples/dart-status-code-example.md)

### DartApiResponse 변경
위치: `stock/infrastructure/stock/dart/dto/DartApiResponse.java`

- `isNoData()` 메서드 추가 (status `"013"` 체크)
- 기존 `isSuccess()` 유지

[예시 코드](./examples/dart-api-response-example.md)

### DartApiClient.callApi() 변경
위치: `stock/infrastructure/stock/dart/DartApiClient.java`

- 013 → 빈 `DartApiResponse` 반환 (예외 발생하지 않음)
- 그 외 에러 → `DartApiException`에 상태 코드 포함하여 throw

[예시 코드](./examples/dart-api-client-example.md)

### DartApiException 변경
위치: `stock/infrastructure/stock/dart/exception/DartApiException.java`

- `statusCode` 필드 추가 (`DartStatusCode` 타입)
- 기존 생성자 유지 (하위 호환), 새 생성자 추가

[예시 코드](./examples/dart-api-exception-example.md)

### GlobalExceptionHandler 변경
위치: `infrastructure/web/GlobalExceptionHandler.java`

- `DartApiException`의 statusCode에 따라 HTTP 상태 코드 분기

[예시 코드](./examples/global-exception-handler-example.md)

## 주의사항

- `DartApiException`의 기존 생성자 `(String message)`, `(String message, Throwable cause)` 유지 — 하위 호환
- `DartCorpCodeCache`에서도 `DartApiException`을 직접 생성하므로 기존 생성자 제거 불가
- `callApi()`에서 013 처리 시 `response.getList()`가 null일 수 있으므로, 빈 리스트로 대체된 응답 객체를 새로 만들어서 반환
