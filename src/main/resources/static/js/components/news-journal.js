/**
 * NewsJournalComponent — 뉴스 기록 (사건 타임라인) 1차 MVP + 주제 분류.
 *
 * 소유 프로퍼티: newsJournal
 * 외부 의존: API (api.js), auth (userId)
 *
 * 설계 문서:
 *  - .claude/designs/newsjournal/news-journal/news-journal.md (1차 MVP)
 *  - .claude/designs/newsjournal/news-event-category/news-event-category.md (Phase B)
 *
 * 범위:
 *  - 사건 CRUD (제목 / 발생일 / 시장영향 / 주제 분류 / WHAT/WHY/HOW / URL 리스트)
 *  - 세로 타임라인 + 주제 탭 + 시장영향 필터 + 페이지네이션
 *  - 주제는 사용자 입력 → 서버가 find-or-create
 */

function _newsJournalEmptyForm() {
    return {
        id: null,
        title: '',
        occurredDate: new Date().toISOString().slice(0, 10),
        impact: 'NEUTRAL',
        category: '',
        what: '',
        why: '',
        how: '',
        links: []
    };
}

const NewsJournalComponent = {
    newsJournal: {
        loading: false,
        error: null,
        items: [],                                   // [{ id, title, occurredDate, impact, category: {id, name}, what, why, how, links: [...] , createdAt, updatedAt }]
        pagination: { page: 0, size: 20, totalCount: 0 },
        filter: { impact: '', from: '', to: '' },
        categories: [],                              // [{ id, name }]
        activeCategoryId: null,                      // null = 전체
        mode: null,                                  // null | 'create' | 'edit'
        form: _newsJournalEmptyForm(),
        formError: null
    },

    // ---------- 조회 ----------
    async newsJournalLoadCategories() {
        try {
            const res = await API.getNewsEventCategories();
            this.newsJournal.categories = Array.isArray(res) ? res : [];
        } catch (e) {
            // 카테고리 로딩 실패는 비치명적: 빈 배열 유지하고 입력은 허용
            this.newsJournal.categories = [];
        }
    },

    async newsJournalLoad() {
        this.newsJournal.loading = true;
        this.newsJournal.error = null;
        try {
            await this.newsJournalLoadCategories();
            const params = {
                page: this.newsJournal.pagination.page,
                size: this.newsJournal.pagination.size,
                impact: this.newsJournal.filter.impact || undefined,
                categoryId: this.newsJournal.activeCategoryId || undefined,
                from: this.newsJournal.filter.from || undefined,
                to: this.newsJournal.filter.to || undefined
            };
            const res = await API.getNewsEvents(params);
            this.newsJournal.items = res?.items || [];
            this.newsJournal.pagination.totalCount = res?.totalCount ?? 0;
            this.newsJournal.pagination.page = res?.page ?? 0;
        } catch (e) {
            this.newsJournal.error = e?.message || '뉴스 기록을 불러오지 못했습니다.';
            this.newsJournal.items = [];
        } finally {
            this.newsJournal.loading = false;
        }
    },

    newsJournalSelectCategoryTab(id) {
        this.newsJournal.activeCategoryId = id;
        this.newsJournal.pagination.page = 0;
        return this.newsJournalLoad();
    },

    newsJournalApplyFilter() {
        this.newsJournal.pagination.page = 0;
        return this.newsJournalLoad();
    },

    newsJournalResetFilter() {
        this.newsJournal.filter = { impact: '', from: '', to: '' };
        this.newsJournal.pagination.page = 0;
        return this.newsJournalLoad();
    },

    newsJournalChangePage(delta) {
        const next = this.newsJournal.pagination.page + delta;
        if (next < 0) return;
        const maxPage = Math.max(0, Math.ceil(
            this.newsJournal.pagination.totalCount / this.newsJournal.pagination.size
        ) - 1);
        if (next > maxPage) return;
        this.newsJournal.pagination.page = next;
        return this.newsJournalLoad();
    },

    newsJournalTotalPages() {
        const { totalCount, size } = this.newsJournal.pagination;
        if (!totalCount) return 1;
        return Math.max(1, Math.ceil(totalCount / size));
    },

    // ---------- 모달 (생성/수정) ----------
    newsJournalOpenCreate() {
        this.newsJournal.form = _newsJournalEmptyForm();
        this.newsJournal.formError = null;
        this.newsJournal.mode = 'create';
    },

    newsJournalOpenEdit(item) {
        this.newsJournal.form = {
            id: item.id,
            title: item.title || '',
            occurredDate: item.occurredDate || '',
            impact: item.impact || 'NEUTRAL',
            category: item.category?.name || '',
            what: item.what || '',
            why: item.why || '',
            how: item.how || '',
            links: (item.links || []).map(l => ({ title: l.title || '', url: l.url || '' }))
        };
        this.newsJournal.formError = null;
        this.newsJournal.mode = 'edit';
    },

    newsJournalCloseModal() {
        this.newsJournal.mode = null;
        this.newsJournal.formError = null;
    },

    newsJournalAddLink() {
        if (this.newsJournal.form.links.length >= 20) {
            this.newsJournal.formError = '링크는 최대 20개까지 추가할 수 있습니다.';
            return;
        }
        this.newsJournal.form.links.push({ title: '', url: '' });
    },

    newsJournalRemoveLink(index) {
        this.newsJournal.form.links.splice(index, 1);
    },

    async newsJournalSave() {
        const f = this.newsJournal.form;
        const today = new Date().toISOString().slice(0, 10);

        if (!f.title || !f.title.trim()) {
            this.newsJournal.formError = '제목을 입력하세요.';
            return;
        }
        if (f.title.length > 200) {
            this.newsJournal.formError = '제목은 200자 이하여야 합니다.';
            return;
        }
        if (!f.occurredDate) {
            this.newsJournal.formError = '발생 일자를 입력하세요.';
            return;
        }
        if (f.occurredDate > today) {
            this.newsJournal.formError = '미래 날짜로 기록할 수 없습니다.';
            return;
        }
        if (!f.impact) {
            this.newsJournal.formError = '시장영향을 선택하세요.';
            return;
        }
        const categoryName = (f.category || '').trim();
        if (!categoryName) {
            this.newsJournal.formError = '주제 분류를 입력하세요.';
            return;
        }
        if (categoryName.length > 50) {
            this.newsJournal.formError = '주제 분류는 50자 이하여야 합니다.';
            return;
        }
        for (const key of ['what', 'why', 'how']) {
            if ((f[key] || '').length > 4000) {
                this.newsJournal.formError = `${key.toUpperCase()} 는 4000자 이하여야 합니다.`;
                return;
            }
        }

        const cleanLinks = (f.links || [])
            .map(l => ({ title: (l.title || '').trim(), url: (l.url || '').trim() }))
            .filter(l => l.title || l.url);
        for (const l of cleanLinks) {
            if (!l.title || !l.url) {
                this.newsJournal.formError = '링크의 제목과 URL을 모두 입력하세요.';
                return;
            }
            if (l.title.length > 200) {
                this.newsJournal.formError = '링크 제목은 200자 이하여야 합니다.';
                return;
            }
            if (l.url.length > 2000) {
                this.newsJournal.formError = '링크 URL은 2000자 이하여야 합니다.';
                return;
            }
        }

        const body = {
            title: f.title.trim(),
            occurredDate: f.occurredDate,
            impact: f.impact,
            category: categoryName,
            what: f.what || null,
            why: f.why || null,
            how: f.how || null,
            links: cleanLinks
        };

        this.newsJournal.loading = true;
        this.newsJournal.formError = null;
        try {
            if (this.newsJournal.mode === 'edit' && f.id != null) {
                await API.updateNewsEvent(f.id, body);
            } else {
                await API.createNewsEvent(body);
            }
            this.newsJournalCloseModal();
            await this.newsJournalLoad();
        } catch (e) {
            this.newsJournal.formError = e?.message || '저장에 실패했습니다.';
        } finally {
            this.newsJournal.loading = false;
        }
    },

    async newsJournalDelete(id) {
        if (id == null) return;
        if (!confirm('이 사건을 삭제할까요? 되돌릴 수 없습니다.')) return;
        this.newsJournal.loading = true;
        try {
            await API.deleteNewsEvent(id);
            await this.newsJournalLoad();
        } catch (e) {
            alert(e?.message || '삭제에 실패했습니다.');
        } finally {
            this.newsJournal.loading = false;
        }
    },

    // ---------- 표시 헬퍼 ----------
    newsJournalImpactLabel(impact) {
        switch (impact) {
            case 'GOOD': return '호재';
            case 'BAD': return '악재';
            case 'NEUTRAL': return '중립';
            default: return impact || '';
        }
    },

    newsJournalImpactBadgeClass(impact) {
        switch (impact) {
            case 'GOOD': return 'bg-emerald-100 text-emerald-700 border-emerald-200';
            case 'BAD': return 'bg-rose-100 text-rose-700 border-rose-200';
            case 'NEUTRAL': return 'bg-slate-100 text-slate-700 border-slate-200';
            default: return 'bg-gray-100 text-gray-600 border-gray-200';
        }
    },

    newsJournalImpactDotClass(impact) {
        switch (impact) {
            case 'GOOD': return 'bg-emerald-500';
            case 'BAD': return 'bg-rose-500';
            case 'NEUTRAL': return 'bg-slate-400';
            default: return 'bg-gray-400';
        }
    }
};