package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialAccountResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockQuantityResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.ValuationMetricResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.Stock;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValuationMetricService {

    private static final String NET_INCOME_ACCOUNT = "당기순이익";
    private static final String EQUITY_ACCOUNT = "자본총계";
    private static final String REPORT_CODE_ANNUAL = "11011";
    private static final int SCALE = 2;

    private final StockFinancialService stockFinancialService;
    private final StockPriceService stockPriceService;
    private final StockPort stockPort;

    public ValuationMetricResponse calculate(String stockCode) {
        List<String> warnings = new ArrayList<>();
        String year = String.valueOf(LocalDate.now().getYear());

        List<FinancialAccountResponse> accounts = safeGetAccounts(stockCode, year, warnings);
        String termName = accounts.stream()
            .map(FinancialAccountResponse::getCurrentTermName)
            .filter(v -> v != null && !v.isBlank())
            .findFirst()
            .orElse(null);

        BigDecimal netIncome = findCurrentAmount(accounts, NET_INCOME_ACCOUNT, warnings);
        BigDecimal equity = findCurrentAmount(accounts, EQUITY_ACCOUNT, warnings);

        BigDecimal shares = fetchDistributedShares(stockCode, year, warnings);
        BigDecimal eps = divide(netIncome, shares);
        BigDecimal bps = divide(equity, shares);

        BigDecimal price = fetchReferencePrice(stockCode, warnings);
        BigDecimal per = divide(price, eps);
        BigDecimal pbr = divide(price, bps);

        return new ValuationMetricResponse(
            termName,
            eps,
            bps,
            per,
            pbr,
            price != null ? LocalDate.now().toString() : null,
            price,
            warnings
        );
    }

    private List<FinancialAccountResponse> safeGetAccounts(String stockCode, String year, List<String> warnings) {
        try {
            return stockFinancialService.getFinancialAccounts(stockCode, year, REPORT_CODE_ANNUAL);
        } catch (Exception e) {
            log.warn("재무계정 조회 실패: stockCode={}, year={}", stockCode, year, e);
            warnings.add("재무계정 조회 실패: " + e.getMessage());
            return List.of();
        }
    }

    private BigDecimal findCurrentAmount(List<FinancialAccountResponse> accounts, String accountName, List<String> warnings) {
        BigDecimal value = accounts.stream()
            .filter(a -> accountName.equals(a.getAccountName()))
            .map(FinancialAccountResponse::getCurrentTermAmount)
            .map(ValuationMetricService::parseAmount)
            .filter(v -> v != null)
            .findFirst()
            .orElse(null);
        if (value == null) {
            warnings.add(accountName + " 값을 찾지 못했습니다.");
        }
        return value;
    }

    private BigDecimal fetchDistributedShares(String stockCode, String year, List<String> warnings) {
        try {
            List<StockQuantityResponse> quantities = stockFinancialService.getStockQuantities(stockCode, year, REPORT_CODE_ANNUAL);
            BigDecimal shares = quantities.stream()
                .map(StockQuantityResponse::getDistributedStockCount)
                .map(ValuationMetricService::parseAmount)
                .filter(v -> v != null && v.signum() > 0)
                .findFirst()
                .orElse(null);
            if (shares == null) {
                warnings.add("유통주식수를 확인하지 못했습니다.");
            }
            return shares;
        } catch (Exception e) {
            log.warn("유통주식수 조회 실패: stockCode={}, year={}", stockCode, year, e);
            warnings.add("유통주식수 조회 실패: " + e.getMessage());
            return null;
        }
    }

    private BigDecimal fetchReferencePrice(String stockCode, List<String> warnings) {
        Optional<Stock> stock = stockPort.findByCode(stockCode);
        if (stock.isEmpty()) {
            warnings.add("종목 시장 정보를 찾지 못해 주가 조회를 건너뜁니다.");
            return null;
        }
        try {
            StockPriceResponse response = stockPriceService.getPrice(
                stockCode, stock.get().marketType(), stock.get().exchangeCode()
            );
            BigDecimal price = parseAmount(response.getCurrentPrice());
            if (price == null || price.signum() <= 0) {
                warnings.add("현재 주가를 확인하지 못했습니다.");
                return null;
            }
            return price;
        } catch (Exception e) {
            log.warn("주가 조회 실패: stockCode={}", stockCode, e);
            warnings.add("주가 조회 실패: " + e.getMessage());
            return null;
        }
    }

    private static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() == 0) {
            return null;
        }
        return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
    }
}