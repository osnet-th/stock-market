/**
 * NewsJournalComponent — 뉴스 기록 (마스터-디테일 리디자인).
 *
 * 소유 프로퍼티: newsJournal
 * 외부 의존: API (api.js), auth (userId — 초안 localStorage 키)
 *
 * 근거 문서:
 *  - docs/brainstorms/2026-08-21-news-journal-redesign-brainstorm.md
 *  - docs/plans/2026-08-21-001-feat-news-journal-redesign-plan.md
 *
 * 구조:
 *  - 목록/디테일 2-pane. 보기·수정·새 기록 모두 디테일 pane 인라인 (모달 없음)
 *  - 서버 필터: q(통합 검색) · impact · categoryId · 기간 · keywords(AND) — 변경 시 리셋 로드
 *  - 화면 통계(stats API): 임팩트/분류 건수 + 사건별 키워드 → 칩·추천 패널·관계도는 프론트 계산
 *  - 새 기록 초안은 localStorage 에 단일 보관 (사용자별 키), 닫아도 유지
 */

const NJ_IMPACT_META = {
    GOOD:    { label: '호재', color: '#2e8b62', bg: '#f0f7f3', dot: '#2e8b62' },
    BAD:     { label: '악재', color: '#c02a22', bg: '#fdf3f2', dot: '#c02a22' },
    NEUTRAL: { label: '중립', color: '#4a5158', bg: '#f2f4f7', dot: '#98a0a7' }
};

const NJ_WWH_META = [
    { key: 'WHAT', field: 'what', hint: '무슨 일인가', color: '#1f4f9e', rows: 3, ph: '사실만 짧게 — 누가 무엇을 발표했나' },
    { key: 'WHY', field: 'why', hint: '왜 일어났나', color: '#8a6a34', rows: 3, ph: '배경과 원인 — 왜 지금인가' },
    { key: 'HOW', field: 'how', hint: '어떻게 흘러갈까', color: '#c02a22', rows: 4, ph: '내 판단과 대응 — 그래서 나는 무엇을 할까' }
];

const NJ_CHO = ['ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'];

function njChosung(s) {
    return String(s).split('').map(ch => {
        const c = ch.charCodeAt(0) - 0xac00;
        return c >= 0 && c <= 11171 ? NJ_CHO[Math.floor(c / 588)] : ch;
    }).join('');
}

function njIsChoQuery(q) {
    return /^[ㄱ-ㅎ]+$/.test(q);
}

function njMatchTag(tag, q) {
    if (!q) return true;
    if (tag.toLowerCase().includes(q)) return true;
    if (njIsChoQuery(q)) return njChosung(tag).includes(q);
    return false;
}

