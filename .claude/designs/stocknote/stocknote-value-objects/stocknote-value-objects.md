# [stocknote] Valuation / FundamentalImpact VO 분리 (옵션 A)

> 분석: [stocknote-value-objects](../../../analyzes/stocknote/stocknote-value-objects/stocknote-value-objects.md). plan task: Phase 10 P1 #15.

## 의도

도메인 model 에 `Valuation` / `FundamentalImpact` record VO 신설. StockNote 의 8개 평면 필드를 2 VO 로 묶음. create/updateBody 인자 21→13 으로 swap 위험 해소. Entity / DB / 응답 contract 무영향 (위임 getter 유지).

## 변경 사항

### 1. 신규 VO — `Valuation`

위치: `stocknote/domain/model/Valuation.java`

```java
package com.thlee.stock.market.stockmarket.stocknote.domain.model;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.VsAverageLevel;

import java.math.BigDecimal;

/**
 * 기록 작성 시점의 밸류에이션 지표 묶음. 모든 필드 nullable (사용자가 일부만 입력 가능).
 */
public record Valuation(
        BigDecimal per,
        BigDecimal pbr,
        BigDecimal evEbitda,
        VsAverageLevel vsAverage
) {
    public static Valuation empty() {
        return new Valuation(null, null, null, null);
    }
}
```

### 2. 신규 VO — `FundamentalImpact`

위치: `stocknote/domain/model/FundamentalImpact.java`

```java
package com.thlee.stock.market.stockmarket.stocknote.domain.model;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.ImpactLevel;

/**
 * 기록 작성 시점의 펀더멘털 영향도 묶음. 모든 필드 nullable.
 */
public record FundamentalImpact(
        ImpactLevel revenueImpact,
        ImpactLevel profitImpact,
        ImpactLevel cashflowImpact,
        boolean oneTime,
        boolean structural
) {
    public static FundamentalImpact empty() {
        return new FundamentalImpact(null, null, null, false, false);
    }
}
```

### 3. `StockNote` — 평면 필드 2 VO 로 묶음 + 위임 getter 유지

```java
@Getter
public class StockNote {

    public static final int TEXT_MAX_LENGTH = 4000;

    private Long id;
    private final Long userId;
    private final String stockCode;
    private final MarketType marketType;
    private final ExchangeCode exchangeCode;
    private final NoteDirection direction;
    private final BigDecimal changePercent;
    private final LocalDate noteDate;

    private String triggerText;
    private String interpretationText;
    private String riskText;
    private boolean preReflected;
    private UserJudgment initialJudgment;

    private Valuation valuation;                  // ← 8개 평면 → 2 VO
    private FundamentalImpact fundamentalImpact;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 재구성용 생성자 (Repository / Mapper 전용) */
    public StockNote(Long id, Long userId, String stockCode, MarketType marketType, ExchangeCode exchangeCode,
                     NoteDirection direction, BigDecimal changePercent, LocalDate noteDate,
                     String triggerText, String interpretationText, String riskText,
                     boolean preReflected, UserJudgment initialJudgment,
                     Valuation valuation, FundamentalImpact fundamentalImpact,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        // 필드 할당
    }

    /** 신규 기록 생성 팩토리. */
    public static StockNote create(Long userId, String stockCode, MarketType marketType, ExchangeCode exchangeCode,
                                   NoteDirection direction, BigDecimal changePercent, LocalDate noteDate, LocalDate today,
                                   String triggerText, String interpretationText, String riskText,
                                   boolean preReflected, UserJudgment initialJudgment,
                                   Valuation valuation, FundamentalImpact fundamentalImpact) {
        // 검증 + 생성
        Valuation v = valuation == null ? Valuation.empty() : valuation;
        FundamentalImpact fi = fundamentalImpact == null ? FundamentalImpact.empty() : fundamentalImpact;
        // ...
    }

    /** 본문 + 분석 항목 수정. */
    public void updateBody(String triggerText, String interpretationText, String riskText,
                           boolean preReflected, UserJudgment initialJudgment,
                           Valuation valuation, FundamentalImpact fundamentalImpact) {
        // ...
    }

    // 위임 getter — 응답 contract 호환 (DetailResponse.NoteDto 가 n.getPer() 호출 그대로 사용)
    public BigDecimal getPer() { return valuation.per(); }
    public BigDecimal getPbr() { return valuation.pbr(); }
    public BigDecimal getEvEbitda() { return valuation.evEbitda(); }
    public VsAverageLevel getVsAverage() { return valuation.vsAverage(); }
    public ImpactLevel getRevenueImpact() { return fundamentalImpact.revenueImpact(); }
    public ImpactLevel getProfitImpact() { return fundamentalImpact.profitImpact(); }
    public ImpactLevel getCashflowImpact() { return fundamentalImpact.cashflowImpact(); }
    public boolean isOneTime() { return fundamentalImpact.oneTime(); }
    public boolean isStructural() { return fundamentalImpact.structural(); }
    // 기존 getter (id, userId, ..., valuation, fundamentalImpact) 는 @Getter Lombok 으로 자동
}
```

