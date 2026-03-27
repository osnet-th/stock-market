/**
 * KeywordComponent - 키워드 CRUD, 필터링
 *
 * 소유 프로퍼티: keywords
 * 메서드: loadKeywords, getFilteredKeywords, addKeyword, toggleKeyword, removeKeyword
 */
const KeywordComponent = {
    keywords: {
        list: [],
        filter: 'all',
        regionFilter: 'all',
        showAddModal: false,
        newKeyword: { keyword: '', region: 'DOMESTIC' }
    },

    async loadKeywords() {
        try {
            const active = this.keywords.filter === 'active' ? true
                : this.keywords.filter === 'inactive' ? false : null;
            this.keywords.list = await API.getKeywords(this.auth.userId, active) || [];
        } catch (e) {
            console.error('키워드 로드 실패:', e);
            this.keywords.list = [];
        }
    },

    getFilteredKeywords() {
        let list = this.keywords.list;
        if (this.keywords.regionFilter !== 'all') {
            list = list.filter((k) => k.region === this.keywords.regionFilter);
        }
        return list;
    },

    async addKeyword() {
        const kw = this.keywords.newKeyword;
        if (!kw.keyword.trim()) return;
        try {
            await API.registerKeyword(kw.keyword.trim(), this.auth.userId, kw.region);
            this.keywords.showAddModal = false;
            this.keywords.newKeyword = { keyword: '', region: 'DOMESTIC' };
            await this.loadKeywords();
        } catch (e) {
            console.error('키워드 등록 실패:', e);
            alert('키워드 등록에 실패했습니다.');
        }
    },

    async toggleKeyword(kw) {
        try {
            if (kw.active) {
                await API.deactivateKeyword(kw.id, this.auth.userId);
            } else {
                await API.activateKeyword(kw.id, this.auth.userId);
            }
            await this.loadKeywords();
        } catch (e) {
            console.error('키워드 상태 변경 실패:', e);
        }
    },

    async removeKeyword(id) {
        if (!confirm('키워드를 삭제하시겠습니까?')) return;
        try {
            await API.deleteKeyword(id, this.auth.userId);
            await this.loadKeywords();
        } catch (e) {
            console.error('키워드 삭제 실패:', e);
        }
    }
};