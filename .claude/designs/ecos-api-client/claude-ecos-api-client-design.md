# ECOS (한국은행) API Client 인프라 계층 설계

- 작성일: 2026-02-18

---

## 목표

한국은행 ECOS Open API의 100대 주요 경제지표(KeyStatisticList)를 요청하고 응답을 파싱하는 인프라 계층 구현

---

## 대상 API

- 엔드포인트: `https://ecos.bok.or.kr/api/KeyStatisticList/{API_KEY}/json/kr/{startCount}/{endCount}`
- 인증: URL Path에 API Key 포함그러면 
- 응답 형식: JSON

---

## 응답 구조

```
{
  "KeyStatisticList": {
    "list_total_count": 101,        // 전체 건수
    "row_count": 101,               // 반환 건수
    "row": [
      {
        "CLASS_NAME": "시장금리",      // 분류명
        "KEYSTAT_NAME": "한국은행 기준금리",  // 지표명
        "DATA_VALUE": "2.5",          // 값 (String, null 가능)
        "CYCLE": "20260216",          // 기준시점 (YYYYMMDD / YYYYMM / YYYYQn / YYYY, null 가능)
        "UNIT_NAME": "%"              // 단위 (null 가능)
      }
    ]
  }
}
```

### 특이사항

- `DATA_VALUE`: String 타입, null 가능
- `CYCLE`: 형식 혼재 (일별/월별/분기별/연간), null 가능
- `UNIT_NAME`: null 가능

---

## 패키지 구조

```
economics/
└── infrastructure/
    ├── korea/
    │   └── ecos/
    │       ├── EcosApiClient.java            // API 호출 담당
    │       ├── config/
    │       │   └── EcosProperties.java       // 설정 바인딩 (@ConfigurationProperties)
    │       ├── dto/
    │       │   ├── EcosKeyStatResponse.java  // 최상위 응답 래퍼
    │       │   └── EcosKeyStatRow.java       // row 항목
    │       └── exception/
    │           └── EcosApiException.java     // API 호출 예외
    └── global/
        └── fred/                             // 향후 FRED API 구현 위치
```

---

## 클래스별 역할

### EcosProperties

- `@ConfigurationProperties(prefix = "economics.api.korea.ecos")` 바인딩
- 필드: `baseUrl`, `apiKey`, `startCount` (기본값: 1), `endCount` (기본값: 200)

### EcosKeyStatResponse

- 최상위 JSON 래퍼
- 필드: `KeyStatisticList` (내부에 `list_total_count`, `row_count`, `row`)

### EcosKeyStatRow

- `row` 배열의 각 항목 매핑
- 필드: `CLASS_NAME`, `KEYSTAT_NAME`, `DATA_VALUE`, `CYCLE`, `UNIT_NAME` (모두 String)

### EcosApiClient

- `RestClient`를 사용하여 ECOS API 호출
 - 메서드: `fetchKeyStatistics()` -> `EcosKeyStatResponse` 반환 (startCount/endCount는 `EcosProperties`에서 주입)
- 실패 시 `EcosApiException` throw

### EcosApiException

- `RuntimeException` 상속
- 기존 `NewsApiException` 패턴과 동일

---

## 환경변수 설정

### .env 파일

```
# ECOS (한국은행) API
ECOS_API_KEY=your_ecos_api_key
```

### .env.example 파일 추가 항목

```
# ECOS (한국은행) API
ECOS_API_KEY=your_ecos_api_key
```

### application.yml 설정

```yaml
economics:
  api:
    korea:
      ecos:
        base-url: https://ecos.bok.or.kr/api/KeyStatisticList
        api-key: ${ECOS_API_KEY}
        start-count: 1
        end-count: 200
    global:
      fred:
        # 향후 FRED API 설정 위치
```

- 기존 프로젝트 패턴과 동일하게 `${ECOS_API_KEY}`로 환경변수 참조
- `.env` 파일에 실제 키 값 설정, `.env.example`에 템플릿 추가

---

## 코드 예시

- [EcosProperties](examples/EcosProperties-example.md)
- [EcosApiClient](examples/EcosApiClient-example.md)
- [EcosKeyStatResponse / EcosKeyStatRow](examples/EcosDto-example.md)
- [EcosApiException](examples/EcosApiException-example.md)

---

## 기존 패턴 참조

- `GNewsApiClient` / `GNewsProperties` / `GNewsResponse` 패턴을 동일하게 적용
- `RestClient` Bean은 기존 `RestClientConfig`에서 제공하는 것을 공유