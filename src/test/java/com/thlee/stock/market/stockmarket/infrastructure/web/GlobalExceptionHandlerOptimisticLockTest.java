package com.thlee.stock.market.stockmarket.infrastructure.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler — OptimisticLocking 매핑")
class GlobalExceptionHandlerOptimisticLockTest {

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    GlobalExceptionHandler handler;

    @Test
    @DisplayName("ObjectOptimisticLockingFailureException → 409 CONFLICT")
    void optimisticLock_returns409() {
        ObjectOptimisticLockingFailureException e =
                new ObjectOptimisticLockingFailureException("PortfolioItem", 100L);

        ResponseEntity<Map<String, Object>> resp = handler.handleOptimisticLocking(e);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().get("error")).isEqualTo("OPTIMISTIC_LOCK_CONFLICT");
        assertThat(resp.getBody().get("message").toString()).contains("새로고침");
    }
}