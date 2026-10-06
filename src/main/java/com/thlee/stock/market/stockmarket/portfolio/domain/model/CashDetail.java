package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.CashSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.DepositMode;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.TaxType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class CashDetail {
    private static final int MIN_DEPOSIT_DAY = 1;
    private static final int MAX_DEPOSIT_DAY = 31;

    private final CashSubType subType;
    private final BigDecimal interestRate;
    private final LocalDate startDate;
    private final LocalDate maturityDate;
    private final TaxType taxType;
    private final BigDecimal monthlyDepositAmount;
    private final Integer depositDay;
    private final DepositMode depositMode;

    public CashDetail(CashSubType subType,
                      BigDecimal interestRate,
                      LocalDate startDate,
                      LocalDate maturityDate,
                      TaxType taxType) {
        this(subType, interestRate, startDate, maturityDate, taxType, null, null);
    }

    public CashDetail(CashSubType subType,
                      BigDecimal interestRate,
                      LocalDate startDate,
                      LocalDate maturityDate,
                      TaxType taxType,
                      BigDecimal monthlyDepositAmount,
                      Integer depositDay) {
        this(subType, interestRate, startDate, maturityDate, taxType, monthlyDepositAmount, depositDay, null);
    }

    /**
     * 처리 방식이 없으면 알림 확인(NOTIFY)으로 둔다. 처리 방식 컬럼이 없던 기존 행도 같다.
     */
    public CashDetail(CashSubType subType,
                      BigDecimal interestRate,
                      LocalDate startDate,
                      LocalDate maturityDate,
                      TaxType taxType,
                      BigDecimal monthlyDepositAmount,
                      Integer depositDay,
                      DepositMode depositMode) {
        this.subType = subType;
        this.interestRate = interestRate;
        this.startDate = startDate;
        this.maturityDate = maturityDate;
        this.taxType = taxType;
        this.monthlyDepositAmount = monthlyDepositAmount;
        this.depositDay = depositDay;
        this.depositMode = depositMode != null ? depositMode : DepositMode.NOTIFY;
    }

    /**
     * 자동 반영이면 월 납입액(0 초과)과 납입일(1~31일)이 있어야 한다.
     * 등록·수정 때만 검사하고, 저장된 값을 읽을 때는 검사하지 않는다.
     */
    public void validateDepositMode() {
        if (depositMode != DepositMode.AUTO) {
            return;
        }
        boolean hasAmount = monthlyDepositAmount != null && monthlyDepositAmount.signum() > 0;
        boolean hasDay = depositDay != null && depositDay >= MIN_DEPOSIT_DAY && depositDay <= MAX_DEPOSIT_DAY;
        if (!hasAmount || !hasDay) {
            throw new IllegalArgumentException(
                    "자동 납입은 월 납입액과 납입일(" + MIN_DEPOSIT_DAY + "~" + MAX_DEPOSIT_DAY + "일)을 입력해야 합니다.");
        }
    }

    /**
     * 주어진 날짜가 자동 납입을 기록할 날인지 판정한다.
     * 자동 반영이고 월 납입액·납입일이 있으며, 시작일 전이 아니고 만기일 전이며, 그 날이 이번 달 납입일이면 true 다.
     * 비어 있는 시작일·만기일은 조건에서 뺀다. 납입일이 그달 일수보다 크면 말일로 당긴다(리마인더 판정과 같은 규칙).
     */
    public boolean isAutoDepositDueOn(LocalDate date) {
        if (depositMode != DepositMode.AUTO || monthlyDepositAmount == null
                || monthlyDepositAmount.signum() <= 0 || depositDay == null) {
            return false;
        }
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        if (maturityDate != null && !date.isBefore(maturityDate)) {
            return false;
        }
        return date.getDayOfMonth() == Math.min(depositDay, date.lengthOfMonth());
    }
}
