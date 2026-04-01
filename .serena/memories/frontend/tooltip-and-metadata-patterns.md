# Frontend Tooltip & Metadata Patterns - Learnings

**Date**: 2026-03-24
**Scope**: Tooltip implementations, financial indicator metadata, UI description patterns

## Key Findings from docs/solutions

### 1. Established Tooltip Pattern (Pure CSS, Verified)

**Location**: `src/main/resources/static/index.html` (lines 478-525, 370-395)

**Structure**:
```html
<div class="flex items-center gap-1">
  <span class="text-xs font-medium text-gray-500" x-text="sp.name"></span>
  <div class="group/d relative inline-block">
    <!-- Info icon (ℹ️ = &#9432;) -->
    <span class="text-gray-300 hover:text-gray-500 cursor-help text-xs">&#9432;</span>
    
    <!-- Tooltip - pure CSS hover state -->
    <div class="invisible opacity-0 group-hover/d:visible group-hover/d:opacity-100 
                absolute z-10 bottom-full left-0 mb-2 
                px-3 py-2 text-xs text-white bg-gray-800 rounded-lg shadow-lg 
                w-72 max-w-xs transition-opacity duration-200" 
         role="tooltip">
      <span x-text="sp.description"></span>
    </div>
  </div>
</div>
```

**Key Classes**:
- `group/d` and `group-hover/d:` — Tailwind CSS group modifiers for grouped hover states
- `invisible opacity-0` → `group-hover/d:visible group-hover/d:opacity-100` — Pure CSS visibility toggle
- `absolute z-10 bottom-full left-0` — Position tooltip above element
- `mb-2` — Margin between icon and tooltip
- `transition-opacity duration-200` — Smooth fade-in animation
- `w-72 max-w-xs` — Width constraints (288px max, responsive)
- `role="tooltip"` — ARIA accessibility

**Why This Pattern Works**:
1. No JS library dependency (Popper, Floating UI, etc.)
2. Low DOM weight — just one extra div per item
3. Tailwind-native — no custom CSS needed
4. Accessible — semantic HTML + ARIA roles
5. Works with Alpine.js reactivity — uses `x-text` binding

### 2. Metadata Structure in JavaScript

**Location**: `src/main/resources/static/js/app.js` (various lines with `description:` fields)

**Pattern** - Flat object with indicator properties:
```javascript
{
  name: 'Indicator Name',
  description: 'Human-friendly explanation in Korean...',  // 1-2 sentences, beginner-friendly
  unit: '%p',  // Optional unit display
  value: 5.2,  // Current value
  sub: 'KRW',  // Optional sub-label
  desc: 'Change indicator'  // Optional secondary description
}
```

**Description Style Guide** (from analysis):
- Level: Beginner-friendly Korean (초보자 눈높이)
- Length: 1-2 sentences max
- No jargon or only with explanation
- Focus on practical impact ("What does this mean for me?")
- Examples from actual code:
  - ✅ `'가계가 빌린 돈이 은행 전체 예금 대비 어느 수준인지. 1에 가까울수록 "빚으로 버티는 시장"이라는 뜻'`
  - ✅ `'높을수록 투자자들이 주식(위험자산) 선호, 낮으면 채권(안전자산) 선호'`

### 3. Metadata Management - Backend Pattern

**Reference Design**: `.claude/designs/economics/ecos-indicator-metadata-db/ecos-indicator-metadata-db.md`

**Key Decision**: Metadata can be stored as:
1. **Frontend JS** (current for ECOS indicators) — Fast, no API call, ideal for small/stable data
2. **Backend DB** (new pattern, proven in ECOS redesign) — Scalable, runtime-updatable

