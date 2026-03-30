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
    // 금리/금융
    'INTEREST_RATE': '높으면 긴축(경기 과열 억제), 낮으면 완화(경기 부양)',
    'CENTRAL_BANK_BALANCE_SHEET': '증가면 양적완화(유동성 공급), 감소면 긴축(유동성 회수)',
    'GOLD_RESERVES': '외환보유 안전자산. 증가 시 불확실성 대비 강화 신호',
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
    'MILITARY_EXPENDITURE': 'GDP 대비 비율로 판단. 지정학적 리스크 간접 지표',
};

const GlobalComponent = {
    globalData: {
        categories: [],
        selectedCategory: null,
        selectedIndicator: null,
        indicatorData: null,
        loading: false,
        _requestGeneration: 0
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