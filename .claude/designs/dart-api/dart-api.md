# DART OpenAPI 설계 문서

## 1. 개요

DART(Data Analysis, Retrieval and Transfer System)는 금융감독원이 운영하는 전자공시시스템이다.
OpenDART는 DART에 공시된 기업의 재무제표, 공시정보 등을 REST API로 제공하는 서비스이다.

- 공식 사이트: https://opendart.fss.or.kr
- 개발가이드: https://opendart.fss.or.kr/guide/main.do

---

## 2. 인증키 발급 및 이용 조건

### 인증키 발급

1. https://opendart.fss.or.kr 회원가입
2. 로그인 후 "인증키 신청/관리" 메뉴에서 인증키 발급
3. 발급된 40자리 인증키(`crtfc_key`)를 모든 API 요청에 포함

### 이용 조건

- 회원당 인증키 **최대 1개** 발급 가능 (2개 이상 불가)
- 인증키 제3자 공유 금지
- 일일 요청 횟수 제한 있음 (구체적 수치는 홈페이지 공지)
- 과도한 접속, 해킹 시도 시 이용 제한 가능
- 서비스 운영: 연중무휴 24시간 (점검 시 제외)

---

## 3. API 공통 사항

### 요청 방식

- **Method**: GET
- **인코딩**: UTF-8
- **포맷**: URL 끝에 `.json` 또는 `.xml`으로 응답 포맷 지정
- **Base URL**: `https://opendart.fss.or.kr/api`

### 요청 URL 구조

```
https://opendart.fss.or.kr/api/{apiName}.json?crtfc_key={KEY}&param1=value1&param2=value2
```

### 공통 응답 필드

| 필드 | 설명 |
|------|------|
| status | 에러 코드 (`000`: 정상, `010`~`901`: 에러) |
| message | 에러 메시지 |

### 공통 코드

**보고서 코드 (reprt_code)**

| 코드 | 보고서 유형 |
|------|-----------|
| 11013 | 1분기보고서 |
| 11012 | 반기보고서 |
| 11014 | 3분기보고서 |
| 11011 | 사업보고서 |

**법인구분 (corp_cls)**

| 코드 | 구분 |
|------|------|
| Y | 유가증권시장 |
| K | 코스닥 |
| N | 코넥스 |
| E | 기타 |

---

## 4. API 카테고리 및 목록

### 4.1 공시정보 (DS001)

#### 4.1.1 공시검색

- **URL**: `/api/list.json`
- **설명**: 공시보고서 검색

**요청 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| crtfc_key | STRING(40) | Y | API 인증키 |
| corp_code | STRING(8) | N | 고유번호 (8자리) |
| bgn_de | STRING(8) | N | 검색시작 접수일자 (YYYYMMDD) |
| end_de | STRING(8) | N | 검색종료 접수일자 (YYYYMMDD) |
| last_reprt_at | STRING(1) | N | 최종보고서 검색여부 (Y/N) |
| pblntf_ty | STRING(1) | N | 공시유형 (A~J) |
| pblntf_detail_ty | STRING(4) | N | 공시상세유형 (A001~J009) |
| corp_cls | STRING(1) | N | 법인구분 (Y/K/N/E) |
| sort | STRING(4) | N | 정렬 (date, crp, rpt) |
| sort_mth | STRING(4) | N | 정렬방법 (asc, desc) |
| page_no | STRING(5) | N | 페이지 번호 (기본값: 1) |
| page_count | STRING(3) | N | 페이지별 건수 (1~100, 기본값: 10) |

**공시유형 코드 (pblntf_ty)**

| 코드 | 유형 |
|------|------|
| A | 정기공시 |
| B | 주요사항보고 |
| C | 발행공시 |
| D | 지분공시 |
| E | 기타공시 |
| F | 외부감사관련 |
| G | 펀드공시 |
| H | 자산유동화 |
| I | 거래소공시 |
| J | 공정위공시 |

