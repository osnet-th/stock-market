package com.thlee.stock.market.stockmarket.realestate.presentation;

import com.thlee.stock.market.stockmarket.realestate.application.RealEstateRegionService;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.RegionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/realestate/regions")
@RequiredArgsConstructor
public class RealEstateRegionController {

    private final RealEstateRegionService regionService;

    @GetMapping
    public ResponseEntity<RegionResponse> listSidoGrouped() {
        return ResponseEntity.ok(regionService.listSidoGrouped());
    }

    @GetMapping("/{regionCode}/emds")
    public ResponseEntity<List<RegionResponse.Emd>> listEmds(@PathVariable String regionCode) {
        return ResponseEntity.ok(regionService.listEmds(regionCode));
    }
}