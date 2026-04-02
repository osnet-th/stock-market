---
date: 2026-04-03
topic: kis-overseas-news
---

# KIS 해외주식 뉴스 조회

## Problem Frame

포트폴리오에 해외 주식을 보유한 사용자가 해당 종목의 최신 뉴스와 속보를 빠르게 확인할 수 없다. 한국투자증권이 제공하는 해외뉴스종합/해외속보 API를 활용하여, 포트폴리오 화면에서 해외 종목의 뉴스를 실시간으로 조회할 수 있도록 한다.

## User Flow

```
포트폴리오 목록
  └─ 해외주식 종목 선택
       └─ [속보] 버튼 클릭
            └─ 뉴스 패널 표시
                 ├─ [해외속보] 탭 ── KIS 해외속보 API 실시간 호출 ── 뉴스 목록 표시
                 └─ [해외뉴스종합] 탭 ── KIS 해외뉴스종합 API 실시간 호출 ── 뉴스 목록 표시
```

## Requirements

**포트폴리오 UI**
- R1. 포트폴리오 화면에서 해외주식(MarketType.OVERSEAS) 종목에 [속보] 버튼을 표시한다
- R2. [속보] 버튼 클릭 시 뉴스 패널이 열리고, "해외속보" / "해외뉴스종합" 두 개의 탭으로 구성된다

**해외속보 탭**
- R3. KIS 해외속보(제목) API(`/uapi/overseas-price/v1/quotations/brknews-title`, tr_id: `FHKST01011801`)를 호출하여 해당 종목의 속보 목록을 표시한다
- R4. 표시 항목: 작성일시(`data_dt` + `data_tm`), 제목(`hts_pbnt_titl_cntt`), 자료원(`dorg`)
- R5. 최대 100건 조회 (API 제한, 페이지네이션 없음)

**해외뉴스종합 탭**
- R6. KIS 해외뉴스종합(제목) API(`/uapi/overseas-price/v1/quotations/news-title`, tr_id: `HHPSTH60100C1`)를 호출하여 해당 종목의 뉴스 목록을 표시한다
- R7. 표시 항목: 조회일시(`data_dt` + `data_tm`), 제목(`title`), 중분류명(`class_name`), 자료원(`source`), 종목명(`symb_name`)
- R8. 페이지네이션 지원 — `tr_cont` 헤더(M/F)로 다음 페이지 존재 여부 판단, "더보기" 방식으로 추가 로드

**데이터 조회 방식**
- R9. DB 저장 없이 온디맨드 API 호출 방식 — 탭 클릭 시 KIS API를 직접 호출하여 최신 데이터 표시 (자동 폴링 없음, 사용자 액션 기반)
- R10. 기존 뉴스 시스템(키워드 기반)과 완전 분리된 별도 도메인으로 구현

**UI 스타일**
- R11. 뉴스 목록 UI는 기존 키워드 뉴스 표시 방식(NewsComponent)을 참고하여 일관된 디자인 유지

## Success Criteria

- 포트폴리오에서 해외 종목 선택 후 [속보] 버튼으로 해외속보/해외뉴스종합을 탭 전환하며 조회할 수 있다
- KIS API 응답이 뉴스 목록으로 정상 표시된다
- 기존 뉴스 시스템에 영향 없이 독립적으로 동작한다

## Scope Boundaries

- 뉴스 본문(상세 내용) 조회는 이번 범위에 포함하지 않음 (제목만 표시)
- DB 저장/스케줄러 수집 없음 — 실시간 조회만
- 기존 키워드 뉴스 시스템 변경 없음
- 국내 주식 뉴스는 이번 범위에 포함하지 않음

## Key Decisions

- **기존 뉴스 시스템과 분리**: KIS 해외뉴스는 종목코드 기반 피드형 API로, 키워드 기반 기존 시스템과 데이터 모델이 다르므로 별도 도메인으로 구현
- **실시간 API 호출**: 속보성 데이터이므로 DB 캐싱보다 실시간 조회가 적합
- **포트폴리오 화면 내 배치**: 보유 종목 맥락에서 뉴스를 확인하는 것이 가장 자연스러운 UX

## KIS API Reference

### 해외속보(제목) [해외주식-055]

| 항목 | 값 |
|---|---|
| 경로 | `/uapi/overseas-price/v1/quotations/brknews-title` |
| tr_id | `FHKST01011801` |
| 주요 파라미터 | `FID_NEWS_OFER_ENTP_CODE` (0=전체), `FID_COND_SCR_DIV_CODE` (11801), `FID_INPUT_ISCD` (종목코드), `FID_TITL_CNTT` (제목검색) |
| 응답 | `data_dt`, `data_tm`, `hts_pbnt_titl_cntt`, `dorg`, `iscd1~10`, `kor_isnm1~10` |
| 제한 | 최대 100건 |

### 해외뉴스종합(제목) [해외주식-053]

| 항목 | 값 |
|---|---|
| 경로 | `/uapi/overseas-price/v1/quotations/news-title` |
| tr_id | `HHPSTH60100C1` |
| 주요 파라미터 | `NATION_CD` (국가코드), `EXCHANGE_CD` (거래소코드), `SYMB` (종목코드), `DATA_DT`, `DATA_TM`, `CTS` (다음키) |
| 응답 | `data_dt`, `data_tm`, `title`, `class_name`, `source`, `nation_cd`, `symb`, `symb_name` |
| 페이지네이션 | `tr_cont` 헤더 (M/F = 다음 페이지 있음) |

## Outstanding Questions

### Deferred to Planning
- [Affects R3, R6][Technical] 해외속보 API의 `FID_INPUT_ISCD`와 해외뉴스종합 API의 `SYMB` 파라미터에 전달할 종목코드 형식 확인 필요 (포트폴리오에 저장된 종목코드와 KIS API가 기대하는 형식이 일치하는지)
- [Affects R2][Needs research] 포트폴리오 화면의 현재 구조를 확인하여 [속보] 버튼과 뉴스 패널의 최적 배치 위치 결정
- [Affects R1][Needs research] 포트폴리오에서 해외주식 여부를 판별하는 필드 확인 (`MarketType.OVERSEAS` 등 포트폴리오 도메인의 실제 식별 방식)
- [Affects R8][Technical] 기존 `KisApiClient`가 응답 헤더(`tr_cont`)를 반환하지 않으므로, 뉴스종합 API 페이지네이션을 위한 클라이언트 확장 범위 확인
- [Affects R3, R6][Technical] KIS 뉴스 API의 실제 응답 구조 확인 (`output` vs `output1` 등 기존 `KisApiResponse`와의 호환성)

## Next Steps

→ `/ce:plan` for structured implementation planning
