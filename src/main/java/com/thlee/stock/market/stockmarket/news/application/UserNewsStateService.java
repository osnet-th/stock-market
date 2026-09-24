package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.domain.model.UserNewsState;
import com.thlee.stock.market.stockmarket.news.domain.repository.UserNewsStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 뉴스 읽음/저장 상태 유스케이스 (#115).
 *
 * <p>상태 행은 상호작용이 있을 때만 만든다 — 안 읽은 기사까지 행을 만들면
 * 뉴스 수만큼 행이 불어난다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserNewsStateService {

    private final UserNewsStateRepository userNewsStateRepository;

    @Transactional
    public void markRead(Long userId, Long newsId) {
        UserNewsState state = userNewsStateRepository.findByUserIdAndNewsId(userId, newsId)
                .orElseGet(() -> UserNewsState.create(userId, newsId));
        if (state.isRead()) {
            return;
        }
        state.markRead();
        userNewsStateRepository.save(state);
    }

    /**
     * 저장(★) 토글.
     *
     * @return 토글 후 저장 여부
     */
    @Transactional
    public boolean toggleSaved(Long userId, Long newsId) {
        UserNewsState state = userNewsStateRepository.findByUserIdAndNewsId(userId, newsId)
                .orElseGet(() -> UserNewsState.create(userId, newsId));
        state.changeSaved(!state.isSaved());
        userNewsStateRepository.save(state);
        return state.isSaved();
    }

    /**
     * 목업 `모두 읽음` — 지금 화면에 보이는 기사만 대상으로 한다.
     * 전체 뉴스를 대상으로 하면 사용자가 보지도 않은 기사까지 읽음이 된다.
     *
     * @return 새로 읽음이 된 건수
     */
    @Transactional
    public int markAllRead(Long userId, List<Long> newsIds) {
        return userNewsStateRepository.markAllRead(userId, newsIds);
    }
}
