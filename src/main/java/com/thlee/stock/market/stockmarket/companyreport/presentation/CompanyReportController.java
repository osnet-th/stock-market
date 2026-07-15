package com.thlee.stock.market.stockmarket.companyreport.presentation;

import com.thlee.stock.market.stockmarket.companyreport.application.CompanyReportReadService;
import com.thlee.stock.market.stockmarket.companyreport.application.CompanyReportWriteService;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.CompanyReportResults;
import com.thlee.stock.market.stockmarket.companyreport.presentation.dto.CreateCompanyReportRequest;
import com.thlee.stock.market.stockmarket.companyreport.presentation.dto.UpdateCompanyReportRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/company-reports")
@RequiredArgsConstructor
public class CompanyReportController {

    private final CompanyReportWriteService writeService;
    private final CompanyReportReadService readService;

    /**
     * 저장 전 자동 산출 미리보기 (스냅샷 조립, 저장 안 함)
     */
    @GetMapping("/preview")
    public ResponseEntity<CompanyReportResults.Preview> preview(@RequestParam String stockCode) {
        CompanyReportSecurityContext.currentUserId();
        return ResponseEntity.ok(readService.preview(stockCode));
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody CreateCompanyReportRequest request) {
        Long userId = CompanyReportSecurityContext.currentUserId();
        Long id = writeService.create(request.toCommand(userId));
        URI location = UriComponentsBuilder.fromPath("/api/company-reports/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(Map.of("id", id));
    }

    @GetMapping
    public ResponseEntity<CompanyReportResults.ListResult> findList(
            @RequestParam(required = false) String stockName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = CompanyReportSecurityContext.currentUserId();
        return ResponseEntity.ok(readService.findList(userId, blankToNull(stockName), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyReportResults.Detail> findDetail(@PathVariable Long id) {
        Long userId = CompanyReportSecurityContext.currentUserId();
        return ResponseEntity.ok(readService.findDetail(id, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable Long id, @Valid @RequestBody UpdateCompanyReportRequest request) {
        Long userId = CompanyReportSecurityContext.currentUserId();
        writeService.update(request.toCommand(id, userId));
        return ResponseEntity.noContent().build();
    }

    /**
     * 스냅샷 새로고침 (DART/KIS 재호출)
     */
    @PostMapping("/{id}/refresh")
    public ResponseEntity<CompanyReportResults.Detail> refresh(@PathVariable Long id) {
        Long userId = CompanyReportSecurityContext.currentUserId();
        writeService.refreshSnapshot(id, userId);
        return ResponseEntity.ok(readService.findDetail(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long userId = CompanyReportSecurityContext.currentUserId();
        writeService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
