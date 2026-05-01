/**
 * FavoriteComponent - 관심 지표 상태 관리
 * 소유 프로퍼티: favorites
 */
const FavoriteComponent = {
    favorites: {
        list: [],
        loading: false,
    },

    initFavorites() {
        Object.defineProperty(this.favorites, '_set', {
            value: new Set(), writable: true, enumerable: false
        });
        Object.defineProperty(this.favorites, '_togglePending', {
            value: false, writable: true, enumerable: false
        });
    },

    async loadFavorites() {
        if (!this.checkLoggedIn()) return;
        try {
            const result = await API.getFavorites();
            this.favorites.list = result || [];
            if (!this.favorites._set) this.initFavorites();
            this.favorites._set.clear();
            this.favorites.list.forEach(f => {
                this.favorites._set.add(f.sourceType + '::' + f.indicatorCode);
            });
        } catch (e) {
            console.error('관심 지표 로드 실패:', e);
        }
    },

    isFavorited(sourceType, indicatorCode) {
        if (!this.favorites._set) return false;
        return this.favorites._set.has(sourceType + '::' + indicatorCode);
    },

    async toggleFavorite(sourceType, indicatorCode) {
        if (!this.checkLoggedIn()) return;
        if (!this.favorites._set) this.initFavorites();
        if (this.favorites._togglePending) return;
        this.favorites._togglePending = true;
        setTimeout(() => { this.favorites._togglePending = false; }, 300);

        const key = sourceType + '::' + indicatorCode;
        const wasFavorited = this.favorites._set.has(key);

        // Optimistic UI
        if (wasFavorited) {
            this.favorites._set.delete(key);
            this.favorites.list = this.favorites.list.filter(f =>
                !(f.sourceType === sourceType && f.indicatorCode === indicatorCode));
        } else {
            this.favorites._set.add(key);
            this.favorites.list.push({ sourceType, indicatorCode });
        }

        try {
            if (wasFavorited) {
                await API.deleteFavorite(sourceType, indicatorCode);
            } else {
                await API.addFavorite(sourceType, indicatorCode);
            }
        } catch (e) {
            // Rollback
            if (wasFavorited) {
                this.favorites._set.add(key);
                this.favorites.list.push({ sourceType, indicatorCode });
            } else {
                this.favorites._set.delete(key);
                this.favorites.list = this.favorites.list.filter(f =>
                    !(f.sourceType === sourceType && f.indicatorCode === indicatorCode));
            }
            console.error('관심 지표 토글 실패:', e);
        }
    },

    /**
     * ECOS 지표의 indicatorCode 생성 (className::keystatName)
     */
    ecosIndicatorCode(indicator) {
        return indicator.className + '::' + indicator.keystatName;
    },

    /**
     * Global 지표의 indicatorCode 생성 (countryName::indicatorType)
     */
    globalIndicatorCode(countryName, indicatorType) {
        return countryName + '::' + indicatorType;
    },

    /**
     * 수평 스크롤
     */
    scrollFavorites(direction, containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;
        container.scrollBy({ left: direction * 320, behavior: 'smooth' });
    },

    /**
     * 글로벌 관심 지표 카드 단일 재조회.
     * 동일 카드 연타는 refreshing 플래그로 방지.
     * 성공 응답은 동일 indicatorType 을 가진 모든 카드(여러 국가)의 값을 갱신한다.
     */
    async refreshGlobal(card) {
        if (!card || card.refreshing) return;
        if (card.failed && !card.refreshable) return;

        card.refreshing = true;
        try {
            const response = await API.refreshGlobalIndicator(card.indicatorType);
            const items = (response && response.items) || [];
            const itemMap = {};
            items.forEach(item => { itemMap[item.indicatorCode] = item; });

            const globals = this.homeSummary?.enrichedFavorites?.global;
            if (Array.isArray(globals)) {
                for (let i = 0; i < globals.length; i++) {
                    const fresh = itemMap[globals[i].indicatorCode];
                    if (fresh) {
                        globals[i] = { ...fresh, refreshing: false };
                    }
                }
            }
        } catch (e) {
            console.error('관심 지표 재조회 실패:', e);
            card.refreshing = false;
            alert('재조회에 실패했어요. 잠시 후 다시 시도해주세요');
        }
    },

    /**
     * 카드 failureReason 을 사람 친화 메시지로 변환.
     */
    globalFailureMessage(card) {
        if (!card || !card.failed) return '';
        switch (card.failureReason) {
            case 'FETCH': return '실시간 조회 실패 (네트워크). 잠시 후 재조회해주세요';
            case 'PARSE': return '데이터 구조 변경으로 조회 실패. 관리자 확인이 필요해요';
            case 'INVALID_CODE': return '알 수 없는 지표 코드입니다. 관심 지표에서 제거해주세요';
            default: return '실시간 조회 실패. 잠시 후 재조회해주세요';
        }
    },

    /**
     * 관심 지표 표시 모드 토글 (INDICATOR ↔ GRAPH).
     * 토글 후 enriched 재조회로 history 동기화.
     */
    async toggleDisplayMode(card, sourceType) {
        if (!this.checkLoggedIn()) return;
        if (!card || card._displayModePending) return;
        card._displayModePending = true;

        const oldMode = card.displayMode || 'INDICATOR';
        const newMode = oldMode === 'GRAPH' ? 'INDICATOR' : 'GRAPH';

        try {
            await API.changeFavoriteDisplayMode(sourceType, card.indicatorCode, newMode);

            const fresh = await API.getEnrichedFavorites();
            if (fresh && this.homeSummary) {
                this.homeSummary.enrichedFavorites = fresh;
            }
        } catch (e) {
            console.error('표시 모드 변경 실패:', e);
            alert('표시 모드 변경에 실패했어요. 잠시 후 다시 시도해주세요');
        } finally {
            card._displayModePending = false;
        }
    },

    /**
     * 카드 안 canvas 에 시계열 라인 차트 렌더.
     * 동일 indicatorCode 의 기존 차트는 destroy 후 재생성한다.
     */
    renderFavoriteChart(canvasEl, history, indicatorCode) {
        if (!canvasEl) return;
        if (!this.favorites._charts) this.favorites._charts = {};

        const prev = this.favorites._charts[indicatorCode];
        if (prev) {
            try { prev.destroy(); } catch (_) { /* noop */ }
            delete this.favorites._charts[indicatorCode];
        }

        if (!history || history.length === 0) return;

        const labels = history.map(p => p.snapshotDate);
        const values = history.map(p => {
            const num = parseFloat(String(p.dataValue || '').replace(/,/g, ''));
            return Number.isFinite(num) ? num : null;
        });

        const chart = new Chart(canvasEl, {
            type: 'line',
            data: {
                labels,
                datasets: [{
                    data: values,
                    borderColor: '#3b82f6',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    borderWidth: 1.5,
                    tension: 0.3,
                    pointRadius: 0,
                    spanGaps: true,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            title: (items) => items[0]?.label || '',
                            label: (item) => item.formattedValue
                        }
                    }
                },
                scales: {
                    x: { display: false },
                    y: { display: false }
                }
            }
        });

        this.favorites._charts[indicatorCode] = chart;
    }
};