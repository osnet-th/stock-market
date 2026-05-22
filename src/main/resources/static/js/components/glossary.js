/**
 * GlossaryComponent — 개인 용어 사전 (이슈 #43).
 *
 * 소유 프로퍼티: glossary
 * 외부 의존: API (api.js), auth
 *
 * 범위:
 *  - 용어 CRUD (이름 / 설명 / 카테고리)
 *  - 사용자별 카테고리 CRUD (미분류는 가상)
 *  - LIKE 검색(이름 + 설명) + 카테고리 필터 + 정렬(등록일/가나다) + 페이지네이션
 *  - 인라인 카테고리 생성 (find-or-create — categoryName 전송, 정확 일치 매핑)
 *  - R6: 카테고리 삭제 시 영향 건수 미리보기 + 미분류로 이동 확인 다이얼로그
 *
 * XSS 정책: 모든 사용자 입력 렌더링은 x-text/textContent 만. x-html/innerHTML 금지.
 */

function _glossaryEmptyForm() {
    return {
        id: null,
        name: '',
        definition: '',
        categoryId: null,
        categoryName: ''
    };
}

const GlossaryComponent = {
    glossary: {
        loading: false,
        error: null,

        categories: [],                              // [{ id, name }]
        activeCategoryFilter: null,                  // null=전체, 'uncategorized'=미분류, <id>=특정 카테고리

        terms: [],                                   // [{ id, name, definition, categoryId, createdAt, updatedAt }]
        pagination: { page: 0, size: 20, totalCount: 0 },

        filter: { q: '', definitionQ: '' },
        sort: 'REGISTERED_DESC',

        mode: null,                                  // null | 'create' | 'edit'
        form: _glossaryEmptyForm(),
        formError: null,

        categoryEditId: null,                        // 인라인 카테고리 이름 변경 중일 때 id
        categoryEditName: '',
        categoryEditError: null,

        newCategoryName: '',                         // 사이드바 신규 카테고리 입력
        newCategoryError: null,

        deleteImpact: null,                          // { categoryId, count } | null — 확인 다이얼로그 노출 트리거
        _searchDebounce: null
    },

    // ---------- 조회 ----------
    async glossaryLoad() {
        if (this.glossary.loading) return;
        this.glossary.loading = true;
        this.glossary.error = null;
        try {
            await this.glossaryLoadCategories();
            await this.glossaryLoadTerms();
        } catch (e) {
            this.glossary.error = e?.message || '용어 사전을 불러오지 못했습니다.';
            this.glossary.terms = [];
        } finally {
            this.glossary.loading = false;
        }
    },

    async glossaryLoadCategories() {
        try {
            const res = await API.getGlossaryCategories();
            this.glossary.categories = Array.isArray(res) ? res : [];
        } catch (e) {
            this.glossary.categories = [];
        }
    },

    async glossaryLoadTerms() {
        const params = {
            page: this.glossary.pagination.page,
            size: this.glossary.pagination.size,
            sort: this.glossary.sort
        };
        const q = (this.glossary.filter.q || '').trim();
        if (q) params.q = q;
        const dq = (this.glossary.filter.definitionQ || '').trim();
        if (dq) params.definitionQ = dq;
        if (this.glossary.activeCategoryFilter === 'uncategorized') {
            params.uncategorized = true;
        } else if (this.glossary.activeCategoryFilter != null) {
            params.categoryId = this.glossary.activeCategoryFilter;
        }
        const res = await API.getGlossaryTerms(params);
        this.glossary.terms = res?.items || [];
        this.glossary.pagination.totalCount = res?.totalCount ?? 0;
        this.glossary.pagination.page = res?.page ?? 0;
    },

    glossarySelectCategoryFilter(value) {
        this.glossary.activeCategoryFilter = value;
        this.glossary.pagination.page = 0;
        return this.glossaryLoadTerms().catch(e => {
            this.glossary.error = e?.message || '목록을 불러오지 못했습니다.';
        });
    },

    glossaryApplyQuery() {
        if (this.glossary._searchDebounce) clearTimeout(this.glossary._searchDebounce);
        this.glossary._searchDebounce = setTimeout(() => {
            this.glossary.pagination.page = 0;
            this.glossaryLoadTerms().catch(e => {
                this.glossary.error = e?.message || '검색에 실패했습니다.';
            });
        }, 250);
    },

    glossaryChangeSort(sort) {
        this.glossary.sort = sort;
        this.glossary.pagination.page = 0;
        return this.glossaryLoadTerms();
    },

    glossaryChangePage(delta) {
        const next = this.glossary.pagination.page + delta;
        if (next < 0) return;
        const max = Math.max(0, Math.ceil(
            this.glossary.pagination.totalCount / this.glossary.pagination.size
        ) - 1);
        if (next > max) return;
        this.glossary.pagination.page = next;
        return this.glossaryLoadTerms();
    },

    glossaryTotalPages() {
        const { totalCount, size } = this.glossary.pagination;
        if (!totalCount) return 1;
        return Math.max(1, Math.ceil(totalCount / size));
    },

    glossaryCategoryName(categoryId) {
        if (categoryId == null) return '미분류';
        const cat = this.glossary.categories.find(c => c.id === categoryId);
        return cat ? cat.name : '미분류';
    },

    // ---------- 용어 모달 ----------
    glossaryOpenCreate() {
        this.glossary.form = _glossaryEmptyForm();
        this.glossary.formError = null;
        this.glossary.mode = 'create';
    },

    glossaryOpenEdit(item) {
        this.glossary.form = {
            id: item.id,
            name: item.name || '',
            definition: item.definition || '',
            categoryId: item.categoryId ?? null,
            categoryName: ''
        };
        this.glossary.formError = null;
        this.glossary.mode = 'edit';
    },

    glossaryCloseModal() {
        this.glossary.mode = null;
        this.glossary.formError = null;
    },

    async glossarySave() {
        const f = this.glossary.form;
        const name = (f.name || '').trim();
        if (!name) {
            this.glossary.formError = '용어명을 입력하세요.';
            return;
        }
        if (name.length > 200) {
            this.glossary.formError = '용어명은 200자 이하여야 합니다.';
            return;
        }
        const definition = (f.definition || '').trim();
        if (definition.length > 4000) {
            this.glossary.formError = '설명은 4000자 이하여야 합니다.';
            return;
        }
        const inlineName = (f.categoryName || '').trim();
        if (inlineName === '미분류') {
            this.glossary.formError = '"미분류"는 예약어입니다. 다른 이름을 사용하세요.';
            return;
        }
        if (inlineName.length > 50) {
            this.glossary.formError = '카테고리명은 50자 이하여야 합니다.';
            return;
        }

        const body = {
            name,
            definition: definition || null,
            categoryId: f.categoryId ?? null,
            categoryName: inlineName || null
        };

        this.glossary.formError = null;
        try {
            if (this.glossary.mode === 'edit' && f.id != null) {
                await API.updateGlossaryTerm(f.id, body);
            } else {
                await API.createGlossaryTerm(body);
            }
            this.glossaryCloseModal();
            await this.glossaryLoad();
        } catch (e) {
            this.glossary.formError = this._glossaryHumanError(e, '저장에 실패했습니다.');
        }
    },

    async glossaryDeleteTerm(id) {
        if (id == null) return;
        if (!confirm('이 용어를 삭제할까요? 되돌릴 수 없습니다.')) return;
        try {
            await API.deleteGlossaryTerm(id);
            await this.glossaryLoadTerms();
        } catch (e) {
            alert(this._glossaryHumanError(e, '삭제에 실패했습니다.'));
        }
    },

    // ---------- 카테고리 ----------
    async glossaryAddCategory() {
        const name = (this.glossary.newCategoryName || '').trim();
        this.glossary.newCategoryError = null;
        if (!name) {
            this.glossary.newCategoryError = '카테고리명을 입력하세요.';
            return;
        }
        if (name === '미분류') {
            this.glossary.newCategoryError = '"미분류"는 예약어입니다.';
            return;
        }
        if (name.length > 50) {
            this.glossary.newCategoryError = '카테고리명은 50자 이하여야 합니다.';
            return;
        }
        try {
            await API.createGlossaryCategory({ name });
            this.glossary.newCategoryName = '';
            await this.glossaryLoadCategories();
        } catch (e) {
            this.glossary.newCategoryError = this._glossaryHumanError(e, '카테고리 생성에 실패했습니다.');
        }
    },

    glossaryStartRenameCategory(cat) {
        this.glossary.categoryEditId = cat.id;
        this.glossary.categoryEditName = cat.name;
        this.glossary.categoryEditError = null;
    },

    glossaryCancelRenameCategory() {
        this.glossary.categoryEditId = null;
        this.glossary.categoryEditName = '';
        this.glossary.categoryEditError = null;
    },

    async glossarySaveRenameCategory() {
        const id = this.glossary.categoryEditId;
        const name = (this.glossary.categoryEditName || '').trim();
        if (id == null) return;
        if (!name) {
            this.glossary.categoryEditError = '카테고리명을 입력하세요.';
            return;
        }
        if (name === '미분류') {
            this.glossary.categoryEditError = '"미분류"는 예약어입니다.';
            return;
        }
        if (name.length > 50) {
            this.glossary.categoryEditError = '카테고리명은 50자 이하여야 합니다.';
            return;
        }
        try {
            await API.updateGlossaryCategory(id, { name });
            this.glossaryCancelRenameCategory();
            await this.glossaryLoadCategories();
        } catch (e) {
            this.glossary.categoryEditError = this._glossaryHumanError(e, '수정에 실패했습니다.');
        }
    },

    async glossaryRequestDeleteCategory(cat) {
        try {
            const res = await API.previewGlossaryCategoryDelete(cat.id);
            this.glossary.deleteImpact = { categoryId: cat.id, name: cat.name, count: res?.count ?? 0 };
        } catch (e) {
            alert(this._glossaryHumanError(e, '미리보기에 실패했습니다.'));
        }
    },

    glossaryCancelDeleteCategory() {
        this.glossary.deleteImpact = null;
    },

    async glossaryConfirmDeleteCategory() {
        const impact = this.glossary.deleteImpact;
        if (!impact) return;
        try {
            await API.deleteGlossaryCategory(impact.categoryId);
            this.glossary.deleteImpact = null;
            // 활성 필터가 삭제된 카테고리였다면 '전체'로 리셋
            if (this.glossary.activeCategoryFilter === impact.categoryId) {
                this.glossary.activeCategoryFilter = null;
            }
            await this.glossaryLoad();
        } catch (e) {
            alert(this._glossaryHumanError(e, '삭제에 실패했습니다.'));
        }
    },

    // ---------- 에러 매핑 ----------
    _glossaryHumanError(e, fallback) {
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