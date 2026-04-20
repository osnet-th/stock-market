/**
 * AdminLogsComponent — 운영자 전용 로그 조회 페이지
 *
 * 소유 프로퍼티: adminLogs
 * 주요 메서드: loadAdminLogs / searchAdminLogs / loadMoreAdminLogs
 *              aggregateAdminLogs / downloadAdminLogs
 *              openAdminLogModal / closeAdminLogModal
 */
const AdminLogsComponent = {
    adminLogs: {
        // 접근 제어
        forbidden: false,

        // 현재 선택 도메인
        domain: 'audit',  // 'audit' | 'error' | 'business'

        // 필터 (UI → 서버 요청 변환)
        filter: {
            from: '',       // datetime-local 형식 (KST)
            to: '',
            userId: '',
            q: '',
            status: '',
            exceptionClass: ''
        },
        _debounceTimer: null,

        // 조회 결과
        list: [],
        total: 0,
        nextSearchAfter: null,
        loading: false,

        // 집계
        aggregation: {
            interval: 'day',
            counts: []
        },
        _chartInstance: null,

        // 디스크 사용량
        diskUsage: {
            totalBytes: 0,
            totalDocs: 0,
            indices: []
        },

        // payload 모달
        modal: {
            open: false,
            item: null,
            preview: '',
            truncatedNotice: false
        }
    },

    // ────────────────────────────────────────────────────────────────
    // 초기 진입
    // ────────────────────────────────────────────────────────────────

    async loadAdminLogs() {
        this.adminLogs.forbidden = false;
        // 기본 기간: 최근 24시간 (KST datetime-local)
        if (!this.adminLogs.filter.from || !this.adminLogs.filter.to) {
            const now = new Date();
            const from = new Date(now.getTime() - 24 * 60 * 60 * 1000);
            this.adminLogs.filter.to = this._toDatetimeLocal(now);
            this.adminLogs.filter.from = this._toDatetimeLocal(from);
        }
        await Promise.all([
            this._fetchAdminDiskUsage(),
            this.searchAdminLogs(),
            this.aggregateAdminLogs()
        ]);
    },

    // ────────────────────────────────────────────────────────────────
    // 검색 / 페이징
    // ────────────────────────────────────────────────────────────────

    async searchAdminLogs() {
        if (this.adminLogs.forbidden) return;
        this.adminLogs.loading = true;
        this.adminLogs.nextSearchAfter = null;
        try {
            const params = this._buildSearchParams();
            const result = await API.searchAdminLogs(this.adminLogs.domain, params);
            this.adminLogs.list = result.items || [];
            this.adminLogs.total = result.total || 0;
            this.adminLogs.nextSearchAfter = result.nextSearchAfter || null;
        } catch (e) {
            if (this._isForbidden(e)) {
                this.adminLogs.forbidden = true;
                this._clearAdminResults();
            } else {
                console.error('관리자 로그 검색 실패:', e);
                this._clearAdminResults();
            }
        } finally {
            this.adminLogs.loading = false;
        }
    },

    async loadMoreAdminLogs() {
        if (!this.adminLogs.nextSearchAfter || this.adminLogs.loading) return;
        this.adminLogs.loading = true;
        try {
            const params = this._buildSearchParams();
            params.searchAfter = this.adminLogs.nextSearchAfter.map(v => String(v));
            const result = await API.searchAdminLogs(this.adminLogs.domain, params);
            this.adminLogs.list.push(...(result.items || []));
            this.adminLogs.nextSearchAfter = result.nextSearchAfter || null;
        } catch (e) {
            console.error('관리자 로그 추가 로드 실패:', e);
        } finally {
            this.adminLogs.loading = false;
        }
    },

    // ────────────────────────────────────────────────────────────────
    // 집계 차트
    // ────────────────────────────────────────────────────────────────

    async aggregateAdminLogs() {
        if (this.adminLogs.forbidden) return;
        try {
            const result = await API.aggregateAdminLogs(this.adminLogs.domain, {
                from: this._toIso(this.adminLogs.filter.from),
                to: this._toIso(this.adminLogs.filter.to)
            });
            this.adminLogs.aggregation = {
                interval: result.interval || 'day',
                counts: result.counts || []
            };
            this.$nextTick(() => this._renderAdminChart());
        } catch (e) {
            if (this._isForbidden(e)) {
                this.adminLogs.forbidden = true;
            } else {
                console.error('관리자 로그 집계 실패:', e);
            }
        }
    },

    _renderAdminChart() {
        const canvas = document.getElementById('admin-logs-chart');
        if (!canvas) return;
        if (this.adminLogs._chartInstance) {
            this.adminLogs._chartInstance.destroy();
            this.adminLogs._chartInstance = null;
        }
        const counts = this.adminLogs.aggregation.counts || [];
        const labels = counts.map(c => c.date);
        const data = counts.map(c => c.count);
        this.adminLogs._chartInstance = new Chart(canvas, {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label: this.adminLogs.domain + ' logs',
                    data,
                    backgroundColor: '#3b82f6'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                onClick: (evt, elements) => {
                    if (!elements || elements.length === 0) return;
                    const idx = elements[0].index;
                    const dateStr = labels[idx];
                    if (!dateStr) return;
                    // 해당 일자 0시 ~ 다음 일자 0시 (KST)
                    const from = new Date(dateStr + 'T00:00:00');
                    const to = new Date(from.getTime() + 24 * 60 * 60 * 1000);
                    this.adminLogs.filter.from = this._toDatetimeLocal(from);
                    this.adminLogs.filter.to = this._toDatetimeLocal(to);
                    this.searchAdminLogs();
                }
            }
        });
    },

    // ────────────────────────────────────────────────────────────────
    // 도메인 탭 전환
    // ────────────────────────────────────────────────────────────────

    async switchAdminLogDomain(domain) {
        if (domain === this.adminLogs.domain) return;
        this.adminLogs.domain = domain;
        await Promise.all([
            this.searchAdminLogs(),
            this.aggregateAdminLogs()
        ]);
    },

    // ────────────────────────────────────────────────────────────────
    // 필터 debounce — 키워드/userId 입력 시 300ms 후 재검색
    // ────────────────────────────────────────────────────────────────

    debounceAdminLogSearch() {
        if (this.adminLogs._debounceTimer) {
            clearTimeout(this.adminLogs._debounceTimer);
        }
        this.adminLogs._debounceTimer = setTimeout(() => {
            this.searchAdminLogs();
            this.aggregateAdminLogs();
        }, 300);
    },

    // ────────────────────────────────────────────────────────────────
    // 다운로드
    // ────────────────────────────────────────────────────────────────

    async downloadAdminLogs() {
        try {
            const params = this._buildSearchParams();
            delete params.size;
            await API.downloadAdminLogs(this.adminLogs.domain, params);
        } catch (e) {
            if (this._isForbidden(e)) {
                this.adminLogs.forbidden = true;
            } else {
                console.error('로그 다운로드 실패:', e);
                alert('다운로드에 실패했습니다. 잠시 후 다시 시도해 주세요.');
            }
        }
    },

    // ────────────────────────────────────────────────────────────────
    // payload 모달
    // ────────────────────────────────────────────────────────────────

    openAdminLogModal(item) {
        let json = '';
        try {
            json = JSON.stringify(item.payload || {}, null, 2);
        } catch (e) {
            json = String(item.payload);
        }
        const PREVIEW_LIMIT = 2 * 1024;
        const truncated = json.length > PREVIEW_LIMIT;
        this.adminLogs.modal = {
            open: true,
            item,
            preview: truncated ? json.slice(0, PREVIEW_LIMIT) : json,
            truncatedNotice: truncated
        };
    },

    closeAdminLogModal() {
        this.adminLogs.modal.open = false;
        this.adminLogs.modal.item = null;
        this.adminLogs.modal.preview = '';
        this.adminLogs.modal.truncatedNotice = false;
    },

    downloadAdminLogModalPayload() {
        const item = this.adminLogs.modal.item;
        if (!item) return;
        const json = JSON.stringify(item, null, 2);
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `log-${item.id || 'item'}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    },

    // ────────────────────────────────────────────────────────────────
    // 디스크 사용량 배지
    // ────────────────────────────────────────────────────────────────

    async _fetchAdminDiskUsage() {
        try {
            const result = await API.getAdminLogsDiskUsage();
            this.adminLogs.diskUsage = result || { totalBytes: 0, totalDocs: 0, indices: [] };
        } catch (e) {
            if (this._isForbidden(e)) {
                this.adminLogs.forbidden = true;
            } else {
                console.error('디스크 사용량 조회 실패:', e);
            }
        }
    },

    formatAdminLogBytes(bytes) {
        if (!bytes || bytes <= 0) return '0 B';
        const units = ['B', 'KB', 'MB', 'GB', 'TB'];
        let i = 0;
        let n = bytes;
        while (n >= 1024 && i < units.length - 1) {
            n /= 1024;
            i++;
        }
        return `${n.toFixed(1)} ${units[i]}`;
    },

    formatAdminLogTimestamp(instant) {
        if (!instant) return '';
        try {
            const d = new Date(instant);
            return d.toLocaleString('ko-KR', { hour12: false });
        } catch (e) {
            return String(instant);
        }
    },

    // ────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ────────────────────────────────────────────────────────────────

    _buildSearchParams() {
        const f = this.adminLogs.filter;
        return {
            from: this._toIso(f.from),
            to: this._toIso(f.to),
            userId: f.userId ? Number(f.userId) : '',
            q: f.q,
            status: f.status ? Number(f.status) : '',
            exceptionClass: f.exceptionClass,
            size: 20
        };
    },

    _clearAdminResults() {
        this.adminLogs.list = [];
        this.adminLogs.total = 0;
        this.adminLogs.nextSearchAfter = null;
    },

    _isForbidden(error) {
        const msg = error && error.message ? String(error.message) : '';
        return msg.includes('403') || msg === 'FORBIDDEN';
    },

    /**
     * input[type=datetime-local] 값 ↔ ISO 8601(Z) 변환.
     * datetime-local 값은 초/타임존 없음 — 브라우저 로컬 시각으로 해석.
     */
    _toDatetimeLocal(date) {
        const pad = n => String(n).padStart(2, '0');
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
               `T${pad(date.getHours())}:${pad(date.getMinutes())}`;
    },

    _toIso(localValue) {
        if (!localValue) return '';
        const d = new Date(localValue);
        if (isNaN(d.getTime())) return '';
        return d.toISOString();
    }
};