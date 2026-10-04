package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.logging.application.DomainEventLogger;
import com.thlee.stock.market.stockmarket.news.application.KeywordService;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.UserKeywordRepository;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioItemResponse;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.BondDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.CashDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.FundDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PensionDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.RealEstateDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.BondSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.CashSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.FundSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PensionSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PriceCurrency;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.RealEstateSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.StockSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.TaxType;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.CashStockLinkRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.DepositHistoryRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.StockPurchaseHistoryRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.StockSaleHistoryRepository;
import com.thlee.stock.market.stockmarket.stock.domain.service.ExchangeRatePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PortfolioService — 금융기관 등록·수정")
class PortfolioServiceInstitutionTest {

    @Mock PortfolioItemRepository portfolioItemRepository;
    @Mock StockPurchaseHistoryRepository purchaseHistoryRepository;
    @Mock StockSaleHistoryRepository stockSaleHistoryRepository;
    @Mock DepositHistoryRepository depositHistoryRepository;
    @Mock CashStockLinkRepository cashStockLinkRepository;
    @Mock PortfolioEvaluationService portfolioEvaluationService;
    @Mock ExchangeRatePort exchangeRatePort;
    @Mock KeywordService keywordService;
    @Mock KeywordRepository keywordRepository;
    @Mock UserKeywordRepository userKeywordRepository;
    @Mock DomainEventLogger domainEventLogger;

    @InjectMocks PortfolioService portfolioService;

    private static final Long USER_ID = 1L;
    private static final Long SAVED_ID = 900L;
    private static final Long CMA_ID = 50L;
    private static final String REGION = "DOMESTIC";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(1_000_000);
    private static final LocalDate MATURITY = LocalDate.of(2030, 1, 1);

    @BeforeEach
    void setUp() {
        // 저장은 받은 항목을 돌려준다. 새 항목은 저장소처럼 id를 붙인다(주식-현금 연결에 id가 필요하다).
        given(portfolioItemRepository.save(any(PortfolioItem.class))).willAnswer(invocation -> {
            PortfolioItem item = invocation.getArgument(0);
            return item.getId() != null ? item : withId(item, SAVED_ID);
        });
    }

    @Test
    @DisplayName("S1 등록 7종 모두 요청한 금융기관을 공백을 지워 응답에 담는다")
    void addAllTypes_reflectsNormalizedInstitution() {
        String requested = "  키움증권 ";

        List<PortfolioItemResponse> responses = List.of(
                portfolioService.addStockItem(USER_ID, "삼성전자", REGION, null, requested,
                        "INDIVIDUAL", "005930", "KOSPI", "KRX", "KR",
                        10, BigDecimal.valueOf(70_000), null, "KRW", null, null),
                portfolioService.addBondItem(USER_ID, "국고채", AMOUNT, REGION, null, requested,
                        "GOVERNMENT", MATURITY, BigDecimal.valueOf(3), "AAA"),
                portfolioService.addRealEstateItem(USER_ID, "아파트", AMOUNT, REGION, null, requested,
                        "APARTMENT", "서울", BigDecimal.valueOf(84)),
                portfolioService.addFundItem(USER_ID, "주식형 펀드", AMOUNT, REGION, null, requested,
                        "EQUITY_FUND", BigDecimal.ONE, null, null),
                portfolioService.addPensionItem(USER_ID, "IRP", AMOUNT, REGION, null, requested,
                        "IRP", "미래에셋", null, null, null),
                portfolioService.addCashItem(USER_ID, "정기예금", AMOUNT, REGION, null, requested,
                        "DEPOSIT", BigDecimal.valueOf(3), null, null, "GENERAL", null, null),
                portfolioService.addGeneralItem(USER_ID, "CRYPTO", "비트코인", AMOUNT, REGION, null, requested,
                        null));

        assertThat(responses).hasSize(7)
                .extracting(PortfolioItemResponse::getInstitution)
                .containsOnly("키움증권");
    }

    @Test
    @DisplayName("S2 등록 시 금융기관이 없으면 미지정으로 응답한다")
    void add_withoutInstitution_isUnassigned() {
        PortfolioItemResponse response = portfolioService.addBondItem(USER_ID, "국고채", AMOUNT, REGION, null, null,
                "GOVERNMENT", MATURITY, BigDecimal.valueOf(3), "AAA");

        assertThat(response.getInstitution()).isNull();
    }

