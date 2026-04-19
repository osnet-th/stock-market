package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsIndexPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 기존 뉴스 데이터를 ES에 일괄 인덱싱하는 백필 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsBackfillService {

    private static final int BATCH_SIZE = 1000;

    private final NewsRepository newsRepository;
    private final NewsIndexPort newsIndexPort;

    public int backfill() {
        int totalIndexed = 0;
        int page = 0;

        log.info("뉴스 백필 시작");

        while (true) {
            PageResult<News> pageResult = newsRepository.findAll(page, BATCH_SIZE);

            if (pageResult.getContent().isEmpty()) {
                break;
            }

            int indexed = newsIndexPort.indexAll(pageResult.getContent());
            totalIndexed += indexed;

            log.info("뉴스 백필 진행: {}건 완료 / 전체 {}건", totalIndexed, pageResult.getTotalElements());

            if (page >= pageResult.getTotalPages() - 1) {
                break;
            }
            page++;
        }

        log.info("뉴스 백필 완료: 총 {}건 인덱싱", totalIndexed);
        return totalIndexed;
    }
}