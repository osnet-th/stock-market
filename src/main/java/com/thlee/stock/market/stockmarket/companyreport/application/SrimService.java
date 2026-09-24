package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.SrimData;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.SrimInputData;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.SrimCalculator;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.SrimInput;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.SrimValidator;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.SrimValuation;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.StockMarketCode;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class SrimService {
    public SrimData preview(SrimInputData input) {
        if (input == null) throw new IllegalArgumentException("S-RIM 입력이 필요합니다.");
        return SrimData.from(SrimCalculator.calculate(input.toDomain(), true, Instant.now().toString()));
    }

    /** srim과 clear를 모두 생략한 요청은 저장된 값을 그대로 유지한다 (기존 클라이언트 호환, plan 62행). */
    public SrimValuation resolve(SrimValuation previous, SrimInputData data, boolean clear, boolean draft, String stockCode) {
        if (clear && data != null) throw new IllegalArgumentException("S-RIM 입력과 삭제를 동시에 요청할 수 없습니다.");
        if (clear) return null;
        if (data == null) return previous;
        return evaluate(previous, data.toDomain(), draft, stockCode);
    }

    private SrimValuation evaluate(SrimValuation previous, SrimInput input, boolean draft, String stockCode) {
        String currency = StockMarketCode.currencyOf(stockCode);
        if (!currency.equals(input.currency())) throw new IllegalArgumentException("리포트 통화와 S-RIM 통화가 다릅니다.");
        SrimValidator.validate(input, draft);
        if (previous != null && previous.input().equals(input)) return previous;
        return SrimCalculator.calculate(input, draft, Instant.now().toString());
    }
}
