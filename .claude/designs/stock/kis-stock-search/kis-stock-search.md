# KIS 마스터파일 기반 종목 검색 설계

## 작업 리스트

- [x] MarketType enum 작성 (domain)
- [x] ExchangeCode enum 작성 (domain)
- [x] Stock 도메인 모델 수정 (enum 적용 + 해외주식 필드)
- [x] StockResponse DTO 수정
- [x] KisMasterStock DTO 작성 (국내/해외 공통)
- [x] KisDomesticMasterFileParser 작성 (국내 .mst 파싱)
- [x] KisOverseasMasterFileParser 작성 (해외 .cod 파싱)
- [x] KisMasterFileClient 작성 (ZIP 다운로드 + 파서 호출)
- [x] KisStockMasterCache 작성 (Caffeine 캐시 + 검색)
- [x] KisStockAdapter 작성 (`StockPort` 구현체)
- [x] ~~DataGoKrStockAdapter `@Component` 제거~~ → KisStockAdapter에 `@Primary` 추가로 대체
- [x] KisProperties에 마스터파일 설정 추가
- [x] application.yml 설정 추가

## 배경

KIS Open API는 종목명 검색 API를 제공하지 않는다. 대신 국내(KOSPI/KOSDAQ)와 해외(나스닥/뉴욕/아멕스 등) 종목코드 마스터파일을 제공한다. 이를 다운로드/파싱하여 종목명 부분일치 검색을 구현한다.

## 핵심 결정

- **MarketType / ExchangeCode enum**: 시장구분, 거래소코드를 domain enum으로 타입 안전하게 관리
- **전체 시장 지원**: 국내 3개(KOSPI, KOSDAQ, KONEX) + 해외 11개 시장
- **국내/해외 파서 분리**: 국내(.mst)와 해외(.cod)는 파일 포맷이 완전히 다르므로 별도 파서
- **Caffeine 캐시 (TTL 24시간)**: 마스터파일은 매일 갱신, 24시간 후 자동 재다운로드
- **domain 모델 변경**: `Stock`에 `englishName` 추가, `marketType`/`exchangeCode`를 enum으로 변경
- **부분일치 검색**: 한글명 + 영문명 모두 검색 대상

## 마스터파일 포맷

### 국내 (.mst) — 고정길이 바이너리

```
행 구조: [단축코드(9)][표준코드(12)][한글명(가변)] + [부가정보(228 고정, 바이트 기준)]
- 단축코드: row[0:9].trim()  → "005930"
- 표준코드: row[9:21].trim() → "KR7005930003"
- 한글명: row[21:length-228].trim() → "삼성전자"
- 인코딩: CP949
- 다운로드: https://new.real.download.dws.co.kr/common/master/{kospi_code|kosdaq_code|konex_code}.mst.zip
```

### 해외 (.cod) — TSV (탭 구분)

```
컬럼(탭 구분, 24개):
  [0] National code    [1] Exchange id     [2] Exchange code   [3] Exchange name
  [4] Symbol           [5] realtime symbol  [6] Korea name      [7] English name
  [8] Security type    [9] currency        [10] float position  ...
- 종목코드: col[4] (Symbol)
- 한글명: col[6] (Korea name)
- 영문명: col[7] (English name)
- 거래소: col[2] (Exchange code)
- 인코딩: CP949
- 다운로드: https://new.real.download.dws.co.kr/common/master/{nas|nys|ams|shs|shi|szs|szi|tse|hks|hnx|hsx}mst.cod.zip
```

## 패키지 구조

```
stock/infrastructure/stock/kis/
├── config/
│   └── KisProperties.java                  # (기존) + master 설정 추가
├── dto/
│   ├── KisTokenResponse.java               # (기존)
│   └── KisMasterStock.java                 # 국내/해외 공통 파싱 결과
├── exception/
│   └── KisApiException.java                # (기존)
├── KisTokenManager.java                    # (기존)
├── KisDomesticMasterFileParser.java        # 국내 .mst 파싱
├── KisOverseasMasterFileParser.java        # 해외 .cod 파싱
├── KisMasterFileClient.java                # ZIP 다운로드 + 파서 호출
├── KisStockMasterCache.java                # Caffeine 캐시 + 검색
└── KisStockAdapter.java                    # StockPort 구현체
```

