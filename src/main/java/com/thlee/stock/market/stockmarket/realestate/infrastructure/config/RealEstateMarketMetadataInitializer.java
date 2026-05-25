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
        verifyLevelBackfilled();
        seedRegionsIfEmpty();
        seedSidosIfMissing();
        seedMetadataIfEmpty();
    }

    /**
     * level 컬럼 backfill 누락을 명확한 메시지로 fail-fast. JPA @PostLoad가 IllegalStateException으로
     * 부풀려 던지기 전 단일 카운트 쿼리로 사전 검증. 마이그레이션 SQL이 적용되지 않았다면 여기서 멈춘다.
     */
    private void verifyLevelBackfilled() {
        try {
            long missing = regionRepository.countMissingLevel();
            if (missing > 0) {
                throw new IllegalStateException(
                        "real_estate_region.level 컬럼 backfill 누락: " + missing + " row. "
                                + "코드 머지 전 db/migration/add_real_estate_region_level.sql 적용 필요.");
            }
        } catch (org.springframework.dao.InvalidDataAccessResourceUsageException e) {
            // 컬럼 자체가 없는 경우 (마이그레이션 SQL 미적용) — 동일한 의미로 fail-fast
            throw new IllegalStateException(
                    "real_estate_region.level 컬럼 부재. "
                            + "코드 머지 전 db/migration/add_real_estate_region_level.sql 적용 필요.", e);
        }
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

    /**
     * SIDO row 17개를 idempotent 적재.
     * 전체 region count()와 무관하게 SIDO row 존재 여부만 검사 — 기존 SIGUNGU 56 row가
     * 적재된 환경에서도 SIDO만 따로 추가 가능.
     */
    private void seedSidosIfMissing() {
        if (!regionRepository.findAllSidoOnly().isEmpty()) {
            return;
        }
        try {
            List<RealEstateRegion> sidos = loadSidos();
            regionRepository.saveAll(sidos);
            log.info("[realestate] sido seed completed: {} rows", sidos.size());
        } catch (Exception e) {
            log.error("[realestate] sido seed failed", e);
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
    private List<RealEstateRegion> loadSidos() throws Exception {
        Yaml yaml = new Yaml();
        try (InputStream is = new ClassPathResource(REGION_RESOURCE).getInputStream()) {
            Map<String, Object> root = yaml.load(is);
            Map<String, Object> realestate = (Map<String, Object>) root.get("realestate");
            Map<String, Object> region = (Map<String, Object>) realestate.get("region");
            List<Map<String, Object>> entries = (List<Map<String, Object>>) region.get("sido-entries");
            if (entries == null) {
                return List.of();
            }

            List<RealEstateRegion> result = new ArrayList<>();
            int order = 0;
            for (Map<String, Object> entry : entries) {
                result.add(RealEstateRegion.builder()
                        .regionCode((String) entry.get("region-code"))
                        .sidoCode((String) entry.get("sido-code"))
                        .sidoName((String) entry.get("sido-name"))
                        .sigunguName((String) entry.get("sigungu-name"))
                        .emdCode("")
                        .emdName(null)
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