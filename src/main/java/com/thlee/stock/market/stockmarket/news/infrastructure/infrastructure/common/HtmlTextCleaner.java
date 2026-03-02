package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.common;

import java.util.regex.Pattern;

public class HtmlTextCleaner {
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    public static String clean(String text) {
        if (text == null) {
            return null;
        }
        return HTML_TAG_PATTERN.matcher(text).replaceAll("");
    }
}