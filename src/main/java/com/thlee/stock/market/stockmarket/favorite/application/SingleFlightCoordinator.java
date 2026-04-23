package com.thlee.stock.market.stockmarket.favorite.application;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 동일 indicatorType 에 대한 재조회 중복 실행을 억제한다.
 * type 단위 ReentrantLock 으로 스크래핑 폭주 방지.
 */
@Component
public class SingleFlightCoordinator {

    private final ConcurrentHashMap<GlobalEconomicIndicatorType, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T run(GlobalEconomicIndicatorType indicatorType, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(indicatorType, k -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}