## 구현

### MarketType enum

위치: `stock/domain/model/MarketType.java`

전체 시장 구분 enum. 국내 3개 + 해외 11개.

[예시 코드](./examples/domain-model-example.md)

### ExchangeCode enum

위치: `stock/domain/model/ExchangeCode.java`

거래소코드 enum. 국내 1개(KRX) + 해외 10개.

[예시 코드](./examples/domain-model-example.md)

### Stock 도메인 모델 수정

위치: `stock/domain/model/Stock.java`

`marketType` → `MarketType` enum, `exchangeCode` → `ExchangeCode` enum, `englishName` 추가.

[예시 코드](./examples/domain-model-example.md)

### KisMasterStock

위치: `stock/infrastructure/stock/kis/dto/KisMasterStock.java`

국내/해외 마스터파일 파싱 결과를 담는 infrastructure DTO.

[예시 코드](./examples/infrastructure-dto-example.md)

### KisDomesticMasterFileParser

위치: `stock/infrastructure/stock/kis/KisDomesticMasterFileParser.java`

- 국내 .mst 파일(CP949, 고정길이) 파싱
- 뒤 228바이트 부가정보 제외 후, 단축코드 + 표준코드 + 한글명 추출
- 시장구분은 호출 시 파라미터로 전달 (KOSPI / KOSDAQ)

[예시 코드](./examples/infrastructure-parser-example.md)

### KisOverseasMasterFileParser

위치: `stock/infrastructure/stock/kis/KisOverseasMasterFileParser.java`

- 해외 .cod 파일(CP949, TSV) 파싱
- 탭 구분 24개 컬럼에서 Symbol, Korea name, English name, Exchange code 추출

[예시 코드](./examples/infrastructure-parser-example.md)

### KisMasterFileClient

위치: `stock/infrastructure/stock/kis/KisMasterFileClient.java`

- ZIP 다운로드 → 임시파일 → 압축 해제 → 파서 호출
- `downloadAllStocks()`: 국내(KOSPI, KOSDAQ, KONEX) + 해외(NAS, NYS, AMS 등 11개) 전체 다운로드
- 각 파서에게 파싱 위임, 결과 합산

[예시 코드](./examples/infrastructure-master-file-client-example.md)

### KisStockMasterCache

위치: `stock/infrastructure/stock/kis/KisStockMasterCache.java`

- Caffeine 캐시에 전체 종목 리스트 저장 (TTL 24시간, maximumSize 1)
- `searchByName(keyword)`: 한글명 또는 영문명에 keyword 포함 여부로 필터링
- 캐시 미스 시 `KisMasterFileClient.downloadAllStocks()` 호출

[예시 코드](./examples/infrastructure-master-cache-example.md)

### KisStockAdapter

위치: `stock/infrastructure/stock/kis/KisStockAdapter.java`

`StockPort` 구현체. `KisMasterStock` → `Stock` 도메인 모델 변환.

[예시 코드](./examples/infrastructure-adapter-example.md)

### DataGoKrStockAdapter 비활성화

`DataGoKrStockAdapter`에서 `@Component` 제거. `StockPort` 구현체 충돌 방지.

### KisProperties 설정 추가

위치: `stock/infrastructure/stock/kis/config/KisProperties.java`

```yaml
kis:
  api:
    base-url: https://openapi.koreainvestment.com:9443
    app-key: ${KIS_APP_KEY}
    app-secret: ${KIS_APP_SECRET}
    master:
      base-url: https://new.real.download.dws.co.kr/common/master
```

마스터파일 URL 생성에 필요한 시장 목록과 파일명 규칙은 `MarketType` enum에서 관리하므로 yaml에 시장 목록을 별도 설정하지 않음.

## 주의사항

- 국내 .mst 파일은 **바이트 단위** 고정길이 → CP949 바이트 배열로 변환 후 뒤 228바이트 제거
- 해외 .cod 파일은 **탭 구분** 텍스트 → `split("\t")` 파싱, 컬럼 수 검증 필요
- ZIP 다운로드 시 임시파일 생성 후 반드시 삭제
- 첫 요청 시 14개 파일 다운로드로 지연 발생 가능 (이후 24시간 캐시)
