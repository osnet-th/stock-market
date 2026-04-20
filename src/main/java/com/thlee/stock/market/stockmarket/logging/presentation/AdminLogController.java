package com.thlee.stock.market.stockmarket.logging.presentation;

import com.thlee.stock.market.stockmarket.logging.application.LogSearchService;
import com.thlee.stock.market.stockmarket.logging.domain.model.LogDomain;
import com.thlee.stock.market.stockmarket.logging.presentation.dto.LogDailyCountResponse;
import com.thlee.stock.market.stockmarket.logging.presentation.dto.LogDiskUsageResponse;
import com.thlee.stock.market.stockmarket.logging.presentation.dto.LogSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 운영자 전용 로그 조회 API.
 *
 * <p>인가는 {@code AdminGuardInterceptor} 가 {@code /api/admin/logs/**} 경로에서 처리 —
 * 여기서는 컨트롤러 레벨 인가 로직을 두지 않는다.</p>
 *
 * 엔드포인트:
 * <ul>
 *   <li>GET /api/admin/logs/{domain}                — 검색 (search_after cursor 페이징)</li>
 *   <li>GET /api/admin/logs/{domain}/aggregations   — 일별/주별 건수 집계 (60초 캐시)</li>
 *   <li>GET /api/admin/logs/disk-usage              — 로그 인덱스 디스크 사용량 (60초 캐시)</li>
 *   <li>GET /api/admin/logs/{domain}/download       — 현재 페이지 원문 JSON 다운로드</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private static final DateTimeFormatter FILENAME_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final LogSearchService logSearchService;

    @GetMapping("/{domain}")
    public LogSearchResponse search(
            @PathVariable String domain,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String exceptionClass,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> searchAfter
    ) {
        LogDomain parsed = parseDomain(domain);
        List<Object> cursor = searchAfter != null ? new ArrayList<>(searchAfter) : null;
        return logSearchService.search(parsed, from, to, userId, q, status, exceptionClass, size, cursor);
    }

    @GetMapping("/{domain}/aggregations")
    public LogDailyCountResponse aggregate(
            @PathVariable String domain,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        return logSearchService.aggregateByDate(parseDomain(domain), from, to);
    }

    @GetMapping("/disk-usage")
    public LogDiskUsageResponse diskUsage() {
        return logSearchService.diskUsage();
    }

    @GetMapping("/{domain}/download")
    public ResponseEntity<LogSearchResponse> download(
            @PathVariable String domain,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String exceptionClass
    ) {
        LogDomain parsed = parseDomain(domain);
        LogSearchResponse result = logSearchService.search(
                parsed, from, to, userId, q, status, exceptionClass, 100, null);
        // filename 은 서버가 생성 — Content-Disposition 에 사용자 입력 주입 불가 (CRLF injection 방지)
        String filename = "logs-" + parsed.getIndexSuffix() + "-"
                + LocalDateTime.now(SEOUL).format(FILENAME_TS) + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    private LogDomain parseDomain(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "domain is required");
        }
        try {
            return LogDomain.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown domain: " + raw);
        }
    }
}