package com.thlee.stock.market.stockmarket.glossary.presentation;

import com.thlee.stock.market.stockmarket.glossary.application.GlossaryTermService;
import com.thlee.stock.market.stockmarket.glossary.application.dto.GlossaryTermListResult;
import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryTerm;
import com.thlee.stock.market.stockmarket.glossary.domain.repository.GlossaryTermSort;
import com.thlee.stock.market.stockmarket.glossary.presentation.dto.CreateGlossaryTermRequest;
import com.thlee.stock.market.stockmarket.glossary.presentation.dto.GlossaryTermListResponse;
import com.thlee.stock.market.stockmarket.glossary.presentation.dto.GlossaryTermResponse;
import com.thlee.stock.market.stockmarket.glossary.presentation.dto.UpdateGlossaryTermRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

/**
 * 용어 사전 본체 REST API.
 *
 * <p>모든 엔드포인트는 인증 필수. ownership 위반/미존재는 404 통일.
 *
 * <p>{@code categoryId} 쿼리에 sentinel {@code __uncategorized__} (별도 boolean flag
 * {@code uncategorized=true}) 가 들어오면 미분류 필터로 해석.
 */
@RestController
@RequestMapping("/api/glossary/terms")
@RequiredArgsConstructor
@Validated
public class GlossaryTermController {

    private final GlossaryTermService termService;

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody CreateGlossaryTermRequest request) {
        Long userId = GlossarySecurityContext.currentUserId();
        GlossaryTerm created = termService.create(userId, request.toCommand());
        URI location = UriComponentsBuilder.fromPath("/api/glossary/terms/{id}")
                .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(Map.of("id", created.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GlossaryTermResponse> findById(@PathVariable Long id) {
        GlossaryTerm term = termService.find(GlossarySecurityContext.currentUserId(), id);
        return ResponseEntity.ok(GlossaryTermResponse.from(term));
    }

    @GetMapping
    public ResponseEntity<GlossaryTermListResponse> list(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) @Size(max = 100) String definitionQ,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "false") boolean uncategorized,
            @RequestParam(defaultValue = "REGISTERED_DESC") GlossaryTermSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        GlossaryTermListResult result = termService.list(
                GlossarySecurityContext.currentUserId(),
                q, definitionQ, categoryId, uncategorized, sort, page, size
        );
        return ResponseEntity.ok(GlossaryTermListResponse.from(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody UpdateGlossaryTermRequest request) {
        termService.update(GlossarySecurityContext.currentUserId(), id, request.toCommand());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        termService.delete(GlossarySecurityContext.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}