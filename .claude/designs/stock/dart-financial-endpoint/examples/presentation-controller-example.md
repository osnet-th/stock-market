# DartFinancialController 구현 예시

```java
package com.thlee.stock.market.stockmarket.stock.presentation;

import com.thlee.stock.market.stockmarket.stock.application.DartFinancialService;
import com.thlee.stock.market.stockmarket.stock.application.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dart")
@RequiredArgsConstructor
public class DartFinancialController {

    private final DartFinancialService dartFinancialService;

    /**
     * 단일회사 재무계정 조회
     */
    @GetMapping("/accounts/single")
    public ResponseEntity<List<FinancialAccountResponse>> getSingleAccount(
            @RequestParam String corpCode,
            @RequestParam String bsnsYear,
            @RequestParam String reprtCode) {
        return ResponseEntity.ok(dartFinancialService.getSingleAccount(corpCode, bsnsYear, reprtCode));
    }

    /**
     * 다중회사 재무계정 조회
     * @param corpCodes 고유번호 목록 (쉼표 구분, 최대 100건)
     */
    @GetMapping("/accounts/multi")
    public ResponseEntity<List<FinancialAccountResponse>> getMultiAccount(
            @RequestParam String corpCodes,
            @RequestParam String bsnsYear,
            @RequestParam String reprtCode) {
        return ResponseEntity.ok(dartFinancialService.getMultiAccount(corpCodes, bsnsYear, reprtCode));
    }

    /**
     * 단일회사 재무지표 조회
     * @param idxClCode 지표분류코드 (M210000:수익성, M220000:안정성, M230000:성장성, M240000:활동성)
     */
    @GetMapping("/indices/single")
    public ResponseEntity<List<FinancialIndexResponse>> getSingleFinancialIndex(
            @RequestParam String corpCode,
            @RequestParam String bsnsYear,
            @RequestParam String reprtCode,
            @RequestParam String idxClCode) {
        return ResponseEntity.ok(
                dartFinancialService.getSingleFinancialIndex(corpCode, bsnsYear, reprtCode, idxClCode));
    }

    /**
     * 다중회사 재무지표 조회
     * @param corpCodes 고유번호 목록 (쉼표 구분)
     * @param idxClCode 지표분류코드 (M210000:수익성, M220000:안정성, M230000:성장성, M240000:활동성)
     */
    @GetMapping("/indices/multi")
    public ResponseEntity<List<FinancialIndexResponse>> getMultiFinancialIndex(
            @RequestParam String corpCodes,
            @RequestParam String bsnsYear,
            @RequestParam String reprtCode,
            @RequestParam String idxClCode) {
        return ResponseEntity.ok(
                dartFinancialService.getMultiFinancialIndex(corpCodes, bsnsYear, reprtCode, idxClCode));
    }

    /**
     * 단일회사 전체 재무제표 조회
     * @param fsDiv 개별/연결구분 (OFS: 재무제표, CFS: 연결재무제표)
     */
    @GetMapping("/statements")
    public ResponseEntity<List<FullFinancialStatementResponse>> getFullFinancialStatement(
            @RequestParam String corpCode,
            @RequestParam String bsnsYear,
            @RequestParam String reprtCode,
            @RequestParam String fsDiv) {
        return ResponseEntity.ok(
                dartFinancialService.getFullFinancialStatement(corpCode, bsnsYear, reprtCode, fsDiv));
    }

    /**
     * 주식의 총수 현황 조회
     */
    @GetMapping("/stock-total")
    public ResponseEntity<List<StockTotalQuantityResponse>> getStockTotalQuantity(
            @RequestParam String corpCode,
            @RequestParam String bsnsYear,
            @RequestParam String reprtCode) {
        return ResponseEntity.ok(dartFinancialService.getStockTotalQuantity(corpCode, bsnsYear, reprtCode));
    }

    /**
     * 배당에 관한 사항 조회
     */
    @GetMapping("/dividends")
    public ResponseEntity<List<DividendInfoResponse>> getDividendInfo(
            @RequestParam String corpCode,
            @RequestParam String bsnsYear,
            @RequestParam String reprtCode) {
        return ResponseEntity.ok(dartFinancialService.getDividendInfo(corpCode, bsnsYear, reprtCode));
    }

    /**
     * 소송 등의 제기 조회
     * @param bgnDe 검색시작 접수일자 (YYYYMMDD)
     * @param endDe 검색종료 접수일자 (YYYYMMDD)
     */
    @GetMapping("/lawsuits")
    public ResponseEntity<List<LawsuitInfoResponse>> getLawsuits(
            @RequestParam String corpCode,
            @RequestParam String bgnDe,
            @RequestParam String endDe) {
        return ResponseEntity.ok(dartFinancialService.getLawsuits(corpCode, bgnDe, endDe));
    }

    /**
     * 사모자금 사용내역 조회
     */
    @GetMapping("/funds/private")
    public ResponseEntity<List<FundUsageResponse>> getPrivateFundUsage(
            @RequestParam String corpCode,
            @RequestParam String bsnsYear,
            @RequestParam String reprtCode) {
        return ResponseEntity.ok(dartFinancialService.getPrivateFundUsage(corpCode, bsnsYear, reprtCode));
    }

    /**
     * 공모자금 사용내역 조회
     */
    @GetMapping("/funds/public")
    public ResponseEntity<List<FundUsageResponse>> getPublicFundUsage(
            @RequestParam String corpCode,
            @RequestParam String bsnsYear,
            @RequestParam String reprtCode) {
        return ResponseEntity.ok(dartFinancialService.getPublicFundUsage(corpCode, bsnsYear, reprtCode));
    }
}
```