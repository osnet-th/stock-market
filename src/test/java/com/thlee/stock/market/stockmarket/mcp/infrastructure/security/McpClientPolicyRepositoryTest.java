package com.thlee.stock.market.stockmarket.mcp.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpClientPolicyRepositoryTest {

    private static final String CLAUDE_CALLBACK = "https://claude.ai/api/mcp/auth_callback";

    @Test
    @DisplayName("P1: URL 형태 client_id는 허용 호스트의 https만 조회하고 나머지는 없는 클라이언트로 본다")
    void urlClientIdAllowlist() {
        // given
        FakeRepository fake = new FakeRepository();
        String[] ids = {"https://claude.ai/oauth/claude-metadata", "https://mcp.claude.com/client.json",
                "https://evil.example/meta", "http://claude.ai/meta", "https://claude.ai.evil.com/meta",
                "https://10.0.0.1/meta", "https://evilclaude.ai/meta"};
        for (String id : ids) {
            fake.add(client(id, id));
        }
        McpClientPolicyRepository repository = new McpClientPolicyRepository(fake);

        // when / then
        assertThat(repository.findByClientId("https://claude.ai/oauth/claude-metadata")).isNotNull();
        assertThat(repository.findByClientId("https://mcp.claude.com/client.json")).isNotNull();
        assertThat(repository.findByClientId("https://evil.example/meta")).isNull();
        assertThat(repository.findByClientId("http://claude.ai/meta")).isNull();
        assertThat(repository.findByClientId("https://claude.ai.evil.com/meta")).isNull();
        assertThat(repository.findByClientId("https://10.0.0.1/meta")).isNull();
        assertThat(repository.findByClientId("https://evilclaude.ai/meta")).isNull();
        assertThat(repository.findById("https://evil.example/meta")).isNull();
    }

    @Test
    @DisplayName("P2: CIMD 클라이언트가 id 조회 캐시에서 빠져도 문서 URL로 다시 찾는다")
    void cimdFallbackToClientId() {
        // given: id 조회는 비고(캐시 만료), client_id 조회는 문서를 다시 가져온다
        String url = "https://claude.ai/oauth/claude-metadata";
        FakeRepository fake = new FakeRepository();
        fake.addResolvableOnlyByClientId(client(url, url));
        McpClientPolicyRepository repository = new McpClientPolicyRepository(fake);

        // when
        RegisteredClient found = repository.findById(url);

        // then
        assertThat(found).isNotNull();
        assertThat(found.getClientId()).isEqualTo(url);
        assertThat(found.getRedirectUris()).containsExactly(CLAUDE_CALLBACK);
        assertThat(found.getClientSettings().isRequireProofKey()).isTrue();
    }

    @Test
    @DisplayName("P3: 동적 등록 클라이언트(UUID id)는 그대로 조회된다")
    void dcrClient() {
        // given
        FakeRepository fake = new FakeRepository();
        fake.add(client("9f1c0a52-7d3e-4c1b-9a51-2f0d7b6e8c11", "dcr-client-id"));
        McpClientPolicyRepository repository = new McpClientPolicyRepository(fake);

        // when / then
        assertThat(repository.findById("9f1c0a52-7d3e-4c1b-9a51-2f0d7b6e8c11")).isNotNull();
        assertThat(repository.findByClientId("dcr-client-id")).isNotNull();
        assertThat(repository.findById("unknown")).isNull();
    }

    @Test
    @DisplayName("P4: 외부 키 주소를 받아 오는 클라이언트 인증(private_key_jwt·client_secret_jwt)은 뺀다")
    void jwtClientAuthenticationRemoved() {
        // given
        FakeRepository fake = new FakeRepository();
        fake.add(RegisteredClient.from(client("dcr-1", "dcr-jwt"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_JWT)
                .build());
        McpClientPolicyRepository repository = new McpClientPolicyRepository(fake);

        // when
        RegisteredClient found = repository.findByClientId("dcr-jwt");

        // then
        assertThat(found.getClientAuthenticationMethods()).containsExactly(ClientAuthenticationMethod.NONE);
    }

    private static RegisteredClient client(String id, String clientId) {
        return RegisteredClient.withId(id)
                .clientId(clientId)
                .clientName("Claude")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(CLAUDE_CALLBACK)
                .build();
    }

    /** 메모리 저장소. byIdHidden에 넣은 클라이언트는 id로는 안 보이고 client_id로만 보인다 */
    private static class FakeRepository implements RegisteredClientRepository {
        private final Map<String, RegisteredClient> byId = new HashMap<>();
        private final Map<String, RegisteredClient> byClientId = new HashMap<>();

        void add(RegisteredClient client) {
            byId.put(client.getId(), client);
            byClientId.put(client.getClientId(), client);
        }

        void addResolvableOnlyByClientId(RegisteredClient client) {
            byClientId.put(client.getClientId(), client);
        }

        @Override
        public void save(RegisteredClient registeredClient) {
            add(registeredClient);
        }

        @Override
        public RegisteredClient findById(String id) {
            return byId.get(id);
        }

        @Override
        public RegisteredClient findByClientId(String clientId) {
            return byClientId.get(clientId);
        }
    }
}
