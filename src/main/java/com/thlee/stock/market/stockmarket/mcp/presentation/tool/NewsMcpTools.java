package com.thlee.stock.market.stockmarket.mcp.presentation.tool;

import com.thlee.stock.market.stockmarket.mcp.presentation.McpUserContext;
import com.thlee.stock.market.stockmarket.mcp.presentation.dto.McpNewsPage;
import com.thlee.stock.market.stockmarket.news.application.KeywordService;
import com.thlee.stock.market.stockmarket.news.application.NewsSearchApplicationService;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchCriteria;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchField;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchSort;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 뉴스 검색 MCP 도구 (조회 전용). 내가 구독한 키워드로 저장된 뉴스만 찾는다.
 */
@Component
@RequiredArgsConstructor
public class NewsMcpTools {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 30;
    private static final int MAX_QUERY_LENGTH = 200;

    private final NewsSearchApplicationService newsSearchApplicationService;
    private final KeywordService keywordService;

    @McpTool(name = "search_news", title = "뉴스 검색",
            annotations = @McpTool.McpAnnotations(title = "뉴스 검색", readOnlyHint = true,
                    destructiveHint = false, idempotentHint = true, openWorldHint = false),
            description = "내가 구독한 키워드로 수집·저장된 뉴스를 검색한다(인터넷 전체 검색이 아님). "
                    + "query가 있으면 제목·본문 검색, 없으면 최신순. 본문은 앞 300자만 담는다.")
    public McpNewsPage searchNews(
            @McpToolParam(description = "검색어(200자 이내). 생략하면 최신 뉴스", required = false) String query,
            @McpToolParam(description = "0부터 시작하는 페이지 번호. 기본 0", required = false) Integer page,
            @McpToolParam(description = "페이지 크기(1~30). 기본 10", required = false) Integer size) {
        Long userId = McpUserContext.currentUserId();
        String q = StringUtils.hasText(query) ? query.strip() : null;
        if (q != null && q.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("검색어는 200자 이내여야 합니다.");
        }
        int safePage = page == null ? 0 : Math.max(page, 0);
        int safeSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);
        NewsSearchCriteria criteria = new NewsSearchCriteria(q, keywordService.getSubscribedKeywordIds(userId),
                null, null, null, NewsSearchField.TITLE_CONTENT, NewsSearchSort.LATEST, false, safePage, safeSize);
        return McpNewsPage.from(newsSearchApplicationService.search(criteria, userId));
    }
}
