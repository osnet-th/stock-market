package com.thlee.stock.market.stockmarket.news.infrastructure;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsSearchPort;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsSearchPortFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Region별 NewsSearchPort 제공 팩토리 구현체
 */
@Component
@RequiredArgsConstructor
public class NewsSearchPortFactoryImpl implements NewsSearchPortFactory {

    private final Map<Region, List<NewsSearchPort>> portMap;

    public NewsSearchPortFactoryImpl(List<NewsSearchPort> ports) {
        this.portMap = ports.stream()
                .collect(Collectors.groupingBy(NewsSearchPort::supportedRegion));
    }

    @Override
    public List<NewsSearchPort> getPorts(Region region) {
        List<NewsSearchPort> ports = portMap.get(region);
        if (ports == null || ports.isEmpty()) {
            return Collections.emptyList();
        }
        return ports;
    }
}