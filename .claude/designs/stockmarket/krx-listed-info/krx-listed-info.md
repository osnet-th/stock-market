# KRX 상장종목정보 조회 설계 문서

## 1. 개요

상장 종목 정보를 외부 API로 조회하는 기능을 구현한다.
현재는 공공데이터포털 API를 사용하되, 추후 KRX Open API(`https://openapi.krx.co.kr`)로 교체 가능하도록 설계한다.

**신규 도메인**: `stock` — 포트폴리오와 독립된 주식 도메인으로 분리한다.
추후 코스피 지수, 종목 종가 등 주식 관련 기능을 이 도메인에 확장한다.

**핵심 설계 방향**: domain 계층에 Port 인터페이스를 정의하고, infrastructure 계층에서 API별 Client + Adapter를 구현한다. API 변경 시 Adapter + Client만 교체하면 기존 흐름이 유지된다.

---

## 2. 계층 구조

```
stock/                                          # 주식 도메인 (신규)
├── domain/
│   ├── model/
│   │   └── ListedStock.java                   # 도메인 모델
│   └── service/
│       └── ListedStockPort.java               # 포트 인터페이스
├── application/
│   ├── ListedStockSearchService.java          # 종목 검색 유스케이스
│   └── dto/
│       └── ListedStockResponse.java           # 응답 DTO
├── infrastructure/
│   └── datagokr/                              # 공공데이터포털 구현체 (현재)
│       ├── DataGoKrStockApiClient.java        # HTTP 클라이언트
│       ├── DataGoKrListedStockAdapter.java    # 어댑터 (Port 구현체)
│       ├── config/
│       │   └── DataGoKrProperties.java        # 설정
│       ├── dto/
│       │   ├── DataGoKrStockResponse.java     # API 응답 DTO
│       │   └── DataGoKrStockItem.java         # API 응답 항목 DTO
│       └── exception/
│           └── DataGoKrApiException.java      # API 예외
└── presentation/
    └── ListedStockController.java             # REST 컨트롤러
```

패키지 경로: `com.thlee.stock.market.stockmarket.stock`

---

## 3. Domain 계층

### 3.1 ListedStock (도메인 모델)

API 출처에 무관한 순수 도메인 모델. record로 정의.

- [코드 예시](examples/domain-model-example.md)

### 3.2 ListedStockPort (포트 인터페이스)

domain.service 패키지에 인터페이스만 정의. infrastructure에서 구현.

- `searchByName(String stockName)`: 종목명 부분 일치 검색
- 반환: `List<ListedStock>`
- API 출처를 모르는 순수 인터페이스

- [코드 예시](examples/domain-model-example.md)

---

## 4. Application 계층

### 4.1 ListedStockSearchService

`ListedStockPort`를 주입받아 종목 검색 유스케이스를 수행한다.
도메인 모델 → 응답 DTO 변환을 담당.

- `searchStocks(String stockName)`: Port 호출 → `ListedStockResponse` 리스트 반환
- `@Transactional` 불필요 (외부 API 조회만, DB 변경 없음)

### 4.2 ListedStockResponse (응답 DTO)

presentation 계층에 반환할 DTO. `ListedStock` 도메인 모델에서 변환.

- [코드 예시](examples/application-example.md)

---

## 5. Infrastructure 계층

### 5.1 DataGoKrProperties (설정)

`@ConfigurationProperties`로 프로퍼티 바인딩. 기존 `EcosProperties` 패턴과 동일.

- `baseUrl`: API Base URL
- `serviceKey`: 공공데이터포털 인증키
- `numOfRows`: 한 페이지 결과 수 (기본: 100)

- [코드 예시](examples/infrastructure-example.md)

### 5.2 DataGoKrStockResponse / DataGoKrStockItem (응답 DTO)

공공데이터포털 JSON 응답 구조에 매핑되는 DTO.

```
response.body.items.item[] → List<DataGoKrStockItem>
```

- [코드 예시](examples/infrastructure-example.md)

### 5.3 DataGoKrStockApiClient (HTTP 클라이언트)

`RestClient`를 사용하여 공공데이터포털 API를 호출한다. 기존 `EcosApiClient` 패턴과 동일.

- 역할: HTTP 호출만 담당 (변환 로직 없음)
- 예외: `DataGoKrApiException`으로 래핑

- [코드 예시](examples/infrastructure-example.md)

### 5.4 DataGoKrListedStockAdapter (어댑터)

`ListedStockPort` 구현체. 응답 DTO → 도메인 모델 변환. 기존 `EcosIndicatorAdapter` 패턴과 동일.

- `DataGoKrStockApiClient` 호출 → `DataGoKrStockResponse` 수신
- `DataGoKrStockItem` → `ListedStock` 변환

- [코드 예시](examples/infrastructure-example.md)

### 5.5 DataGoKrApiException (예외)

기존 `EcosApiException` 패턴과 동일.

