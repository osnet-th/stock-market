package com.thlee.stock.market.stockmarket.logging.infrastructure.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

/**
 * 운영자 화이트리스트 설정. {@code app.logging.admin.*}.
 *
 * <ul>
 *   <li>bind 실패(잘못된 포맷 등) → {@code BindException} 으로 <b>앱 기동 실패</b> (fail-fast)</li>
 *   <li>빈 리스트 또는 null 값 → 전면 차단 (fail-close)</li>
 *   <li>여기에 포함된 userId 만 {@code /api/admin/logs/**} 및 {@code /api/admin/dashboard/**}
 *       접근 허용</li>
 * </ul>
 *
 * <p>ADMIN_LOG_ACCESS meta-audit 면제는 본 properties 가 아니라 컨트롤러 메서드에 부착하는
 * {@code @SkipAdminAudit} 어노테이션으로 제어한다 — 면제 의도가 endpoint 정의와 같은 곳에 위치.
 *
 * {@code .env} 예시: {@code ADMIN_USER_IDS=1,2,3}
 */
@ConfigurationProperties(prefix = "app.logging.admin")
@Validated
public record AdminProperties(
        @NotNull Set<@Positive Long> userIds
) {

    public AdminProperties {
        userIds = userIds == null ? Set.of() : Set.copyOf(userIds);
    }
}