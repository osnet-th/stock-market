package com.thlee.stock.market.stockmarket.stock.infrastructure.exchangerate.koreaexim;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "koreaexim.api")
public class KoreaEximProperties {

    private String url = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON";
    private String authkey;
}