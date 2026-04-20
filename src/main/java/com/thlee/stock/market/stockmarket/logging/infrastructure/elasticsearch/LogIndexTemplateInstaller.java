package com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.cluster.PutClusterSettingsRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest;
import co.elastic.clients.elasticsearch.indices.put_index_template.IndexTemplateMapping;
import co.elastic.clients.json.JsonData;
import jakarta.json.spi.JsonProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.List;

/**
 * 애플리케이션 로그용 ES 인덱스 템플릿/클러스터 설정 인스톨러.
 *
 * 앱 기동 완료 직후 1회 실행:
 *  1. {@code app-log} composable index template 등록
 *     - 패턴: {@code app-audit-*}, {@code app-error-*}, {@code app-business-*}
 *     - 설정: shards=1, replicas=0, refresh_interval=30s
 *  2. 클러스터 설정 {@code action.destructive_requires_name=true} (wildcard delete 사고 방지)
 *
 * 둘 다 best-effort — 실패해도 앱 기동 차단 없음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogIndexTemplateInstaller {

    private static final String TEMPLATE_NAME = "app-log";
    private static final List<String> INDEX_PATTERNS = List.of(
            "app-audit-*",
            "app-error-*",
            "app-business-*"
    );

    private final ElasticsearchClient elasticsearchClient;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        installIndexTemplate();
        enableDestructiveRequiresName();
    }

    private void installIndexTemplate() {
        try {
            IndexSettings settings = IndexSettings.of(s -> s
                    .numberOfShards("1")
                    .numberOfReplicas("0")
                    .refreshInterval(Time.of(t -> t.time("30s")))
            );

            TypeMapping mappings = parseMappings();

            PutIndexTemplateRequest request = PutIndexTemplateRequest.of(t -> t
                    .name(TEMPLATE_NAME)
                    .indexPatterns(INDEX_PATTERNS)
                    .priority(100L)
                    .template(IndexTemplateMapping.of(m -> m
                            .settings(settings)
                            .mappings(mappings)
                    ))
            );

            elasticsearchClient.indices().putIndexTemplate(request);
            log.info("ES 인덱스 템플릿 등록 완료: {} patterns={}", TEMPLATE_NAME, INDEX_PATTERNS);
        } catch (Exception e) {
            log.warn("ES 인덱스 템플릿 등록 실패 (계속 진행): {}", e.getMessage());
        }
    }

    private void enableDestructiveRequiresName() {
        try {
            PutClusterSettingsRequest request = PutClusterSettingsRequest.of(r -> r
                    .persistent("action.destructive_requires_name", JsonData.of(true))
            );
            elasticsearchClient.cluster().putSettings(request);
            log.info("ES 클러스터 설정 적용: action.destructive_requires_name=true");
        } catch (Exception e) {
            log.warn("ES 클러스터 설정 적용 실패 (계속 진행): {}", e.getMessage());
        }
    }

    private TypeMapping parseMappings() {
        String mappingJson = """
                {
                  "properties": {
                    "timestamp":      { "type": "date", "format": "strict_date_time" },
                    "domain":         { "type": "keyword" },
                    "userId":         { "type": "long" },
                    "requestId":      { "type": "keyword" },
                    "payload":        { "type": "object", "enabled": false },
                    "truncated":      { "type": "boolean" },
                    "originalSize":   { "type": "integer" },
                    "status":         { "type": "integer" },
                    "exceptionClass": { "type": "keyword" }
                  }
                }
                """;
        return TypeMapping._DESERIALIZER.deserialize(
                JsonProvider.provider().createParser(new StringReader(mappingJson)),
                elasticsearchClient._jsonpMapper()
        );
    }
}