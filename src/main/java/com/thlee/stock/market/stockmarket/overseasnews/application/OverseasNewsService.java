package com.thlee.stock.market.stockmarket.overseasnews.application;

import com.thlee.stock.market.stockmarket.overseasnews.application.dto.BreakingNewsResponse;
import com.thlee.stock.market.stockmarket.overseasnews.application.dto.ComprehensiveNewsResponse;
import com.thlee.stock.market.stockmarket.overseasnews.infrastructure.kis.KisOverseasNewsClient;
import com.thlee.stock.market.stockmarket.overseasnews.infrastructure.kis.dto.KisBreakingNewsOutput;
import com.thlee.stock.market.stockmarket.overseasnews.infrastructure.kis.dto.KisNewsOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisApiResult;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 해외뉴스 조회 서비스.
 * KIS API를 호출하고 결과를 프론트엔드 응답 DTO로 변환한다.
 * DB 저장 없이 온디맨드 pass-through 방식.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OverseasNewsService {

    private final KisOverseasNewsClient kisOverseasNewsClient;

    /**
     * 해외속보(제목) 조회.
     */
    public List<BreakingNewsResponse> getBreakingNews(String stockCode, String exchangeCode) {
        try {
            List<KisBreakingNewsOutput> outputs = kisOverseasNewsClient.getBreakingNews(stockCode, exchangeCode);
            if (outputs == null) {
                return List.of();
            }

            return outputs.stream()
                .map(output -> BreakingNewsResponse.builder()
                    .dateTime(formatDateTime(output.getDataDate(), output.getDataTime()))
                    .title(output.getTitle())
                    .source(output.getSource())
                    .build())
                .toList();
        } catch (KisApiException e) {
            log.error("해외속보 조회 실패 [{}:{}]: {}", exchangeCode, stockCode, e.getMessage());
            throw new KisApiException("뉴스를 불러올 수 없습니다");
        }
    }

    /**
     * 해외뉴스종합(제목) 조회.
     */
    public ComprehensiveNewsResponse getComprehensiveNews(String stockCode,
                                                          String exchangeCode,
                                                          String countryCode,
                                                          String dataDt,
                                                          String dataTm) {
        try {
            KisApiResult<List<KisNewsOutput>> result =
                kisOverseasNewsClient.getComprehensiveNews(stockCode, exchangeCode, countryCode, dataDt, dataTm);

            List<KisNewsOutput> outputs = result.getData();
            if (outputs == null) {
                return ComprehensiveNewsResponse.builder()
                    .items(List.of())
                    .hasMore(false)
                    .lastDataDt("")
                    .lastDataTm("")
                    .build();
            }

            List<ComprehensiveNewsResponse.NewsItem> items = outputs.stream()
                .map(output -> ComprehensiveNewsResponse.NewsItem.builder()
                    .dateTime(formatDateTime(output.getDataDate(), output.getDataTime()))
                    .title(output.getTitle())
                    .className(output.getClassName())
                    .source(output.getSource())
                    .stockName(output.getStockName())
                    .build())
                .toList();

            String lastDt = outputs.isEmpty() ? "" : outputs.get(outputs.size() - 1).getDataDate();
            String lastTm = outputs.isEmpty() ? "" : outputs.get(outputs.size() - 1).getDataTime();

            return ComprehensiveNewsResponse.builder()
                .items(items)
                .hasMore(result.isHasNext())
                .lastDataDt(lastDt)
                .lastDataTm(lastTm)
                .build();
        } catch (KisApiException e) {
            log.error("해외뉴스종합 조회 실패 [{}:{}]: {}", exchangeCode, stockCode, e.getMessage());
            throw new KisApiException("뉴스를 불러올 수 없습니다");
        }
    }

    /**
     * KIS 날짜(YYYYMMDD) + 시간(HHMMSS) → "YYYY-MM-DD HH:MM:SS" 형식 변환.
     */
    private String formatDateTime(String date, String time) {
        if (date == null || date.length() < 8) {
            return "";
        }
        String formatted = date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
        if (time != null && time.length() >= 6) {
            formatted += " " + time.substring(0, 2) + ":" + time.substring(2, 4) + ":" + time.substring(4, 6);
        }
        return formatted;
    }
}