package com.thlee.stock.market.stockmarket.logging.application;

import com.thlee.stock.market.stockmarket.logging.application.event.ApplicationLogEvent;
import com.thlee.stock.market.stockmarket.logging.domain.model.LogDomain;
import com.thlee.stock.market.stockmarket.logging.infrastructure.filter.RequestIdFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 도메인별 비즈니스 이벤트를 로깅 파이프라인으로 흘리는 공개 API.
 *
 * <strong>호출 계층 제약</strong>: 반드시 <b>application 계층</b> (Service 등) 에서만 호출한다.
 * Domain 계층에서 호출 금지 (ARCHITECTURE.md §4 — domain 은 Spring/infra 의존 금지).
 *
 * audit/error 는 AOP 와 전역 예외 핸들러가 자동으로 기록하므로 본 API 는 거의 <b>business 이벤트 전용</b>이다
 * (포트폴리오 변경, 챗봇 질의/응답 메타데이터, 회원가입 등).
 *
 * 커밋 후 처리: 본 API 가 발행한 BUSINESS 이벤트는 {@code @TransactionalEventListener(AFTER_COMMIT)}
 * 에서 처리되어 롤백된 트랜잭션의 오팬 로그를 방지한다.
 */
@Service
@RequiredArgsConstructor
public class DomainEventLogger {

    private static final String UNKNOWN_REQUEST_ID = "unknown";

    private final ApplicationEventPublisher publisher;

    /**
     * 비즈니스 이벤트 기록. payload 에 {@code eventType} 이 자동 병합된다.
     *
     * @param eventType 예: "PORTFOLIO_ITEM_CREATED", "CHATBOT_QUERY_COMPLETED"
     * @param userId    행위 주체 (nullable — 시스템/스케줄러 실행)
     * @param payload   도메인별 자유 필드 (개인정보는 민감 패턴에 맞춰 적재 전 정제됨)
     */
    public void logBusiness(String eventType, Long userId, Map<String, Object> payload) {
        Map<String, Object> enriched = new LinkedHashMap<>();
        enriched.put("eventType", eventType);
        if (payload != null) {
            enriched.putAll(payload);
        }
        publisher.publishEvent(ApplicationLogEvent.of(
                LogDomain.BUSINESS,
                userId,
                currentRequestId(),
                enriched
        ));
    }

    private String currentRequestId() {
        String id = MDC.get(RequestIdFilter.MDC_KEY);
        return id != null ? id : UNKNOWN_REQUEST_ID;
    }
}