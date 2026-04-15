/**
 * GlobalComponent - 글로벌 경제지표
 *
 * 소유 프로퍼티: globalData
 * 메서드: loadGlobalCategories, selectGlobalCategory, selectGlobalIndicator,
 *         getCurrentCategoryIndicators, getChangeInfo, getGlobalCategoryLabel
 */
const GLOBAL_INDICATOR_DESCRIPTIONS = {
    // 무역/GDP
    'BALANCE_OF_TRADE': '양수면 수출 우위(무역흑자), 음수면 수입 초과(무역적자)',
    'GDP_ANNUAL_GROWTH_RATE': '높을수록 경기 확장. 마이너스면 경기 침체 신호',
    'GDP': '국가 경제 규모의 절대적 크기. 전기 대비 증감으로 성장 판단',
    'GDP_FROM_AGRICULTURE': '농업 부문 생산. 개도국일수록 비중이 높음',
    'GDP_FROM_CONSTRUCTION': '건설 경기 활성화 지표. 하락 시 투자 위축 신호',
    'GDP_FROM_MANUFACTURING': '제조업 부문 생산. 산업국 핵심 성장 동력',
    'GDP_FROM_MINING': '자원 수출국의 핵심 지표. 원자재 가격과 연동',
    'GDP_FROM_PUBLIC_ADMINISTRATION': '정부 지출 규모. 재정정책 확장/긴축 판단',
    'GDP_FROM_SERVICES': '선진국 GDP의 60~80% 차지. 내수 경기 바로미터',
    'GDP_FROM_TRANSPORT': '물류/교역 활동 수준. 경기 선행 지표 성격',
    'GDP_FROM_UTILITIES': '전기/가스/수도. 안정적이나 산업활동 변동 반영',
    // 고용
    'UNEMPLOYMENT_RATE': '낮을수록 노동시장 호조. 과도하게 낮으면 임금 인플레이션 우려',
    'WAGE_GROWTH': '높으면 소비 여력 확대, 동시에 기업 비용 증가·인플레 압력',
    'EMPLOYMENT_CHANGE': '양수면 일자리 증가. 경기 회복/확장기에 증가',
    // 물가/소비
    'PRODUCER_PRICES': '기업 투입 비용. 상승 시 소비자 물가 전이 가능성',
    'CONSUMER_PRICE_INDEX': '높으면 인플레이션 압력. 중앙은행 금리 인상 근거',
    'CORE_CONSUMER_PRICES': '식품·에너지 제외. 기조적 인플레이션 추세 파악용',
    'RETAIL_SALES_MOM': '양수면 소비 확대. 내수 경기 체감 지표',
    'INFLATION_RATE': '높으면 화폐가치 하락. 중앙은행 금리 인상 압력',
    'INFLATION_EXPECTATIONS': '미래 물가 전망. 높으면 선제적 긴축 가능성',
    'INFLATION_RATE_MOM': '전월 대비 물가 변동. 단기 인플레이션 추세 파악용',
    // 금리/금융
    'INTEREST_RATE': '높으면 긴축(경기 과열 억제), 낮으면 완화(경기 부양)',
    'CENTRAL_BANK_BALANCE_SHEET': '증가면 양적완화(유동성 공급), 감소면 긴축(유동성 회수)',
    'GOLD_RESERVES': '외환보유 안전자산. 증가 시 불확실성 대비 강화 신호',
    'LENDING_RATE': '은행 대출 기준금리. 높으면 차입 비용 증가, 경기 위축 요인',
    'INTERBANK_RATE': '은행 간 단기 자금 거래 금리. 유동성 상황 직접 반영',
    // 신뢰/선행 지수
    'LEADING_ECONOMIC_INDEX': '100 이상이면 향후 경기 확장 예상, 미만이면 둔화 예상',
    'CONSUMER_CONFIDENCE': '높으면 소비 심리 낙관. 소비 지출 확대 기대',
    'BUSINESS_CONFIDENCE': '높으면 기업 투자·고용 확대 의향. 경기 확장 신호',
    // PMI/산업
    'MANUFACTURING_PMI': '50 이상이면 제조업 확장, 미만이면 위축',
    'SERVICES_PMI': '50 이상이면 서비스업 확장, 미만이면 위축',
    'INDUSTRIAL_PRODUCTION_MOM': '양수면 산업활동 증가. 제조업 경기 직접 반영',
    // 재정/국가
    'GOVERNMENT_DEBT': 'GDP 대비 비율로 판단. 높을수록 재정 건전성 우려',
    'GOVERNMENT_REVENUES': '세수 규모. 경기 호황 시 증가, 침체 시 감소',
    'FISCAL_EXPENDITURE': '높으면 재정정책 확장. GDP 대비 비율로 판단',
    'CURRENT_ACCOUNT': '양수면 해외 소득 초과(흑자), 음수면 적자',
    'FOREIGN_EXCHANGE_RESERVES': '외화 자산 보유량. 높을수록 통화 안정성·대외 신인도 강화',
    // 통화량
    'MONEY_SUPPLY_M0': '현금 통화. 중앙은행이 직접 공급한 본원통화',
    'MONEY_SUPPLY_M1': '협의 통화. 현금 + 요구불예금. 즉시 사용 가능한 유동성',
    'MONEY_SUPPLY_M2': '광의 통화. M1 + 정기예금 등. 실질적 유동성 지표',
    'MONEY_SUPPLY_M3': '총유동성. M2 + 비은행 금융상품. 전체 통화량 파악용',
};

