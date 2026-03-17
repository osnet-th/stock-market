package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.config;

import com.thlee.stock.market.stockmarket.economics.application.EcosIndicatorMetadataService;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorMetadata;
import com.thlee.stock.market.stockmarket.economics.domain.repository.EcosIndicatorMetadataRepository;
import lombok.RequiredArgsConstructor;
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
 * 앱 시작 시 DB가 비어있으면 ecos-indicator-metadata.yml에서 초기 데이터 시딩
 */
@Component
@RequiredArgsConstructor
public class EcosIndicatorMetadataInitializer implements ApplicationRunner {

    private final EcosIndicatorMetadataRepository metadataRepository;
    private final EcosIndicatorMetadataService metadataService;

    @Override
    public void run(ApplicationArguments args) {
        if (metadataRepository.count() == 0) {
            List<EcosIndicatorMetadata> metadataList = loadFromYml();
            metadataRepository.saveAll(metadataList);
        }

        metadataService.loadCache();
    }

    @SuppressWarnings("unchecked")
    private List<EcosIndicatorMetadata> loadFromYml() {
        Yaml yaml = new Yaml();
        ClassPathResource resource = new ClassPathResource("ecos-indicator-metadata.yml");

        try (InputStream is = resource.getInputStream()) {
            Map<String, Object> root = yaml.load(is);
            Map<String, Object> ecos = (Map<String, Object>) root.get("ecos");
            Map<String, Object> indicator = (Map<String, Object>) ecos.get("indicator");
            Map<String, Object> metadata = (Map<String, Object>) indicator.get("metadata");
            List<Map<String, Object>> indicators = (List<Map<String, Object>>) metadata.get("indicators");

            List<EcosIndicatorMetadata> result = new ArrayList<>();
            for (Map<String, Object> item : indicators) {
                String className = (String) item.get("class-name");
                String keystatName = (String) item.get("keystat-name");
                String description = (String) item.get("description");
                String direction = (String) item.getOrDefault("positive-direction", "NEUTRAL");
                boolean keyIndicator = (Boolean) item.getOrDefault("key-indicator", false);

                result.add(new EcosIndicatorMetadata(
                    className,
                    keystatName,
                    description,
                    EcosIndicatorMetadata.PositiveDirection.valueOf(direction),
                    keyIndicator
                ));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("ecos-indicator-metadata.yml 파싱 실패", e);
        }
    }
}