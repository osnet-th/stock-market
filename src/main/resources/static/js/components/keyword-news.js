/**
 * KeywordNewsComponent - 키워드 뉴스 워크스페이스 (#115)
 *
 * 기존 keyword.js + news.js + news-search.js 를 하나로 통합했다.
 * 좌측 레일에서 키워드를 고르면 우측 뉴스 스트림이 그 키워드로 좁혀지고,
 * 검색어가 없으면 "저장된 전체 뉴스"(내 키워드 전체)가 최신순으로 보인다.
 *
 * 소유 프로퍼티: kwn
 */
const KeywordNewsComponent = {
    kwn: {
        // 키워드 레일
        keywords: [],            // KeywordResponse[]
        stats: {},               // keywordId -> 통계 Item
        todayTotal: 0,
        kwLoading: false,
        kwFilter: '',
        kwState: '전체',          // 전체 | 활성 | 비활성
        kwSort: '뉴스많은순',      // 뉴스많은순 | 오늘많은순
        selected: [],            // 선택된 keywordId (다중)
        menuId: null,            // 케밥 메뉴가 열린 keywordId

        // 뉴스 스트림
        list: [],
        page: 0,
        size: 20,
        totalPages: 0,
        totalElements: 0,
        loading: false,

        // 필터
        query: '',
        field: 'TITLE_CONTENT',  // TITLE_CONTENT | TITLE
        sort: 'LATEST',          // LATEST | RELEVANCE
        region: '전체',           // 전체 | 국내 | 해외
        period: 999,             // 1 | 7 | 30 | 999(전체) | 0(직접 지정)
        from: '',
        to: '',
        unreadOnly: false,

        // 스케줄 상태
        schedule: null,

        // 모달
        showAddModal: false,
        newKeyword: { keyword: '', region: 'DOMESTIC' },
        editTarget: null,        // { id, keyword, region, active }
        deleteTargets: [],       // 삭제 확인 대상 (단건/다건 공용)
        collectingId: null,
        saving: false
    },

    // ==================== 진입 ====================

    async kwnEnter() {
        if (!this.checkLoggedIn()) return;
        this.kwnResetTransient();
        await Promise.allSettled([
            this.loadKwnKeywords(),
            this.loadKwnSchedule()
        ]);
        await this.loadKwnNews(0);
    },

    /** 화면을 떠났다 돌아왔을 때 남아 있으면 안 되는 상태만 지운다. */
    kwnResetTransient() {
        this.kwn.menuId = null;
        this.kwn.editTarget = null;
        this.kwn.deleteTargets = [];
        this.kwn.showAddModal = false;
    },

    // ==================== 키워드 레일 ====================

    async loadKwnKeywords() {
        this.kwn.kwLoading = true;
        try {
            const [list, stats] = await Promise.all([
                API.getKeywords(this.auth.userId),
                API.getKeywordStats(this.auth.userId).catch(() => null)
            ]);
            this.kwn.keywords = list || [];

            const byId = {};
            if (stats && stats.items) {
                stats.items.forEach(item => { byId[item.keywordId] = item; });
            }
            this.kwn.stats = byId;
            this.kwn.todayTotal = stats ? stats.todayTotal : 0;

            // 삭제된 키워드가 선택에 남아 있으면 조회가 403 이 된다
            const alive = new Set(this.kwn.keywords.map(k => k.id));
            this.kwn.selected = this.kwn.selected.filter(id => alive.has(id));
        } catch (e) {
            console.error('키워드 로드 실패:', e);
            this.kwn.keywords = [];
            this.kwn.stats = {};
        } finally {
            this.kwn.kwLoading = false;
        }
    },

    async loadKwnSchedule() {
        try {
            this.kwn.schedule = await API.getCollectorSchedule(this.auth.userId);
        } catch (e) {
            // 스케줄 표시는 부가 정보 — 실패해도 화면 전체를 막지 않는다
            this.kwn.schedule = null;
        }
    },

    /** 레일에 그릴 키워드 목록 (필터 + 정렬 적용). */
    getKwnRailKeywords() {
        const s = this.kwn;
        let list = s.keywords.slice();

        if (s.kwState !== '전체') {
            const wantActive = s.kwState === '활성';
            list = list.filter(k => k.active === wantActive);
        }
        if (s.region !== '전체') {
            list = list.filter(k => this.kwnRegionLabel(k.region) === s.region);
        }
        const term = s.kwFilter.trim();
        if (term) {
            list = list.filter(k => k.keyword.includes(term));
        }

        return list.sort((a, b) => {
            const sa = s.stats[a.id] || {};
            const sb = s.stats[b.id] || {};
            if (s.kwSort === '오늘많은순') {
                return (sb.todayCount || 0) - (sa.todayCount || 0)
                    || (sb.totalCount || 0) - (sa.totalCount || 0);
            }
            return (sb.totalCount || 0) - (sa.totalCount || 0);
        });
    },

    kwnRegionLabel(region) {
        return region === 'DOMESTIC' ? '국내' : '해외';
    },

    kwnStat(keywordId) {
        return this.kwn.stats[keywordId] || { totalCount: 0, todayCount: 0, lastSuccessAt: null, failureStreak: 0, daily: [] };
    },

    /**
     * 레일 행의 회색 메타 문구. 실패 중이면 그 사실을 먼저 알린다.
     *
     * 레일이 296px 이고 스파크라인·건수·케밥이 자리를 차지해 이 텍스트에 쓸 수 있는 폭이
     * 약 117px(한글 12자 남짓)뿐이다. 넘치면 두 줄이 되어 행 높이가 어긋나므로 짧게 끊고,
     * 자세한 내용은 {@link kwnRailMetaTitle} 로 툴팁에 담는다.
     */
    kwnRailMeta(kw) {
        const stat = this.kwnStat(kw.id);
        if (!kw.active) {
            return '비활성 · 수집 중단';
        }
        if (stat.failureStreak > 0) {
            return '연속 실패 ' + stat.failureStreak + '회';
        }
        if (!stat.lastSuccessAt) {
            return '수집 이력 없음';
        }
        return '마지막 성공 ' + this.kwnRelativeTime(stat.lastSuccessAt);
    },

    /** 레일 메타의 전체 내용 (hover 툴팁). 짧게 끊은 문구가 감춘 정보를 여기서 보여준다. */
    kwnRailMetaTitle(kw) {
        const stat = this.kwnStat(kw.id);
        const last = stat.lastSuccessAt
            ? '마지막 성공 ' + this.kwnRelativeTime(stat.lastSuccessAt)
            : '수집 성공 이력 없음';
        if (!kw.active) {
            return '비활성 · 수집 중단 · ' + last;
        }
        if (stat.failureStreak > 0) {
            return '연속 실패 ' + stat.failureStreak + '회 · ' + last;
        }
        return last;
    },

    kwnRailMetaColor(kw) {
        const stat = this.kwnStat(kw.id);
        if (stat.failureStreak > 0) return 'var(--dc-red)';
        return kw.active ? '#98a0a7' : '#b2b8bd';
    },

    /** 7일 스파크라인 (인라인 SVG — 키워드 수만큼 그려지므로 경량이어야 한다) */
    kwnSparkline(kw) {
        const data = this.kwnStat(kw.id).daily || [];
        if (data.length === 0) return '';
        const W = 52, H = 22;
        const max = Math.max.apply(null, data.concat([1]));
        const bw = W / data.length - 2;
        const on = this.kwn.selected.includes(kw.id);
        const color = on ? 'var(--dc-blue)' : (kw.active ? '#8fa8c9' : '#c9ced6');
        const bars = data.map((v, i) => {
            const h = Math.max(1.5, (v / max) * (H - 3));
            const op = i === data.length - 1 ? 1 : 0.42;
            return '<rect x="' + (i * (W / data.length)) + '" y="' + (H - h) + '" width="' + bw
                + '" height="' + h + '" rx="1" fill="' + color + '" opacity="' + op + '"></rect>';
        }).join('');
        return '<svg viewBox="0 0 ' + W + ' ' + H + '" style="width:100%;height:100%;display:block">' + bars + '</svg>';
    },

    toggleKwnSelect(kw) {
        const i = this.kwn.selected.indexOf(kw.id);
        if (i >= 0) this.kwn.selected.splice(i, 1);
        else this.kwn.selected.push(kw.id);
        this.loadKwnNews(0);
    },

    clearKwnSelection() {
        this.kwn.selected = [];
        this.loadKwnNews(0);
    },

    getKwnSelectedKeywords() {
        return this.kwn.keywords.filter(k => this.kwn.selected.includes(k.id));
    },

    toggleKwnKwSort() {
        this.kwn.kwSort = this.kwn.kwSort === '뉴스많은순' ? '오늘많은순' : '뉴스많은순';
    },

    // ==================== 뉴스 스트림 ====================

    async loadKwnNews(page) {
        if (!this.checkLoggedIn()) return;
        this.kwn.loading = true;
        try {
            const result = await API.searchNews(this.auth.userId, {
                query: this.kwn.query.trim(),
                keywordIds: this.kwn.selected,
                startDate: this.kwnStartDate(),
                endDate: this.kwnEndDate(),
                region: this.kwnRegionParam(),
                field: this.kwn.field,
                sort: this.kwn.sort,
                unreadOnly: this.kwn.unreadOnly,
                page: page || 0,
                size: this.kwn.size
            });
            // 더보기는 누적, 그 외에는 교체
            const rows = result.content || [];
            this.kwn.list = (page && page > 0) ? this.kwn.list.concat(rows) : rows;
            this.kwn.page = result.page;
            this.kwn.totalPages = result.totalPages;
            this.kwn.totalElements = result.totalElements;
        } catch (e) {
            console.error('뉴스 조회 실패:', e);
            this.kwn.list = [];
            this.kwn.totalElements = 0;
            this.kwn.totalPages = 0;
        } finally {
            this.kwn.loading = false;
        }
    },

    async loadMoreKwnNews() {
        if (this.kwn.page + 1 >= this.kwn.totalPages) return;
        await this.loadKwnNews(this.kwn.page + 1);
    },

    kwnRegionParam() {
        if (this.kwn.region === '국내') return 'DOMESTIC';
        if (this.kwn.region === '해외') return 'INTERNATIONAL';
        return null;
    },

    /** 기간 프리셋을 실제 날짜로 바꾼다. 전체(999)는 조건 없음. */
    kwnStartDate() {
        const p = this.kwn.period;
        if (p === 0) return this.kwn.from || null;
        if (p >= 999) return null;
        const d = new Date();
        d.setDate(d.getDate() - (p - 1));
        return this.kwnFormatDate(d);
    },

    kwnEndDate() {
        if (this.kwn.period === 0) return this.kwn.to || null;
        return null;
    },

    kwnFormatDate(d) {
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return d.getFullYear() + '-' + m + '-' + day;
    },

    /** 스트림을 발행일 기준으로 묶는다. */
    getKwnDayGroups() {
        const groups = [];
        const index = {};
        const todayKey = this.kwnFormatDate(new Date());

        this.kwn.list.forEach(item => {
            const key = item.publishedAt ? item.publishedAt.substring(0, 10) : '날짜 미상';
            if (!index[key]) {
                index[key] = { key: key, label: this.kwnDayLabel(key, todayKey), rows: [] };
                groups.push(index[key]);
            }
            index[key].rows.push(item);
        });
        return groups;
    },

    kwnDayLabel(key, todayKey) {
        if (key === '날짜 미상') return key;
        const d = new Date(key.replace(/-/g, '/'));
        const wd = ['일', '월', '화', '수', '목', '금', '토'][d.getDay()];
        return (key === todayKey ? '오늘 · ' : '') + key.replace(/-/g, '.') + ' (' + wd + ')';
    },

    kwnArticleTime(item) {
        if (!item.publishedAt) return '';
        return item.publishedAt.substring(11, 16);
    },

    /** 기사의 키워드 칩. 뉴스는 keywordId 하나에 묶여 있다. */
    kwnArticleKeyword(item) {
        const kw = this.kwn.keywords.find(k => k.id === item.keywordId);
        return kw ? kw.keyword : null;
    },

    /** 언론사. 이 컬럼 도입 전 데이터는 null 이라 URL 도메인으로 폴백한다. */
    kwnArticleSource(item) {
        if (item.source) return item.source;
        if (!item.originalUrl) return '';
        try {
            return new URL(item.originalUrl).hostname.replace(/^www\./, '');
        } catch (e) {
            return '';
        }
    },

    kwnResultTitle() {
        const q = this.kwn.query.trim();
        if (q) return '‘' + q + '’ 검색 결과';
        const names = this.getKwnSelectedKeywords().map(k => k.keyword);
        if (names.length === 0) return '저장된 전체 뉴스';
        return names.slice(0, 2).join(' · ') + (names.length > 2 ? ' 외 ' + (names.length - 2) + '개' : '');
    },

    kwnResultNote() {
        const p = this.kwn.period;
        if (p === 0) {
            if (!this.kwn.from && !this.kwn.to) return '기간 미지정';
            return (this.kwn.from || '').replace(/-/g, '.') + ' ~ ' + (this.kwn.to || '').replace(/-/g, '.');
        }
        if (p >= 999) return '전체 기간';
        return '최근 ' + p + '일';
    },

    kwnSearchPlaceholder() {
        const n = this.kwn.selected.length;
        return n > 0
            ? '선택한 키워드 ' + n + '개 안에서 검색'
            : '저장된 모든 뉴스를 검색 (예: 삼성전자, HBM, 금리)';
    },

    kwnRemainLabel() {
        const remain = Math.max(0, this.kwn.totalElements - this.kwn.list.length);
        return remain > 0 ? '남은 ' + remain.toLocaleString('ko-KR') + '건' : '마지막 페이지';
    },

    setKwnPeriod(v) {
        this.kwn.period = v;
        if (v === 0 && !this.kwn.from) {
            const d = new Date();
            this.kwn.to = this.kwnFormatDate(d);
            d.setDate(d.getDate() - 6);
            this.kwn.from = this.kwnFormatDate(d);
        }
        this.loadKwnNews(0);
    },

    resetKwnFilters() {
        Object.assign(this.kwn, {
            query: '', selected: [], region: '전체', period: 999,
            unreadOnly: false, field: 'TITLE_CONTENT', sort: 'LATEST'
        });
        this.loadKwnNews(0);
    },

    // ==================== 읽음 / 저장 ====================

    async openKwnArticle(item) {
        if (item.originalUrl) {
            window.open(item.originalUrl, '_blank', 'noopener');
        }
        if (item.read || !item.id) return;
        item.read = true;   // 낙관적 반영 — 목록 전체를 다시 불러오지 않는다
        try {
            await API.markNewsRead(item.id, this.auth.userId);
        } catch (e) {
            item.read = false;
            console.error('읽음 처리 실패:', e);
        }
    },

    async toggleKwnSaved(item) {
        if (!item.id) return;
        const before = item.saved;
        item.saved = !before;
        try {
            item.saved = await API.toggleNewsSaved(item.id, this.auth.userId);
        } catch (e) {
            item.saved = before;
            console.error('저장 토글 실패:', e);
        }
    },

    /** 목업 `모두 읽음` — 지금 화면에 보이는 기사만 대상이다. */
    async markAllKwnRead() {
        const ids = this.kwn.list.filter(i => i.id && !i.read).map(i => i.id);
        if (ids.length === 0) return;
        try {
            await API.markAllNewsRead(this.auth.userId, ids);
            this.kwn.list.forEach(i => { if (ids.includes(i.id)) i.read = true; });
            if (this.kwn.unreadOnly) await this.loadKwnNews(0);
        } catch (e) {
            console.error('모두 읽음 실패:', e);
        }
    },

    // ==================== 키워드 CRUD ====================

    toggleKwnMenu(kw, ev) {
        if (ev) ev.stopPropagation();
        this.kwn.menuId = this.kwn.menuId === kw.id ? null : kw.id;
    },

    closeKwnMenus() {
        this.kwn.menuId = null;
    },

    async collectKwnKeyword(kw, ev) {
        if (ev) ev.stopPropagation();
        this.kwn.menuId = null;
        if (this.kwn.collectingId) return;
        this.kwn.collectingId = kw.id;
        try {
            const result = await API.collectNewsByKeyword(kw.id, kw.keyword, kw.region);
            let msg = '수집 완료: ' + result.successCount + '건 저장';
            if (result.ignoredCount > 0) msg += ', ' + result.ignoredCount + '건 중복';
            alert(msg);
            await Promise.allSettled([this.loadKwnKeywords(), this.loadKwnSchedule()]);
            await this.loadKwnNews(0);
        } catch (e) {
            console.error('뉴스 수집 실패:', e);
            alert('뉴스 수집에 실패했습니다.');
        } finally {
            this.kwn.collectingId = null;
        }
    },

    /** 헤더 `지금 수집` — 선택이 있으면 선택분, 없으면 활성 키워드 전체. */
    async collectKwnAll() {
        const targets = this.kwn.selected.length > 0
            ? this.getKwnSelectedKeywords()
            : this.kwn.keywords.filter(k => k.active);
        if (targets.length === 0) {
            alert('수집할 활성 키워드가 없습니다.');
            return;
        }
        if (this.kwn.collectingId) return;
        this.kwn.collectingId = -1;
        let saved = 0;
        try {
            for (const kw of targets) {
                try {
                    const r = await API.collectNewsByKeyword(kw.id, kw.keyword, kw.region);
                    saved += r.successCount;
                } catch (e) {
                    console.error('수집 실패:', kw.keyword, e);
                }
            }
            alert('수집 완료: ' + saved + '건 저장');
            await Promise.allSettled([this.loadKwnKeywords(), this.loadKwnSchedule()]);
            await this.loadKwnNews(0);
        } finally {
            this.kwn.collectingId = null;
        }
    },

    async toggleKwnActive(kw, ev) {
        if (ev) ev.stopPropagation();
        this.kwn.menuId = null;
        try {
            if (kw.active) await API.deactivateKeyword(kw.id, this.auth.userId);
            else await API.activateKeyword(kw.id, this.auth.userId);
            await this.loadKwnKeywords();
        } catch (e) {
            console.error('키워드 상태 변경 실패:', e);
            alert('상태 변경에 실패했습니다.');
        }
    },

    openKwnAdd() {
        this.kwn.menuId = null;
        this.kwn.newKeyword = { keyword: this.kwn.query.trim(), region: 'DOMESTIC' };
        this.kwn.showAddModal = true;
    },

    async addKwnKeyword() {
        const kw = this.kwn.newKeyword;
        if (!kw.keyword.trim() || this.kwn.saving) return;
        this.kwn.saving = true;
        try {
            await API.registerKeyword(kw.keyword.trim(), this.auth.userId, kw.region);
            this.kwn.showAddModal = false;
            this.kwn.newKeyword = { keyword: '', region: 'DOMESTIC' };
            await this.loadKwnKeywords();
            await this.loadKwnNews(0);
        } catch (e) {
            console.error('키워드 등록 실패:', e);
            alert((e && e.userMessage) || '키워드 등록에 실패했습니다.');
        } finally {
            this.kwn.saving = false;
        }
    },

    openKwnEdit(kw, ev) {
        if (ev) ev.stopPropagation();
        this.kwn.menuId = null;
        this.kwn.editTarget = { id: kw.id, keyword: kw.keyword, region: kw.region, active: kw.active };
    },

    closeKwnEdit() {
        this.kwn.editTarget = null;
    },

    kwnEditCount() {
        const t = this.kwn.editTarget;
        return t ? this.kwnStat(t.id).totalCount.toLocaleString('ko-KR') : '0';
    },

    async saveKwnEdit() {
        const t = this.kwn.editTarget;
        if (!t || !t.keyword.trim() || this.kwn.saving) return;
        this.kwn.saving = true;
        try {
            // 서버가 재구독으로 처리하므로 이름/지역이 바뀌면 id 가 달라진다
            const updated = await API.updateKeyword(
                t.id, this.auth.userId, t.keyword.trim(), t.region, t.active);

            const i = this.kwn.selected.indexOf(t.id);
            if (i >= 0 && updated && updated.id) this.kwn.selected.splice(i, 1, updated.id);

            this.kwn.editTarget = null;
            await this.loadKwnKeywords();
            await this.loadKwnNews(0);
        } catch (e) {
            console.error('키워드 수정 실패:', e);
            // userMessage 는 서버가 내려준 사람이 읽을 문구다. 없으면 JSON 원문 대신 기본 문구를 쓴다
            alert((e && e.userMessage) || '키워드 수정에 실패했습니다.');
        } finally {
            this.kwn.saving = false;
        }
    },

    askKwnDelete(keywords, ev) {
        if (ev) ev.stopPropagation();
        this.kwn.menuId = null;
        this.kwn.deleteTargets = Array.isArray(keywords) ? keywords : [keywords];
    },

    askKwnDeleteFromEdit() {
        const t = this.kwn.editTarget;
        if (!t) return;
        const kw = this.kwn.keywords.find(k => k.id === t.id);
        if (kw) this.kwn.deleteTargets = [kw];
    },

    closeKwnDelete() {
        this.kwn.deleteTargets = [];
    },

    /** 삭제 대상 기사 합계 — 모달에서 무엇이 사라지는지 알려준다. */
    kwnDeleteArticleCount() {
        return this.kwn.deleteTargets
            .reduce((sum, k) => sum + this.kwnStat(k.id).totalCount, 0)
            .toLocaleString('ko-KR');
    },

    async confirmKwnDelete() {
        const targets = this.kwn.deleteTargets;
        if (targets.length === 0 || this.kwn.saving) return;
        this.kwn.saving = true;
        try {
            if (targets.length === 1) {
                await API.deleteKeyword(targets[0].id, this.auth.userId);
            } else {
                await API.bulkDeleteKeywords(this.auth.userId, targets.map(k => k.id));
            }
            const removed = new Set(targets.map(k => k.id));
            this.kwn.selected = this.kwn.selected.filter(id => !removed.has(id));
            this.kwn.deleteTargets = [];
            this.kwn.editTarget = null;
            await this.loadKwnKeywords();
            await this.loadKwnNews(0);
        } catch (e) {
            console.error('키워드 삭제 실패:', e);
            alert((e && e.userMessage) || '키워드 삭제에 실패했습니다.');
        } finally {
            this.kwn.saving = false;
        }
    },

    async bulkKwnDeactivate() {
        const targets = this.getKwnSelectedKeywords();
        if (targets.length === 0) return;
        try {
            await API.bulkDeactivateKeywords(this.auth.userId, targets.map(k => k.id));
            await this.loadKwnKeywords();
        } catch (e) {
            console.error('일괄 중단 실패:', e);
            alert('일괄 중단에 실패했습니다.');
        }
    },

    // ==================== 스케줄 표시 ====================

    kwnScheduleLabel() {
        return this.kwn.schedule ? this.kwn.schedule.scheduleLabel : '';
    },

    kwnScheduleDotColor() {
        if (!this.kwn.schedule) return '#c9ced6';
        return this.kwn.schedule.healthy ? '#2e8b62' : 'var(--dc-red)';
    },

    kwnFailedCount() {
        return this.kwn.schedule ? this.kwn.schedule.failedKeywordCount : 0;
    },

    /** "12분 전" / "45분 후" — 과거·미래를 모두 다룬다. */
    kwnRelativeTime(iso) {
        if (!iso) return '-';
        const t = new Date(iso.replace(/-/g, '/').replace('T', ' '));
        if (isNaN(t.getTime())) return '-';
        const diffMin = Math.round((t.getTime() - Date.now()) / 60000);
        const abs = Math.abs(diffMin);
        let label;
        if (abs < 1) label = '방금';
        else if (abs < 60) label = abs + '분';
        else if (abs < 60 * 24) label = Math.floor(abs / 60) + '시간';
        else label = Math.floor(abs / (60 * 24)) + '일';
        if (label === '방금') return label;
        return label + (diffMin >= 0 ? ' 후' : ' 전');
    }
};
