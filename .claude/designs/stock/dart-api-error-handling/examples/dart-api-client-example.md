# DartApiClient.callApi() 변경 예시

```java
/**
 * API 호출 공통 처리
 * - 013(데이터 없음): 빈 응답 반환 (에러 아님)
 * - 그 외 실패: DartApiException throw (상태 코드 포함)
 */
private <T> DartApiResponse<T> callApi(
        String uri, ParameterizedTypeReference<DartApiResponse<T>> typeRef) {
    try {
        DartApiResponse<T> response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(typeRef);

        if (response == null) {
            throw new DartApiException("DART API 응답이 null입니다");
        }

        if (response.isSuccess()) {
            return response;
        }

        // 013: 데이터 없음 → 빈 리스트 반환 (정상 처리)
        if (response.isNoData()) {
            return DartApiResponse.empty();
        }

        // 그 외 에러: 상태 코드 포함하여 예외 발생
        throw new DartApiException(
                response.getStatusCode(),
                "DART API 오류 [" + response.getStatus() + "]: " + response.getMessage());

    } catch (RestClientException e) {
        throw new DartApiException("DART API 호출 실패: " + e.getMessage(), e);
    }
}
```

## 변경 포인트

- `isNoData()` 체크 추가 → `DartApiResponse.empty()` 반환
- 에러 시 `DartApiException`에 `DartStatusCode` 전달
- 기존 `!response.isSuccess()` 분기를 성공/데이터없음/에러 3단계로 세분화