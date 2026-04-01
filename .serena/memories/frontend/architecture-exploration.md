# Frontend Architecture Exploration Summary

## Stack & Framework
- **SPA Framework**: Alpine.js (x-data, x-show, x-if, x-for)
- **CSS Framework**: Tailwind CSS (CDN) + Custom CSS (/css/custom.css)
- **API Calls**: Vanilla fetch (no Axios or other library)
- **Authentication**: Bearer Token (localStorage: accessToken, userId)
- **HTML Template**: Single index.html with page sections controlled by `currentPage` state
- **Charts**: Chart.js (line, bar, pie, donut)

## HTML Structure
- **Layout Pattern**: Header + Fixed Sidebar + Main Content
  - Header: Fixed top bar, h-14, displays user info + logout
  - Sidebar: Fixed left (collapsible, w-56 or w-14), navigation menu
  - Main: Flex-1, dynamic content based on `currentPage`
  
- **Routing**: URL-less SPA — navigation via `currentPage` state variable (home, keywords, ecos, global, portfolio)
- **Pages**: 
  - home: Summary cards (4 cards for keywords, ECOS, global, portfolio)
  - keywords: Keyword management with add/edit/delete
  - ecos: Domestic economic indicators (categories + indicators)
  - global: Global economic indicators (by category)
  - portfolio: Asset allocation (donut chart, bar chart, collapsible asset sections)

## Modal & Panel Patterns

### Modals (Centered, fixed inset-0, z-50)
1. **Add/Edit Modal Pattern**:
   ```html
   <div x-show="state.showAddModal" x-cloak
       class="modal-backdrop fixed inset-0 bg-black/30 flex items-center justify-center z-50">
     <div class="bg-white rounded-xl p-6 w-[480px] max-h-[80vh] overflow-y-auto shadow-xl"
         @click.outside="state.showAddModal = false">
   ```
   - Used for: Portfolio add modal, keywords add modal, purchase modal
   - Features: `@click.outside` to close, scrollable content, responsive width

2. **Confirmation Modal Pattern** (Delete confirmation):
   ```html
   <div x-show="portfolio.deleteConfirm.show" x-cloak
       class="modal-backdrop fixed inset-0 bg-black/30 flex items-center justify-center z-50">
     <div class="bg-white rounded-xl p-6 w-[400px] shadow-xl"
         @click.outside="cancelDelete()">
   ```

### Slide Panel (Right-side overlay)
1. **Stock Financial Detail Panel**:
   - Location: Right edge (w-[65%])
   - Entry: `x-transition:enter` with `translate-x-full` → `translate-x-0`
   - Exit: `x-transition:leave` with `translate-x-0` → `translate-x-full`
   - Features: Sticky header, escape key handler, z-50, overlay (z-40)
   - Content: Menu buttons + dropdown filters + results table

### Tabs/Menu Button Pattern
```html
<div class="flex flex-wrap gap-2 mb-4">
  <template x-for="menu in portfolio.financialMenus" :key="menu.key">
    <button @click="selectFinancialMenu(menu.key)"
            class="text-xs px-3 py-1.5 rounded-full border transition"
            :class="portfolio.selectedFinancialMenu === menu.key
                ? 'bg-indigo-600 text-white border-indigo-600'
                : 'bg-white text-gray-600 border-gray-300 hover:border-indigo-400 hover:text-indigo-600'">
```
- Pill-shaped buttons with active state styling
- State-driven activation (selectedFinancialMenu)

### Collapsible Sections
```html
<button @click="toggleSection(alloc.assetType)"
    class="w-full flex items-center justify-between px-5 py-3 hover:bg-gray-50 transition">
<div x-show="portfolio.expandedSections[alloc.assetType]" class="p-3 space-y-2">
```
- Stored in `expandedSections` object keyed by type
- Show/hide with x-show