**When to use Frontend JS**:
- Fixed descriptions (don't change weekly)
- <500 items total
- Data is presentation-only (no business logic)
- Fast load required

**When to use Backend DB**:
- Descriptions update frequently
- Large dataset (100+ items)
- Need role-based access control
- Want to track metadata change history

**Current Project**: Using Frontend JS for financial indicators is valid (similar to ECOS pattern).

### 4. HTML Structure - Financial Statement Panels

**Reference**: `docs/brainstorms/2026-03-24-financial-indicator-descriptions-brainstorm.md`

**Scope**: Adding ℹ️ tooltips to financial statement panels (already approved)
- **Target**: All 8 menu tabs (재무계정, 재무지표, 배당정보, 전체재무제표, 주식수량, 소송현황, 사모자금사용, 공모자금사용)
- **Data Location**: Frontend JS metadata object (no backend API change)
- **UI Pattern**: Same as ECOS → ℹ️ icon + hover tooltip
- **Descriptions**: Beginner-friendly, non-technical

## Implementation Gotchas & Best Practices

### 1. Alpine.js + Tailwind Hover Groups

**Gotcha**: Using generic `group` classes with multiple hover elements on same page can cause interference.
**Solution**: Use scoped group modifiers like `group/d`, `group/sp` to namespace hover states.

```html
<!-- ✅ Correct - scoped to this specific group -->
<div class="group/d relative inline-block">
  <span>Icon</span>
  <div class="group-hover/d:visible">Tooltip</div>
</div>

<!-- ❌ Avoid - can interfere with other groups on page -->
<div class="group relative inline-block">
  <span>Icon</span>
  <div class="group-hover:visible">Tooltip</div>
</div>
```

### 2. Tooltip Positioning

**Current Pattern**: `bottom-full left-0 mb-2` (positions above, left-aligned)
**Consider**:
- If near right edge of viewport → use `right-0` instead of `left-0`
- If near top of viewport → use `top-full mb-2` (below) instead of `bottom-full`
- Current `w-72 max-w-xs` handles responsiveness

### 3. Accessibility Requirements (Must Include)

From ECOS redesign research:
- `role="tooltip"` on tooltip div — semantic HTML
- `aria-label` on icon if needed — screen reader text
- Hover change should be CSS-only (no JS dependency)
- Icon should have `cursor-help` class to signal help text available

### 4. Performance Considerations

From ECOS card dashboard plan (lines 62-64):
> 카테고리당 2~4개 카드 = 30-50개 DOM 요소. Alpine.js에 전혀 부담 없는 규모
> (성능 이슈는 500+ 요소부터)

**Safe threshold**: <500 tooltip elements per page. Financial statement panels + 8 tabs = ~100-200 items max = well within budget.

### 5. Data Binding with x-text

Current pattern uses `x-text="sp.description"` for dynamic content.
**Important**: Ensures escaping of HTML, prevents XSS.

## Related Design Documents

1. **ECOS Indicator Metadata DB** — `.claude/designs/economics/ecos-indicator-metadata-db/ecos-indicator-metadata-db.md`
   - Shows when to move metadata to database
   - Caching strategy (Map-based, no Caffeine needed for small datasets)

2. **ECOS Dashboard Improvement** — `.claude/designs/economics/ecos-dashboard-improvement/ecos-dashboard-improvement.md`
   - Detailed plan for tooltip + card UI + change indicators
   - Verifies pure CSS tooltip approach is production-ready
   - Lines 74-76: Confirmed CSS `group-hover` is preferred over external libs

3. **Financial Indicator Descriptions Brainstorm** — `docs/brainstorms/2026-03-24-financial-indicator-descriptions-brainstorm.md`
   - Specific scope for financial statement panels
   - Metadata structure examples
   - Beginner-friendly description guidelines

## Recommended Approach for Your Task

1. **Reuse Existing Pattern**: Copy HTML structure from lines 478-525 of `index.html`
2. **Create Frontend Metadata Object**: In `app.js`, create structured object mirroring ECOS pattern:
   ```javascript
   const financialDescriptions = {
     accounts: {
       '매출액': '기업이 제품이나 서비스를 판매하여 벌어들인 총 수입입니다.',
       '영업이익': '매출에서 원가와 판매·관리비를 뺀 금액으로, 본업에서 얼마나 벌었는지 보여줍니다.',
       // ...
     },
     indices: { /* PER, ROE, etc. */ },
     dividends: { /* 배당정보 */ }
     // ...
   }
   ```
3. **No Backend Changes**: Keep existing API, add client-side metadata mapping
4. **CSS Patterns**: Use exact Tailwind classes from existing implementation
5. **Accessibility**: Ensure `role="tooltip"` is always present

## Testing Checklist

- [ ] Tooltip visible on hover (not just disappearing at page edges)
- [ ] Text wraps properly in `w-72` container
- [ ] Smooth fade-in with `transition-opacity duration-200`
- [ ] Icon color correct: `text-gray-300 hover:text-gray-500`
- [ ] ARIA `role="tooltip"` present
- [ ] Works in both light and dark table rows (check contrast)
- [ ] Multiple tooltips on same page don't interfere (scoped `group/` names)

---

## Summary

**Green Light**: Using frontend-only tooltips with pure CSS hover is a proven, verified pattern in your codebase (ECOS dashboard). It's lightweight, accessible, and requires no library dependencies. The metadata structure is straightforward JavaScript objects with `description` fields. No backend changes needed for financial statement panels.
