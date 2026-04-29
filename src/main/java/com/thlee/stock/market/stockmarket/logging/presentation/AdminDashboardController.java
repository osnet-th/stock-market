package com.thlee.stock.market.stockmarket.logging.presentation;

import com.thlee.stock.market.stockmarket.logging.application.LogSearchService;
import com.thlee.stock.market.stockmarket.logging.application.annotation.SkipAdminAudit;
import com.thlee.stock.market.stockmarket.logging.application.dto.IncidentCountResult;
import com.thlee.stock.market.stockmarket.logging.presentation.dto.IncidentCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메인 대시보드 운영자 카드용 관리자 전용 REST API.
 *
 * <p>인가는 {@code AdminGuardInterceptor} 가 {@code /api/admin/dashboard/**} 경로에서 처리.
 * {@code AdminLogController} 의 {@code /{domain}} path-variable 패턴과 sibling 충돌을 피하기 위해
 * 매핑을 분리했다 — 본 컨트롤러는 logs 클래스 매핑 밖에 위치한다.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final LogSearchService logSearchService;

    /**
     * 메인 대시보드 운영자 카드용 — 오늘 ERROR 도메인 카운트(KST).
     *
     * <p>{@link SkipAdminAudit} — admin 사용자의 home 진입마다 호출되므로 정상 응답(2xx) 의
     * ADMIN_LOG_ACCESS audit 는 발행하지 않음. 4xx/5xx 응답은 audit 발행 유지.
     */
    @SkipAdminAudit
    @GetMapping("/incidents/today")
    public IncidentCountResponse incidentsToday() {
        IncidentCountResult result = logSearchService.countErrorsForToday();
        return IncidentCountResponse.from(result);
    }
}