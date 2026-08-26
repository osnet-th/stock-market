package com.thlee.stock.market.stockmarket.glossary.presentation;

import com.thlee.stock.market.stockmarket.glossary.application.GlossaryCategoryService;
import com.thlee.stock.market.stockmarket.glossary.application.dto.GlossaryCategoryDeleteResult;
import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryCategory;
import com.thlee.stock.market.stockmarket.glossary.presentation.dto.CreateGlossaryCategoryRequest;
import com.thlee.stock.market.stockmarket.glossary.presentation.dto.GlossaryCategoryDeleteResponse;
import com.thlee.stock.market.stockmarket.glossary.presentation.dto.GlossaryCategoryResponse;
import com.thlee.stock.market.stockmarket.glossary.presentation.dto.UpdateGlossaryCategoryRequest;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 용어 사전 카테고리 REST API.
 *
 * <p>모든 엔드포인트는 인증 필수. {@link GlossarySecurityContext#currentUserId()} 로 안전 추출한 userId 로 스코프.
 * 권한 없거나 미존재 카테고리 접근은 404 통일 (IDOR 방지).
 */
@RestController
@RequestMapping("/api/glossary/categories")
@RequiredArgsConstructor
public class GlossaryCategoryController {

    private final GlossaryCategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<GlossaryCategoryResponse>> findAll() {
        List<GlossaryCategory> categories = categoryService.findAll(GlossarySecurityContext.currentUserId());
        return ResponseEntity.ok(categories.stream().map(GlossaryCategoryResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody CreateGlossaryCategoryRequest request) {
        Long userId = GlossarySecurityContext.currentUserId();
        GlossaryCategory created = categoryService.create(userId, request.toCommand());
        URI location = UriComponentsBuilder.fromPath("/api/glossary/categories/{id}")
                .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(Map.of("id", created.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody UpdateGlossaryCategoryRequest request) {
        categoryService.update(GlossarySecurityContext.currentUserId(), id, request.toCommand());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GlossaryCategoryDeleteResponse> delete(@PathVariable Long id) {
        GlossaryCategoryDeleteResult result =
                categoryService.delete(GlossarySecurityContext.currentUserId(), id);
        return ResponseEntity.ok(GlossaryCategoryDeleteResponse.from(result));
    }

    /** R6 확인 다이얼로그용 영향 건수 미리보기 (best-effort, 실제 응답이 권위). */
    @GetMapping("/{id}/delete-impact")
    public ResponseEntity<Map<String, Long>> previewDeleteImpact(@PathVariable Long id) {
        long count = categoryService.previewDeleteImpact(GlossarySecurityContext.currentUserId(), id);
        return ResponseEntity.ok(Map.of("count", count));
    }
}