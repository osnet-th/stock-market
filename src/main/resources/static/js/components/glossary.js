/**
 * GlossaryComponent — 개인 용어 사전 (이슈 #43 → 2026-08-21 마스터-디테일 리디자인).
 *
 * 소유 프로퍼티: glossary
 * 외부 의존: API (api.js)
 *
 * 범위:
 *  - 마스터-디테일 2-pane (목록 + 디테일), 인라인 보기/편집 (모달 없음)
 *  - 구조화 콘텐츠: 약어 · 한 줄 정의 · 풀이(definition) · 기준·읽는 법 · 예시 · 투자 관점
 *  - 통합 검색(전체 텍스트 필드) + 한글 초성 검색, 가나다(초성)/최신(월별) 그룹
 *  - 카테고리 칩 필터(건수) + 관리 팝오버(이름 변경 · R6 삭제 영향 미리보기)
 *  - 함께 볼 용어 (relatedTermIds — 용어 간 상호 링크)
 *  - 새 용어 초안 localStorage 자동 보관 · 이어쓰기 (수정 편집은 보관하지 않음 — 목업 동일)
 *  - 전량 로드(200건/페이지 루프) 후 검색/그룹/건수 클라이언트 사이드 (개인 사전 규모 전제)
 *
 * XSS 정책: 모든 사용자 입력 렌더링은 x-text/textContent 만. x-html/innerHTML 금지.
 * Alpine :style 은 항상 객체 바인딩 (문자열 바인딩은 정적 style 을 지우는 실버그 — #117 참조).
 */

/** 구조화 섹션 정의 — 순서/색/힌트/placeholder 는 목업 실측값 */
const GL_FIELDS = [
    {
        key: 'definition', label: '풀이', hint: '무엇을 어떻게 측정하는가 · 구성 항목',
        color: '#1f4f9e', bg: '#fbfcfd', rows: 5,
        ph: '가계가 실제로 소비하는 품목들의 가격 변화를 조사\n- 식료품\n- 주거비\n- 교통'
    },
    {
        key: 'scaleNote', label: '기준 · 읽는 법', hint: '숫자를 어떻게 해석하는가',
        color: '#2e6b52', bg: '#f4f8f6', rows: 4,
        ph: '기준은 100 — 특정 기준연도를 100으로 놓고 계산\n- 105 → 기준연도보다 물가가 5% 높음'
    },
    {
        key: 'example', label: '예시', hint: '실제 숫자로 한 번 계산해 보기',
        color: '#8a6a34', bg: '#fbf8f3', rows: 4,
        ph: '작년 6월 CPI 110, 올해 6월 113.3 이면\n(113.3 − 110) ÷ 110 × 100 = 3% → 1년 사이 3% 올랐다는 뜻'
    },
    {
        key: 'takeaway', label: '투자 관점', hint: '이 지표가 시장에 주는 신호 · 내 판단',
        color: '#c02a22', bg: '#fdf6f5', rows: 4,
        ph: '- 예상보다 높음 → 금리 상승 예상, 주식 부담\n- 예상보다 낮음 → 금리 인하 기대, 주식 긍정\n값 자체보다 예상치와의 차이가 중요'
    }
];

const GL_DRAFT_KEY = 'glossaryDraft.v1';
const GL_RELATED_MAX = 20;

const GL_CHO = ['ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'];

function _glEmptyForm() {
    return {
        id: null,
        name: '',
        abbreviation: '',
        oneLine: '',
        definition: '',
        scaleNote: '',
        example: '',
        takeaway: '',
        categoryId: null,
        categoryName: '',
        relatedTermIds: []
    };
}

