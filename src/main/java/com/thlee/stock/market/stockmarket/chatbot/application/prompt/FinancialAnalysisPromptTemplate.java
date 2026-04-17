package com.thlee.stock.market.stockmarket.chatbot.application.prompt;

import com.thlee.stock.market.stockmarket.chatbot.application.dto.AnalysisTask;
import org.springframework.stereotype.Component;

@Component
public class FinancialAnalysisPromptTemplate {

    private static final String COMMON_HEADER = """
            당신은 한국 주식 종목 분석 전문가입니다.
            아래 '제공된 팩트' 만을 1순위 근거로 사용하세요.
            업종 평균이 필요하면 일반지식으로 추정하되, 추정치임을 반드시 명시하세요.

            """;

    public String render(AnalysisTask task, String financialContext) {
        String instruction = switch (task) {
            case UNDERVALUATION -> """
                    ## 요청
                    제공된 3개년 재무 데이터와 최근 가치평가 지표를 기반으로 이 종목이 저평가인지 고평가인지 판단하세요.
                    - 자기 자신의 과거 3년 추세를 1순위 근거로 사용
                    - 업종 평균 대비는 일반지식으로 보조 판단 (추정치 명시)
                    - 결론(저평가/고평가/중립)을 명확히 제시
                    """;
            case TREND_SUMMARY -> """
                    ## 요청
                    제공된 재무 데이터의 3개년 추세를 요약하세요.
                    - 매출/이익/자산 흐름의 방향성
                    - 수익성·성장성의 개선/악화 항목을 구분
                    """;
            case RISK_DIAGNOSIS -> """
                    ## 요청
                    제공된 재무 데이터에서 투자 리스크 요인을 진단하세요.
                    - 재무안정성·유동성·자본효율(활동성) 관점
                    - 시급한 리스크 vs 관찰 필요 리스크 구분
                    """;
            case INVESTMENT_OPINION -> """
                    ## 요청
                    제공된 재무 데이터 기반으로 투자 적정성 의견을 제시하세요.
                    - 매수/보유/매도 관점의 근거
                    - 이 데이터만으로 판단하기 어려운 항목 명시
                    """;
        };
        return COMMON_HEADER + instruction + "\n## 제공된 팩트\n" + financialContext;
    }
}
