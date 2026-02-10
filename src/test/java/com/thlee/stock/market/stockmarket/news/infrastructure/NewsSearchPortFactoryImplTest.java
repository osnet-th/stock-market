package com.thlee.stock.market.stockmarket.news.infrastructure;

import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchResult;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsSearchPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NewsSearchPortFactoryImplTest {

    @Test
    void getPorts_returns_ports_for_given_region() {
        // Given
        NewsSearchPort domesticPort1 = new TestPort(Region.DOMESTIC);
        NewsSearchPort domesticPort2 = new TestPort(Region.DOMESTIC);
        NewsSearchPort internationalPort = new TestPort(Region.INTERNATIONAL);

        List<NewsSearchPort> ports = List.of(domesticPort1, domesticPort2, internationalPort);
        NewsSearchPortFactoryImpl factory = new NewsSearchPortFactoryImpl(ports);

        // When
        List<NewsSearchPort> domesticPorts = factory.getPorts(Region.DOMESTIC);

        // Then
        assertThat(domesticPorts).hasSize(2);
        assertThat(domesticPorts).containsExactlyInAnyOrder(domesticPort1, domesticPort2);
    }

    @Test
    void getPorts_returns_empty_list_when_no_port_for_region() {
        // Given
        NewsSearchPort domesticPort = new TestPort(Region.DOMESTIC);
        List<NewsSearchPort> ports = List.of(domesticPort);
        NewsSearchPortFactoryImpl factory = new NewsSearchPortFactoryImpl(ports);

        // When
        List<NewsSearchPort> internationalPorts = factory.getPorts(Region.INTERNATIONAL);

        // Then
        assertThat(internationalPorts).isEmpty();
    }

    @Test
    void getPorts_returns_single_port_when_only_one_for_region() {
        // Given
        NewsSearchPort internationalPort = new TestPort(Region.INTERNATIONAL);
        List<NewsSearchPort> ports = List.of(internationalPort);
        NewsSearchPortFactoryImpl factory = new NewsSearchPortFactoryImpl(ports);

        // When
        List<NewsSearchPort> internationalPorts = factory.getPorts(Region.INTERNATIONAL);

        // Then
        assertThat(internationalPorts).hasSize(1);
        assertThat(internationalPorts).containsExactly(internationalPort);
    }

    private static class TestPort implements NewsSearchPort {
        private final Region region;

        TestPort(Region region) {
            this.region = region;
        }

        @Override
        public List<NewsSearchResult> search(String keyword, LocalDateTime fromDateTime) {
            return Collections.emptyList();
        }

        @Override
        public Region supportedRegion() {
            return region;
        }
    }
}