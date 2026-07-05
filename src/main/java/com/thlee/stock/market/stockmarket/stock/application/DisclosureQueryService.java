package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.DisclosureResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.Disclosure;
import com.thlee.stock.market.stockmarket.stock.domain.model.PublicationType;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockFinancialPort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.config.FinancialTimelineExecutorConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 공시 목록 조회 유스케이스.
 * list.json은 요청당 유형 1개만 받으므로, 다중 유형은 유형별 병렬 호출 후 접수일 최신순 병합한다.
 */
@Service
public class DisclosureQueryService {

    private final StockFinancialPort stockFinancialPort;
    private final Executor executor;

    public DisclosureQueryService(
            StockFinancialPort stockFinancialPort,
            @Qualifier(FinancialTimelineExecutorConfig.EXECUTOR_NAME) Executor executor) {
        this.stockFinancialPort = stockFinancialPort;
        this.executor = executor;
    }

    public List<DisclosureResponse> getDisclosures(
            String stockCode, String fromDate, String toDate, List<PublicationType> types) {
        validate(stockCode);
        String from = resolveFrom(fromDate);
        String to = resolveTo(toDate);
        return fetch(stockCode, from, to, types).stream()
                .sorted(Comparator.comparing(Disclosure::receiptDate).reversed())
                .map(DisclosureResponse::from)
                .toList();
    }

    private List<Disclosure> fetch(String stockCode, String from, String to, List<PublicationType> types) {
        if (types == null || types.isEmpty()) {
            return stockFinancialPort.getDisclosures(stockCode, from, to, null);
        }
        return fetchByTypes(stockCode, from, to, types);
    }

    /**
     * 유형별 병렬 호출 후 평탄화 (유형끼리 중복 없음)
     */
    private List<Disclosure> fetchByTypes(String stockCode, String from, String to, List<PublicationType> types) {
        List<CompletableFuture<List<Disclosure>>> futures = types.stream()
                .map(type -> CompletableFuture.supplyAsync(
                        () -> stockFinancialPort.getDisclosures(stockCode, from, to, type.getCode()), executor))
                .toList();
        return futures.stream().flatMap(future -> future.join().stream()).toList();
    }

    private void validate(String stockCode) {
        if (stockCode == null || !stockCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("종목코드는 6자리 숫자여야 합니다: " + stockCode);
        }
    }

    private String resolveFrom(String fromDate) {
        if (fromDate != null) {
            return fromDate;
        }
        return LocalDate.now().minusYears(1).format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private String resolveTo(String toDate) {
        if (toDate != null) {
            return toDate;
        }
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }
}
