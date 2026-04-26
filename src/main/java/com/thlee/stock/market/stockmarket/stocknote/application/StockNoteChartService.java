package com.thlee.stock.market.stockmarket.stocknote.application;

import com.thlee.stock.market.stockmarket.stock.application.StockPriceService;
import com.thlee.stock.market.stockmarket.stock.domain.model.DailyPrice;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.ChartDataResult;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNote;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNotePriceSnapshot;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteVerification;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotStatus;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotType;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNotePriceSnapshotRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 종목 차트 데이터 조립 (line 일봉 + scatter 기록점).
 *
 * <p>일봉은 {@link StockPriceService#getDailyHistory} 경유 — 현재 포트 기본 구현은 빈 리스트라
 * 차트 탭은 "데이터 없음" 으로 degrade 된다. 기록점(notes) 은 이상없이 조립된다.
 */
@Service
@RequiredArgsConstructor
public class StockNoteChartService {

    private static final int DEFAULT_PERIOD_DAYS = 90;
    private static final int MAX_PERIOD_DAYS = 365;

    private final StockNoteRepository noteRepository;
    private final StockNotePriceSnapshotRepository snapshotRepository;
    private final StockNoteVerificationRepository verificationRepository;
    private final StockPriceService stockPriceService;

    @Transactional(readOnly = true)
    public ChartDataResult getChartData(Long userId, String stockCode, Integer period) {
        int days = resolvePeriod(period);
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days);

        List<StockNote> notes = noteRepository.findByUserAndStock(userId, stockCode, from, today);
        if (notes.isEmpty()) {
            return new ChartDataResult(stockCode, List.of(), List.of());
        }
        StockNote sample = notes.get(0);
        List<DailyPrice> prices = stockPriceService.getDailyHistory(
                stockCode, sample.getMarketType(), sample.getExchangeCode(), from, today);

        List<Long> noteIds = notes.stream().map(StockNote::getId).toList();
        Map<Long, List<StockNotePriceSnapshot>> snapshotMap = snapshotRepository.findAllByNoteIds(noteIds);
        Map<Long, StockNoteVerification> verificationMap = verificationRepository.findAllByNoteIds(noteIds);

        List<ChartDataResult.NotePoint> notePoints = new ArrayList<>(notes.size());
        for (StockNote n : notes) {
            BigDecimal priceAtNote = resolvePriceAtNote(snapshotMap.get(n.getId()));
            notePoints.add(new ChartDataResult.NotePoint(
                    n.getId(), n.getNoteDate(), n.getDirection(),
                    priceAtNote, n.getChangePercent(),
                    n.getInitialJudgment(),
                    summarize(n.getTriggerText(), 80),
                    verificationMap.containsKey(n.getId())
            ));
        }
        return new ChartDataResult(stockCode, prices, notePoints);
    }

    private BigDecimal resolvePriceAtNote(List<StockNotePriceSnapshot> snapshots) {
        if (snapshots == null) {
            return null;
        }
        for (StockNotePriceSnapshot s : snapshots) {
            if (s.getSnapshotType() == SnapshotType.AT_NOTE && s.getStatus() == SnapshotStatus.SUCCESS) {
                return s.getClosePrice();
            }
        }
        return null;
    }

    private int resolvePeriod(Integer period) {
        if (period == null || period < 1) {
            return DEFAULT_PERIOD_DAYS;
        }
        return Math.min(period, MAX_PERIOD_DAYS);
    }

    private static String summarize(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}