const COUNTRY_COLORS = [
    '#3B82F6', '#EF4444', '#10B981', '#F59E0B', '#8B5CF6',
    '#EC4899', '#06B6D4', '#F97316', '#84CC16', '#6366F1',
    '#14B8A6', '#E11D48', '#0EA5E9', '#A855F7', '#D97706',
    '#059669', '#DC2626', '#7C3AED', '#2563EB', '#CA8A04'
];

const GlobalComponent = {
    globalData: {
        categories: [],
        selectedCategory: null,
        selectedIndicator: null,
        indicatorData: null,
        loading: false,
        viewMode: 'table',
        historyData: null,
        historyLoading: false,
        _requestGeneration: 0,
        _historyGeneration: 0,
        _historyCache: {}
    },

    initGlobalCharts() {
        Object.defineProperty(this.globalData, '_chartInstances', {
            value: new Map(), writable: true, enumerable: false
        });
    },

    async loadGlobalCategories() {
        try {
            this.globalData.categories = await API.getGlobalCategories() || [];
            if (this.globalData.categories.length > 0 && !this.globalData.selectedCategory) {
                this.globalData.selectedCategory = this.globalData.categories[0].key;
                const firstIndicators = this.globalData.categories[0].indicators;
                if (firstIndicators && firstIndicators.length > 0) {
                    await this.selectGlobalIndicator(firstIndicators[0].key);
                }
            }
        } catch (e) {
            console.error('글로벌 카테고리 로드 실패:', e);
            this.globalData.categories = [];
        }
    },

    async selectGlobalCategory(categoryKey) {
        this.globalData.selectedCategory = categoryKey;
        this.globalData.selectedIndicator = null;
        this.globalData.indicatorData = null;
        this.destroyGlobalCharts();
        const cat = this.globalData.categories.find((c) => c.key === categoryKey);
        if (cat && cat.indicators && cat.indicators.length > 0) {
            await this.selectGlobalIndicator(cat.indicators[0].key);
        }
    },

    async selectGlobalIndicator(indicatorKey) {
        this.globalData.selectedIndicator = indicatorKey;

        const thisGeneration = ++this.globalData._requestGeneration;
        this.globalData.loading = true;

        try {
            const result = await API.getGlobalIndicator(indicatorKey);
            if (thisGeneration !== this.globalData._requestGeneration) return;
            this.globalData.indicatorData = result;
        } catch (e) {
            if (thisGeneration !== this.globalData._requestGeneration) return;
            console.error('글로벌 지표 로드 실패:', e);
            this.globalData.indicatorData = null;
        } finally {
            if (thisGeneration === this.globalData._requestGeneration) {
                this.globalData.loading = false;
            }
        }

        if (this.globalData.viewMode === 'chart') {
            await this.loadGlobalHistory();
        }
    },

    async switchGlobalViewMode(mode) {
        this.globalData.viewMode = mode;
        if (mode === 'chart') {
            await this.loadGlobalHistory();
        } else {
            this.destroyGlobalCharts();
        }
    },

    async loadGlobalHistory() {
        if (!this.globalData.selectedIndicator) return;

        const cached = this.globalData._historyCache[this.globalData.selectedIndicator];
        if (cached) {
            this.globalData.historyData = cached;
            this.$nextTick(() => this.renderGlobalCharts());
            return;
        }

        const thisGeneration = ++this.globalData._historyGeneration;
        this.globalData.historyLoading = true;

        try {
            const result = await API.getGlobalIndicatorHistory(this.globalData.selectedIndicator);
            if (thisGeneration !== this.globalData._historyGeneration) return;
            this.globalData.historyData = result;

            // LRU 캐시: 최대 5개 지표타입
            const cacheKeys = Object.keys(this.globalData._historyCache);
            if (cacheKeys.length >= 5) {
                delete this.globalData._historyCache[cacheKeys[0]];
            }
            this.globalData._historyCache[this.globalData.selectedIndicator] = result;

            this.$nextTick(() => this.renderGlobalCharts());
        } catch (e) {
            if (thisGeneration !== this.globalData._historyGeneration) return;
            console.error('글로벌 히스토리 로드 실패:', e);
            this.globalData.historyData = null;
        } finally {
            if (thisGeneration === this.globalData._historyGeneration) {
                this.globalData.historyLoading = false;
            }
        }
    },

    renderGlobalCharts() {
        this.destroyGlobalCharts();
        if (!this.globalData._chartInstances) this.initGlobalCharts();

        const data = this.globalData.historyData;
        if (!data || !data.countries || data.countries.length === 0) return;

        // 모든 국가의 cycle을 합쳐서 유니크한 라벨 생성
        const allCycles = new Set();
        data.countries.forEach(country => {
            country.history.forEach(h => allCycles.add(h.cycle));
        });
        const labels = [...allCycles];

        const canvasId = 'global-history-chart';
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;

        const datasets = data.countries.map((country, idx) => {
            // cycle → dataValue 매핑
            const valueMap = {};
            country.history.forEach(h => { valueMap[h.cycle] = h.dataValue; });

            return {
                label: country.countryName,
                data: labels.map(cycle => {
                    const raw = valueMap[cycle];
                    if (!raw) return null;
                    const v = parseFloat(raw.replace(/,/g, ''));
                    return isNaN(v) ? null : v;
                }),
                borderColor: COUNTRY_COLORS[idx % COUNTRY_COLORS.length],
                borderWidth: 1.5,
                pointRadius: 0,
                pointHoverRadius: 4,
                tension: 0.3,
                fill: false,
                spanGaps: false
            };
        });

        const chart = new Chart(canvas, {
            type: 'line',
            data: { labels, datasets },
            options: {
                animation: false,
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true,
                        position: 'bottom',
                        labels: {
                            boxWidth: 12,
                            padding: 8,
                            font: { size: 10 }
                        }
                    },
                    tooltip: {
                        enabled: true,
                        mode: 'index',
                        intersect: false,
                        callbacks: {
                            label: (ctx) => {
                                const val = ctx.parsed.y;
                                return val !== null
                                    ? `${ctx.dataset.label}: ${val} ${data.unit || ''}`
                                    : `${ctx.dataset.label}: 데이터 없음`;
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        display: true,
                        ticks: { maxRotation: 45, autoSkip: true, maxTicksLimit: 10, font: { size: 10 } },
                        grid: { display: false }
                    },
                    y: {
                        display: true,
                        ticks: { maxTicksLimit: 6, font: { size: 10 } }
                    }
                }
            }
        });
        this.globalData._chartInstances.set(canvasId, chart);
    },

    destroyGlobalCharts() {
        if (!this.globalData._chartInstances) return;
        this.globalData._chartInstances.forEach(chart => chart.destroy());
        this.globalData._chartInstances.clear();
    },

    getCurrentCategoryIndicators() {
        if (!this.globalData.selectedCategory) return [];
        const cat = this.globalData.categories.find((c) => c.key === this.globalData.selectedCategory);
        return cat ? cat.indicators || [] : [];
    },

    getChangeInfo(current, previous) {
        return Format.change(current, previous);
    },

    getGlobalCategoryLabel(key) {
        const cat = this.globalData.categories.find((c) => c.key === key);
        return cat ? cat.displayName : key;
    },

    getGlobalIndicatorDescription(indicatorKey) {
        return GLOBAL_INDICATOR_DESCRIPTIONS[indicatorKey] || '';
    }
};