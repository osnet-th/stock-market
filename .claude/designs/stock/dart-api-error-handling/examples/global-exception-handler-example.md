# GlobalExceptionHandler 변경 예시

```java
// DartApiException 전용 핸들러 추가 (기존 handleExternalApi보다 우선 적용)
@ExceptionHandler(DartApiException.class)
public ResponseEntity<Map<String, Object>> handleDartApi(DartApiException e) {
    DartStatusCode statusCode = e.getStatusCode();

    // statusCode 없는 경우 (기존 생성자로 생성된 예외) → 기본 502
    if (statusCode == null) {
        return buildResponse(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR", e.getMessage());
    }

    return switch (statusCode) {
        case RATE_LIMIT_EXCEEDED ->
                buildResponse(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", e.getMessage());
        case INVALID_FIELD ->
                buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        case SYSTEM_MAINTENANCE ->
                buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", e.getMessage());
        default ->
                buildResponse(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR", e.getMessage());
    };
}

// 기존 핸들러에서 DartApiException 제거
@ExceptionHandler({
        KisApiException.class,
        EcosApiException.class,
        NewsApiException.class,
        DataGoKrApiException.class,
        TradingEconomicsFetchException.class,
        TradingEconomicsParseException.class
})
public ResponseEntity<Map<String, Object>> handleExternalApi(RuntimeException e) {
    return buildResponse(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR", e.getMessage());
}
```

## 변경 포인트

- `DartApiException` 전용 핸들러 분리 (상태 코드별 HTTP 매핑)
- 기존 `handleExternalApi()`에서 `DartApiException.class` 제거
- 013(NO_DATA)은 `callApi()`에서 이미 처리되어 여기까지 도달하지 않음
- `statusCode == null`인 경우 기존과 동일하게 502 반환 (하위 호환)