    @Test
    @DisplayName("S3 수정 7종 모두 요청한 금융기관으로 바꾼다")
    void updateAllTypes_replacesInstitution() {
        givenExisting(1L, stockItem());
        givenExisting(2L, PortfolioItem.createWithBond(USER_ID, "국고채", AMOUNT, Region.DOMESTIC,
                new BondDetail(BondSubType.GOVERNMENT, MATURITY, BigDecimal.valueOf(3), "AAA")));
        givenExisting(3L, PortfolioItem.createWithRealEstate(USER_ID, "아파트", AMOUNT, Region.DOMESTIC,
                new RealEstateDetail(RealEstateSubType.APARTMENT, "서울", BigDecimal.valueOf(84))));
        givenExisting(4L, PortfolioItem.createWithFund(USER_ID, "주식형 펀드", AMOUNT, Region.DOMESTIC,
                new FundDetail(FundSubType.EQUITY_FUND, BigDecimal.ONE)));
        givenExisting(5L, PortfolioItem.createWithPension(USER_ID, "IRP", AMOUNT, Region.DOMESTIC,
                new PensionDetail(PensionSubType.IRP, "미래에셋", null, null, null)));
        givenExisting(6L, depositItem());
        givenExisting(7L, PortfolioItem.create(USER_ID, "비트코인", AssetType.CRYPTO, AMOUNT, Region.DOMESTIC));

        List<PortfolioItemResponse> responses = List.of(
                portfolioService.updateStockItem(USER_ID, 1L, "삼성전자", null, "신한은행",
                        "INDIVIDUAL", "005930", "KOSPI", "KRX", "KR",
                        10, BigDecimal.valueOf(70_000), null, "KRW", null, null, false),
                portfolioService.updateBondItem(USER_ID, 2L, "국고채", AMOUNT, null, "신한은행",
                        "GOVERNMENT", MATURITY, BigDecimal.valueOf(3), "AAA"),
                portfolioService.updateRealEstateItem(USER_ID, 3L, "아파트", AMOUNT, null, "신한은행",
                        "APARTMENT", "서울", BigDecimal.valueOf(84)),
                portfolioService.updateFundItem(USER_ID, 4L, "주식형 펀드", AMOUNT, null, "신한은행",
                        "EQUITY_FUND", BigDecimal.ONE, null, null),
                portfolioService.updatePensionItem(USER_ID, 5L, "IRP", AMOUNT, null, "신한은행",
                        "IRP", "미래에셋", null, null, null),
                portfolioService.updateCashItem(USER_ID, 6L, "정기예금", AMOUNT, null, "신한은행",
                        BigDecimal.valueOf(3), null, null, "GENERAL", null, null),
                portfolioService.updateGeneralItem(USER_ID, 7L, "비트코인", AMOUNT, null, "신한은행", null));

        assertThat(responses).hasSize(7)
                .extracting(PortfolioItemResponse::getInstitution)
                .containsOnly("신한은행");
    }

    @Test
    @DisplayName("S4 수정 요청에 금융기관이 없으면 기존 값을 지운다")
    void update_withoutInstitution_clearsValue() {
        givenExisting(6L, depositItem());

        PortfolioItemResponse response = portfolioService.updateCashItem(USER_ID, 6L, "정기예금", AMOUNT, null, null,
                BigDecimal.valueOf(3), null, null, "GENERAL", null, null);

        assertThat(response.getInstitution()).isNull();
    }

    @Test
    @DisplayName("S5 등록 시 금융기관이 50자를 넘으면 거부한다")
    void add_overFiftyCharacters_throws() {
        assertThatThrownBy(() -> portfolioService.addCashItem(USER_ID, "정기예금", AMOUNT, REGION, null, "가".repeat(51),
                "DEPOSIT", BigDecimal.valueOf(3), null, null, "GENERAL", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금융기관은 50자 이하로 입력해 주세요.");
    }

    @Test
    @DisplayName("S6 연결한 현금 자산의 금융기관을 이어받지 않고, 차감 뒤에도 현금 자산의 값은 그대로다")
    void addStock_linkedCash_doesNotInheritInstitution() {
        PortfolioItem cma = withId(PortfolioItem.createWithCash(USER_ID, "CMA", BigDecimal.valueOf(10_000_000), Region.DOMESTIC,
                new CashDetail(CashSubType.CMA, BigDecimal.valueOf(3), null, null, null)), CMA_ID);
        cma.updateInstitution("국민은행");
        given(portfolioItemRepository.findById(CMA_ID)).willReturn(Optional.of(cma));

        PortfolioItemResponse stock = portfolioService.addStockItem(USER_ID, "삼성전자", REGION, null, null,
                "INDIVIDUAL", "005930", "KOSPI", "KRX", "KR",
                10, BigDecimal.valueOf(70_000), null, "KRW", null, CMA_ID);

        assertThat(stock.getInstitution()).isNull();
        assertThat(cma.getInvestedAmount()).isEqualByComparingTo(BigDecimal.valueOf(9_300_000));
        assertThat(cma.getInstitution()).isEqualTo("국민은행");
    }

    private void givenExisting(Long id, PortfolioItem item) {
        PortfolioItem existing = withId(item, id);
        existing.updateInstitution("국민은행");
        given(portfolioItemRepository.findById(id)).willReturn(Optional.of(existing));
    }

    private static PortfolioItem stockItem() {
        return PortfolioItem.createWithStock(USER_ID, "삼성전자", Region.DOMESTIC,
                new StockDetail(StockSubType.INDIVIDUAL, "005930", "KOSPI", "KRX", "KR",
                        10, BigDecimal.valueOf(70_000), null, PriceCurrency.KRW, null));
    }

    private static PortfolioItem depositItem() {
        return PortfolioItem.createWithCash(USER_ID, "정기예금", AMOUNT, Region.DOMESTIC,
                new CashDetail(CashSubType.DEPOSIT, BigDecimal.valueOf(3), null, null, TaxType.GENERAL));
    }

    private static PortfolioItem withId(PortfolioItem origin, Long id) {
        return new PortfolioItem(
                id, origin.getUserId(), origin.getItemName(), origin.getAssetType(),
                origin.getInvestedAmount(), origin.isNewsEnabled(), origin.getRegion(),
                origin.getMemo(), origin.getInstitution(), origin.getStatus(), origin.getVersion(),
                origin.getCreatedAt(), origin.getUpdatedAt(),
                origin.getStockDetail(), origin.getBondDetail(), origin.getRealEstateDetail(),
                origin.getFundDetail(), origin.getCashDetail(), origin.getGoldDetail(), origin.getPensionDetail());
    }
}
