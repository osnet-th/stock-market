package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.config;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicatorMetadata;
import com.thlee.stock.market.stockmarket.economics.domain.repository.GlobalIndicatorMetadataRepository;
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
 * 앱 시작 시 DB가 비어있으면 global-indicator-metadata.yml에서 초기 데이터 시딩
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalIndicatorMetadataInitializer implements ApplicationRunner {

    private final GlobalIndicatorMetadataRepository metadataRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (metadataRepository.count() == 0) {
            List<GlobalIndicatorMetadata> metadataList = loadFromYml();
            metadataRepository.saveAll(metadataList);
            log.info("글로벌 경제지표 메타데이터 시딩 완료: {}건", metadataList.size());
        }
    }

    @SuppressWarnings("unchecked")
    private List<GlobalIndicatorMetadata> loadFromYml() {
        Yaml yaml = new Yaml();
        ClassPathResource resource = new ClassPathResource("global-indicator-metadata.yml");

        try (InputStream is = resource.getInputStream()) {
            Map<String, Object> root = yaml.load(is);
            Map<String, Object> global = (Map<String, Object>) root.get("global");
            Map<String, Object> indicator = (Map<String, Object>) global.get("indicator");
            Map<String, Object> metadata = (Map<String, Object>) indicator.get("metadata");
            List<Map<String, Object>> indicators = (List<Map<String, Object>>) metadata.get("indicators");

            List<GlobalIndicatorMetadata> result = new ArrayList<>();
            for (Map<String, Object> item : indicators) {
                String typeName = (String) item.get("indicator-type");
                String description = (String) item.get("description");

                GlobalEconomicIndicatorType indicatorType = GlobalEconomicIndicatorType.valueOf(typeName);
                result.add(new GlobalIndicatorMetadata(indicatorType, description));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("global-indicator-metadata.yml 파싱 실패", e);
        }
    }
}