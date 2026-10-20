package com.thlee.stock.market.stockmarket.mcp.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * MCP 연결 OAuth 저장 테이블을 기동 시 만든다. JPA Entity가 아니어서 ddl-auto로 생기지 않는다.
 * 스키마는 CREATE TABLE IF NOT EXISTS라 이미 있으면 건너뛴다.
 */
@Component
@RequiredArgsConstructor
public class McpOAuthSchemaInitializer implements InitializingBean {

    private final DataSource dataSource;

    @Override
    public void afterPropertiesSet() {
        new ResourceDatabasePopulator(new ClassPathResource("mcp/mcp-oauth-schema.sql")).execute(dataSource);
    }
}