const GlossaryComponent = {
    glossary: {
        loading: false,
        error: null,

        categories: [],                              // [{ id, name }]
        terms: [],                                   // 서버 DTO 전량 (전 필드 포함)

        selId: null,                                 // 선택 용어 id
        mode: 'view',                                // 'view' | 'edit'
        pane: 'list',                                // 좁은 화면 단일 pane: 'list' | 'detail'
        vw: typeof window !== 'undefined' ? window.innerWidth : 1560,

        query: '',
        cat: 'all',                                  // 'all' | 'none'(미분류) | <categoryId>
        sort: 'ALPHA',                               // 'ALPHA'(가나다) | 'RECENT'(최신)

        form: _glEmptyForm(),                        // 편집 버퍼 (id null = 새 용어)
        dirty: false,
        saving: false,
        formError: null,
        newCat: '',                                  // 편집 폼 인라인 새 카테고리 입력
        savedDraft: null,                            // localStorage 초안 미러

        manageOpen: false,                           // 카테고리 관리 팝오버
        manageNewName: '',
        manageError: null,
        catEditId: null,
        catEditName: '',
        catError: null,
        deleteImpact: null                           // { categoryId, name, count } | null
    },

    // ---------- 진입/로드 ----------
    async glossaryLoad() {
        if (this.glossary.loading) return;
        this.glossary.loading = true;
        this.glossary.error = null;
        try {
            const [cats, terms] = await Promise.all([
                API.getGlossaryCategories().catch(() => []),
                this._glFetchAllTerms()
            ]);
            this.glossary.categories = Array.isArray(cats) ? cats : [];
            this.glossary.terms = terms;
            this._glEnsureSelection();
        } catch (e) {
            this.glossary.error = e?.message || '용어 사전을 불러오지 못했습니다.';
            this.glossary.terms = [];
        } finally {
            this.glossary.loading = false;
        }
        this.glossary.vw = window.innerWidth;
        this.glossary.savedDraft = this._glReadDraft();
    },

    /** 전량 로드 — 서버 페이지(최대 200건)를 총 건수까지 루프 */
    async _glFetchAllTerms() {
        const size = 200;
        const all = [];
        for (let page = 0; page <= 50; page++) {
            const res = await API.getGlossaryTerms({ page, size, sort: 'REGISTERED_DESC' });
            const items = res?.items || [];
            all.push(...items);
            const total = res?.totalCount ?? all.length;
            if (items.length === 0 || all.length >= total) break;
        }
        return all;
    },

    _glEnsureSelection() {
        const g = this.glossary;
        if (g.selId != null && !g.terms.some(t => t.id === g.selId)) g.selId = null;
        if (g.selId == null && !this.glIsNarrow() && g.terms.length > 0) {
            const rows = this.glRows();
            g.selId = rows.length > 0 ? rows[0].id : null;
        }
    },

    // ---------- 초성/그룹 유틸 ----------
    _glChosung(s) {
        return String(s || '').split('').map(ch => {
            const c = ch.charCodeAt(0) - 0xac00;
            return c >= 0 && c <= 11171 ? GL_CHO[Math.floor(c / 588)] : ch;
        }).join('');
    },

    _glIsCho(q) {
        return /^[ㄱ-ㅎ]+$/.test(q);
    },

    _glInitial(s) {
        const str = String(s || '');
        const c = str.charCodeAt(0) - 0xac00;
        if (c >= 0 && c <= 11171) return GL_CHO[Math.floor(c / 588)];
        return /[a-zA-Z]/.test(str[0] || '') ? 'A–Z' : '#';
    },

    // ---------- 목록 (필터/정렬/그룹) ----------
    glFieldDefs() {
        return GL_FIELDS;
    },

    glFilledCount(t) {
        return GL_FIELDS.filter(f => String(t?.[f.key] || '').trim()).length;
    },

    _glInCat(t) {
        const cat = this.glossary.cat;
        if (cat === 'all') return true;
        if (cat === 'none') return t.categoryId == null;
        return t.categoryId === cat;
    },

    _glMatch(t, q) {
        if (!q) return true;
        const hay = [t.name, t.abbreviation, t.oneLine, t.definition, t.scaleNote, t.example, t.takeaway]
            .map(v => v || '').join(' ').toLowerCase();
        if (hay.includes(q)) return true;
        return this._glIsCho(q) && this._glChosung(t.name).includes(q);
    },

    glRows() {
        const q = this.glossary.query.trim().toLowerCase();
        const rows = this.glossary.terms.filter(t => this._glInCat(t) && this._glMatch(t, q));
        const bySort = this.glossary.sort === 'ALPHA'
            ? (a, b) => a.name.localeCompare(b.name, 'ko')
            : (a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || ''))
                || a.name.localeCompare(b.name, 'ko');
        return rows.slice().sort(bySort);
    },

    glGroups() {
        const alpha = this.glossary.sort === 'ALPHA';
        const keyOf = alpha ? t => this._glInitial(t.name) : t => String(t.createdAt || '').slice(0, 7);
        const labelOf = alpha ? k => k : k => k.replace('-', '년 ') + '월';
        const keys = [];
        const map = {};
        this.glRows().forEach(t => {
            const k = keyOf(t);
            if (!map[k]) { map[k] = []; keys.push(k); }
            map[k].push(t);
        });
        return keys.map(k => ({ key: k, label: labelOf(k), count: map[k].length + '개', rows: map[k] }));
    },

    glCats() {
        const counts = {};
        let none = 0;
        this.glossary.terms.forEach(t => {
            if (t.categoryId == null) none++;
            else counts[t.categoryId] = (counts[t.categoryId] || 0) + 1;
        });
        const chips = [{ key: 'all', name: '전체', count: this.glossary.terms.length }];
        if (none > 0) chips.push({ key: 'none', name: '미분류', count: none });
        this.glossary.categories.forEach(c => chips.push({ key: c.id, name: c.name, count: counts[c.id] || 0 }));
        return chips;
    },

    glSetCat(key) {
        this.glossary.cat = key;
    },

    glSetSort(sort) {
        this.glossary.sort = sort;
    },

    glCatName(categoryId) {
        if (categoryId == null) return '미분류';
        const cat = this.glossary.categories.find(c => c.id === categoryId);
        return cat ? cat.name : '미분류';
    },

    glCountNote() {
        const total = this.glossary.terms.length;
        const thin = this.glossary.terms.filter(t => this.glFilledCount(t) < GL_FIELDS.length).length;
        return '등록 ' + total + '개 · 채움 필요 ' + thin + '개';
    },

    glResultNote() {
        const q = this.glossary.query.trim();
        let note = this.glRows().length + '개';
        if (q) note += ' · ‘' + q + '’' + (this._glIsCho(q.toLowerCase()) ? ' 초성' : '');
        return note;
    },

    glRowDate(t) {
        return String(t.createdAt || '').slice(2, 10).replace(/-/g, '.');
    },

    // ---------- pane/선택 ----------
    glIsNarrow() {
        // 목업 1080px + 앱 사이드바(224px) 보정 (#117 실측 선례)
        return this.glossary.vw < 1280;
    },

    glShowList() {
        return !this.glIsNarrow() || this.glossary.pane === 'list';
    },

    glShowDetail() {
        return !this.glIsNarrow() || this.glossary.pane === 'detail';
    },

    glListPaneStyle() {
        if (this.glIsNarrow()) return { flex: '1 1 100%', maxWidth: '100%' };
        return { flex: '1 1 360px', maxWidth: '400px' };
    },

    glDetailPaneStyle() {
        if (this.glIsNarrow()) return { flex: '1 1 100%' };
        return { flex: '2 1 560px' };
    },

    glSearchW() {
        return this.glIsNarrow() ? '240px' : '400px';
    },

    glCurrent() {
        return this.glossary.terms.find(t => t.id === this.glossary.selId) || null;
    },

    glSelect(id) {
        if (this.glossary.mode === 'edit') this._glStash();
        this.glossary.selId = id;
        this.glossary.mode = 'view';
        this.glossary.pane = 'detail';
    },

    glBackToList() {
        this.glossary.pane = 'list';
    },

    // ---------- 디테일 (보기) ----------
    _glBlocks(text) {
        return String(text || '').split('\n')
            .map(s => s.replace(/\s+$/, ''))
            .filter(s => s.trim())
            .map(l => {
                const sub = /^\s*[-•]/.test(l);
                return {
                    text: l.replace(/^\s*[-•]\s?/, ''),
                    indent: sub ? '14px' : '0px',
                    dot: sub ? '3px' : '4px',
                    size: sub ? '12px' : '12.5px'
                };
            });
    },

    glDetailSections() {
        const cur = this.glCurrent();
        if (!cur) return [];
        return GL_FIELDS
            .filter(f => String(cur[f.key] || '').trim())
            .map(f => ({
                key: f.key, label: f.label, hint: f.hint, color: f.color, bg: f.bg,
                blocks: this._glBlocks(cur[f.key])
            }));
    },

    glJumpChips() {
        const cur = this.glCurrent();
        if (!cur) return [];
        return GL_FIELDS.map(f => {
            const has = !!String(cur[f.key] || '').trim();
            return {
                key: f.key,
                label: has ? f.label : f.label + ' 없음',
                style: has
                    ? { border: '1px solid rgba(0,0,0,.1)', background: f.bg, color: f.color }
                    : { border: '1px solid rgba(0,0,0,.07)', background: '#f7f8fa', color: '#b2b8bd' }
            };
        });
    },

    glMissingLabels() {
        const cur = this.glCurrent();
        if (!cur) return [];
        return GL_FIELDS.filter(f => !String(cur[f.key] || '').trim()).map(f => f.label);
    },

    glMissingNote() {
        return this.glMissingLabels().join(' · ')
            + ' 항목이 비어 있습니다 — 나중에 다시 읽을 때 가장 아쉬운 부분입니다';
    },

    glDetailDate() {
        const cur = this.glCurrent();
        return cur ? String(cur.createdAt || '').slice(0, 10).replace(/-/g, '.') : '';
    },

    glRelatedObjs() {
        const cur = this.glCurrent();
        if (!cur) return [];
        return (cur.relatedTermIds || [])
            .map(id => this.glossary.terms.find(t => t.id === id))
            .filter(Boolean);
    },

    // ---------- 편집 진입/이탈 ----------
    glNewTerm() {
        this._glOpenEdit(_glEmptyForm());
    },

    glNewFromQuery() {
        const form = _glEmptyForm();
        form.name = this.glossary.query.trim();
        this._glOpenEdit(form);
    },

    glNewFromQueryLabel() {
        const q = this.glossary.query.trim();
        return q ? '‘' + q + '’ 용어로 등록' : '+ 용어 등록';
    },

    glEditCurrent() {
        const cur = this.glCurrent();
        if (!cur) return;
        this._glOpenEdit({
            id: cur.id,
            name: cur.name || '',
            abbreviation: cur.abbreviation || '',
            oneLine: cur.oneLine || '',
            definition: cur.definition || '',
            scaleNote: cur.scaleNote || '',
            example: cur.example || '',
            takeaway: cur.takeaway || '',
            categoryId: cur.categoryId ?? null,
            categoryName: '',
            relatedTermIds: (cur.relatedTermIds || []).slice()
        });
    },

    glResumeDraft() {
        const draft = this.glossary.savedDraft;
        if (!draft) return;
        this._glOpenEdit({ ..._glEmptyForm(), ...draft, id: null, relatedTermIds: (draft.relatedTermIds || []).slice() });
    },

    _glOpenEdit(form) {
        if (this.glossary.mode === 'edit') this._glStash();
        this.glossary.form = form;
        this.glossary.mode = 'edit';
        this.glossary.pane = 'detail';
        this.glossary.dirty = false;
        this.glossary.formError = null;
        this.glossary.newCat = '';
    },

    /** 닫기/Escape — 새 용어 초안은 자동 보관, 수정 편집은 폐기 (목업 동일) */
    glCloseEdit() {
        this._glStash();
    },

    _glStash() {
        const g = this.glossary;
        if (g.mode !== 'edit') return;
        if (!g.form.id) {
            if (this._glHasContent(g.form)) this._glWriteDraft(g.form);
            else this._glClearDraft();
        }
        g.mode = 'view';
        g.form = _glEmptyForm();
        g.dirty = false;
        g.formError = null;
    },

    glEsc() {
        if (this.currentPage !== 'glossary') return;
        if (this.glossary.manageOpen) { this.glossary.manageOpen = false; return; }
        if (this.glossary.mode === 'edit') this.glCloseEdit();
    },

    glDraftNote() {
        return this.glossary.dirty
            ? '작성 내용은 자동 보관됩니다 — 닫아도 사라지지 않습니다'
            : '정의 · 풀이 · 예시 · 해석을 나눠 적으면 나중에 필요한 부분만 읽을 수 있습니다';
    },

    /** 입력 변경 공통 훅 — 새 용어는 즉시 localStorage 보관 (자동 보관) */
    glTouch() {
        this.glossary.dirty = true;
        if (!this.glossary.form.id) {
            if (this._glHasContent(this.glossary.form)) this._glWriteDraft(this.glossary.form);
            else this._glClearDraft();
        }
    },

    glFieldLen(key) {
        const v = this.glossary.form?.[key];
        return v ? String(v).length + '자' : '';
    },

    // ---------- 초안 (localStorage) ----------
    _glHasContent(d) {
        if (!d || d.id) return false;
        return !!(String(d.name || '').trim() || String(d.oneLine || '').trim()
            || GL_FIELDS.some(f => String(d[f.key] || '').trim()));
    },

    glHasSavedDraft() {
        return this._glHasContent(this.glossary.savedDraft) && this.glossary.mode !== 'edit';
    },

    glDraftPreview() {
        const d = this.glossary.savedDraft;
        if (!d) return '';
        return (String(d.name || '').trim() || String(d.oneLine || '').trim()).slice(0, 18);
    },

    _glReadDraft() {
        try {
            const raw = localStorage.getItem(GL_DRAFT_KEY);
            const parsed = raw ? JSON.parse(raw) : null;
            return parsed && typeof parsed === 'object' ? parsed : null;
        } catch (e) {
            return null;
        }
    },

    _glWriteDraft(form) {
        this.glossary.savedDraft = { ...form, id: null, relatedTermIds: (form.relatedTermIds || []).slice() };
        try {
            localStorage.setItem(GL_DRAFT_KEY, JSON.stringify(this.glossary.savedDraft));
        } catch (e) { /* 저장 불가(프라이빗 모드 등) — 세션 내 미러만 유지 */ }
    },

    _glClearDraft() {
        this.glossary.savedDraft = null;
        try {
            localStorage.removeItem(GL_DRAFT_KEY);
        } catch (e) { /* noop */ }
    },

    // ---------- 편집 폼 (카테고리/함께 볼 용어) ----------
    glCatPicks() {
        const f = this.glossary.form;
        const pendingName = (f.categoryName || '').trim();
        const picks = [{ key: 'none', name: '미분류', on: !pendingName && f.categoryId == null }];
        this.glossary.categories.forEach(c => picks.push({ key: c.id, name: c.name, on: !pendingName && f.categoryId === c.id }));
        if (pendingName) picks.push({ key: 'pending', name: pendingName + ' (신규)', on: true });
        return picks;
    },

    glPickCat(pick) {
        if (pick.key === 'pending') return;
        this.glossary.form.categoryId = pick.key === 'none' ? null : pick.key;
        this.glossary.form.categoryName = '';
        this.glTouch();
    },

    /** 편집 폼 "+ 새 카테고리 후 Enter" — 저장 시 find-or-create (R13) 로 전달 */
    glNewCatEnter() {
        const name = (this.glossary.newCat || '').trim();
        if (!name) return;
        if (name === '미분류') {
            this.glossary.formError = '"미분류"는 예약어입니다. 다른 이름을 사용하세요.';
            return;
        }
        this.glossary.form.categoryName = name;
        this.glossary.form.categoryId = null;
        this.glossary.newCat = '';
        this.glossary.formError = null;
        this.glTouch();
    },

    glRelatedChips() {
        return (this.glossary.form.relatedTermIds || []).map(id => {
            const t = this.glossary.terms.find(x => x.id === id);
            return { id, name: t ? t.name : '#' + id };
        });
    },

    glRelCandidates() {
        const f = this.glossary.form;
        const chosen = f.relatedTermIds || [];
        return this.glossary.terms
            .filter(t => t.id !== f.id && !chosen.includes(t.id))
            .slice()
            .sort((a, b) => a.name.localeCompare(b.name, 'ko'))
            .slice(0, 6);
    },

    glAddRelated(id) {
        const list = this.glossary.form.relatedTermIds;
        if (list.includes(id)) return;
        if (list.length >= GL_RELATED_MAX) {
            this.glossary.formError = '함께 볼 용어는 ' + GL_RELATED_MAX + '개까지 연결할 수 있습니다.';
            return;
        }
        list.push(id);
        this.glTouch();
    },

    glRemoveRelated(id) {
        this.glossary.form.relatedTermIds = this.glossary.form.relatedTermIds.filter(x => x !== id);
        this.glTouch();
    },

    // ---------- 저장/삭제 ----------
    _glValidateForm(f) {
        const name = (f.name || '').trim();
        if (!name) return '용어명을 입력하세요.';
        if (name.length > 200) return '용어명은 200자 이하여야 합니다.';
        if ((f.abbreviation || '').length > 200) return '약어·영문은 200자 이하여야 합니다.';
        if ((f.oneLine || '').length > 300) return '한 줄 정의는 300자 이하여야 합니다.';
        const over = GL_FIELDS.find(fd => (f[fd.key] || '').length > 4000);
        if (over) return over.label + ' 은(는) 4000자 이하여야 합니다.';
        if ((f.categoryName || '').trim().length > 50) return '카테고리명은 50자 이하여야 합니다.';
        if ((f.relatedTermIds || []).length > GL_RELATED_MAX) return '함께 볼 용어는 ' + GL_RELATED_MAX + '개 이하여야 합니다.';
        return null;
    },

    _glBody(f) {
        const opt = v => {
            const s = String(v ?? '');
            return s.trim() ? s : null;
        };
        const pendingName = (f.categoryName || '').trim();
        return {
            name: (f.name || '').trim(),
            abbreviation: opt(f.abbreviation),
            oneLine: opt(f.oneLine),
            definition: opt(f.definition),
            scaleNote: opt(f.scaleNote),
            example: opt(f.example),
            takeaway: opt(f.takeaway),
            categoryId: pendingName ? null : (f.categoryId ?? null),
            categoryName: pendingName || null,
            relatedTermIds: (f.relatedTermIds || []).slice()
        };
    },

    async glSave() {
        const g = this.glossary;
        if (g.saving) return;
        const f = g.form;
        const error = this._glValidateForm(f);
        if (error) { g.formError = error; return; }
        g.formError = null;
        g.saving = true;
        try {
            const isEdit = f.id != null;
            const res = isEdit
                ? await API.updateGlossaryTerm(f.id, this._glBody(f))
                : await API.createGlossaryTerm(this._glBody(f));
            const savedId = isEdit ? f.id : res?.id;
            if (!isEdit) this._glClearDraft();
            g.mode = 'view';
            g.form = _glEmptyForm();
            g.dirty = false;
            await this.glossaryLoad();
            if (savedId != null) { g.selId = savedId; g.pane = 'detail'; }
        } catch (e) {
            g.formError = this._glError(e, '저장에 실패했습니다.');
        } finally {
            g.saving = false;
        }
    },

    async glDeleteTerm() {
        const cur = this.glCurrent();
        if (!cur) return;
        if (!confirm('이 용어를 삭제할까요? 되돌릴 수 없습니다.')) return;
        try {
            await API.deleteGlossaryTerm(cur.id);
            this.glossary.selId = null;
            if (this.glIsNarrow()) this.glossary.pane = 'list';
            await this.glossaryLoad();
        } catch (e) {
            alert(this._glError(e, '삭제에 실패했습니다.'));
        }
    },

    // ---------- 카테고리 관리 (팝오버) ----------
    glToggleManage() {
        this.glossary.manageOpen = !this.glossary.manageOpen;
        this.glossary.manageError = null;
        this.glCancelRename();
    },

    _glValidateCatName(name) {
        if (!name) return '카테고리명을 입력하세요.';
        if (name === '미분류') return '"미분류"는 예약어입니다.';
        if (name.length > 50) return '카테고리명은 50자 이하여야 합니다.';
        return null;
    },

    async glAddCategory() {
        const name = (this.glossary.manageNewName || '').trim();
        const error = this._glValidateCatName(name);
        if (error) { this.glossary.manageError = error; return; }
        this.glossary.manageError = null;
        try {
            await API.createGlossaryCategory({ name });
            this.glossary.manageNewName = '';
            this.glossary.categories = await API.getGlossaryCategories();
        } catch (e) {
            this.glossary.manageError = this._glError(e, '카테고리 생성에 실패했습니다.');
        }
    },

    glStartRename(cat) {
        this.glossary.catEditId = cat.id;
        this.glossary.catEditName = cat.name;
        this.glossary.catError = null;
    },

    glCancelRename() {
        this.glossary.catEditId = null;
        this.glossary.catEditName = '';
        this.glossary.catError = null;
    },

    async glSaveRename() {
        const id = this.glossary.catEditId;
        if (id == null) return;
        const name = (this.glossary.catEditName || '').trim();
        const error = this._glValidateCatName(name);
        if (error) { this.glossary.catError = error; return; }
        try {
            await API.updateGlossaryCategory(id, { name });
            this.glCancelRename();
            this.glossary.categories = await API.getGlossaryCategories();
        } catch (e) {
            this.glossary.catError = this._glError(e, '수정에 실패했습니다.');
        }
    },

    async glRequestDeleteCategory(cat) {
        try {
            const res = await API.previewGlossaryCategoryDelete(cat.id);
            this.glossary.deleteImpact = { categoryId: cat.id, name: cat.name, count: res?.count ?? 0 };
        } catch (e) {
            alert(this._glError(e, '미리보기에 실패했습니다.'));
        }
    },

    glCancelDeleteCategory() {
        this.glossary.deleteImpact = null;
    },

    async glConfirmDeleteCategory() {
        const impact = this.glossary.deleteImpact;
        if (!impact) return;
        try {
            await API.deleteGlossaryCategory(impact.categoryId);
            this.glossary.deleteImpact = null;
            if (this.glossary.cat === impact.categoryId) this.glossary.cat = 'all';
            await this.glossaryLoad();
        } catch (e) {
            alert(this._glError(e, '삭제에 실패했습니다.'));
        }
    },

    // ---------- 에러 매핑 ----------
    _glError(e, fallback) {
        const code = e?.payload?.error || e?.error;
        switch (code) {
            case 'DUPLICATE_CATEGORY_NAME': return '이미 같은 이름의 카테고리가 있습니다.';
            case 'RESERVED_CATEGORY_NAME': return '"미분류"는 예약어입니다. 다른 이름을 사용하세요.';
            case 'NOT_FOUND': return '용어/카테고리를 찾을 수 없거나 접근 권한이 없습니다.';
            case 'VALIDATION_FAILED': return '입력값이 올바르지 않습니다.';
            case 'UNAUTHORIZED': return '로그인이 필요합니다.';
            default: return e?.message || fallback;
        }
    }
};