- [코드 예시](examples/infrastructure-example.md)

### 5.6 추후 KRX Open API 교체 시

`infrastructure/krx/` 패키지를 추가하고 `KrxListedStockAdapter implements ListedStockPort`를 구현한다.
기존 `DataGoKrListedStockAdapter`의 `@Component`를 제거하거나 `@Profile`로 전환하면 된다.

```
교체 범위: infrastructure/ 하위 패키지만
유지 범위: domain, application, presentation 전부 변경 없음
```

---

## 6. Presentation 계층

### 6.1 ListedStockController

종목 검색 REST API 엔드포인트.

- `GET /api/stocks/search?name={stockName}` → `ListedStockSearchService.searchStocks()` 호출
- 인증 필요 (JWT)

- [코드 예시](examples/presentation-example.md)

---

## 7. API 명세

### 7.1 공공데이터포털 - KRX 상장종목정보 (현재)

- **URL**: `https://apis.data.go.kr/1160100/service/GetKrxListedInfoService/getItemInfo`
- **Method**: GET

**요청 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| serviceKey | STRING | Y | 공공데이터포털 인증키 |
| resultType | STRING | N | 응답 포맷 (`json` / `xml`, 기본: xml) |
| numOfRows | INT | N | 한 페이지 결과 수 (기본: 10) |
| pageNo | INT | N | 페이지 번호 (기본: 1) |
| basDt | STRING(8) | N | 기준일자 (YYYYMMDD) |
| srtnCd | STRING | N | 단축코드 (종목코드 6자리) |
| isinCd | STRING | N | ISIN코드 |
| itmsNm | STRING | N | 종목명 (정확히 일치) |
| likeItmsNm | STRING | N | 종목명 (부분 일치 검색) |
| mrktCtg | STRING | N | 시장구분 (`KOSPI` / `KOSDAQ` / `KONEX`) |
| crno | STRING | N | 법인등록번호 |
| corpNm | STRING | N | 법인명 |
| likeCorpNm | STRING | N | 법인명 (부분 일치 검색) |

**응답 JSON 구조**

```json
{
  "response": {
    "header": {
      "resultCode": "00",
      "resultMsg": "NORMAL SERVICE"
    },
    "body": {
      "numOfRows": 100,
      "pageNo": 1,
      "totalCount": 15,
      "items": {
        "item": [
          {
            "basDt": "20260306",
            "srtnCd": "005930",
            "isinCd": "KR7005930003",
            "mrktCtg": "KOSPI",
            "itmsNm": "삼성전자",
            "crno": "1301110006246",
            "corpNm": "삼성전자주식회사"
          }
        ]
      }
    }
  }
}
```

**요청 예제**

종목명 검색 (부분 일치):
```
GET https://apis.data.go.kr/1160100/service/GetKrxListedInfoService/getItemInfo
  ?serviceKey={KEY}
  &resultType=json
  &numOfRows=100
  &pageNo=1
  &likeItmsNm=삼성
```

### 7.2 KRX Open API (추후)

- **URL**: `https://openapi.krx.co.kr` (추후 확인)
- 별도 설계 문서에서 다룸

---

## 8. 주의사항

- `basDt`는 영업일 기준으로만 데이터 존재 (주말/공휴일 → 직전 영업일 사용)
- `numOfRows`를 충분히 크게 설정 (상장 종목 약 2,500개+)
- 데이터 갱신: 기준일 다음 영업일 오후 1시 이후

---

## 9. 사전 준비

1. https://www.data.go.kr 회원가입
2. "금융위원회_KRX상장종목정보" 검색 → 활용 신청
3. 마이페이지에서 발급된 `serviceKey` 확인
4. `application-global.properties`에 키 등록

---

## 10. 작업 리스트

### Domain 계층
- [x] `ListedStock` 도메인 모델 생성
- [x] `ListedStockPort` 포트 인터페이스 정의

### Application 계층
- [x] `ListedStockResponse` 응답 DTO 정의
- [x] `ListedStockSearchService` 유스케이스 구현

### Infrastructure 계층
- [x] `DataGoKrProperties` 설정 클래스 생성
- [x] `DataGoKrStockResponse`, `DataGoKrStockItem` 응답 DTO 정의
- [x] `DataGoKrApiException` 예외 클래스 생성
- [x] `DataGoKrStockApiClient` HTTP 클라이언트 구현
- [x] `DataGoKrListedStockAdapter` 어댑터 구현 (Port 구현체)
- [x] 프로퍼티 등록 (`serviceKey`, `baseUrl`, `numOfRows`)

### Presentation 계층
- [x] `ListedStockController` REST 컨트롤러 구현

---

## 11. 코드 예시

- [도메인 모델 + 포트 예시](examples/domain-model-example.md)
- [Application 계층 예시](examples/application-example.md)
- [Infrastructure 계층 예시](examples/infrastructure-example.md)
- [Presentation 계층 예시](examples/presentation-example.md)