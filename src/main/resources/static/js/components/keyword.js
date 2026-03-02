const KeywordComponent = {
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
            list = list.filter(k => k.region === this.keywords.regionFilter);
        }
        return list;
    },

    async addKeyword() {
        const { keyword, region } = this.keywords.newKeyword;
        if (!keyword.trim()) return;

        try {
            await API.registerKeyword(keyword.trim(), this.auth.userId, region);
            this.keywords.showAddModal = false;
            this.keywords.newKeyword = { keyword: '', region: 'KOREA' };
            await this.loadKeywords();
        } catch (e) {
            console.error('키워드 등록 실패:', e);
            alert('키워드 등록에 실패했습니다.');
        }
    },

    async toggleKeyword(kw) {
        try {
            if (kw.active) {
                await API.deactivateKeyword(kw.id);
            } else {
                await API.activateKeyword(kw.id);
            }
            await this.loadKeywords();
        } catch (e) {
            console.error('키워드 상태 변경 실패:', e);
        }
    },

    async removeKeyword(id) {
        if (!confirm('키워드를 삭제하시겠습니까?')) return;
        try {
            await API.deleteKeyword(id);
            await this.loadKeywords();
        } catch (e) {
            console.error('키워드 삭제 실패:', e);
        }
    }
};