## State Management (Alpine.js data object)
```javascript
dashboard() {
  return {
    // Global state
    currentPage: 'home',
    sidebarCollapsed: false,
    auth: { token, userId, role, displayName },
    
    // Component states
    keywords: { list, filter, regionFilter, showAddModal, newKeyword },
    portfolio: { 
      items, allocation, loading,
      showAddModal, showEditModal, editingItem,
      selectedAssetType, addForm, editForm,
      expandedSections, selectedNewsItemId,
      // Financial detail
      selectedStockItem, financialYear, financialReportCode,
      selectedFinancialMenu, financialIndexClass,
      financialResult, financialLoading
    },
    ecos: { categories, selectedCategory, indicators, loading },
    globalData: { categories, selectedCategory, selectedIndicator, indicatorData },
    news: { selectedKeywordId, list, page, size, totalPages, totalElements },
    
    // Methods for lifecycle, CRUD, navigation
    init(), navigateTo(), logout(),
    loadKeywords(), addKeyword(), removeKeyword(),
    loadPortfolioItems(), openAddModal(), openEditModal(),
    // ... etc
  }
}
```

## API Client Pattern (api.js)
```javascript
const API = {
  baseUrl: '',
  getHeaders() { /* Bearer token in Authorization header */ },
  async request(method, url, body = null) { /* fetch with error handling */ },
  
  // Grouped by feature
  getMyProfile(), signup(), logout(),
  getKeywords(), registerKeyword(), activateKeyword(), deactivateKeyword(),
  getPortfolioItems(), addStockItem(), updateStockItem(), deletePortfolioItem(),
  getStockPrices(), searchStocks(),
  getFinancialOptions(), getFinancialIndices(), getFinancialDividends(),
  // ... etc
}
```

## Styling Patterns
- **Colors**: Tailwind defaults (blue-600, red-500, gray-50, etc)
- **Spacing**: Consistent 4px (px-4, py-3, gap-4)
- **Borders**: Subtle gray-200 borders, rounded-lg/rounded-xl
- **Shadows**: shadow-sm (cards), shadow-xl (modals)
- **Transitions**: transition class for smooth hover/state changes
- **Responsive**: grid grid-cols-1 md:grid-cols-2/4
- **Custom Scrollbar**: Defined in custom.css

## Key Files
- `/static/index.html` (4300+ lines, all pages embedded)
- `/static/js/app.js` (8000+ lines, dashboard state + all methods)
- `/static/js/api.js` (240+ lines, API client)
- `/static/js/components/keyword.js` (59 lines, KeywordComponent mixin)
- `/static/js/utils/format.js` (number formatting utilities)
- `/static/css/custom.css` (81 lines, animations, scrollbar, spinner)

## How to Add New UI Element (Chat Panel)
1. **Add state to dashboard()**: 
   - `chat: { messages: [], inputText: '', isOpen: false, isLoading: false }`
2. **Create page section in HTML**:
   - `<div x-show="currentPage === 'chat'" x-cloak>`
3. **Add menu item** to `menus` array in dashboard()
4. **Choose container pattern**:
   - Option A: Full-page (like portfolio)
   - Option B: Slide panel from right (like financial detail)
   - Option C: Fixed panel bottom-right (chat bubble pattern)
5. **Implement methods** in dashboard():
   - `sendChatMessage()`, `loadMessages()`, `toggleChat()`
6. **API integration** via api.js:
   - Add `sendChatMessage(userId, message, mode, stockCode)` that calls `/api/chat`
7. **Stream handling** (if SSE):
   - Use `EventSource` or `fetch` with streaming body reader
   - Append chunks to messages array as they arrive

## WebSocket/Streaming Pattern (Currently Used)
- No WebSocket patterns found in current frontend code
- API calls use fetch with JSON responses
- For chatbot SSE: Will need to add EventSource listener or ReadableStream handler

## Component File Pattern
- Small components extracted as objects with methods (KeywordComponent, GlobalComponent, EcosComponent)
- Mixed into dashboard state or called directly from methods
- Example: `await KeywordComponent.loadKeywords.call(this)`

## Custom CSS Classes
- `.x-cloak`: Hides elements during Alpine initialization
- `.sidebar-item`: Smooth transitions for menu items
- `.data-table tbody tr:hover`: Table row hover effect
- `.spinner`: Loading animation (CSS keyframe)
- `.modal-backdrop`: Fade-in animation
- `.summary-card`: Hover shadow effect
- `.tab-active::after`: Blue underline for active tabs (unused in current code)