### 4. `StockNoteWriteService` — cmd → VO 조립

```java
@Transactional
public Long create(CreateStockNoteCommand cmd) {
    LocalDate today = LocalDate.now();
    Valuation valuation = new Valuation(cmd.per(), cmd.pbr(), cmd.evEbitda(), cmd.vsAverage());
    FundamentalImpact fi = new FundamentalImpact(
            cmd.revenueImpact(), cmd.profitImpact(), cmd.cashflowImpact(),
            cmd.oneTime(), cmd.structural());
    StockNote note = StockNote.create(
            cmd.userId(), cmd.stockCode(), cmd.marketType(), cmd.exchangeCode(),
            cmd.direction(), cmd.changePercent(), cmd.noteDate(), today,
            cmd.triggerText(), cmd.interpretationText(), cmd.riskText(),
            cmd.preReflected(), cmd.initialJudgment(),
            valuation, fi
    );
    // ... 기존 흐름 유지
}

@Transactional
public void update(UpdateStockNoteCommand cmd) {
    // ...
    Valuation valuation = new Valuation(cmd.per(), cmd.pbr(), cmd.evEbitda(), cmd.vsAverage());
    FundamentalImpact fi = new FundamentalImpact(
            cmd.revenueImpact(), cmd.profitImpact(), cmd.cashflowImpact(),
            cmd.oneTime(), cmd.structural());
    note.updateBody(
            cmd.triggerText(), cmd.interpretationText(), cmd.riskText(),
            cmd.preReflected(), cmd.initialJudgment(),
            valuation, fi
    );
    // ...
}
```

### 5. `StockNoteMapper` — Entity 평면 ↔ Domain VO 변환

```java
public static StockNote toDomain(StockNoteEntity e) {
    Valuation valuation = new Valuation(e.getPer(), e.getPbr(), e.getEvEbitda(), e.getVsAverage());
    FundamentalImpact fi = new FundamentalImpact(
            e.getRevenueImpact(), e.getProfitImpact(), e.getCashflowImpact(),
            e.isOneTime(), e.isStructural());
    return new StockNote(
            e.getId(), e.getUserId(), e.getStockCode(), e.getMarketType(), e.getExchangeCode(),
            e.getDirection(), e.getChangePercent(), e.getNoteDate(),
            e.getTriggerText(), e.getInterpretationText(), e.getRiskText(),
            e.isPreReflected(), e.getInitialJudgment(),
            valuation, fi,
            e.getCreatedAt(), e.getUpdatedAt()
    );
}

public static StockNoteEntity toEntity(StockNote n) {
    // 위임 getter 사용 — n.getPer(), n.getPbr() 등 그대로
}
```

### 6. `StockNoteDetailResponse.NoteDto` — 변경 없음

위임 getter 덕분에 기존 호출 (`n.getPer()`, `n.getRevenueImpact()`, `n.isOneTime()`) 그대로 동작. NoteDto 응답 필드 평면 유지.

## 회귀 위험

| 위험 | 영향 | 완화 |
|---|---|---|
| StockNote 생성자 시그니처 변경 — Mapper 호출 | Mapper 변경 동시 진행 | 컴파일러 검증 |
| WriteService.create / update 호출 변경 | 본 PR 에서 동시 변경 | 컴파일러 검증 |
| 위임 getter 누락 | DetailResponse 응답 필드 누락 | 모든 8개 위임 getter 명시 |
| Lombok @Getter 가 valuation / fundamentalImpact 도 자동 생성 | 이중 노출 (n.getValuation() + n.getPer() 둘 다 가능) | 의도된 동작 |

## 작업 리스트

- [ ] `Valuation.java` record 신규
- [ ] `FundamentalImpact.java` record 신규
- [ ] `StockNote` 필드 + 생성자 + create + updateBody 시그니처 변경 + 위임 getter 추가
- [ ] `StockNoteWriteService.create / update` cmd → VO 조립
- [ ] `StockNoteMapper.toDomain / toEntity` VO 변환
- [ ] 컴파일 확인
- [ ] plan checkbox 갱신 (P1 #15)

## 승인 대기

태형님 승인 후 구현 진행.