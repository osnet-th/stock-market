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
                        "양수(+)면 정상적인 우상향 금리 곡선. 0에 가까워지거나 음수(−)면 장단기 금리 역전으로 경기침체 신호",
                        ind("시장금리", "국고채수익률(5년)"), FormulaOperator.SUB, ind("시장금리", "CD수익률(91일)")),
                preset("interest-deposit-loan-spread", "예대금리차", "%p",
                        "은행이 예금자에게 주는 이자와 대출자에게 받는 이자의 차이. 클수록 대출자 부담이 크고 은행 수익성이 높음",
                        ind("여수신금리", "예금은행 대출금리"), FormulaOperator.SUB, ind("여수신금리", "예금은행 수신금리")),
                preset("interest-credit-spread", "신용 스프레드", "%p",
                        "기업 채권과 국채의 금리 차이. 벌어지면 시장이 기업 부도 위험을 높게 보는 것, 좁으면 안정적",
                        ind("시장금리", "회사채수익률(3년,AA-)"), FormulaOperator.SUB, ind("시장금리", "국고채수익률(3년)")),
                // ── 통화/금융(MONEY_FINANCE) ──
                preset("money-m2-m1-ratio", "M2/M1 비율", "배",
                        "1에 가까우면 바로 쓸 수 있는 돈이 많다는 뜻. 높을수록 정기예금 등 묶인 돈이 많아 소비·투자로 바로 이어지기 어려움",
                        ind("통화량", "M2(광의통화, 평잔)"), FormulaOperator.DIV, ind("통화량", "M1(협의통화, 평잔)")),
                preset("money-ldr", "대출/예금 비율 (LDR)", "배",
                        "1 초과면 예금보다 대출이 많은 상태. 은행이 외부 차입에 의존하는 정도를 보여줌. 규제 기준 예대율 100%",
                        ind("예금/대출금", "예금은행대출금(말잔)"), FormulaOperator.DIV, ind("예금/대출금", "예금은행총예금(말잔)")),
                preset("money-household-credit-m2", "가계신용/M2", "배",
                        "시중 통화량 대비 가계부채 비중. 높을수록 유동성이 실물이 아닌 가계 빚으로 흘러갔다는 신호",
                        ind("예금/대출금", "가계신용"), FormulaOperator.DIV, ind("통화량", "M2(광의통화, 평잔)")),
                // ── 주식/채권(STOCK_BOND) ──
                preset("stock-large-vs-growth", "대형 vs 성장주", "배",
                        "높으면 대형주 강세, 낮으면 성장·중소형주 강세",
                        ind("주식", "코스피지수"), FormulaOperator.DIV, ind("주식", "코스닥지수"))
        );
    }

    private static FormulaOperand ind(String className, String keystatName) {
        return FormulaOperand.indicator(className, keystatName);
    }

    private static DerivedIndicatorPreset preset(String key, String name, String unit, String description,
                                                 FormulaOperand left, FormulaOperator op, FormulaOperand right) {
        DerivedFormula formula = new DerivedFormula(List.of(left, right), List.of(op));
        return new DerivedIndicatorPreset(key, name, unit, description, formula);
    }
}
