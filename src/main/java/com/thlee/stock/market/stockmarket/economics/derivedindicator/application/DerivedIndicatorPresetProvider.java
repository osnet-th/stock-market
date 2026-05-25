package com.thlee.stock.market.stockmarket.economics.derivedindicator.application;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.DerivedFormula;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperand;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 시스템 제공 파생지표 프리셋 카탈로그(코드 정의). 별도 테이블 없음.
 * <p>
 * 카탈로그 상수는 Unit 5에서 기존 EcosDerivedIndicatorService 계산식을 실제 메타데이터 기준으로
 * 수동 전사해 채운다(className/keystatName 정확성 확보, R3 예외, 최소 N개 보장, 4항 이상 제외).
 */
@Component
public class DerivedIndicatorPresetProvider {

    private final List<DerivedIndicatorPreset> presets = buildPresets();

    public List<DerivedIndicatorPreset> all() {
        return presets;
    }

    public Optional<DerivedIndicatorPreset> findByKey(String key) {
        return presets.stream().filter(p -> p.key().equals(key)).findFirst();
    }

    /**
     * 기존 EcosDerivedIndicatorService 대표 spread를 실제 메타데이터(ecos-indicator-metadata.yml)의
     * (class-name, keystat-name)로 수동 전사. 모두 2항 동일 카테고리(빈화면 방지 최소 보장).
     * 4항 중첩(수출÷내수합)·특수 반올림 항목은 2~3항 제약으로 제외(Scope 비대상).
     */
    private static List<DerivedIndicatorPreset> buildPresets() {
        return List.of(
                // ── 금리(INTEREST_RATE) ──
                preset("interest-term-spread", "장단기 금리차", "%p",
                        ind("시장금리", "국고채수익률(5년)"), FormulaOperator.SUB, ind("시장금리", "CD수익률(91일)")),
                preset("interest-deposit-loan-spread", "예대금리차", "%p",
                        ind("여수신금리", "예금은행 대출금리"), FormulaOperator.SUB, ind("여수신금리", "예금은행 수신금리")),
                preset("interest-credit-spread", "신용 스프레드", "%p",
                        ind("시장금리", "회사채수익률(3년,AA-)"), FormulaOperator.SUB, ind("시장금리", "국고채수익률(3년)")),
                // ── 통화/금융(MONEY_FINANCE) ──
                preset("money-m2-m1-ratio", "M2/M1 비율", "배",
                        ind("통화량", "M2(광의통화, 평잔)"), FormulaOperator.DIV, ind("통화량", "M1(협의통화, 평잔)")),
                preset("money-ldr", "대출/예금 비율 (LDR)", "배",
                        ind("예금/대출금", "예금은행대출금(말잔)"), FormulaOperator.DIV, ind("예금/대출금", "예금은행총예금(말잔)")),
                preset("money-household-credit-m2", "가계신용/M2", "배",
                        ind("예금/대출금", "가계신용"), FormulaOperator.DIV, ind("통화량", "M2(광의통화, 평잔)")),
                // ── 주식/채권(STOCK_BOND) ──
                preset("stock-large-vs-growth", "대형 vs 성장주", "배",
                        ind("주식", "코스피지수"), FormulaOperator.DIV, ind("주식", "코스닥지수"))
        );
    }

    private static FormulaOperand ind(String className, String keystatName) {
        return FormulaOperand.indicator(className, keystatName);
    }

    private static DerivedIndicatorPreset preset(String key, String name, String unit,
                                                 FormulaOperand left, FormulaOperator op, FormulaOperand right) {
        DerivedFormula formula = new DerivedFormula(List.of(left, right), List.of(op));
        return new DerivedIndicatorPreset(key, name, unit, formula);
    }
}
