package com.thlee.stock.market.stockmarket.salary.domain.model;

import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 사용자 정의 지출 카테고리.
 *
 * <p>기본 8종은 {@link SpendingCategory} enum 이름을 code로 하여 사용자별 최초 접근 시
 * lazy 시드된다 (기존 enum 저장 행과 code가 그대로 매칭 — 데이터 이관 불필요).
 * 삭제는 soft delete(active=false)로 과거 월 이력을 보존한다.
 */
@Getter
public class UserSpendingCategory {

    public static final int NAME_MAX_LENGTH = 40;
    public static final int CODE_MAX_LENGTH = 20;

    /** 기본 8종 시드 색 (목업 실측색) */
    private static final Map<SpendingCategory, String> SEED_COLORS = Map.of(
            SpendingCategory.FOOD, "#c02a22",
            SpendingCategory.HOUSING, "#d9564a",
            SpendingCategory.TRANSPORT, "#e0a04a",
            SpendingCategory.EVENTS, "#2e8b62",
            SpendingCategory.COMMUNICATION, "#3aa88b",
            SpendingCategory.LEISURE, "#3f7ecb",
            SpendingCategory.SAVINGS_INVESTMENT, "#7a5cd6",
            SpendingCategory.ETC, "#7a828a"
    );

    /** 커스텀 카테고리 생성 시 순환 팔레트 */
    private static final List<String> CUSTOM_COLORS = List.of(
            "#5b8a72", "#a86bc9", "#c97b4a", "#4a90a4", "#8a6a34", "#6b737a");

    private Long id;
    private Long userId;
    private String code;
    private String name;
    private String color;

    /** 저축률 계산에 산입되는 카테고리 여부 (기본 8종 중 저축·투자) */
    private boolean savings;

    /** 기본 8종 여부 — 이름 변경 불가 */
    private boolean system;

    private boolean active;
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 재구성용 생성자 (Repository 조회 시) */
    public UserSpendingCategory(Long id, Long userId, String code, String name, String color,
                                boolean savings, boolean system, boolean active, int sortOrder,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.code = code;
        this.name = name;
        this.color = color;
        this.savings = savings;
        this.system = system;
        this.active = active;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 기본 8종 시드 세트 */
    public static List<UserSpendingCategory> defaultSet(Long userId) {
        validateUserId(userId);
        List<UserSpendingCategory> seeds = new ArrayList<>();
        for (SpendingCategory cat : SpendingCategory.values()) {
            seeds.add(seedOf(userId, cat, seeds.size()));
        }
        return seeds;
    }

    private static UserSpendingCategory seedOf(Long userId, SpendingCategory cat, int order) {
        LocalDateTime now = LocalDateTime.now();
        boolean savings = cat == SpendingCategory.SAVINGS_INVESTMENT;
        return new UserSpendingCategory(null, userId, cat.name(), cat.getDisplayName(),
                SEED_COLORS.get(cat), savings, true, true, order, now, now);
    }

    /** 커스텀 카테고리 생성. code는 서버 생성('U'+base36), 색은 팔레트 순환 */
    public static UserSpendingCategory createCustom(Long userId, String name, int sortOrder, long seq) {
        validateUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        String code = generateCode(seq);
        String color = CUSTOM_COLORS.get((int) (seq % CUSTOM_COLORS.size()));
        return new UserSpendingCategory(null, userId, code, normalizeName(name), color,
                false, false, true, sortOrder, now, now);
    }

    /** 이름 변경 — 커스텀 카테고리만 허용 */
    public void rename(String name) {
        if (this.system) {
            return;
        }
        this.name = normalizeName(name);
        this.updatedAt = LocalDateTime.now();
    }

    /** soft delete */
    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    /** 동명 재추가 시 이력 연결을 위한 재활성 */
    public void reactivate(int sortOrder) {
        this.active = true;
        this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }

    private static String generateCode(long seq) {
        String code = "U" + Long.toString(System.currentTimeMillis(), 36)
                + Long.toString(Math.floorMod(seq, 36L * 36), 36);
        return code.length() > CODE_MAX_LENGTH ? code.substring(0, CODE_MAX_LENGTH) : code;
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            normalized = "새 카테고리";
        }
        if (normalized.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("카테고리 이름은 " + NAME_MAX_LENGTH + "자 이내여야 합니다.");
        }
        return normalized;
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
    }
}
