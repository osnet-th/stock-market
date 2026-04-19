---
title: Spring Data ES 6.0.x publishedAt LocalDateTime → LocalDate 변환 에러
date: 2026-04-20
category: integration-issues
module: news
problem_type: integration_issue
component: elasticsearch
symptoms:
  - "MappingConversionException: Unable to convert value '2026-04-20' to java.time.LocalDateTime"
  - "검색 API가 빈 결과를 반환 (ES에 데이터는 존재하지만 변환 실패로 catch 블록에서 빈 결과 반환)"
root_cause: logic_error
resolution_type: code_fix
severity: high
tags:
  - elasticsearch
  - spring-data-elasticsearch
  - localdatetime
  - localdate
  - type-conversion
  - serialization
---

# Spring Data ES 6.0.x publishedAt LocalDateTime → LocalDate 변환 에러

## Problem

Spring Data Elasticsearch 6.0.x에서 ES 문서를 역직렬화할 때 `publishedAt` 필드의 날짜 형식 불일치로 `MappingConversionException`이 발생하여, 뉴스 검색 API가 빈 결과를 반환했다.

## Symptoms

- `MappingConversionException: Conversion exception when converting document id ...`
- `Caused by: ConversionException: Unable to convert value '2026-04-20' to java.time.LocalDateTime for property 'publishedAt'`
- `GET /api/news/search?query=현대차` 호출 시 ES에 매칭 문서가 있지만 빈 결과(`content: []`) 반환
- `NewsElasticsearchSearcher`의 try-catch가 예외를 삼켜서, 에러가 아닌 "검색 결과 없음"처럼 보임

## What Didn't Work

- `@Field(type = FieldType.Date, format = DateFormat.date_optional_time, pattern = "yyyy-MM-dd'T'HH:mm:ss||yyyy-MM-dd")` 어노테이션 추가
- **실패 이유**: Spring Data ES 6.0.x의 `TemporalPropertyValueConverter`는 대상 타입이 `LocalDateTime`이면 커스텀 pattern을 무시하고 항상 datetime 문자열을 기대함

## Solution

`NewsDocument.publishedAt` 필드 타입을 `LocalDateTime` → `LocalDate`로 변경하고, 매퍼에서 타입 변환을 처리한다.

**NewsDocument.java (Before):**
```java
@Field(type = FieldType.Date)
private LocalDateTime publishedAt;
```

**NewsDocument.java (After):**
```java
@Field(type = FieldType.Date, format = DateFormat.date)
private LocalDate publishedAt;
```

**NewsDocumentMapper.toDocument() — LocalDateTime → LocalDate 변환:**
```java
news.getPublishedAt() != null ? news.getPublishedAt().toLocalDate() : null
```

**NewsElasticsearchSearcher.toNews() — LocalDate → LocalDateTime 복원:**
```java
doc.getPublishedAt() != null ? doc.getPublishedAt().atStartOfDay() : null
```

**인덱스 재생성 필요:**
```bash
curl -X DELETE http://localhost:9200/news
# 앱 재시작 후
curl -X POST http://localhost:8080/api/news/backfill
```

## Why This Works

ES는 `LocalDateTime`의 시간이 자정(00:00:00)이면 날짜 부분만 저장한다 (`"2026-04-20"`). Spring Data ES 6.0.x의 `TemporalPropertyValueConverter`는 `LocalDate` 타입에 대해서는 날짜만 있는 문자열을 정상 파싱하지만, `LocalDateTime` 타입에 대해서는 시간 정보가 반드시 포함되어야 한다. ES 문서의 `publishedAt`을 `LocalDate`로 저장하고, 도메인 모델과의 변환은 매퍼에서 처리하는 것이 올바른 접근이다.

## Prevention

- ES 문서 필드의 Java 타입을 정할 때, ES가 실제로 저장하는 형식을 확인할 것 (`curl localhost:9200/index/_search`로 실제 저장 값 확인)
- `LocalDateTime`을 ES에 저장할 때 시간이 자정이면 날짜만 저장될 수 있으므로, 날짜 정밀도만 필요한 필드는 `LocalDate`를 사용할 것
- 검색 어댑터의 try-catch에서 예외를 삼키는 경우, 개발 중에는 스택트레이스 포함 로그(`log.warn("...", e)`)를 남길 것

## Related Issues

- ES 계획서: `docs/plans/2026-04-12-001-feat-elasticsearch-news-search-plan.md`
- 요구사항: `docs/brainstorms/2026-04-12-elasticsearch-news-search-requirements.md`