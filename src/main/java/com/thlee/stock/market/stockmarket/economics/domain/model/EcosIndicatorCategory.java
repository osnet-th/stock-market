package com.thlee.stock.market.stockmarket.economics.domain.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum EcosIndicatorCategory {

    INTEREST_RATE("금리", Set.of("시장금리", "여수신금리")),
    MONEY_FINANCE("통화/금융", Set.of("통화량", "예금/대출금")),
    STOCK_BOND("주식/채권", Set.of("주식", "채권")),
    EXCHANGE_RATE("환율", Set.of("환율")),
    GROWTH_INCOME("성장/소득", Set.of("성장률", "소득", "GDP대비 비율")),
    PRODUCTION("생산", Set.of("생산")),
    CONSUMPTION_INVESTMENT("소비/투자", Set.of("소비", "투자")),
    PRICE("물가", Set.of("소비자/생산자 물가", "수출입 물가")),
    EMPLOYMENT_LABOR("고용/노동", Set.of("고용", "노동")),
    SENTIMENT("경기심리", Set.of("경기순환지표", "심리지표")),
    EXTERNAL_ECONOMY("대외경제", Set.of("국제수지", "통관수출입", "대외채권/채무")),
    CORPORATE_HOUSEHOLD("기업/가계", Set.of("기업경영지표", "가계", "소득분배지표")),
    REAL_ESTATE("부동산", Set.of("부동산 가격")),
    POPULATION("인구", Set.of("인구")),
    COMMODITY("원자재", Set.of("국제원자재가격"));

    private final String label;
    private final Set<String> classNames;

    private static final Map<String, EcosIndicatorCategory> CLASS_NAME_MAP;

    static {
        CLASS_NAME_MAP = Arrays.stream(values())
            .flatMap(category -> category.classNames.stream()
                .map(className -> Map.entry(className, category)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    EcosIndicatorCategory(String label, Set<String> classNames) {
        this.label = label;
        this.classNames = classNames;
    }

    public String getLabel() {
        return label;
    }

    public Set<String> getClassNames() {
        return classNames;
    }

    public static EcosIndicatorCategory fromClassName(String className) {
        return CLASS_NAME_MAP.get(className);
    }

    public boolean contains(String className) {
        return classNames.contains(className);
    }
}