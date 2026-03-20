const KeywordComponent = {
    async loadKeywords() {
        try {
            var active = this.keywords.filter === 'active' ? true
                : this.keywords.filter === 'inactive' ? false : null;
            this.keywords.list = await API.getKeywords(this.auth.userId, active) || [];
        } catch (e) {
            console.error('키워드 로드 실패:', e);
            this.keywords.list = [];
        }
    },

    getFilteredKeywords() {
        var list = this.keywords.list;
        if (this.keywords.regionFilter !== 'all') {
            list = list.filter(function(k) { return k.region === this.keywords.regionFilter; }.bind(this));
        }
        return list;
    },

    async addKeyword() {
        var kw = this.keywords.newKeyword;
        if (!kw.keyword.trim()) return;

        try {
            await API.registerKeyword(kw.keyword.trim(), this.auth.userId, kw.region);
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
