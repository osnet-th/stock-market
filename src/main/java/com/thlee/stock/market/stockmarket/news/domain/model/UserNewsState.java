package com.thlee.stock.market.stockmarket.news.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자별 뉴스 읽음/저장 상태 (#115).
 *
 * <p>뉴스는 키워드에 딸린 공유 데이터라 읽음 여부를 뉴스 행에 둘 수 없다 —
 * 같은 기사를 여러 사용자가 볼 수 있으므로 사용자×뉴스로 분리한다.
 *
 * <p>행은 <b>상호작용이 있을 때만</b> 생긴다. 행이 없으면 "안 읽음 · 미저장"이다.
 */
@Getter
public class UserNewsState {

    private Long id;
    private final Long userId;
    private final Long newsId;
    private boolean read;
    private boolean saved;
    private LocalDateTime updatedAt;

    public UserNewsState(Long id, Long userId, Long newsId, boolean read, boolean saved, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.newsId = newsId;
        this.read = read;
        this.saved = saved;
        this.updatedAt = updatedAt;
    }

    public static UserNewsState create(Long userId, Long newsId) {
        return new UserNewsState(null, userId, newsId, false, false, LocalDateTime.now());
    }

    public void markRead() {
        this.read = true;
        touch();
    }

    public void changeSaved(boolean saved) {
        this.saved = saved;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
