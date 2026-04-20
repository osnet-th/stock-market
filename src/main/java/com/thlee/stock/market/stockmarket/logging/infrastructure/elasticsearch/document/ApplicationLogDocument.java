package com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch.document;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.Map;

/**
 * 애플리케이션 로그 Elasticsearch 문서 모델 (AUDIT/ERROR/BUSINESS 공용).
 *
 * 실제 인덱스명은 저장 시점에 {@code IndexCoordinates} 로 동적으로 주입한다
 * (월 단위 롤링: {@code app-audit-YYYY.MM} 등). {@code indexName} 은 템플릿 매칭 기본값.
 *
 * 타임스탬프는 {@link DateFormat#date_time} (ISO 8601) 로 직렬화한다.
 * {@code epoch_millis} 는 Spring Data ES 직렬화 버그(#2318)로 사용 금지.
 */
@Document(indexName = "app-log")
@Getter
public class ApplicationLogDocument {

    @Id
    private String id;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant timestamp;

    @Field(type = FieldType.Keyword)
    private String domain;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Keyword)
    private String requestId;

    @Field(type = FieldType.Object, enabled = false)
    private Map<String, Object> payload;

    @Field(type = FieldType.Boolean)
    private boolean truncated;

    @Field(type = FieldType.Integer)
    private Integer originalSize;

    // 검색 필터 지원용 최상위 프로젝션 필드 (payload 내부는 enabled=false 라 직접 검색 불가)
    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Keyword)
    private String exceptionClass;

    protected ApplicationLogDocument() {
    }

    public ApplicationLogDocument(String id,
                                  Instant timestamp,
                                  String domain,
                                  Long userId,
                                  String requestId,
                                  Map<String, Object> payload,
                                  boolean truncated,
                                  Integer originalSize,
                                  Integer status,
                                  String exceptionClass) {
        this.id = id;
        this.timestamp = timestamp;
        this.domain = domain;
        this.userId = userId;
        this.requestId = requestId;
        this.payload = payload;
        this.truncated = truncated;
        this.originalSize = originalSize;
        this.status = status;
        this.exceptionClass = exceptionClass;
    }
}