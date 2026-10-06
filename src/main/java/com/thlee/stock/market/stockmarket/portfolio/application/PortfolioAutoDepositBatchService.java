package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 자동 납입 일배치.
 *
 * <p>납입 처리 방식이 자동 반영인 현금성 항목 중 오늘(KST)이 납입일인 항목에 월 납입액을 납입 이력으로 남긴다.
 * 트랜잭션을 걸지 않는다. 기록은 {@link PortfolioService#recordAutoDeposit} 이 항목별로 맡으며, 배치 전체를
 * 한 트랜잭션으로 묶으면 한 항목의 실패가 전체를 롤백시킨다.</p>
 *
 * <p>놓친 날은 소급하지 않는다. 기록이 빠진 달은 기존 납입일 당일·미납 알림이 안내한다.</p>
 */
@Slf4j
@Service
public class PortfolioAutoDepositBatchService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PortfolioItemRepository portfolioItemRepository;
    private final PortfolioService portfolioService;
    private final Clock clock;

    @Autowired
    public PortfolioAutoDepositBatchService(PortfolioItemRepository portfolioItemRepository,
                                            PortfolioService portfolioService) {
        this(portfolioItemRepository, portfolioService, Clock.system(KST));
    }

    PortfolioAutoDepositBatchService(PortfolioItemRepository portfolioItemRepository,
                                     PortfolioService portfolioService,
                                     Clock clock) {
        this.portfolioItemRepository = portfolioItemRepository;
        this.portfolioService = portfolioService;
        this.clock = clock;
    }

    /**
     * 오늘(KST)이 납입일인 자동 반영 항목의 납입을 기록한다.
     *
     * @return 기록한 항목 수
     */
    public int recordDueAutoDeposits() {
        LocalDate today = LocalDate.now(clock);
        List<PortfolioItem> targets = portfolioItemRepository.findActiveAutoDepositCashItems();

        int dueCount = 0;
        int recordedCount = 0;
        for (PortfolioItem item : targets) {
            if (item.getCashDetail() == null || !item.getCashDetail().isAutoDepositDueOn(today)) {
                continue;
            }
            dueCount++;
            try {
                if (portfolioService.recordAutoDeposit(item.getId(), today)) {
                    recordedCount++;
                }
            } catch (Exception e) {
                log.error("자동 납입 기록 실패: itemId={}, date={}", item.getId(), today, e);
            }
        }

        log.info("자동 납입 기록 완료: 기록={}/오늘 납입일={}/자동 반영 전체={}, date={}",
                recordedCount, dueCount, targets.size(), today);
        return recordedCount;
    }
}
