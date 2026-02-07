package com.thlee.stock.market.stockmarket.news.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NewsTest {

    @Test
    void create_requires_mandatory_fields() {
        assertThrows(IllegalArgumentException.class, () -> News.create(
                null,
                1L,
                "title",
                "content",
                LocalDateTime.now(),
                NewsPurpose.KEYWORD,
                "keyword"
        ));

        assertThrows(IllegalArgumentException.class, () -> News.create(
                "url",
                null,
                "title",
                "content",
                LocalDateTime.now(),
                NewsPurpose.KEYWORD,
                "keyword"
        ));

        assertThrows(IllegalArgumentException.class, () -> News.create(
                "url",
                1L,
                "",
                "content",
                LocalDateTime.now(),
                NewsPurpose.KEYWORD,
                "keyword"
        ));

        assertThrows(IllegalArgumentException.class, () -> News.create(
                "url",
                1L,
                "title",
                "content",
                null,
                NewsPurpose.KEYWORD,
                "keyword"
        ));

        assertThrows(IllegalArgumentException.class, () -> News.create(
                "url",
                1L,
                "title",
                "content",
                LocalDateTime.now(),
                null,
                "keyword"
        ));

        assertThrows(IllegalArgumentException.class, () -> News.create(
                "url",
                1L,
                "title",
                "content",
                LocalDateTime.now(),
                NewsPurpose.KEYWORD,
                ""
        ));
    }

    @Test
    void create_success() {
        assertDoesNotThrow(() -> News.create(
                "url",
                1L,
                "title",
                "content",
                LocalDateTime.now(),
                NewsPurpose.KEYWORD,
                "keyword"
        ));
    }
}
