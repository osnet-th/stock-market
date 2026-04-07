package com.thlee.stock.market.stockmarket.economics.application;

import com.thlee.stock.market.stockmarket.economics.domain.model.DerivedIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import com.thlee.stock.market.stockmarket.economics.domain.model.KeyStatIndicator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ECOS 경제지표 파생지표 계산 서비스
 * ecos.js의 getCurrentSpreads() 디스패치 맵과 동일한 카테고리-파생지표 매핑
 */
@Service
public class EcosDerivedIndicatorService {

    public List<DerivedIndicator> calculate(EcosIndicatorCategory category, List<KeyStatIndicator> indicators) {
        Map<String, Double> map = buildIndicatorMap(indicators);

        return switch (category) {
            case INTEREST_RATE -> calcInterestRateSpreads(map);
            case MONEY_FINANCE -> calcMoneyFinanceSpreads(map);
            case STOCK_BOND -> calcStockBondSpreads(map);
            case GROWTH_INCOME -> calcGrowthIncomeSpreads(map);
            case PRODUCTION -> calcProductionSpreads(map);
            case CONSUMPTION_INVESTMENT -> calcConsumptionInvestmentSpreads(map);
            case PRICE -> calcPriceSpreads(map);
            case EMPLOYMENT_LABOR -> calcEmploymentLaborSpreads(map);
            case SENTIMENT -> calcSentimentSpreads(map);
            case EXTERNAL_ECONOMY -> calcExternalEconomySpreads(map);
            case REAL_ESTATE -> calcRealEstateSpreads(map);
            case CORPORATE_HOUSEHOLD -> calcCorporateHouseholdSpreads(map);
            case EXCHANGE_RATE, POPULATION, COMMODITY -> Collections.emptyList();
        };
    }

