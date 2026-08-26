package com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryCategory;
import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryTerm;
import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryTermContent;
import com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence.GlossaryCategoryEntity;
import com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence.GlossaryTermEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * glossary 도메인의 Entity ↔ Domain 변환 모음.
 * {@code categoryId == null} (미분류) 라운드트립 보존을 책임진다.
 * 관계 컬렉션은 양방향 모두 방어 복사한다 (Hibernate PersistentBag 공유 방지).
 */
public final class GlossaryMapper {

    private GlossaryMapper() {
    }

    // -------- GlossaryCategory --------
    public static GlossaryCategoryEntity toEntity(GlossaryCategory d) {
        return new GlossaryCategoryEntity(
                d.getId(), d.getUserId(), d.getName(), d.getCreatedAt(), d.getUpdatedAt()
        );
    }

    public static GlossaryCategory toDomain(GlossaryCategoryEntity e) {
        return new GlossaryCategory(
                e.getId(), e.getUserId(), e.getName(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    // -------- GlossaryTerm --------
    public static GlossaryTermEntity toEntity(GlossaryTerm d) {
        GlossaryTermContent c = d.getContent();
        return new GlossaryTermEntity(
                d.getId(), d.getUserId(), d.getName(),
                c.abbreviation(), c.oneLine(), c.definition(), c.scaleNote(), c.example(), c.takeaway(),
                d.getCategoryId(), new ArrayList<>(d.getRelatedTermIds()),
                d.getCreatedAt(), d.getUpdatedAt()
        );
    }

    public static GlossaryTerm toDomain(GlossaryTermEntity e) {
        GlossaryTermContent content = new GlossaryTermContent(
                e.getAbbreviation(), e.getOneLine(), e.getDefinition(),
                e.getScaleNote(), e.getExample(), e.getTakeaway()
        );
        return new GlossaryTerm(
                e.getId(), e.getUserId(), e.getName(), content, e.getCategoryId(),
                copyRelated(e.getRelatedTermIds()), e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    /**
     * 관계 컬렉션 방어 복사.
     * <ul>
     *   <li>null 컬렉션(미초기화) → 빈 목록</li>
     *   <li>null 요소 필터 — @OrderColumn 은 inbound 정리로 position 구멍이 생기면
     *       해당 자리를 null 로 채워 로드한다 (다음 save 에서 position 재정렬됨)</li>
     * </ul>
     */
    private static List<Long> copyRelated(List<Long> relatedTermIds) {
        if (relatedTermIds == null) {
            return List.of();
        }
        return relatedTermIds.stream().filter(Objects::nonNull).toList();
    }
}
