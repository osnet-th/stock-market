# DART 재무/공시 API 클라이언트 설계

## 작업 리스트

- [x] DartProperties 구현
- [x] DartApiException 구현
- [x] DART 공통 응답 DTO 구현 (DartApiResponse)
- [x] 10개 API 응답 DTO 구현
- [x] DartApiClient 구현
- [x] application.yml DART 설정 추가

## 배경

DART OpenAPI에서 재무계정, 재무지표, 재무제표, 주식현황, 배당, 소송, 자금사용내역 등 10개 API를 호출하는 인프라 클라이언트를 구현한다.
기존 프로젝트의 외부 API 패턴(Properties + ApiClient + DTO + Exception)을 따른다.

## 핵심 결정

- **위치**: `stock/infrastructure/stock/dart/` (KIS, DataGoKr과 동일 레벨)
- **클라이언트 설계**: 단일 `DartApiClient` 클래스에 API별 메서드 정의 (10개 API가 모두 동일한 base URL, 인증키 사용)
- **응답 구조**: 공통 wrapper `DartApiResponse<T>`로 status/message 처리, list 데이터는 제네릭 타입
- **도메인 포트/어댑터**: 이번 설계 범위 외 (요청 부분만 구현)

## 대상 API

| API명 | 엔드포인트 | 파라미터 방식 |
|-------|-----------|-------------|
| 단일회사 재무계정 | `/api/fnlttSinglAcnt.json` | corp_code + bsns_year + reprt_code |
| 다중회사 재무계정 | `/api/fnlttMultiAcnt.json` | corp_code(쉼표구분, 최대100건) + bsns_year + reprt_code |
| 단일회사 재무지표 | `/api/fnlttSinglIndx.json` | corp_code + bsns_year + reprt_code + idx_cl_code |
| 다중회사 재무지표 | `/api/fnlttCmpnyIndx.json` | corp_code(쉼표구분) + bsns_year + reprt_code + idx_cl_code |
| 단일회사 전체 재무제표 | `/api/fnlttSinglAcntAll.json` | corp_code + bsns_year + reprt_code + fs_div |
| 주식의 총수 현황 | `/api/stockTotqySttus.json` | corp_code + bsns_year + reprt_code |
| 배당에 관한 사항 | `/api/alotMatter.json` | corp_code + bsns_year + reprt_code |
| 소송 등의 제기 | `/api/lwstLg.json` | corp_code + bgn_de + end_de |
| 사모자금 사용내역 | `/api/prvsrpCptalUseDtls.json` | corp_code + bsns_year + reprt_code |
| 공모자금 사용내역 | `/api/pssrpCptalUseDtls.json` | corp_code + bsns_year + reprt_code |

### 지표분류코드 (idx_cl_code)

| 코드 | 분류 |
|------|------|
| M210000 | 수익성 |
| M220000 | 안정성 |
| M230000 | 성장성 |
| M240000 | 활동성 |

## 구현

### 패키지 구조

```
stock/infrastructure/stock/dart/
├── config/
│   └── DartProperties.java
├── dto/
│   ├── DartApiResponse.java              # 공통 응답 wrapper
│   ├── DartSinglAcntItem.java            # 단일회사 재무계정
│   ├── DartMultiAcntItem.java            # 다중회사 재무계정
│   ├── DartSinglIndxItem.java            # 단일회사 재무지표
│   ├── DartCmpnyIndxItem.java            # 다중회사 재무지표
│   ├── DartSinglAcntAllItem.java         # 단일회사 전체 재무제표
│   ├── DartStockTotqyItem.java           # 주식의 총수 현황
│   ├── DartAlotMatterItem.java           # 배당에 관한 사항
│   ├── DartLawsuitItem.java              # 소송 등의 제기
│   ├── DartPrivateFundItem.java          # 사모자금 사용내역
│   └── DartPublicFundItem.java           # 공모자금 사용내역
├── exception/
│   └── DartApiException.java
└── DartApiClient.java
```

### DartProperties

위치: `stock/infrastructure/stock/dart/config/DartProperties.java`

[예시 코드](./examples/infrastructure-config-example.md)

### DartApiException

위치: `stock/infrastructure/stock/dart/exception/DartApiException.java`

[예시 코드](./examples/infrastructure-config-example.md)

### DartApiResponse (공통 응답)

위치: `stock/infrastructure/stock/dart/dto/DartApiResponse.java`

DART API는 모든 응답에 `status`, `message` 필드를 포함하며, 리스트 데이터는 `list` 배열로 제공.

[예시 코드](./examples/infrastructure-dto-example.md)

### API별 응답 DTO

위치: `stock/infrastructure/stock/dart/dto/Dart*Item.java`

[예시 코드](./examples/infrastructure-dto-example.md)

### DartApiClient

위치: `stock/infrastructure/stock/dart/DartApiClient.java`

10개 API 호출 메서드를 단일 클라이언트에서 제공. 공통 에러 처리 포함.

[예시 코드](./examples/infrastructure-client-example.md)

### application.yml 설정

```yaml
dart:
  api:
    base-url: https://opendart.fss.or.kr/api
    crtfc-key: ${DART_API_KEY}
```

## 주의사항

- DART API 일일 요청 횟수 제한이 있으므로, 추후 Rate Limiting 고려 필요
- 다중회사 재무계정은 `corp_code`를 쉼표로 구분하여 최대 100건까지 조회 가능
- 소송 등의 제기 API만 기간 검색(bgn_de/end_de) 방식이고 나머지는 사업연도/보고서코드 방식
- 사모/공모 자금 응답필드는 2018.01.18 기준으로 구/신 필드가 분리됨 → DTO에 양쪽 모두 포함
- 재무지표 API는 2023년 3분기 이후 데이터만 제공 (나머지는 2015년 이후)
- 재무지표 조회 시 `idx_cl_code` 필수 (수익성/안정성/성장성/활동성 중 택 1)