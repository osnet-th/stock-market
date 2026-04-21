package com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.cluster.PutClusterSettingsRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest;
import co.elastic.clients.elasticsearch.indices.put_index_template.IndexTemplateMapping;
import co.elastic.clients.json.JsonData;
import jakarta.annotation.PreDestroy;
import jakarta.json.spi.JsonProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 애플리케이션 로그용 ES 인덱스 템플릿/클러스터 설정 인스톨러.
 *
 * 앱 기동 시 ES 가 아직 올라오지 않았을 수 있으므로 <b>exponential backoff 재시도</b> 를 수행한다.
 *  1. 5초 후 1차 시도
 *  2. 실패 시 10s → 20s → 40s → ... 최대 300s 간격으로 최대 {@value #MAX_ATTEMPTS} 회
 *  3. 성공하면 스케줄러 종료, 최종 실패 시 ERROR 로그 후 포기
 *
 * 템플릿 설치와 클러스터 설정은 독립적으로 상태 추적하여 한 쪽만 성공해도 다른 쪽만 계속 재시도한다.
 * 두 작업 모두 best-effort — 포기 후에도 앱 기동 차단 없음.
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

    static final int MAX_ATTEMPTS = 10;
    private static final long INITIAL_DELAY_MS = 5_000L;
    private static final long MAX_DELAY_MS = 300_000L;

    private final ElasticsearchClient elasticsearchClient;

    private final AtomicBoolean templateInstalled = new AtomicBoolean(false);
    private final AtomicBoolean clusterSettingsApplied = new AtomicBoolean(false);
    private final AtomicInteger attempt = new AtomicInteger(0);
    private ScheduledExecutorService retryScheduler;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        retryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "log-template-installer");
            t.setDaemon(true);
            return t;
        });
        schedule(INITIAL_DELAY_MS);
    }

    @PreDestroy
    public void shutdown() {
        if (retryScheduler != null) {
            retryScheduler.shutdownNow();
        }
    }

    // ──────────────────────────────────────────────────────────────────

    private void schedule(long delayMs) {
        if (retryScheduler == null || retryScheduler.isShutdown()) {
            return;
        }
        retryScheduler.schedule(this::runAttempt, delayMs, TimeUnit.MILLISECONDS);
    }

    private void runAttempt() {
        int current = attempt.incrementAndGet();

        if (!templateInstalled.get()) {
            if (installIndexTemplate()) {
                templateInstalled.set(true);
            }
        }
        if (!clusterSettingsApplied.get()) {
            if (enableDestructiveRequiresName()) {
                clusterSettingsApplied.set(true);
            }
        }

        if (templateInstalled.get() && clusterSettingsApplied.get()) {
            log.info("ES 초기 설정 완료 (attempt={})", current);
            retryScheduler.shutdown();
            return;
        }

        if (current >= MAX_ATTEMPTS) {
            log.error("ES 초기 설정 {}회 재시도 후 포기 (templateInstalled={}, clusterSettingsApplied={})",
                    MAX_ATTEMPTS, templateInstalled.get(), clusterSettingsApplied.get());
            retryScheduler.shutdown();
            return;
        }

        long nextDelay = Math.min(INITIAL_DELAY_MS * (1L << Math.min(current - 1, 6)), MAX_DELAY_MS);
        log.warn("ES 초기 설정 재시도 예정: attempt={}, nextDelayMs={}, templateInstalled={}, clusterSettingsApplied={}",
                current, nextDelay, templateInstalled.get(), clusterSettingsApplied.get());
        schedule(nextDelay);
    }

    private boolean installIndexTemplate() {
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
            return true;
        } catch (Exception e) {
            log.warn("ES 인덱스 템플릿 등록 실패: {}", e.getMessage());
            return false;
        }
    }

    private boolean enableDestructiveRequiresName() {
        try {
            PutClusterSettingsRequest request = PutClusterSettingsRequest.of(r -> r
                    .persistent("action.destructive_requires_name", JsonData.of(true))
            );
            elasticsearchClient.cluster().putSettings(request);
            log.info("ES 클러스터 설정 적용: action.destructive_requires_name=true");
            return true;
        } catch (Exception e) {
            log.warn("ES 클러스터 설정 적용 실패: {}", e.getMessage());
            return false;
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
