package com.thlee.stock.market.stockmarket.salary.presentation;

import com.thlee.stock.market.stockmarket.salary.application.SalaryService;
import com.thlee.stock.market.stockmarket.salary.application.dto.MonthlySalaryResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.SalaryTrendResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.UpsertResultResponse;
import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
import com.thlee.stock.market.stockmarket.salary.presentation.dto.UpsertIncomeRequest;
import com.thlee.stock.market.stockmarket.salary.presentation.dto.UpsertSpendingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

/**
 * 월급 사용 비율 도메인의 HTTP 엔드포인트.
 *
 * <p><b>userId 권한 검증</b>: portfolio 도메인과 동일하게 {@code @RequestParam Long userId}로
 * 전달받는다. SecurityContext 대조는 추후 보안 작업 범위이며 현 시점에서는 신뢰한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/salary")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;

    /** 특정 월의 월급 사용 현황 (상속 적용). */
    @GetMapping("/monthly/{yearMonth}")
    public ResponseEntity<MonthlySalaryResponse> getMonthly(
            @RequestParam Long userId,
            @PathVariable YearMonth yearMonth) {
        return ResponseEntity.ok(salaryService.getMonthly(userId, yearMonth));
    }

    /** 최근 N개월 추이 (기본 12개월). */
    @GetMapping("/trend")
    public ResponseEntity<SalaryTrendResponse> getTrend(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(salaryService.getTrend(userId, months));
    }

    /** 변경 레코드가 존재하는 월 목록 (드롭다운 기준). */
    @GetMapping("/months")
    public ResponseEntity<List<YearMonth>> getAvailableMonths(@RequestParam Long userId) {
        return ResponseEntity.ok(salaryService.getAvailableMonths(userId));
    }

    /** 해당 월 기점 월급 upsert. 상속값과 동일하면 NOOP. */
    @PutMapping("/income/{yearMonth}")
    public ResponseEntity<UpsertResultResponse> upsertIncome(
            @RequestParam Long userId,
            @PathVariable YearMonth yearMonth,
            @Valid @RequestBody UpsertIncomeRequest request) {
        return ResponseEntity.ok(salaryService.upsertIncome(userId, yearMonth, request.getAmount()));
    }

    /** 해당 월·카테고리 기점 지출 upsert. 상속값과 동일하면 NOOP. */
    @PutMapping("/spending/{yearMonth}/{category}")
    public ResponseEntity<UpsertResultResponse> upsertSpending(
            @RequestParam Long userId,
            @PathVariable YearMonth yearMonth,
            @PathVariable SpendingCategory category,
            @Valid @RequestBody UpsertSpendingRequest request) {
        return ResponseEntity.ok(salaryService.upsertSpending(
                userId, category, yearMonth, request.getAmount(), request.getMemo()));
    }

    /** 해당 월의 월급 변경 레코드 제거 (이전 값으로 복귀). */
    @DeleteMapping("/income/{yearMonth}")
    public ResponseEntity<Void> deleteIncome(
            @RequestParam Long userId,
            @PathVariable YearMonth yearMonth) {
        salaryService.deleteIncome(userId, yearMonth);
        return ResponseEntity.noContent().build();
    }

    /** 해당 월·카테고리의 지출 변경 레코드 제거 (이전 값으로 복귀). */
    @DeleteMapping("/spending/{yearMonth}/{category}")
    public ResponseEntity<Void> deleteSpending(
            @RequestParam Long userId,
            @PathVariable YearMonth yearMonth,
            @PathVariable SpendingCategory category) {
        salaryService.deleteSpending(userId, category, yearMonth);
        return ResponseEntity.noContent().build();
    }
}
