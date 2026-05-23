package com.thlee.stock.market.stockmarket.realestate.infrastructure.config;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketMetadata;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateRegion;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateMarketMetadataRepository;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateRegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 앱 시작 시 region/metadata 테이블이 비어있으면 yml 리소스에서 1회 시딩한다.
 * <p>
 * 외부 API는 호출하지 않으며, DB read-only 작업만 수행한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RealEstateMarketMetadataInitializer implements ApplicationRunner {

    private static final String REGION_RESOURCE = "realestate-region-codes.yml";
    private static final String METADATA_RESOURCE = "realestate-indicator-metadata.yml";

    private final RealEstateRegionRepository regionRepository;
    private final RealEstateMarketMetadataRepository metadataRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedRegionsIfEmpty();
        seedMetadataIfEmpty();
    }

    private void seedRegionsIfEmpty() {
        if (regionRepository.count() > 0) {
            return;
        }
        try {
            List<RealEstateRegion> regions = loadRegions();
            regionRepository.saveAll(regions);
            log.info("[realestate] region seed completed: {} rows", regions.size());
        } catch (Exception e) {
            log.error("[realestate] region seed failed", e);
        }
    }

    private void seedMetadataIfEmpty() {
        if (metadataRepository.count() > 0) {
            return;
        }
        try {
            List<RealEstateMarketMetadata> metadataList = loadMetadata();
            metadataRepository.saveAll(metadataList);
            log.info("[realestate] metadata seed completed: {} rows", metadataList.size());
        } catch (Exception e) {
            log.error("[realestate] metadata seed failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<RealEstateRegion> loadRegions() throws Exception {
        Yaml yaml = new Yaml();
        try (InputStream is = new ClassPathResource(REGION_RESOURCE).getInputStream()) {
            Map<String, Object> root = yaml.load(is);
            Map<String, Object> realestate = (Map<String, Object>) root.get("realestate");
            Map<String, Object> region = (Map<String, Object>) realestate.get("region");
            List<Map<String, Object>> entries = (List<Map<String, Object>>) region.get("entries");

            List<RealEstateRegion> result = new ArrayList<>();
            int order = 0;
            for (Map<String, Object> entry : entries) {
                String regionCode = (String) entry.get("region-code");
                String emdCode = entry.get("emd-code") == null ? "" : (String) entry.get("emd-code");
                result.add(RealEstateRegion.builder()
                        .regionCode(regionCode)
                        .sidoCode((String) entry.get("sido-code"))
                        .sidoName((String) entry.get("sido-name"))
                        .sigunguName((String) entry.get("sigungu-name"))
                        .emdCode(emdCode)
                        .emdName((String) entry.get("emd-name"))
                        .displayOrder(order++)
                        .build());
            }
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private List<RealEstateMarketMetadata> loadMetadata() throws Exception {
        Yaml yaml = new Yaml();
        try (InputStream is = new ClassPathResource(METADATA_RESOURCE).getInputStream()) {
            Map<String, Object> root = yaml.load(is);
            Map<String, Object> realestate = (Map<String, Object>) root.get("realestate");
            Map<String, Object> indicator = (Map<String, Object>) realestate.get("indicator");
            Map<String, Object> metadata = (Map<String, Object>) indicator.get("metadata");
            List<Map<String, Object>> indicators = (List<Map<String, Object>>) metadata.get("indicators");

            List<RealEstateMarketMetadata> result = new ArrayList<>();
            for (Map<String, Object> item : indicators) {
                result.add(RealEstateMarketMetadata.builder()
                        .category(RealEstateMarketCategory.valueOf((String) item.get("category")))
                        .source(RealEstateMarketSource.valueOf((String) item.get("source")))
                        .indicatorCode((String) item.get("indicator-code"))
                        .displayName((String) item.get("display-name"))
                        .description((String) item.get("description"))
                        .unit((String) item.get("unit"))
                        .defaultVisible(Boolean.TRUE.equals(item.get("default-visible")))
                        .build());
            }
            return result;
        }
    }
}