    private Map<String, Double> buildIndicatorMap(List<KeyStatIndicator> indicators) {
        return indicators.stream()
                .filter(i -> i.dataValue() != null && !i.dataValue().isBlank())
                .collect(Collectors.toMap(
                        KeyStatIndicator::keystatName,
                        i -> parseDouble(i.dataValue()),
                        (a, b) -> b
                ));
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double find(Map<String, Double> map, String name) {
        return map.get(name);
    }

    private Double calc(Double a, Double b) {
        if (a == null || b == null) return null;
        return Math.round((a - b) * 1000.0) / 1000.0;
    }

    private Double ratio(Double a, Double b, int decimals) {
        if (a == null || b == null || b == 0) return null;
        double factor = Math.pow(10, decimals);
        return Math.round((a / b) * factor) / factor;
    }

    private Double ratio(Double a, Double b) {
        return ratio(a, b, 2);
    }

    private void addIfNotNull(List<DerivedIndicator> list, String name, Double value, String unit, String formula, String description) {
        if (value != null) {
            list.add(new DerivedIndicator(name, value, unit, formula, description));
        }
    }

    // ── INTEREST_RATE ──

    private List<DerivedIndicator> calcInterestRateSpreads(Map<String, Double> map) {
        Double bond5 = find(map, "국고채수익률(5년)");
        Double bond3 = find(map, "국고채수익률(3년)");
        Double cd91 = find(map, "CD수익률(91일)");
        Double corpBond = find(map, "회사채수익률(3년,AA-)");
        Double loanRate = find(map, "예금은행 대출금리");
        Double depositRate = find(map, "예금은행 수신금리");
        Double callRate = find(map, "콜금리(익일물)");
        Double baseRate = find(map, "한국은행 기준금리");

        List<DerivedIndicator> result = new ArrayList<>();
        addIfNotNull(result, "장단기 금리차", calc(bond5, cd91), "%p", "국고채수익률(5년) − CD수익률(91일)",
                "양수(+)면 정상적인 우상향 금리 곡선. 0에 가까워지거나 음수(−)면 장단기 금리 역전으로 경기침체 신호");
        addIfNotNull(result, "중기-단기 금리차", calc(bond3, cd91), "%p", "국고채수익률(3년) − CD수익률(91일)",
                "양수(+)가 클수록 시장이 향후 금리 인상을 예상. 줄어들면 금리 인하 기대가 반영된 것");
        addIfNotNull(result, "장기 금리 기울기", calc(bond5, bond3), "%p", "국고채수익률(5년) − 국고채수익률(3년)",
                "양수(+)면 장기 인플레이션이나 경제성장 기대가 있다는 의미. 축소되면 장기 성장 기대가 약해지는 것");
        addIfNotNull(result, "신용 스프레드", calc(corpBond, bond3), "%p", "회사채수익률(3년,AA-) − 국고채수익률(3년)",
                "기업 채권과 국채의 금리 차이. 벌어지면 시장이 기업 부도 위험을 높게 보는 것, 좁으면 안정적");
        addIfNotNull(result, "예대금리차", calc(loanRate, depositRate), "%p", "예금은행 대출금리 − 예금은행 수신금리",
                "은행이 예금자에게 주는 이자와 대출자에게 받는 이자의 차이. 클수록 대출자 부담이 크고 은행 수익성이 높음");
        addIfNotNull(result, "단기 vs 기준금리", calc(callRate, baseRate), "%p", "콜금리(익일물) − 한국은행 기준금리",
                "콜금리가 기준금리보다 높으면 시중 자금이 부족한 상태, 낮으면 유동성이 풍부한 상태");
        return result;
    }

    // ── MONEY_FINANCE ──

    private List<DerivedIndicator> calcMoneyFinanceSpreads(Map<String, Double> map) {
        Double m1 = find(map, "M1(협의통화, 평잔)");
        Double m2 = find(map, "M2(광의통화, 평잔)");
        Double lf = find(map, "Lf(평잔)");
        Double l = find(map, "L(말잔)");
        Double deposit = find(map, "예금은행총예금(말잔)");
        Double loan = find(map, "예금은행대출금(말잔)");
        Double houseCredit = find(map, "가계신용");
        Double delinquency = find(map, "가계대출연체율");

        List<DerivedIndicator> result = new ArrayList<>();
        addIfNotNull(result, "M2/M1 비율", ratio(m2, m1), "배", "M2 ÷ M1",
                "1에 가까우면 돈이 바로 쓸 수 있는 상태로 많이 있다는 뜻. 높을수록 정기예금 등 묶인 돈이 많아 실제 소비·투자로 바로 이어지기 어려움");
        addIfNotNull(result, "Lf/M2 비율", ratio(lf, m2), "배", "Lf ÷ M2",
                "1보다 클수록 보험·증권 등 비은행 금융기관에 돈이 많이 퍼져있다는 뜻. 빠르게 올라가면 그림자 금융 리스크 주의");
        addIfNotNull(result, "L/M2 비율", ratio(l, m2), "배", "L ÷ M2",
                "전체 금융권에 풀린 돈이 은행 중심 통화량의 몇 배인지. 높을수록 금융시스템 전체 유동성이 크게 확장된 상태");
        addIfNotNull(result, "대출/예금 비율 (LDR)", ratio(loan, deposit, 3), "배", "은행대출금 ÷ 은행총예금",
                "1 초과면 예금보다 대출이 많은 상태. 은행이 외부 차입에 의존하는 정도를 보여줌. 규제 기준 예대율 100%");
        addIfNotNull(result, "가계신용/예금", ratio(houseCredit, deposit, 3), "배", "가계신용 ÷ 은행총예금",
                "가계가 빌린 돈이 은행 전체 예금 대비 어느 수준인지. 1에 가까울수록 빚으로 버티는 시장이라는 뜻");
        addIfNotNull(result, "가계신용/M2", ratio(houseCredit, m2, 3), "배", "가계신용 ÷ M2",
                "시중 통화량 대비 가계부채 비중. 높을수록 유동성이 실물이 아닌 가계 빚으로 흘러갔다는 신호");
        if (delinquency != null) {
            result.add(new DerivedIndicator("가계대출 연체율", delinquency, "%", "가계대출 연체 비율",
                    "가계 대출 중 연체된 비율. 빠르게 오르면 금융위기 초기 신호. 장기평균 0.78%, 1% 이상이면 주의"));
        }
        return result;
    }

    // ── STOCK_BOND ──

    private List<DerivedIndicator> calcStockBondSpreads(Map<String, Double> map) {
        List<DerivedIndicator> result = new ArrayList<>();
        addIfNotNull(result, "유동성 비율", ratio(find(map, "주식거래대금(KOSPI)"), find(map, "투자자예탁금")), "배",
                "주식거래대금(KOSPI) ÷ 투자자예탁금", "거래대금이 예탁금보다 크면 대기 자금이 적극 투입된 상태. 높을수록 과열, 낮을수록 관망");
        addIfNotNull(result, "위험자산 선호도", ratio(find(map, "주식거래대금(KOSPI)"), find(map, "채권거래대금"), 3), "배",
                "주식거래대금(KOSPI) ÷ 채권거래대금", "높으면 투자자들이 주식(위험자산) 선호, 낮으면 채권(안전자산) 선호");
        addIfNotNull(result, "대형 vs 성장주", ratio(find(map, "코스피지수"), find(map, "코스닥지수")), "배",
                "코스피지수 ÷ 코스닥지수", "높으면 대형주 강세, 낮으면 성장·중소형주 강세");
        return result;
    }

    // ── GROWTH_INCOME ──

    private List<DerivedIndicator> calcGrowthIncomeSpreads(Map<String, Double> map) {
        Double consumption = find(map, "민간소비증감률(실질, 계절조정 전기대비)");
        Double equipment = find(map, "설비투자증감률(실질, 계절조정 전기대비)");
        Double construction = find(map, "건설투자증감률(실질, 계절조정 전기대비)");
        Double domesticSum = (consumption != null && equipment != null && construction != null)
                ? Math.round((consumption + equipment + construction) * 1000.0) / 1000.0 : null;
        Double exportRate = find(map, "재화의 수출 증감률(실질, 계절조정 전기대비)");

        List<DerivedIndicator> result = new ArrayList<>();
        addIfNotNull(result, "내수 성장 기여도", domesticSum, "%", "민간소비 + 설비투자 + 건설투자 증감률 합",
                "민간소비·설비투자·건설투자 증가율의 합. 양수이면 내수가 경제 성장에 기여, 음수이면 내수 위축");
        Double exportVsDomestic = (exportRate != null && domesticSum != null && domesticSum != 0)
                ? Math.round((exportRate / domesticSum) * 100.0) / 100.0 : null;
        addIfNotNull(result, "수출 vs 내수", exportVsDomestic, "배", "재화의 수출 증감률 ÷ 내수 증감률 합",
                "1보다 크면 수출 주도 성장, 1 미만이면 내수 주도 성장");
        addIfNotNull(result, "자금 잉여/부족", calc(find(map, "총저축률"), find(map, "국내총투자율")), "%p",
                "총저축률 − 총투자율", "양수이면 저축이 투자보다 많아 자금 잉여(해외 투자 가능), 음수이면 자금 부족(해외 차입 필요)");
        return result;
    }

    // ── PRODUCTION ──

    private List<DerivedIndicator> calcProductionSpreads(Map<String, Double> map) {
        List<DerivedIndicator> result = new ArrayList<>();
        addIfNotNull(result, "재고율", ratio(find(map, "제조업재고지수"), find(map, "제조업출하지수"), 3), "배",
                "제조업재고지수 ÷ 제조업출하지수", "1보다 크면 출하 대비 재고가 쌓이는 상태(경기 둔화 신호). 1 미만이면 재고 소진(경기 활성)");
        addIfNotNull(result, "재고 압력", calc(find(map, "제조업생산지수"), find(map, "제조업출하지수")), "p",
                "제조업생산지수 − 제조업출하지수", "양수이면 생산이 판매를 초과해 재고가 쌓이는 중. 음수이면 판매가 생산을 초과");
        addIfNotNull(result, "제조 vs 서비스", calc(find(map, "전산업생산지수"), find(map, "서비스업생산지수")), "p",
                "전산업생산지수 − 서비스업생산지수", "양수이면 제조업 포함 전체가 서비스업보다 활발. 음수이면 서비스업 중심 성장");
        addIfNotNull(result, "설비 활용 효율", ratio(find(map, "제조업가동률지수"), find(map, "제조업생산지수"), 3), "배",
                "제조업가동률지수 ÷ 제조업생산지수", "높으면 기존 설비를 최대한 활용 중. 낮으면 설비 여유가 있는 상태");
        return result;
    }

    // ── CONSUMPTION_INVESTMENT ──

    private List<DerivedIndicator> calcConsumptionInvestmentSpreads(Map<String, Double> map) {
        List<DerivedIndicator> result = new ArrayList<>();
        addIfNotNull(result, "내구재 소비 비중", ratio(find(map, "자동차판매액지수"), find(map, "소매판매액지수"), 3), "배",
                "자동차판매액지수 ÷ 소매판매액지수", "높으면 자동차 등 고가 내구재 소비가 활발. 낮으면 생필품 중심 소비");
        addIfNotNull(result, "투자 선행 vs 현재", ratio(find(map, "설비투자지수"), find(map, "국내기계수주액"), 3), "배",
                "설비투자지수 ÷ 국내기계수주액", "높으면 현재 투자가 주문 대비 활발. 낮으면 향후 투자 확대 가능성");
        addIfNotNull(result, "미래 건설 vs 현재", ratio(find(map, "건설수주액"), find(map, "건설기성액"), 3), "배",
                "건설수주액 ÷ 건설기성액", "1보다 크면 향후 건설 물량 증가 예상. 1 미만이면 건설 위축 가능");
        addIfNotNull(result, "착공률", ratio(find(map, "건축착공면적"), find(map, "건축허가면적"), 3), "배",
                "건축착공면적 ÷ 건축허가면적", "1에 가까울수록 허가된 건물이 실제 착공으로 이어지는 비율이 높음. 낮으면 허가만 받고 착공 지연");
        return result;
    }

    // ── PRICE ──

    private List<DerivedIndicator> calcPriceSpreads(Map<String, Double> map) {
        List<DerivedIndicator> result = new ArrayList<>();
        addIfNotNull(result, "변동성 영향", calc(find(map, "소비자물가지수"), find(map, "농산물 및 석유류제외 소비자물가지수")), "p",
                "소비자물가지수 − 농산물및석유류제외 소비자물가지수", "양수이면 농산물·석유가 물가를 끌어올리는 중. 음수이면 오히려 억제 효과");
        addIfNotNull(result, "가격 전가 압력", calc(find(map, "생산자물가지수"), find(map, "소비자물가지수")), "p",
                "생산자물가지수 − 소비자물가지수", "양수이면 생산자 비용이 아직 소비자에게 전가되지 않은 상태(향후 물가 상승 압력). 음수이면 전가 완료");
        addIfNotNull(result, "수입발 물가 압력", calc(find(map, "수입물가지수"), find(map, "소비자물가지수")), "p",
                "수입물가지수 − 소비자물가지수", "양수이면 수입 가격이 국내 소비자 물가보다 높아 향후 물가 상승 가능. 음수이면 수입발 압력 약화");
        addIfNotNull(result, "체감물가 비율", ratio(find(map, "생활물가지수"), find(map, "소비자물가지수"), 3), "배",
                "생활물가지수 ÷ 소비자물가지수", "1보다 크면 실제 생활에서 느끼는 물가가 공식 통계보다 높음. 1이면 체감과 공식 일치");
        return result;
    }

    // ── EMPLOYMENT_LABOR ──

    private List<DerivedIndicator> calcEmploymentLaborSpreads(Map<String, Double> map) {
        List<DerivedIndicator> result = new ArrayList<>();
        addIfNotNull(result, "실질 취업 비율", ratio(find(map, "취업자수"), find(map, "경제활동인구"), 3), "배",
                "취업자수 ÷ 경제활동인구", "1에 가까울수록 경제활동인구 대부분이 취업한 상태. 낮을수록 실업 비중 높음");
        addIfNotNull(result, "임금 vs 생산성", ratio(find(map, "시간당명목임금지수"), find(map, "노동생산성지수"), 3), "배",
                "시간당명목임금지수 ÷ 노동생산성지수", "1보다 크면 임금이 생산성보다 빠르게 상승(기업 비용 부담 증가). 1 미만이면 생산성이 임금을 앞섬");
        addIfNotNull(result, "기업 부담 지표", ratio(find(map, "단위노동비용지수"), find(map, "노동생산성지수"), 3), "배",
                "단위노동비용지수 ÷ 노동생산성지수", "높을수록 생산성 대비 노동비용이 커 기업 경쟁력 약화. 낮을수록 효율적");
        return result;
    }

    // ── SENTIMENT ──

    private List<DerivedIndicator> calcSentimentSpreads(Map<String, Double> map) {
        List<DerivedIndicator> result = new ArrayList<>();
        addIfNotNull(result, "경기 방향성", calc(find(map, "선행지수순환변동치"), find(map, "동행지수순환변동치")), "p",
                "선행지수순환변동치 − 동행지수순환변동치", "양수이면 현재보다 경기가 좋아질 전망. 음수이면 현재보다 나빠질 전망");
        addIfNotNull(result, "기업 vs 소비 괴리", calc(find(map, "전산업 기업심리지수실적"), find(map, "소비자심리지수")), "p",
                "전산업 기업심리지수실적 − 소비자심리지수", "양수이면 기업이 소비자보다 낙관적. 음수이면 소비자가 더 낙관적. 격차가 크면 경기 전환 신호");
        Double esi = find(map, "경제심리지수");
        if (esi != null) {
            Double overUnder = Math.round((esi - 100) * 1000.0) / 1000.0;
            result.add(new DerivedIndicator("경기 과열/침체", overUnder, "p", "경제심리지수 − 100",
                    "양수이면 경기 낙관(과열 가능), 음수이면 경기 비관(침체 가능). 0이면 중립"));
        }
        return result;
    }

    // ── EXTERNAL_ECONOMY ──

    private List<DerivedIndicator> calcExternalEconomySpreads(Map<String, Double> map) {
        List<DerivedIndicator> result = new ArrayList<>();
        addIfNotNull(result, "순 직접투자", calc(find(map, "직접투자(자산)"), find(map, "직접투자(부채)")), "백만$",
                "직접투자(자산) − 직접투자(부채)", "양수이면 우리 기업이 해외에 더 많이 투자. 음수이면 외국인이 국내에 더 많이 투자");
        addIfNotNull(result, "순 증권투자", calc(find(map, "증권투자(자산)"), find(map, "증권투자(부채)")), "백만$",
                "증권투자(자산) − 증권투자(부채)", "양수이면 국내 자금이 해외로 유출. 음수이면 외국인 자금이 국내로 유입");
        addIfNotNull(result, "순대외자산", calc(find(map, "대외채권"), find(map, "대외채무")), "백만$",
                "대외채권 − 대외채무", "양수이면 받을 돈이 갚을 돈보다 많은 순채권국. 음수이면 순채무국");
        addIfNotNull(result, "무역 밸런스", ratio(find(map, "수출금액지수"), find(map, "수입금액지수"), 3), "배",
                "수출금액지수 ÷ 수입금액지수", "1보다 크면 수출 우위, 1 미만이면 수입 우위. 무역수지 방향을 보여줌");
        addIfNotNull(result, "외환 안정성", ratio(find(map, "경상수지"), find(map, "외환보유액"), 4), "배",
                "경상수지 ÷ 외환보유액", "양수이면 경상수지 흑자 기반 외환 안정. 음수 확대 시 외환 리스크 증가");
        return result;
    }

    // ── REAL_ESTATE ──

    private List<DerivedIndicator> calcRealEstateSpreads(Map<String, Double> map) {
        List<DerivedIndicator> result = new ArrayList<>();
        addIfNotNull(result, "매매-전세 괴리", calc(find(map, "주택매매가격지수"), find(map, "주택전세가격지수")), "p",
                "주택매매가격지수 − 주택전세가격지수", "양수이면 매매가가 전세가보다 많이 상승(갭투자 확대 환경). 음수이면 전세가 상승이 더 큼");
        addIfNotNull(result, "토지 vs 주택", ratio(find(map, "지가변동률(전기대비)"), find(map, "주택매매가격지수"), 4), "",
                "지가변동률(전기대비) ÷ 주택매매가격지수", "높으면 토지 가격 상승이 주택 대비 빠름(개발 기대). 낮으면 주택 중심 시장");
        return result;
    }

    // ── CORPORATE_HOUSEHOLD ──

    private List<DerivedIndicator> calcCorporateHouseholdSpreads(Map<String, Double> map) {
        Double income = find(map, "가구당월평균소득");
        Double propensity = find(map, "평균소비성향");
        Double estConsumption = (income != null && propensity != null)
                ? (double) Math.round(income * propensity / 100.0) : null;

        List<DerivedIndicator> result = new ArrayList<>();
        addIfNotNull(result, "추정 소비 수준", estConsumption != null ? estConsumption.doubleValue() : null, "천원",
                "가구당월평균소득 × 평균소비성향", "가구당 월평균 소득에 소비성향을 곱한 추정 소비 금액. 경기 체감의 기초 지표");
        addIfNotNull(result, "수익성 vs 리스크", ratio(find(map, "제조업매출액세전순이익률"), find(map, "제조업부채비율"), 3), "",
                "제조업매출액세전순이익률 ÷ 제조업부채비율", "높을수록 부채 대비 수익성이 좋음. 낮으면 부채 부담에 비해 수익이 적은 상태");
        addIfNotNull(result, "불평등 구조", ratio(find(map, "5분위배율"), find(map, "지니계수"), 3), "배",
                "5분위배율 ÷ 지니계수", "높으면 상하위 소득 격차가 전반적 불평등보다 두드러짐. 소득 양극화 심화 신호");
        return result;
    }
}