**응답 필드**

| 필드 | 설명 |
|------|------|
| page_no | 페이지 번호 |
| total_count | 총 건수 |
| total_page | 총 페이지 수 |
| list[].corp_name | 종목명/법인명 |
| list[].corp_code | 고유번호 |
| list[].stock_code | 종목코드 |
| list[].report_nm | 보고서명 |
| list[].rcept_no | 접수번호 (14자리) |
| list[].rcept_dt | 접수일자 (YYYYMMDD) |
| list[].corp_cls | 법인구분 |
| list[].flr_nm | 공시제출인명 |

**예제 요청**

```
GET https://opendart.fss.or.kr/api/list.json?crtfc_key={KEY}&corp_code=00126380&bgn_de=20240101&end_de=20240131
```

#### 4.1.2 기업개황

- **URL**: `/api/company.json`
- **설명**: 기업 기본 정보 조회

**요청 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| crtfc_key | STRING(40) | Y | API 인증키 |
| corp_code | STRING(8) | Y | 고유번호 (8자리) |

**응답 필드**

| 필드 | 설명 |
|------|------|
| corp_name | 정식회사명칭 |
| corp_name_eng | 영문정식회사명칭 |
| stock_name | 종목명(약식명칭) |
| stock_code | 종목코드 (6자리) |
| ceo_nm | 대표자명 |
| corp_cls | 법인구분 (Y/K/N/E) |
| jurir_no | 법인등록번호 |
| bizr_no | 사업자등록번호 |
| adres | 주소 |
| hm_url | 홈페이지 |
| ir_url | IR홈페이지 |
| phn_no | 전화번호 |
| fax_no | 팩스번호 |
| induty_code | 업종코드 |
| est_dt | 설립일 (YYYYMMDD) |
| acc_mt | 결산월 (MM) |

#### 4.1.3 고유번호 다운로드

- **URL**: `/api/corpCode.xml`
- **설명**: 전체 기업의 고유번호 목록을 ZIP 파일로 다운로드
- **응답**: ZIP 압축 파일 (XML 포함)

**요청 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| crtfc_key | STRING(40) | Y | API 인증키 |

---

### 4.2 정기보고서 주요정보 (DS002)

사업보고서에 포함된 주요 경영정보를 제공하는 API 그룹.
(임원현황, 직원현황, 배당정보, 감사의견 등)

---

### 4.3 정기보고서 재무정보 (DS003)

#### 4.3.1 단일회사 주요계정

- **URL**: `/api/fnlttSinglAcnt.json`
- **설명**: 단일회사의 재무상태표, 손익계산서 주요계정 조회

**요청 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| crtfc_key | STRING(40) | Y | API 인증키 |
| corp_code | STRING(8) | Y | 고유번호 (8자리) |
| bsns_year | STRING(4) | Y | 사업연도 (2015년 이후) |
| reprt_code | STRING(5) | Y | 보고서 코드 |

**응답 필드**

| 필드 | 설명 |
|------|------|
| rcept_no | 접수번호 |
| bsns_year | 사업연도 |
| stock_code | 종목코드 |
| reprt_code | 보고서 코드 |
| account_nm | 계정명 |
| fs_div | 개별/연결구분 (OFS/CFS) |
| fs_nm | 개별/연결명 |
| sj_div | 재무제표구분 (BS/IS/CIS/CF) |
| sj_nm | 재무제표명 |
| thstrm_nm | 당기명 |
| thstrm_amount | 당기금액 |
| frmtrm_nm | 전기명 |
| frmtrm_amount | 전기금액 |
| bfefrmtrm_nm | 전전기명 |
| bfefrmtrm_amount | 전전기금액 |
| ord | 계정과목 정렬순서 |
| currency | 통화 단위 |

**예제 요청**