function njHost(url) {
    try { return String(url).replace(/^https?:\/\//, '').split('/')[0]; } catch (e) { return ''; }
}

function njLines(text) {
    return String(text || '').split('\n').map(s => s.trim()).filter(Boolean);
}

function njToday() {
    return new Date().toISOString().slice(0, 10);
}

function njEmptyDraft(defaultCategory) {
    return {
        id: null,
        title: '',
        occurredDate: njToday(),
        impact: 'NEUTRAL',
        category: defaultCategory || '',
        what: '',
        why: '',
        how: '',
        links: [],
        keywords: []
    };
}

function njCopyDraft(d) {
    return {
        ...d,
        links: (d.links || []).map(l => ({ title: l.title || '', url: l.url || '' })),
        keywords: (d.keywords || []).slice()
    };
}

const NewsJournalComponent = {
    newsJournal: {
        loading: false,
        loadingMore: false,
        saving: false,
        error: null,
        recs: [],                 // 현재 필터의 로드분 (list API items — WHAT/WHY/HOW 전문 포함)
        totalCount: 0,            // 현재 필터의 전체 건수
        page: 0,
        size: 100,
        stats: null,              // { totalCount, impactCounts, categories, keywordEvents }

        sel: null,                // 선택 사건 스냅샷 (필터 변경으로 목록에서 빠져도 디테일 유지)
        mode: 'view',             // 'view' | 'edit'
        draft: null,
        dirty: false,
        savedDraft: null,         // 새 기록 초안 (localStorage 미러)
        formError: null,

        query: '',
        impact: '',               // '' = 전체 | GOOD | BAD | NEUTRAL
        categoryId: null,
        showRange: false,
        from: '',
        to: '',
        tagSel: [],

        group: '월별',            // '월별' | '분류별'
        view: 'list',             // 'list' | 'graph'
        pane: 'list',             // 좁은 화면에서 'list' | 'detail'
        vw: typeof window !== 'undefined' ? window.innerWidth : 1560,
        catOpen: false,
        catQuery: '',
        tagPanel: false,
        tagSort: '빈도',          // '빈도'(추천) | '이름'(가나다)
        tagQuery: '',
        hoverTag: null,
        newTag: '',
        newCat: ''
    },

    _njKwCache: null,
    _njGraphCache: null,

    // ---------- 진입 / 로드 ----------
    async njEnter() {
        // idempotency 가드: partial 재 mount 시 x-init 중복 trigger 방지
        if (this.newsJournal.loading) return;
        this.newsJournal.vw = window.innerWidth;
        this.newsJournal.savedDraft = this._njReadSavedDraft();
        await Promise.allSettled([this.njReload(), this.njLoadStats()]);
    },

    _njFilterParams() {
        const st = this.newsJournal;
        return {
            page: st.page,
            size: st.size,
            impact: st.impact || undefined,
            categoryId: st.categoryId || undefined,
            from: st.showRange && st.from ? st.from : undefined,
            to: st.showRange && st.to ? st.to : undefined,
            q: st.query.trim() || undefined,
            keywords: st.tagSel.length ? st.tagSel : undefined
        };
    },

    /** 필터 기준 리셋 로드. 선택 사건은 스냅샷으로 유지하되 재조회분이 있으면 갱신. */
    async njReload() {
        const st = this.newsJournal;
        st.loading = true;
        st.error = null;
        st.page = 0;
        try {
            const res = await API.getNewsEvents(this._njFilterParams());
            st.recs = res?.items || [];
            st.totalCount = res?.totalCount ?? 0;
            if (st.sel) {
                const found = st.recs.find(r => r.id === st.sel.id);
                if (found) st.sel = found;
            }
        } catch (e) {
            st.error = e?.message || '뉴스 기록을 불러오지 못했습니다.';
            st.recs = [];
            st.totalCount = 0;
        } finally {
            st.loading = false;
        }
    },

    async njLoadMore() {
        const st = this.newsJournal;
        if (st.loadingMore || st.recs.length >= st.totalCount) return;
        st.loadingMore = true;
        try {
            st.page += 1;
            const res = await API.getNewsEvents(this._njFilterParams());
            st.recs = st.recs.concat(res?.items || []);
            st.totalCount = res?.totalCount ?? st.totalCount;
        } catch (e) {
            st.page -= 1;
            st.error = e?.message || '추가 기록을 불러오지 못했습니다.';
        } finally {
            st.loadingMore = false;
        }
    },

    async njLoadStats() {
        try {
            this.newsJournal.stats = await API.getNewsJournalStats();
        } catch (e) {
            // 통계 실패는 비치명적 — 뱃지/칩 없이 목록은 동작
            this.newsJournal.stats = null;
        }
    },

    njRemaining() {
        return Math.max(0, this.newsJournal.totalCount - this.newsJournal.recs.length);
    },

    // ---------- 필터 ----------
    njQueryChanged() {
        return this.njReload();
    },

    njClearQuery() {
        this.newsJournal.query = '';
        return this.njReload();
    },

    njSetImpact(key) {
        this.newsJournal.impact = key;
        return this.njReload();
    },

    njSetCategory(id) {
        this.newsJournal.categoryId = id;
        this.newsJournal.catOpen = false;
        this.newsJournal.catQuery = '';
        return this.njReload();
    },

    njToggleRange() {
        const st = this.newsJournal;
        st.showRange = !st.showRange;
        if (st.showRange && !st.from) {
            const d = new Date();
            d.setMonth(d.getMonth() - 3);
            st.from = d.toISOString().slice(0, 10);
            st.to = njToday();
        }
        return this.njReload();
    },

    njToggleTag(tag) {
        const st = this.newsJournal;
        if (st.tagSel.includes(tag)) {
            st.tagSel = st.tagSel.filter(t => t !== tag);
        } else {
            if (st.tagSel.length >= 10) return;   // 서버 keywords 상한(10)과 동기 — 초과 선택 무시
            st.tagSel = [...st.tagSel, tag];
        }
        return this.njReload();
    },

    njClearTags() {
        this.newsJournal.tagSel = [];
        return this.njReload();
    },

    njSelectPair(a, b) {
        const st = this.newsJournal;
        st.tagSel = [a, b];
        st.view = 'list';
        st.pane = 'list';
        return this.njReload();
    },

    njResetFilters() {
        const st = this.newsJournal;
        st.query = '';
        st.impact = '';
        st.categoryId = null;
        st.showRange = false;
        st.tagSel = [];
        return this.njReload();
    },

    // ---------- 화면 상태 헬퍼 ----------
    njNarrow() {
        return this.newsJournal.vw < 1280;
    },

    njShowListPane() {
        const st = this.newsJournal;
        return st.view === 'list' && (!this.njNarrow() || st.pane === 'list');
    },

    njShowDetailPane() {
        const st = this.newsJournal;
        return st.view === 'list' && (!this.njNarrow() || st.pane === 'detail');
    },

    njClosePopovers() {
        const st = this.newsJournal;
        if (st.catOpen || st.tagPanel) {
            st.catOpen = false;
            st.tagPanel = false;
        }
    },

    njEsc() {
        if (this.currentPage !== 'news-journal') return;
        const st = this.newsJournal;
        if (st.catOpen || st.tagPanel) {
            st.catOpen = false;
            st.tagPanel = false;
            return;
        }
        if (st.mode === 'edit') this.njCancelEdit();
    },

    njImpactMeta(impact) {
        return NJ_IMPACT_META[impact] || NJ_IMPACT_META.NEUTRAL;
    },

    njImpactLabel(impact) {
        return this.njImpactMeta(impact).label;
    },

    /** 편집 폼 시장영향 선택지 (전체 제외). */
    njImpactPicks() {
        return ['GOOD', 'BAD', 'NEUTRAL'].map(k => ({ key: k, ...NJ_IMPACT_META[k] }));
    },

    njImpactSegs() {
        const st = this.newsJournal;
        const counts = st.stats?.impactCounts || {};
        const total = st.stats?.totalCount ?? 0;
        return [
            { key: '', name: '전체', dot: '#c9ced6', count: total },
            { key: 'GOOD', name: '호재', dot: NJ_IMPACT_META.GOOD.dot, count: counts.GOOD ?? 0 },
            { key: 'BAD', name: '악재', dot: NJ_IMPACT_META.BAD.dot, count: counts.BAD ?? 0 },
            { key: 'NEUTRAL', name: '중립', dot: NJ_IMPACT_META.NEUTRAL.dot, count: counts.NEUTRAL ?? 0 }
        ];
    },

    njTotalNote() {
        const total = this.newsJournal.stats?.totalCount ?? 0;
        return '기록 ' + total + '건 · 사건을 일자별로 정리하고 회고합니다';
    },

    njResultNote() {
        const st = this.newsJournal;
        let note = st.totalCount + '건';
        if (st.query.trim()) note += ' · ‘' + st.query.trim() + '’';
        if (st.impact) note += ' · ' + this.njImpactLabel(st.impact);
        return note;
    },

    // ---------- 분류 ----------
    njCategories() {
        return this.newsJournal.stats?.categories || [];
    },

    njCatName(id) {
        const found = this.njCategories().find(c => c.id === id);
        return found ? found.name : '분류';
    },

    njCatLabel() {
        const id = this.newsJournal.categoryId;
        return id == null ? '분류 전체' : this.njCatName(id);
    },

    njCatBadge() {
        const st = this.newsJournal;
        if (st.categoryId == null) return st.stats?.totalCount ?? 0;
        const found = this.njCategories().find(c => c.id === st.categoryId);
        return found ? found.count : 0;
    },

    njCatOptions() {
        const st = this.newsJournal;
        const q = st.catQuery.trim();
        const options = [{ id: null, name: '전체', count: st.stats?.totalCount ?? 0 }, ...this.njCategories()];
        return options.filter(c => !q || c.name.includes(q));
    },

    njCatNote() {
        return this.njCategories().length + '개 분류 · 기록을 저장하면 새 분류가 자동으로 추가됩니다';
    },

    njRangeLabel() {
        const st = this.newsJournal;
        return st.showRange ? '기간 ' + st.from.slice(5) + '~' + st.to.slice(5) : '기간 지정';
    },

    // ---------- 키워드 통계 (stats 기반, 목업 산식) ----------
    _njKwBase() {
        const stats = this.newsJournal.stats;
        if (!stats) return { freq: {}, pairs: {}, allTags: [], recent: [], events: [] };
        if (this._njKwCache && this._njKwCache.src === stats) return this._njKwCache;
        const freq = {}, pairs = {}, recent = [];
        const events = stats.keywordEvents || [];
        for (const ev of events) {
            const tags = ev.keywords || [];
            for (const t of tags) {
                freq[t] = (freq[t] || 0) + 1;
                if (!recent.includes(t)) recent.push(t);   // events 가 발생일 내림차순
            }
            for (let i = 0; i < tags.length; i++) {
                for (let j = i + 1; j < tags.length; j++) {
                    const key = [tags[i], tags[j]].sort().join('\u0001');
                    pairs[key] = (pairs[key] || 0) + 1;
                }
            }
        }
        const allTags = Object.keys(freq).sort((a, b) => freq[b] - freq[a] || a.localeCompare(b));
        this._njKwCache = { src: stats, freq, pairs, allTags, recent, events };
        return this._njKwCache;
    },

    njTagCount() {
        return this._njKwBase().allTags.length;
    },

    njTagFreq(tag) {
        return this._njKwBase().freq[tag] || 0;
    },

    _njMatchedTags() {
        const q = this.newsJournal.tagQuery.trim().toLowerCase();
        return this._njKwBase().allTags.filter(t => njMatchTag(t, q));
    },

    _njPinnedTags() {
        return Array.from(new Set([...this.newsJournal.tagSel, ...this._njMatchedTags()]));
    },

    njTagChips() {
        return this._njPinnedTags().slice(0, this._njTopN());
    },

    _njTopN() {
        const vw = this.newsJournal.vw;
        return vw < 1400 ? 4 : (vw < 1660 ? 6 : 8);
    },

    njHiddenTagCount() {
        return Math.max(0, this._njPinnedTags().length - this._njTopN());
    },

    njMoreLabel() {
        const hidden = this.njHiddenTagCount();
        return hidden > 0 ? '+' + hidden + '개 더' : '키워드 전체';
    },

    njTagInputW() {
        return this.newsJournal.vw < 1400 ? '108px' : '188px';
    },

    njPanelW() {
        return this.newsJournal.vw < 1100 ? '300px' : '392px';
    },

    /** 선택한 키워드와 함께 등장한 키워드 (다중 선택은 AND — 전부 포함 사건 기준). */
    _njRelatedTags() {
        const sel = this.newsJournal.tagSel;
        if (!sel.length) return [];
        const score = {};
        for (const ev of this._njKwBase().events) {
            const tags = ev.keywords || [];
            if (!sel.every(t => tags.includes(t))) continue;
            for (const t of tags) {
                if (!sel.includes(t)) score[t] = (score[t] || 0) + 1;
            }
        }
        return Object.keys(score).sort((a, b) => score[b] - score[a]);
    },

    njPanelSections() {
        const st = this.newsJournal;
        const base = this._njKwBase();
        const q = st.tagQuery.trim().toLowerCase();
        const src = q
            ? [{ label: '‘' + st.tagQuery.trim() + '’ 검색 결과', hint: njIsChoQuery(q) ? '초성 검색' : '', tags: this._njMatchedTags() }]
            : [
                { label: '선택한 것과 함께 쓰인', hint: '고를수록 좁혀집니다', tags: this._njRelatedTags().slice(0, 12) },
                { label: '자주 쓰는', hint: '기록 수 상위', tags: base.allTags.slice(0, 12) },
                { label: '최근 기록에 쓴', hint: '', tags: base.recent.slice(0, 12) }
            ];
        return src.filter(s => s.tags.length);
    },

    njAlphaGroups() {
        const q = this.newsJournal.tagQuery.trim().toLowerCase();
        const src = q ? this._njMatchedTags() : this._njKwBase().allTags;
        const buckets = {};
        for (const t of src) {
            const c = t.charCodeAt(0) - 0xac00;
            const key = c >= 0 && c <= 11171 ? NJ_CHO[Math.floor(c / 588)] : (/[a-zA-Z]/.test(t[0]) ? 'A–Z' : '#');
            (buckets[key] = buckets[key] || []).push(t);
        }
        return Object.keys(buckets).sort((a, b) => {
            const ia = NJ_CHO.indexOf(a), ib = NJ_CHO.indexOf(b);
            if (ia >= 0 && ib >= 0) return ia - ib;
            if (ia >= 0) return -1;
            if (ib >= 0) return 1;
            return a.localeCompare(b);
        }).map(k => ({ key: k, tags: buckets[k] }));
    },

    njPanelNote() {
        const st = this.newsJournal;
        const q = st.tagQuery.trim().toLowerCase();
        if (q) return this._njMatchedTags().length + '개 일치' + (njIsChoQuery(q) ? ' · 초성' : '');
        return '키워드 ' + this.njTagCount() + '개';
    },

    njNoPanelResult() {
        return this.njPanelSections().length === 0 && this.njAlphaGroups().length === 0;
    },

    njTagSelNote() {
        const sel = this.newsJournal.tagSel;
        return sel.length > 1 ? sel.join(' + ') + ' 모두 포함' : (sel[0] || '') + ' 포함';
    },

    // ---------- 키워드 관계도 ----------
    _njGraphLayout() {
        const stats = this.newsJournal.stats;
        const base = this._njKwBase();
        if (this._njGraphCache && this._njGraphCache.src === stats) return this._njGraphCache;
        const W = 900, H = 470, CX = W / 2, CY = H / 2;
        const nodes = base.allTags.slice(0, 22);
        const deg = {};
        nodes.forEach(t => deg[t] = 0);
        const edges = [];
        for (const key of Object.keys(base.pairs)) {
            const [a, b] = key.split('\u0001');
            if (nodes.includes(a) && nodes.includes(b)) {
                edges.push({ a, b, w: base.pairs[key] });
                deg[a]++;
                deg[b]++;
            }
        }
        const ordered = nodes.slice().sort((a, b) => (deg[b] - deg[a]) || (base.freq[b] - base.freq[a]));
        const inner = ordered.slice(0, 6), outer = ordered.slice(6);
        const pos = {};
        inner.forEach((t, i) => {
            const ang = (i / Math.max(1, inner.length)) * Math.PI * 2 - Math.PI / 2;
            pos[t] = { x: CX + Math.cos(ang) * 108, y: CY + Math.sin(ang) * 88 };
        });
        outer.forEach((t, i) => {
            const ang = (i / Math.max(1, outer.length)) * Math.PI * 2 - Math.PI / 2 + 0.22;
            pos[t] = { x: CX + Math.cos(ang) * 258, y: CY + Math.sin(ang) * 186 };
        });
        const maxW = Math.max(1, ...edges.map(x => x.w));
        this._njGraphCache = { src: stats, W, H, CX, CY, nodes, edges, pos, maxW };
        return this._njGraphCache;
    },

    _njActiveTags() {
        const st = this.newsJournal;
        return st.tagSel.length ? st.tagSel : (st.hoverTag ? [st.hoverTag] : []);
    },

    _njTagLit(tag) {
        const active = this._njActiveTags();
        if (active.length === 0 || active.includes(tag)) return true;
        return this._njGraphLayout().edges.some(x =>
            (x.a === tag && active.includes(x.b)) || (x.b === tag && active.includes(x.a)));
    },

    /** 엣지 SVG 문자열 — 좌표·굵기 숫자만 조립하므로 사용자 입력이 마크업에 닿지 않는다. */
    njGraphEdges() {
        const g = this._njGraphLayout();
        const active = this._njActiveTags();
        return g.edges.map(x => {
            const p = g.pos[x.a], q = g.pos[x.b];
            const hot = active.length > 0 && (active.includes(x.a) || active.includes(x.b));
            const dim = active.length > 0 && !hot;
            const mx = (p.x + q.x) / 2, my = (p.y + q.y) / 2;
            const cx = mx + (my - g.CY) * 0.1, cy = my + (mx - g.CX) * 0.1;
            const sw = (0.8 + (x.w / g.maxW) * 2.4).toFixed(2);
            const opacity = dim ? '.12' : (hot ? '.85' : '.38');
            return '<path d="M' + p.x.toFixed(1) + ' ' + p.y.toFixed(1)
                + ' Q' + cx.toFixed(1) + ' ' + cy.toFixed(1)
                + ' ' + q.x.toFixed(1) + ' ' + q.y.toFixed(1) + '"'
                + ' fill="none" stroke="' + (hot ? '#1f4f9e' : '#b6bbc2') + '"'
                + ' stroke-width="' + sw + '" opacity="' + opacity + '"></path>';
        }).join('');
    },

    njGraphNodes() {
        const g = this._njGraphLayout();
        const base = this._njKwBase();
        const sel = this.newsJournal.tagSel;
        return g.nodes.map(t => {
            const p = g.pos[t];
            const r = 13 + Math.min(16, (base.freq[t] || 0) * 4);
            return {
                tag: t,
                count: base.freq[t] || 0,
                x: p.x, y: p.y, r,
                on: sel.includes(t),
                lit: this._njTagLit(t)
            };
        });
    },

    njGraphNote() {
        const sel = this.newsJournal.tagSel;
        return sel.length
            ? sel.join(' · ') + ' 선택 — 연결된 키워드만 밝게 표시됩니다'
            : '원 크기 = 기록 수, 선 굵기 = 함께 등장한 횟수 · 클릭하면 목록이 그 키워드로 좁혀집니다';
    },

    njCoTop() {
        const base = this._njKwBase();
        return Object.keys(base.pairs).map(key => {
            const [a, b] = key.split('\u0001');
            return { a, b, w: base.pairs[key] };
        }).sort((x, y) => y.w - x.w).slice(0, 6);
    },

    // ---------- 목록 ----------
    njGroups() {
        const st = this.newsJournal;
        const groups = [];
        const indexByKey = {};
        for (const r of st.recs) {
            const key = st.group === '월별' ? r.occurredDate.slice(0, 7) : (r.category?.name || '미분류');
            if (indexByKey[key] === undefined) {
                indexByKey[key] = groups.length;
                groups.push({
                    key,
                    label: st.group === '월별' ? key.replace('-', '년 ') + '월' : key,
                    rows: []
                });
            }
            groups[indexByKey[key]].rows.push(r);
        }
        return groups;
    },

    njRowDate(r) {
        return r.occurredDate.slice(5).replace('-', '.');
    },

    njRowPreview(r) {
        return (njLines(r.what)[0] || njLines(r.why)[0] || '내용 없음').slice(0, 92);
    },

    njRowTags(r) {
        return (r.keywords || []).slice(0, 3);
    },

    njSelectRow(r) {
        const st = this.newsJournal;
        st.sel = r;
        st.mode = 'view';
        st.pane = 'detail';
        st.formError = null;
    },

    // ---------- 디테일 (보기) ----------
    njDetailSections() {
        const sel = this.newsJournal.sel;
        if (!sel) return [];
        return NJ_WWH_META.map(m => ({
            key: m.key,
            hint: m.hint,
            color: m.color,
            lines: njLines(sel[m.field])
        }));
    },

    njDetailDate() {
        const sel = this.newsJournal.sel;
        return sel ? sel.occurredDate.replace(/-/g, '.') : '';
    },

    njDetailLinks() {
        const sel = this.newsJournal.sel;
        return (sel?.links || []).map(l => ({ title: l.title || l.url, url: l.url, host: njHost(l.url) }));
    },

    /** 같은 분류의 다른 기록 — 로드분에서 계산 (최대 3건). */
    njRelated() {
        const st = this.newsJournal;
        const sel = st.sel;
        if (!sel || !sel.category) return [];
        return st.recs
            .filter(r => r.category?.name === sel.category.name && r.id !== sel.id)
            .slice(0, 3);
    },

    // ---------- 초안 (localStorage) ----------
    _njDraftKey() {
        return 'newsJournalDraft:' + (this.auth?.userId || 'anon');
    },

    _njReadSavedDraft() {
        try {
            const raw = localStorage.getItem(this._njDraftKey());
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    },

    _njWriteSavedDraft(draft) {
        try {
            if (draft) localStorage.setItem(this._njDraftKey(), JSON.stringify(draft));
            else localStorage.removeItem(this._njDraftKey());
        } catch (e) {
            // storage 불가 환경 — 화면 상태로만 유지
        }
    },

    /** 새 기록(무 id) 이면서 내용이 남아 있으면 초안 대상. */
    njHasContent(d) {
        return !!d && !d.id && !!(
            String(d.title || '').trim() || String(d.what || '').trim()
            || String(d.why || '').trim() || String(d.how || '').trim()
            || (d.links || []).length || (d.keywords || []).length
        );
    },

    njHasSavedDraft() {
        return this.njHasContent(this.newsJournal.savedDraft) && this.newsJournal.mode !== 'edit';
    },

    njDraftPreview() {
        const d = this.newsJournal.savedDraft;
        if (!d) return '';
        return (String(d.title || '').trim() || String(d.what || '').trim()).slice(0, 22);
    },

    // ---------- 편집 ----------
    njNewRecord() {
        const st = this.newsJournal;
        st.draft = njEmptyDraft(this.njCategories()[0]?.name || '');
        st.mode = 'edit';
        st.pane = 'detail';
        st.view = 'list';
        st.dirty = false;
        st.formError = null;
        st.newTag = '';
        st.newCat = '';
    },

    njResumeDraft() {
        const st = this.newsJournal;
        if (!st.savedDraft) return;
        st.draft = njCopyDraft(st.savedDraft);
        st.mode = 'edit';
        st.pane = 'detail';
        st.view = 'list';
        st.dirty = false;
        st.formError = null;
    },

    njEditCurrent() {
        const st = this.newsJournal;
        const sel = st.sel;
        if (!sel) return;
        st.draft = {
            id: sel.id,
            title: sel.title || '',
            occurredDate: sel.occurredDate || njToday(),
            impact: sel.impact || 'NEUTRAL',
            category: sel.category?.name || '',
            what: sel.what || '',
            why: sel.why || '',
            how: sel.how || '',
            links: (sel.links || []).map(l => ({ title: l.title || '', url: l.url || '' })),
            keywords: (sel.keywords || []).slice()
        };
        st.mode = 'edit';
        st.pane = 'detail';
        st.dirty = false;
        st.formError = null;
        st.newTag = '';
        st.newCat = '';
    },

    /** 닫기 — 새 기록이면 초안으로 보관 (목업 stash 동작), 기존 기록 수정이면 폐기. */
    njCancelEdit() {
        const st = this.newsJournal;
        if (this.njHasContent(st.draft)) {
            st.savedDraft = njCopyDraft(st.draft);
            this._njWriteSavedDraft(st.savedDraft);
        }
        st.mode = 'view';
        st.draft = null;
        st.dirty = false;
        st.formError = null;
    },

    njDraftNote() {
        const st = this.newsJournal;
        if (!st.dirty) return '변경 없음';
        return st.draft && st.draft.id
            ? '닫으면 변경 내용은 저장되지 않습니다'
            : '작성 내용은 자동 보관됩니다 — 닫아도 사라지지 않습니다';
    },

    njPatchDraft(field, value) {
        if (!this.newsJournal.draft) return;
        this.newsJournal.draft[field] = value;
        this.newsJournal.dirty = true;
    },

    njWwhMeta() {
        return NJ_WWH_META;
    },

    njWwhLen(field) {
        const d = this.newsJournal.draft;
        return d && d[field] ? String(d[field]).length + '자' : '';
    },

    njCatPicks() {
        const names = this.njCategories().map(c => c.name);
        const current = this.newsJournal.draft?.category;
        if (current && !names.includes(current)) names.push(current);
        return names;
    },

    njPickCategory(name) {
        this.njPatchDraft('category', name);
    },

    njAddNewCat() {
        const v = this.newsJournal.newCat.trim();
        if (!v) return;
        if (v.length > 50) {
            this.newsJournal.formError = '주제 분류는 50자 이하여야 합니다.';
            return;
        }
        this.njPatchDraft('category', v);
        this.newsJournal.newCat = '';
    },

    njAddLink() {
        const d = this.newsJournal.draft;
        if (!d) return;
        if (d.links.length >= 20) {
            this.newsJournal.formError = '링크는 최대 20개까지 추가할 수 있습니다.';
            return;
        }
        d.links.push({ title: '', url: '' });
        this.newsJournal.dirty = true;
    },

    njRemoveLink(index) {
        const d = this.newsJournal.draft;
        if (!d) return;
        d.links.splice(index, 1);
        this.newsJournal.dirty = true;
    },

    njLinkUrlInvalid(link) {
        return !!link.url && !/^https?:\/\//.test(link.url.trim());
    },

    njAddTag() {
        const st = this.newsJournal;
        const d = st.draft;
        if (!d) return;
        const raw = (st.newTag || '').trim().replace(/^#+/, '').trim();
        if (!raw) return;
        if (raw.length > 50) {
            st.formError = '키워드는 50자 이하여야 합니다.';
            return;
        }
        if (d.keywords.length >= 20) {
            st.formError = '키워드는 최대 20개까지 추가할 수 있습니다.';
            return;
        }
        if (!d.keywords.includes(raw)) d.keywords.push(raw);
        st.newTag = '';
        st.dirty = true;
    },

    njRemoveTag(tag) {
        const d = this.newsJournal.draft;
        if (!d) return;
        d.keywords = d.keywords.filter(t => t !== tag);
        this.newsJournal.dirty = true;
    },

    async njSave() {
        const st = this.newsJournal;
        const d = st.draft;
        if (!d) return;
        const today = njToday();

        if (!d.title || !d.title.trim()) { st.formError = '제목을 입력하세요.'; return; }
        if (d.title.trim().length > 200) { st.formError = '제목은 200자 이하여야 합니다.'; return; }
        if (!d.occurredDate) { st.formError = '발생 일자를 입력하세요.'; return; }
        if (d.occurredDate > today) { st.formError = '미래 날짜로 기록할 수 없습니다.'; return; }
        if (!d.impact) { st.formError = '시장영향을 선택하세요.'; return; }

        let category = (d.category || '').trim();
        if (!category && st.newCat.trim()) {
            category = st.newCat.trim();
            st.newCat = '';
        }
        if (!category) { st.formError = '주제 분류를 선택하거나 입력하세요.'; return; }
        if (category.length > 50) { st.formError = '주제 분류는 50자 이하여야 합니다.'; return; }

        for (const meta of NJ_WWH_META) {
            if ((d[meta.field] || '').length > 4000) {
                st.formError = meta.key + ' 는 4000자 이하여야 합니다.';
                return;
            }
        }

        // 링크: 제목은 선택 — 빈 제목은 URL 로 대체(백엔드 NotBlank). 양쪽 빈 행은 제거.
        const cleanLinks = [];
        for (const l of d.links) {
            const title = (l.title || '').trim();
            const url = (l.url || '').trim();
            if (!title && !url) continue;
            if (!url) { st.formError = '링크 URL을 입력하세요.'; return; }
            if (!/^https?:\/\//.test(url)) { st.formError = '링크 URL은 http(s):// 로 시작해야 합니다.'; return; }
            if (url.length > 2000) { st.formError = '링크 URL은 2000자 이하여야 합니다.'; return; }
            const finalTitle = title || url;
            if (finalTitle.length > 200) { st.formError = '링크 제목은 200자 이하여야 합니다.'; return; }
            cleanLinks.push({ title: finalTitle, url });
        }

        // 입력 중 키워드가 남아 있으면 먼저 반영 (Enter 없이 저장한 경우)
        if ((st.newTag || '').trim()) {
            st.formError = null;
            this.njAddTag();
            if (st.formError) return;
        }
        const keywords = (d.keywords || []).map(k => (k || '').trim()).filter(Boolean);
        if (keywords.length > 20) { st.formError = '키워드는 최대 20개까지 추가할 수 있습니다.'; return; }
        for (const k of keywords) {
            if (k.length > 50) { st.formError = '키워드는 50자 이하여야 합니다.'; return; }
        }

        const body = {
            title: d.title.trim(),
            occurredDate: d.occurredDate,
            impact: d.impact,
            category,
            what: d.what || null,
            why: d.why || null,
            how: d.how || null,
            links: cleanLinks,
            keywords
        };

        const wasNew = !d.id;
        st.saving = true;
        st.formError = null;
        try {
            let savedId = d.id;
            if (wasNew) {
                const res = await API.createNewsEvent(body);
                savedId = res?.id ?? null;
            } else {
                await API.updateNewsEvent(d.id, body);
            }
            st.mode = 'view';
            st.draft = null;
            st.dirty = false;
            if (wasNew) {
                st.savedDraft = null;
                this._njWriteSavedDraft(null);
            }
            await Promise.allSettled([this.njReload(), this.njLoadStats()]);
            if (savedId != null) {
                const found = st.recs.find(r => r.id === savedId);
                if (found) {
                    st.sel = found;
                } else {
                    // 현재 필터에 걸러진 경우 — 상세만 단건 조회해 유지
                    try { st.sel = await API.getNewsEvent(savedId); } catch (e) { /* 목록 밖 유지 실패는 무시 */ }
                }
                st.pane = 'detail';
            }
        } catch (e) {
            st.formError = e?.message || '저장에 실패했습니다.';
            st.mode = 'edit';
        } finally {
            st.saving = false;
        }
    },

    async njDeleteCurrent() {
        const st = this.newsJournal;
        const sel = st.sel;
        if (!sel) return;
        if (!confirm('이 사건을 삭제할까요? 되돌릴 수 없습니다.')) return;
        st.saving = true;
        try {
            await API.deleteNewsEvent(sel.id);
            st.sel = null;
            st.mode = 'view';
            st.pane = 'list';
            await Promise.allSettled([this.njReload(), this.njLoadStats()]);
        } catch (e) {
            alert(e?.message || '삭제에 실패했습니다.');
        } finally {
            st.saving = false;
        }
    }
};
