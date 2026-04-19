---
date: 2026-04-12
topic: elasticsearch-news-search
---

# Elasticsearch 기반 뉴스 전문 검색 도입

## Problem Frame

현재 뉴스 시스템은 매시간 3개 외부 API(Naver, GNews, NewsAPI)에서 ~7,000건/일의 기사를 수집하여 PostgreSQL에 저장하지만, `keyword_id` 기반 조회만 가능하다. 사용자가 "삼성전자 반도체 실적"처럼 자유롭게 뉴스를 검색할 수 없고, AI 챗봇도 관련 뉴스를 컨텍스트로 활용하지 못한다.

Elasticsearch를 도입하여 저장된 뉴스의 제목/본문 전문 검색을 지원하고, 한국어 형태소 분석을 통해 검색 품질을 확보한다.

## Requirements

**사용자 검색 기능**
- R1. 사용자가 검색어를 입력하면 뉴스 제목과 본문에서 전문 검색하여 관련도 순으로 결과를 반환한다 (본문이 null인 기사는 제목만으로 검색)
- R2. 한국어 형태소 분석을 적용하여 "삼성전자" 검색 시 "삼성", "전자" 포함 기사도 매칭한다
- R3. 날짜 범위 필터링을 지원한다 (시작일~종료일)
- R4. 지역(DOMESTIC/INTERNATIONAL) 필터링을 지원한다
- R5. 검색 결과는 페이지네이션을 지원한다

**AI 챗봇 RAG 컨텍스트**
- R6. 챗봇이 사용자 질문에 답변할 때, 관련 뉴스를 ES에서 검색하여 컨텍스트로 제공한다
- R7. RAG 검색은 사용자의 포트폴리오 종목 또는 대화 주제와 관련된 최신 뉴스를 우선 반환한다

**데이터 동기화**
- R8. 배치 수집 완료 후, 저장된 뉴스를 비동기로 ES 인덱스에 동기화한다 (기존 배치 흐름에 연동)
- R9. ES 장애 시에도 기존 뉴스 수집 및 keyword_id 기반 조회는 정상 동작해야 한다 (전문 검색 기능만 일시 불가)

## Success Criteria

- 한국어 뉴스 검색에서 형태소 분석이 적용되어 관련 기사를 정확히 반환한다
- 검색 API 응답 시간이 500ms 이내이다
- 챗봇이 뉴스 컨텍스트를 활용하여 더 구체적인 답변을 제공한다
- ES 장애 시 기존 keyword_id 기반 뉴스 조회는 정상 동작하며, 전문 검색 기능만 일시 불가

## Scope Boundaries

- 뉴스 트렌드 집계(일별 기사 수, 키워드 빈도 분석 등)는 이번 범위에 포함하지 않는다
- 경제지표, 포트폴리오 등 다른 도메인의 ES 인덱싱은 포함하지 않는다
- 외부 API 실시간 검색 연동은 포함하지 않는다 (저장된 뉴스만 대상)
- 검색 자동완성/추천 기능은 포함하지 않는다
- 뉴스 감성 분석(sentiment analysis)은 포함하지 않는다

## Key Decisions

- **Elasticsearch 선택 이유**: PostgreSQL `tsvector`도 전문 검색을 지원하지만, 한국어 형태소 분석(nori 플러그인)과 관련도 스코어링에서 ES가 확실히 우위. 또한 향후 다른 도메인 확장 시 재활용 가능
- **동기화 방식**: 배치 수집 완료 후 비동기 ES 인덱싱 (NewsSaveService에 이벤트 발행 또는 직접 호출 추가 필요). CDC나 별도 파이프라인은 현재 규모에서 과도
- **ES 장애 격리**: ES는 검색 전용 보조 수단으로, 장애 시 기존 keyword_id 기반 조회로 폴백. 전문 검색만 일시 불가
- **RAG 통합 방식**: 현재 ChatMode에 NEWS 모드가 없으므로, 기존 모드(PORTFOLIO/FINANCIAL/ECONOMIC)에 뉴스 컨텍스트를 크로스컷팅으로 주입하거나 새 NEWS 모드를 추가하는 방식 중 Planning에서 결정

## Dependencies / Assumptions

- 미니 홈서버(8GB RAM)에서 Docker Compose로 ES를 운영한다 (싱글 노드, ES heap 2GB 할당 — 다른 서비스와 메모리 여유 확보)
- Elasticsearch 8.x + nori 한국어 형태소 분석 플러그인을 커스텀 Docker 이미지에 포함한다
- 기존 뉴스 데이터 초기 마이그레이션(백필)이 필요하다
- 홈서버 리소스 제약으로 ES 레플리카는 0으로 설정한다 (싱글 노드)
- Spring Boot 4 호환 ES 클라이언트 라이브러리 선택이 필요하다 (RestHighLevelClient 미지원, Elasticsearch Java Client 또는 Spring Data ES 5.x)

## Outstanding Questions

### Deferred to Planning
- [Affects R2][Needs research] nori 플러그인의 커스텀 사전(종목명, 경제 용어 등) 설정 범위
- [Affects R6, R7][Technical] ChatContextBuilder에 뉴스 RAG 컨텍스트 추가 방식 — 새 ChatMode(NEWS) 추가 vs 기존 모드에 크로스컷팅 주입
- [Affects R8][Technical] 기존 뉴스 데이터 백필(초기 마이그레이션) 전략 및 배치 크기
- [Affects R8][Technical] NewsSaveService에 ES 동기화 후크 포인트 추가 방식 (이벤트 발행 vs 직접 호출)
- [Affects R8][Technical] ES 인덱싱 실패 시 처리 전략 (재시도/큐잉/무시)
- [Affects R9][Technical] ES 장애 감지 및 폴백 메커니즘의 구체적 구현 (Circuit Breaker 등)
- [Affects R1][Technical] ES 인덱스 매핑 설계 (필드별 analyzer 설정, 검색 가중치 등)
- [Affects R1][Technical] 검색 API를 기존 NewsController에 추가할지 별도 SearchController로 분리할지
- [Affects all][Needs research] Spring Boot 4와 Elasticsearch Java Client / Spring Data ES 호환성 확인

## Next Steps

→ `/ce:plan` for structured implementation planning