```
GET https://opendart.fss.or.kr/api/fnlttSinglAcnt.json?crtfc_key={KEY}&corp_code=00126380&bsns_year=2023&reprt_code=11011
```

#### 4.3.2 다중회사 주요계정

- **URL**: `/api/fnlttMultiAcnt.json`
- **설명**: 복수 기업의 재무상태표, 손익계산서 주요계정 비교 조회

#### 4.3.3 단일회사 전체 재무제표

- **URL**: `/api/fnlttSinglAcntAll.json`
- **설명**: XBRL 재무제표의 모든 계정과목 조회

**요청 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| crtfc_key | STRING(40) | Y | API 인증키 |
| corp_code | STRING(8) | Y | 고유번호 (8자리) |
| bsns_year | STRING(4) | Y | 사업연도 (2015년 이후) |
| reprt_code | STRING(5) | Y | 보고서 코드 |
| fs_div | STRING(3) | Y | 개별/연결구분 (OFS: 재무제표, CFS: 연결재무제표) |

**응답 필드**

| 필드 | 설명 |
|------|------|
| rcept_no | 접수번호 |
| reprt_code | 보고서 코드 |
| bsns_year | 사업연도 |
| corp_code | 고유번호 |
| sj_div | 재무제표구분 |
| sj_nm | 재무제표명 |
| account_id | 계정ID |
| account_nm | 계정명 |
| account_detail | 계정상세 |
| thstrm_nm | 당기명 |
| thstrm_amount | 당기금액 |
| frmtrm_nm | 전기명 |
| frmtrm_amount | 전기금액 |
| currency | 통화 단위 |

**예제 요청**

```
GET https://opendart.fss.or.kr/api/fnlttSinglAcntAll.json?crtfc_key={KEY}&corp_code=00126380&bsns_year=2023&reprt_code=11011&fs_div=CFS
```

#### 4.3.4 재무제표 원본파일 (XBRL)

- **URL**: `/api/fnlttXbrl.xml`
- **설명**: XBRL 재무제표 원본파일 다운로드 (ZIP 형식)

#### 4.3.5 단일회사 주요 재무지표

- **URL**: `/api/fnlttSinglIndx.json`
- **설명**: 주요 재무지표 (수익성, 안정성 등) 조회

#### 4.3.6 다중회사 주요 재무지표

- **URL**: `/api/fnlttMultiIndx.json`
- **설명**: 복수 기업의 주요 재무지표 비교 조회

#### 4.3.7 XBRL 택사노미 재무제표 양식

- **URL**: `/api/xbrlTaxonomy.json`
- **설명**: IFRS 기반 XBRL 표준계정과목체계 조회

---

### 4.4 지분공시 종합정보 (DS004)

대량보유, 임원/주요주주 지분변동 등 지분 관련 공시 정보 조회.

---

### 4.5 주요사항보고서 주요정보 (DS005)

부도, 영업정지, 회생절차, 유상증자, 합병 등 주요사항 보고 정보 조회.

---

### 4.6 증권신고서 주요정보 (DS006)

증권 발행 관련 신고서 정보 조회.

---

## 5. 재무제표 구분 코드

| 코드 | 구분 |
|------|------|
| BS | 재무상태표 |
| IS | 손익계산서 |
| CIS | 포괄손익계산서 |
| CF | 현금흐름표 |
| SCE | 자본변동표 |

---

## 6. 참고 자료

- [OpenDART 공식 사이트](https://opendart.fss.or.kr)
- [OpenDART 개발가이드](https://opendart.fss.or.kr/guide/main.do)
- [OpenDART 이용약관](https://opendart.fss.or.kr/intro/terms.do)
- [OpenDART 영문 가이드](https://engopendart.fss.or.kr/guide/main.do)
- [dart-fss Python 라이브러리 문서](https://dart-fss.readthedocs.io/en/latest/dart_api.html)
- [OpenDartReader GitHub](https://github.com/FinanceData/OpenDartReader)
