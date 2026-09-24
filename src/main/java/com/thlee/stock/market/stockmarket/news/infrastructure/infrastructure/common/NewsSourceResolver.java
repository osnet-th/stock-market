package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.common;

import java.net.URI;
import java.util.Map;

/**
 * 기사 URL 에서 언론사명을 파생한다 (#115).
 *
 * <p>네이버 뉴스 검색 API 응답에는 언론사 필드가 아예 없어 도메인 파생이 유일한 방법이다.
 * GNews·NewsAPI 는 응답의 {@code source.name} 을 쓰고, 그것이 비어 있을 때만 이 클래스로 폴백한다.
 *
 * <p>매핑에 없는 도메인은 {@code www.} 만 떼고 도메인 문자열을 그대로 돌려준다.
 * 억지로 추측하면 틀린 언론사명을 보여주게 되므로, 모르면 도메인을 노출하는 편이 정직하다.
 */
public final class NewsSourceResolver {

    /** 국내 주요 언론사 도메인 → 표기명. 네이버 수집분이 대부분 이 범위에 든다. */
    private static final Map<String, String> DOMAIN_TO_NAME = Map.ofEntries(
            Map.entry("hankyung.com", "한국경제"),
            Map.entry("mk.co.kr", "매일경제"),
            Map.entry("sedaily.com", "서울경제"),
            Map.entry("edaily.co.kr", "이데일리"),
            Map.entry("mt.co.kr", "머니투데이"),
            Map.entry("fnnews.com", "파이낸셜뉴스"),
            Map.entry("asiae.co.kr", "아시아경제"),
            Map.entry("etnews.com", "전자신문"),
            Map.entry("einfomax.co.kr", "연합인포맥스"),
            Map.entry("yna.co.kr", "연합뉴스"),
            Map.entry("newsis.com", "뉴시스"),
            Map.entry("news1.kr", "뉴스1"),
            Map.entry("chosun.com", "조선일보"),
            Map.entry("joongang.co.kr", "중앙일보"),
            Map.entry("donga.com", "동아일보"),
            Map.entry("hani.co.kr", "한겨레"),
            Map.entry("khan.co.kr", "경향신문"),
            Map.entry("seoul.co.kr", "서울신문"),
            Map.entry("kmib.co.kr", "국민일보"),
            Map.entry("segye.com", "세계일보"),
            Map.entry("hankookilbo.com", "한국일보"),
            Map.entry("munhwa.com", "문화일보"),
            Map.entry("kbs.co.kr", "KBS"),
            Map.entry("imbc.com", "MBC"),
            Map.entry("sbs.co.kr", "SBS"),
            Map.entry("ytn.co.kr", "YTN"),
            Map.entry("jtbc.co.kr", "JTBC"),
            Map.entry("wowtv.co.kr", "한국경제TV"),
            Map.entry("mtn.co.kr", "머니투데이방송"),
            Map.entry("dt.co.kr", "디지털타임스"),
            Map.entry("inews24.com", "아이뉴스24"),
            Map.entry("zdnet.co.kr", "지디넷코리아"),
            Map.entry("thelec.kr", "전자부품 전문 미디어"),
            Map.entry("biz.chosun.com", "조선비즈"),
            Map.entry("daejonilbo.com", "대전일보")
    );

    private NewsSourceResolver() {
    }

    /**
     * URL 의 호스트로 언론사명을 찾는다. 찾지 못하면 호스트 문자열, 파싱 실패면 null.
     */
    public static String fromUrl(String url) {
        String host = extractHost(url);
        if (host == null) {
            return null;
        }

        // 서브도메인이 붙은 경우(news.hankyung.com)도 잡으려면 뒤에서부터 좁혀 본다.
        // biz.chosun.com 처럼 서브도메인 자체가 별도 매체인 경우가 있어 전체 호스트를 먼저 본다.
        String name = DOMAIN_TO_NAME.get(host);
        if (name != null) {
            return name;
        }
        for (int i = host.indexOf('.'); i >= 0; i = host.indexOf('.', i + 1)) {
            name = DOMAIN_TO_NAME.get(host.substring(i + 1));
            if (name != null) {
                return name;
            }
        }
        return host;
    }

    private static String extractHost(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            String host = URI.create(url.trim()).getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            host = host.toLowerCase();
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (IllegalArgumentException e) {
            // 수집 대상 URL 이 깨져 있어도 저장 자체를 막지 않는다
            return null;
        }
    }
}
