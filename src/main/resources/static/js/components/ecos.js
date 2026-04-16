/**
 * EcosComponent - ECOS 카테고리/지표, 스프레드 계산 (11종)
 * 소유 프로퍼티: ecos
 */
const EcosComponent = {
    ecos: {
        categories: [],
        selectedCategory: null,
        indicators: [],
        loading: false,
        viewMode: 'table',
        historyData: [],
        historyLoading: false,
        _requestGeneration: 0,
        _historyGeneration: 0,
        _indicatorMap: {},
        _historyCache: {},
        _tooltipText: '',
        _rawTooltipText: '',
    },

    initEcosCharts() {
        Object.defineProperty(this.ecos, '_chartInstances', {
            value: new Map(), writable: true, enumerable: false
        });
    },

    async loadEcosCategories() {
        try {
            this.ecos.categories = await API.getEcosCategories() || [];
            if (this.ecos.categories.length > 0 && !this.ecos.selectedCategory) {
                this.ecos.selectedCategory = this.ecos.categories[0].name;
                await this.loadEcosIndicators();
            }
        } catch (e) {
            console.error('ECOS 카테고리 로드 실패:', e);
            this.ecos.categories = [];
        }
    },

    async loadEcosIndicators() {
        if (!this.ecos.selectedCategory) return;

        const thisGeneration = ++this.ecos._requestGeneration;
        this.ecos.loading = true;

        try {
            const result = await API.getEcosIndicators(this.ecos.selectedCategory) || [];
            if (thisGeneration !== this.ecos._requestGeneration) return;
            this.ecos.indicators = result;
            const map = {};
            result.forEach(i => { map[i.keystatName] = i; });
            this.ecos._indicatorMap = map;
        } catch (e) {
            if (thisGeneration !== this.ecos._requestGeneration) return;
            console.error('ECOS 지표 로드 실패:', e);
            this.ecos.indicators = [];
        } finally {
            if (thisGeneration === this.ecos._requestGeneration) {
                this.ecos.loading = false;
            }
        }
    },

    async selectEcosCategory(categoryName) {
        this.ecos.selectedCategory = categoryName;
        this.ecos._tooltipText = '';
        this.ecos._rawTooltipText = '';
        await this.loadEcosIndicators();
        if (this.ecos.viewMode === 'chart') {
            await this.loadEcosHistory();
        }
        // ECONOMIC 모드로 챗봇이 열려 있으면 대화 초기화 + 카테고리 갱신
        if (this.chat.chatMode === 'ECONOMIC' && this.chat.isOpen) {
            if (this.chat._abortController) {
                this.chat._abortController.abort();
            }
            this.chat.messages = [];
            this.chat.indicatorCategory = categoryName;
            this.chat.isLoading = false;
        }
    },

    async switchEcosViewMode(mode) {
        this.ecos.viewMode = mode;
        if (mode === 'chart') {
            await this.loadEcosHistory();
        } else {
            this.destroyEcosCharts();
        }
    },

    async loadEcosHistory() {
        if (!this.ecos.selectedCategory) return;

        const cached = this.ecos._historyCache[this.ecos.selectedCategory];
        if (cached) {
            this.ecos.historyData = cached;
            this.$nextTick(() => this.renderEcosCharts());
            return;
        }

        const thisGeneration = ++this.ecos._historyGeneration;
        this.ecos.historyLoading = true;

        try {
            const result = await API.getEcosIndicatorHistory(this.ecos.selectedCategory) || [];
            if (thisGeneration !== this.ecos._historyGeneration) return;
            this.ecos.historyData = result;

            // LRU 캐시: 최대 3개 카테고리
            const cacheKeys = Object.keys(this.ecos._historyCache);
            if (cacheKeys.length >= 3) {
                delete this.ecos._historyCache[cacheKeys[0]];
            }
            this.ecos._historyCache[this.ecos.selectedCategory] = result;

            this.$nextTick(() => this.renderEcosCharts());
        } catch (e) {
            if (thisGeneration !== this.ecos._historyGeneration) return;
            console.error('ECOS 히스토리 로드 실패:', e);
            this.ecos.historyData = [];
        } finally {
            if (thisGeneration === this.ecos._historyGeneration) {
                this.ecos.historyLoading = false;
            }
        }
    },

    renderEcosCharts() {
        this.destroyEcosCharts();
        if (!this.ecos._chartInstances) this.initEcosCharts();

        this.ecos.historyData.forEach(indicator => {
            if (indicator.history.length < 2) return;

            const canvasId = 'ecos-chart-' + indicator.keystatName.replace(/[^a-zA-Z0-9가-힣]/g, '_');
            const canvas = document.getElementById(canvasId);
            if (!canvas) return;

            const chart = new Chart(canvas, {
                type: 'line',
                data: {
                    labels: indicator.history.map(h => h.cycle),
                    datasets: [{
                        data: indicator.history.map(h => {
                            if (!h.dataValue) return null;
                            const v = parseFloat(h.dataValue.replace(/,/g, ''));
                            return isNaN(v) ? null : v;
                        }),
                        borderColor: '#3B82F6',
                        borderWidth: 1.5,
                        pointRadius: 0,
                        pointHoverRadius: 4,
                        tension: 0.3,
                        fill: false,
                        spanGaps: false,
                    }]
                },
                options: {
                    animation: false,
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            enabled: true,
                            mode: 'index',
                            intersect: false,
                            callbacks: {
                                label: (ctx) => {
                                    const val = ctx.parsed.y;
                                    return val !== null ? `${val} ${indicator.unitName || ''}` : '데이터 없음';
                                }
                            }
                        },
                    },
                    scales: {
                        x: {
                            display: true,
                            ticks: { maxRotation: 45, autoSkip: true, maxTicksLimit: 8, font: { size: 10 } },
                            grid: { display: false }
                        },
                        y: {
                            display: true,
                            ticks: { maxTicksLimit: 5, font: { size: 10 } }
                        }
                    }
                }
            });
            this.ecos._chartInstances.set(canvasId, chart);
        });
    },

    destroyEcosCharts() {
        if (!this.ecos._chartInstances) return;
        this.ecos._chartInstances.forEach(chart => chart.destroy());
        this.ecos._chartInstances.clear();
    },

    getEcosSortedIndicators() {
        return [...this.ecos.indicators].sort((a, b) =>
            a.className.localeCompare(b.className)
        );
    },

    getInterestRateSpreads() {
        const find = (name) => {
            const ind = this.ecos._indicatorMap[name];
            return ind ? parseFloat(ind.dataValue) : null;
        };

        const calc = (a, b) => {
            if (a === null || b === null || isNaN(a) || isNaN(b)) return null;
            return Math.round((a - b) * 1000) / 1000;
        };

        const bond5 = find('국고채수익률(5년)');
        const bond3 = find('국고채수익률(3년)');
        const cd91 = find('CD수익률(91일)');
        const corpBond = find('회사채수익률(3년,AA-)');
        const loanRate = find('예금은행 대출금리');
        const depositRate = find('예금은행 수신금리');
        const callRate = find('콜금리(익일물)');
        const baseRate = find('한국은행 기준금리');

        return [
            { name: '장단기 금리차', value: calc(bond5, cd91), desc: '시장 구조 판단', sub: '국고채수익률(5년) − CD수익률(91일)',
              description: '양수(+)면 정상적인 우상향 금리 곡선. 0에 가까워지거나 음수(−)면 장단기 금리 역전으로 경기침체 신호' },
            { name: '중기-단기 금리차', value: calc(bond3, cd91), desc: '금리 기대 방향', sub: '국고채수익률(3년) − CD수익률(91일)',
              description: '양수(+)가 클수록 시장이 향후 금리 인상을 예상. 줄어들면 금리 인하 기대가 반영된 것' },
            { name: '장기 금리 기울기', value: calc(bond5, bond3), desc: '장기 기대 (인플레/성장)', sub: '국고채수익률(5년) − 국고채수익률(3년)',
              description: '양수(+)면 장기 인플레이션이나 경제성장 기대가 있다는 의미. 축소되면 장기 성장 기대가 약해지는 것' },
            { name: '신용 스프레드', value: calc(corpBond, bond3), desc: '시장 리스크 수준', sub: '회사채수익률(3년,AA-) − 국고채수익률(3년)',
              description: '기업 채권과 국채의 금리 차이. 벌어지면 시장이 기업 부도 위험을 높게 보는 것, 좁으면 안정적' },
            { name: '예대금리차', value: calc(loanRate, depositRate), desc: '금융 부담 / 은행 구조', sub: '예금은행 대출금리 − 예금은행 수신금리',
              description: '은행이 예금자에게 주는 이자와 대출자에게 받는 이자의 차이. 클수록 대출자 부담이 크고 은행 수익성이 높음' },
            { name: '단기 vs 기준금리', value: calc(callRate, baseRate), desc: '유동성 상태', sub: '콜금리(익일물) − 한국은행 기준금리',
              description: '콜금리가 기준금리보다 높으면 시중 자금이 부족한 상태, 낮으면 유동성이 풍부한 상태' },
        ];
    },

    getMoneyFinanceSpreads() {
        const find = (name) => {
            const ind = this.ecos._indicatorMap[name];
            return ind ? parseFloat(ind.dataValue) : null;
        };

        const ratio = (a, b, decimals = 2) => {
            if (a === null || b === null || isNaN(a) || isNaN(b) || b === 0) return null;
            return Math.round((a / b) * Math.pow(10, decimals)) / Math.pow(10, decimals);
        };

        const m1 = find('M1(협의통화, 평잔)');
        const m2 = find('M2(광의통화, 평잔)');
        const lf = find('Lf(평잔)');
        const l = find('L(말잔)');
        const deposit = find('예금은행총예금(말잔)');
        const loan = find('예금은행대출금(말잔)');
        const houseCredit = find('가계신용');
        const delinquency = find('가계대출연체율');

        const grade = (value, ranges) => {
            if (value === null) return { label: '-', color: 'text-gray-400', levels: ranges };
            let result = ranges[ranges.length - 1];
            for (const r of ranges) {
                if (value < r.max) { result = r; break; }
            }
            return { label: result.label, color: result.color, levels: ranges };
        };

        return [
            { name: 'M2/M1 비율', value: ratio(m2, m1), unit: '배', sub: 'M2 ÷ M1', desc: '유동성 구조',
              ref: grade(ratio(m2, m1), [
                  { max: 2.5, label: '보수적', color: 'text-blue-500' },
                  { min: 2.5, max: 3.5, label: '정상', color: 'text-green-600' },
                  { min: 3.5, max: Infinity, label: '과잉', color: 'text-red-500' }]),
              description: '1에 가까우면 돈이 바로 쓸 수 있는 상태로 많이 있다는 뜻. 높을수록 정기예금 등 묶인 돈이 많아 실제 소비·투자로 바로 이어지기 어려움' },
            { name: 'Lf/M2 비율', value: ratio(lf, m2), unit: '배', sub: 'Lf ÷ M2', desc: '비은행 유동성 확장',
              ref: grade(ratio(lf, m2), [
                  { max: 1.3, label: '은행 중심', color: 'text-blue-500' },
                  { min: 1.3, max: 1.6, label: '정상', color: 'text-green-600' },
                  { min: 1.6, max: Infinity, label: '비은행 확장', color: 'text-red-500' }]),
              description: '1보다 클수록 보험·증권 등 비은행 금융기관에 돈이 많이 퍼져있다는 뜻. 빠르게 올라가면 그림자 금융 리스크 주의' },
            { name: 'L/M2 비율', value: ratio(l, m2), unit: '배', sub: 'L ÷ M2', desc: '광의 유동성 확장',
              ref: grade(ratio(l, m2), [
                  { max: 1.6, label: '안정', color: 'text-blue-500' },
                  { min: 1.6, max: 2.0, label: '정상', color: 'text-green-600' },
                  { min: 2.0, max: Infinity, label: '과잉', color: 'text-red-500' }]),
              description: '전체 금융권에 풀린 돈이 은행 중심 통화량의 몇 배인지. 높을수록 금융시스템 전체 유동성이 크게 확장된 상태' },
            { name: '대출/예금 비율 (LDR)', value: ratio(loan, deposit, 3), unit: '배', sub: '은행대출금 ÷ 은행총예금', desc: '은행 레버리지',
              ref: grade(ratio(loan, deposit, 3), [
                  { max: 0.9, label: '보수적', color: 'text-blue-500' },
                  { min: 0.9, max: 1.0, label: '정상', color: 'text-green-600' },
                  { min: 1.0, max: 1.1, label: '주의', color: 'text-yellow-600' },
                  { min: 1.1, max: Infinity, label: '규제 초과', color: 'text-red-500' }]),
              refNote: '규제 기준 100%',
              description: '1 초과면 예금보다 대출이 많은 상태. 은행이 외부 차입에 의존하는 정도를 보여줌. 규제 기준 예대율 100%' },
            { name: '가계신용/예금', value: ratio(houseCredit, deposit, 3), unit: '배', sub: '가계신용 ÷ 은행총예금', desc: '가계 레버리지',
              ref: grade(ratio(houseCredit, deposit, 3), [
                  { max: 0.7, label: '안정', color: 'text-blue-500' },
                  { min: 0.7, max: 1.0, label: '정상', color: 'text-green-600' },
                  { min: 1.0, max: Infinity, label: '과도', color: 'text-red-500' }]),
              description: '가계가 빌린 돈이 은행 전체 예금 대비 어느 수준인지. 1에 가까울수록 "빚으로 버티는 시장"이라는 뜻' },
            { name: '가계신용/M2', value: ratio(houseCredit, m2, 3), unit: '배', sub: '가계신용 ÷ M2', desc: '유동성 대비 가계부채',
              ref: grade(ratio(houseCredit, m2, 3), [
                  { max: 0.4, label: '여유', color: 'text-blue-500' },
                  { min: 0.4, max: 0.6, label: '정상', color: 'text-green-600' },
                  { min: 0.6, max: Infinity, label: '위험', color: 'text-red-500' }]),
              description: '시중 통화량 대비 가계부채 비중. 높을수록 유동성이 실물이 아닌 가계 빚으로 흘러갔다는 신호' },
            { name: '가계대출 연체율', value: delinquency, unit: '%', sub: '가계대출 연체 비율', desc: '신용 리스크',
              ref: grade(delinquency, [
                  { max: 0.5, label: '안정', color: 'text-blue-500' },
                  { min: 0.5, max: 1.5, label: '정상', color: 'text-green-600' },
                  { min: 1.5, max: 3.0, label: '주의', color: 'text-yellow-600' },
                  { min: 3.0, max: Infinity, label: '위기', color: 'text-red-500' }]),
              refNote: '장기평균 0.78%',
              description: '가계 대출 중 연체된 비율. 빠르게 오르면 금융위기 초기 신호. 장기평균 0.78%, 1% 이상이면 주의' },
        ];
    },

    getStockBondSpreads() {
        const find = (name) => { const i = this.ecos._indicatorMap[name]; return i ? parseFloat(i.dataValue) : null; };
        const ratio = (a, b, d=2) => (a === null || b === null || isNaN(a) || isNaN(b) || b === 0) ? null : Math.round((a/b)*Math.pow(10,d))/Math.pow(10,d);
        return [
            { name: '유동성 비율', value: ratio(find('주식거래대금(KOSPI)'), find('투자자예탁금')), unit: '배', sub: '주식거래대금(KOSPI) ÷ 투자자예탁금', desc: '시장 유동성',
              description: '거래대금이 예탁금보다 크면 대기 자금이 적극 투입된 상태. 높을수록 과열, 낮을수록 관망' },
            { name: '위험자산 선호도', value: ratio(find('주식거래대금(KOSPI)'), find('채권거래대금'), 3), unit: '배', sub: '주식거래대금(KOSPI) ÷ 채권거래대금', desc: '주식 vs 채권',
              description: '높으면 투자자들이 주식(위험자산) 선호, 낮으면 채권(안전자산) 선호' },
            { name: '대형 vs 성장주', value: ratio(find('코스피지수'), find('코스닥지수')), unit: '배', sub: '코스피지수 ÷ 코스닥지수', desc: '시장 강도 비교',
              description: '높으면 대형주 강세, 낮으면 성장·중소형주 강세' },
        ];
    },

    getGrowthIncomeSpreads() {
        const find = (name) => { const i = this.ecos._indicatorMap[name]; return i ? parseFloat(i.dataValue) : null; };
        const calc = (a, b) => (a === null || b === null || isNaN(a) || isNaN(b)) ? null : Math.round((a-b)*1000)/1000;
        const consumption = find('민간소비증감률(실질, 계절조정 전기대비)'), equipment = find('설비투자증감률(실질, 계절조정 전기대비)'), construction = find('건설투자증감률(실질, 계절조정 전기대비)');
        const domesticSum = (consumption !== null && equipment !== null && construction !== null) ? Math.round((consumption+equipment+construction)*1000)/1000 : null;
        const exportRate = find('재화의 수출 증감률(실질, 계절조정 전기대비)');
        return [
            { name: '내수 성장 기여도', value: domesticSum, unit: '%', sub: '민간소비 + 설비투자 + 건설투자 증감률 합', desc: '내수 동력',
              description: '민간소비·설비투자·건설투자 증가율의 합. 양수이면 내수가 경제 성장에 기여, 음수이면 내수 위축' },
            { name: '수출 vs 내수', value: (exportRate !== null && domesticSum !== null && domesticSum !== 0) ? Math.round((exportRate/domesticSum)*100)/100 : null, unit: '배', sub: '재화의 수출 증감률 ÷ 내수 증감률 합', desc: '성장 구조',
              description: '1보다 크면 수출 주도 성장, 1 미만이면 내수 주도 성장' },
            { name: '자금 잉여/부족', value: calc(find('총저축률'), find('국내총투자율')), unit: '%p', sub: '총저축률 − 총투자율', desc: '자금 구조',
              description: '양수이면 저축이 투자보다 많아 자금 잉여(해외 투자 가능), 음수이면 자금 부족(해외 차입 필요)' },
        ];
    },

    getProductionSpreads() {
        const find = (name) => { const i = this.ecos._indicatorMap[name]; return i ? parseFloat(i.dataValue) : null; };
        const ratio = (a, b, d=3) => (a === null || b === null || isNaN(a) || isNaN(b) || b === 0) ? null : Math.round((a/b)*Math.pow(10,d))/Math.pow(10,d);
        const calc = (a, b) => (a === null || b === null || isNaN(a) || isNaN(b)) ? null : Math.round((a-b)*1000)/1000;
        return [
            { name: '재고율', value: ratio(find('제조업재고지수'), find('제조업출하지수')), unit: '배', sub: '제조업재고지수 ÷ 제조업출하지수', desc: '재고 수준',
              description: '1보다 크면 출하 대비 재고가 쌓이는 상태(경기 둔화 신호). 1 미만이면 재고 소진(경기 활성)' },
            { name: '재고 압력', value: calc(find('제조업생산지수'), find('제조업출하지수')), unit: 'p', sub: '제조업생산지수 − 제조업출하지수', desc: '생산 vs 판매',
              description: '양수이면 생산이 판매를 초과해 재고가 쌓이는 중. 음수이면 판매가 생산을 초과' },
            { name: '제조 vs 서비스', value: calc(find('전산업생산지수'), find('서비스업생산지수')), unit: 'p', sub: '전산업생산지수 − 서비스업생산지수', desc: '산업 구조',
              description: '양수이면 제조업 포함 전체가 서비스업보다 활발. 음수이면 서비스업 중심 성장' },
            { name: '설비 활용 효율', value: ratio(find('제조업가동률지수'), find('제조업생산지수')), unit: '배', sub: '제조업가동률지수 ÷ 제조업생산지수', desc: '설비 효율',
              description: '높으면 기존 설비를 최대한 활용 중. 낮으면 설비 여유가 있는 상태' },
        ];
    },

    getConsumptionInvestmentSpreads() {
        const find = (name) => { const i = this.ecos._indicatorMap[name]; return i ? parseFloat(i.dataValue) : null; };
        const ratio = (a, b, d=3) => (a === null || b === null || isNaN(a) || isNaN(b) || b === 0) ? null : Math.round((a/b)*Math.pow(10,d))/Math.pow(10,d);
        return [
            { name: '내구재 소비 비중', value: ratio(find('자동차판매액지수'), find('소매판매액지수')), unit: '배', sub: '자동차판매액지수 ÷ 소매판매액지수', desc: '소비 구조',
              description: '높으면 자동차 등 고가 내구재 소비가 활발. 낮으면 생필품 중심 소비' },
            { name: '투자 선행 vs 현재', value: ratio(find('설비투자지수'), find('국내기계수주액')), unit: '배', sub: '설비투자지수 ÷ 국내기계수주액', desc: '투자 흐름',
              description: '높으면 현재 투자가 주문 대비 활발. 낮으면 향후 투자 확대 가능성' },
            { name: '미래 건설 vs 현재', value: ratio(find('건설수주액'), find('건설기성액')), unit: '배', sub: '건설수주액 ÷ 건설기성액', desc: '건설 파이프라인',
              description: '1보다 크면 향후 건설 물량 증가 예상. 1 미만이면 건설 위축 가능' },
            { name: '착공률', value: ratio(find('건축착공면적'), find('건축허가면적')), unit: '배', sub: '건축착공면적 ÷ 건축허가면적', desc: '실행력',
              description: '1에 가까울수록 허가된 건물이 실제 착공으로 이어지는 비율이 높음. 낮으면 허가만 받고 착공 지연' },
        ];
    },

    getPriceSpreads() {
        const find = (name) => { const i = this.ecos._indicatorMap[name]; return i ? parseFloat(i.dataValue) : null; };
        const calc = (a, b) => (a === null || b === null || isNaN(a) || isNaN(b)) ? null : Math.round((a-b)*1000)/1000;
        const ratio = (a, b, d=3) => (a === null || b === null || isNaN(a) || isNaN(b) || b === 0) ? null : Math.round((a/b)*Math.pow(10,d))/Math.pow(10,d);
        return [
            { name: '변동성 영향', value: calc(find('소비자물가지수'), find('농산물 및 석유류제외 소비자물가지수')), unit: 'p', sub: '소비자물가지수 − 농산물및석유류제외 소비자물가지수', desc: '농산물·에너지 효과',
              description: '양수이면 농산물·석유가 물가를 끌어올리는 중. 음수이면 오히려 억제 효과' },
            { name: '가격 전가 압력', value: calc(find('생산자물가지수'), find('소비자물가지수')), unit: 'p', sub: '생산자물가지수 − 소비자물가지수', desc: '기업→소비자',
              description: '양수이면 생산자 비용이 아직 소비자에게 전가되지 않은 상태(향후 물가 상승 압력). 음수이면 전가 완료' },
            { name: '수입발 물가 압력', value: calc(find('수입물가지수'), find('소비자물가지수')), unit: 'p', sub: '수입물가지수 − 소비자물가지수', desc: '해외→국내',
              description: '양수이면 수입 가격이 국내 소비자 물가보다 높아 향후 물가 상승 가능. 음수이면 수입발 압력 약화' },
            { name: '체감물가 비율', value: ratio(find('생활물가지수'), find('소비자물가지수')), unit: '배', sub: '생활물가지수 ÷ 소비자물가지수', desc: '체감 vs 공식',
              description: '1보다 크면 실제 생활에서 느끼는 물가가 공식 통계보다 높음. 1이면 체감과 공식 일치' },
        ];
    },

    getEmploymentLaborSpreads() {
        const find = (name) => { const i = this.ecos._indicatorMap[name]; return i ? parseFloat(i.dataValue) : null; };
        const ratio = (a, b, d=3) => (a === null || b === null || isNaN(a) || isNaN(b) || b === 0) ? null : Math.round((a/b)*Math.pow(10,d))/Math.pow(10,d);
        return [
            { name: '실질 취업 비율', value: ratio(find('취업자수'), find('경제활동인구')), unit: '배', sub: '취업자수 ÷ 경제활동인구', desc: '고용 효율',
              description: '1에 가까울수록 경제활동인구 대부분이 취업한 상태. 낮을수록 실업 비중 높음' },
            { name: '임금 vs 생산성', value: ratio(find('시간당명목임금지수'), find('노동생산성지수')), unit: '배', sub: '시간당명목임금지수 ÷ 노동생산성지수', desc: '비용 압력',
              description: '1보다 크면 임금이 생산성보다 빠르게 상승(기업 비용 부담 증가). 1 미만이면 생산성이 임금을 앞섬' },
            { name: '기업 부담 지표', value: ratio(find('단위노동비용지수'), find('노동생산성지수')), unit: '배', sub: '단위노동비용지수 ÷ 노동생산성지수', desc: '기업 경쟁력',
              description: '높을수록 생산성 대비 노동비용이 커 기업 경쟁력 약화. 낮을수록 효율적' },
        ];
    },

    getSentimentSpreads() {
        const find = (name) => { const i = this.ecos._indicatorMap[name]; return i ? parseFloat(i.dataValue) : null; };
        const calc = (a, b) => (a === null || b === null || isNaN(a) || isNaN(b)) ? null : Math.round((a-b)*1000)/1000;
        return [
            { name: '경기 방향성', value: calc(find('선행지수순환변동치'), find('동행지수순환변동치')), unit: 'p', sub: '선행지수순환변동치 − 동행지수순환변동치', desc: '미래 vs 현재',
              description: '양수이면 현재보다 경기가 좋아질 전망. 음수이면 현재보다 나빠질 전망' },
            { name: '기업 vs 소비 괴리', value: calc(find('전산업 기업심리지수실적'), find('소비자심리지수')), unit: 'p', sub: '전산업 기업심리지수실적 − 소비자심리지수', desc: '심리 격차',
              description: '양수이면 기업이 소비자보다 낙관적. 음수이면 소비자가 더 낙관적. 격차가 크면 경기 전환 신호' },
            { name: '경기 과열/침체', value: find('경제심리지수') !== null ? Math.round((find('경제심리지수')-100)*1000)/1000 : null, unit: 'p', sub: '경제심리지수 − 100', desc: '기준선 대비',
              description: '양수이면 경기 낙관(과열 가능), 음수이면 경기 비관(침체 가능). 0이면 중립' },
        ];
    },

    getExternalEconomySpreads() {
        const find = (name) => { const i = this.ecos._indicatorMap[name]; return i ? parseFloat(i.dataValue) : null; };
        const calc = (a, b) => (a === null || b === null || isNaN(a) || isNaN(b)) ? null : Math.round((a-b)*1000)/1000;
        const ratio = (a, b, d=3) => (a === null || b === null || isNaN(a) || isNaN(b) || b === 0) ? null : Math.round((a/b)*Math.pow(10,d))/Math.pow(10,d);
        return [
            { name: '순 직접투자', value: calc(find('직접투자(자산)'), find('직접투자(부채)')), unit: '백만$', sub: '직접투자(자산) − 직접투자(부채)', desc: '투자 흐름',
              description: '양수이면 우리 기업이 해외에 더 많이 투자. 음수이면 외국인이 국내에 더 많이 투자' },
            { name: '순 증권투자', value: calc(find('증권투자(자산)'), find('증권투자(부채)')), unit: '백만$', sub: '증권투자(자산) − 증권투자(부채)', desc: '자금 흐름',
              description: '양수이면 국내 자금이 해외로 유출. 음수이면 외국인 자금이 국내로 유입' },
            { name: '순대외자산', value: calc(find('대외채권'), find('대외채무')), unit: '백만$', sub: '대외채권 − 대외채무', desc: '대외 건전성',
              description: '양수이면 받을 돈이 갚을 돈보다 많은 순채권국. 음수이면 순채무국' },
            { name: '무역 밸런스', value: ratio(find('수출금액지수'), find('수입금액지수')), unit: '배', sub: '수출금액지수 ÷ 수입금액지수', desc: '교역 균형',
              description: '1보다 크면 수출 우위, 1 미만이면 수입 우위. 무역수지 방향을 보여줌' },
            { name: '외환 안정성', value: ratio(find('경상수지'), find('외환보유액'), 4), unit: '배', sub: '경상수지 ÷ 외환보유액', desc: '외환 건전성',
              description: '양수이면 경상수지 흑자 기반 외환 안정. 음수 확대 시 외환 리스크 증가' },
        ];
    },

    getRealEstateSpreads() {
        const find = (name) => { const i = this.ecos._indicatorMap[name]; return i ? parseFloat(i.dataValue) : null; };
        const calc = (a, b) => (a === null || b === null || isNaN(a) || isNaN(b)) ? null : Math.round((a-b)*1000)/1000;
        const ratio = (a, b, d=3) => (a === null || b === null || isNaN(a) || isNaN(b) || b === 0) ? null : Math.round((a/b)*Math.pow(10,d))/Math.pow(10,d);
        return [
            { name: '매매-전세 괴리', value: calc(find('주택매매가격지수'), find('주택전세가격지수')), unit: 'p', sub: '주택매매가격지수 − 주택전세가격지수', desc: '갭투자 환경',
              description: '양수이면 매매가가 전세가보다 많이 상승(갭투자 확대 환경). 음수이면 전세가 상승이 더 큼' },
            { name: '토지 vs 주택', value: ratio(find('지가변동률(전기대비)'), find('주택매매가격지수'), 4), unit: '', sub: '지가변동률(전기대비) ÷ 주택매매가격지수', desc: '부동산 구조',
              description: '높으면 토지 가격 상승이 주택 대비 빠름(개발 기대). 낮으면 주택 중심 시장' },
        ];
    },

    getCorporateHouseholdSpreads() {
        const find = (name) => { const i = this.ecos._indicatorMap[name]; return i ? parseFloat(i.dataValue) : null; };
        const ratio = (a, b, d=3) => (a === null || b === null || isNaN(a) || isNaN(b) || b === 0) ? null : Math.round((a/b)*Math.pow(10,d))/Math.pow(10,d);
        const income = find('가구당월평균소득'), propensity = find('평균소비성향');
        const estConsumption = (income !== null && propensity !== null) ? Math.round(income * propensity / 100) : null;
        return [
            { name: '추정 소비 수준', value: estConsumption, unit: '천원', sub: '가구당월평균소득 × 평균소비성향', desc: '가계 소비력',
              description: '가구당 월평균 소득에 소비성향을 곱한 추정 소비 금액. 경기 체감의 기초 지표' },
            { name: '수익성 vs 리스크', value: ratio(find('제조업매출액세전순이익률'), find('제조업부채비율')), unit: '', sub: '제조업매출액세전순이익률 ÷ 제조업부채비율', desc: '기업 건전성',
              description: '높을수록 부채 대비 수익성이 좋음. 낮으면 부채 부담에 비해 수익이 적은 상태' },
            { name: '불평등 구조', value: ratio(find('5분위배율'), find('지니계수')), unit: '배', sub: '5분위배율 ÷ 지니계수', desc: '분배 구조',
              description: '높으면 상하위 소득 격차가 전반적 불평등보다 두드러짐. 소득 양극화 심화 신호' },
        ];
    },

    getEcosCategoryLabel(name) {
        const cat = this.ecos.categories.find(c => c.name === name);
        return cat ? cat.label : name;
    },

    /**
     * 선택된 카테고리의 스프레드 목록 반환 (통합 접근)
     */
    getCurrentSpreads() {
        const cat = this.ecos.selectedCategory;
        if (!cat || this.ecos.indicators.length === 0) return [];
        const map = {
            'INTEREST_RATE': () => this.getInterestRateSpreads(),
            'MONEY_FINANCE': () => this.getMoneyFinanceSpreads(),
            'STOCK_BOND': () => this.getStockBondSpreads(),
            'GROWTH_INCOME': () => this.getGrowthIncomeSpreads(),
            'PRODUCTION': () => this.getProductionSpreads(),
            'CONSUMPTION_INVESTMENT': () => this.getConsumptionInvestmentSpreads(),
            'PRICE': () => this.getPriceSpreads(),
            'EMPLOYMENT_LABOR': () => this.getEmploymentLaborSpreads(),
            'SENTIMENT': () => this.getSentimentSpreads(),
            'EXTERNAL_ECONOMY': () => this.getExternalEconomySpreads(),
            'REAL_ESTATE': () => this.getRealEstateSpreads(),
            'CORPORATE_HOUSEHOLD': () => this.getCorporateHouseholdSpreads(),
        };
        return map[cat] ? map[cat]() : [];
    },

    /**
     * 스프레드의 상태 클래스 반환 (카드 좌측 바, 뱃지용)
     */
    getSpreadStatus(sp) {
        if (sp.ref && sp.ref.label) {
            const label = sp.ref.label;
            if (label === '안정' || label === '여유') return 'safe';
            if (label === '정상' || label === '은행 중심') return 'normal';
            if (label === '주의') return 'caution';
            if (label === '위기' || label === '과잉' || label === '과도' || label === '위험' || label === '규제 초과' || label === '비은행 확장') return 'danger';
            if (label === '보수적') return 'conservative';
            return 'normal';
        }
        if (sp.value === null) return '';
        if (sp.value > 0) return 'normal';
        if (sp.value < 0) return 'caution';
        return '';
    },

    /**
     * 게이지 바 마커 위치 계산 (0~100%)
     * ref.levels 배열이 있는 경우에만 사용
     */
    getGaugePosition(sp) {
        if (!sp.ref || !sp.ref.levels || sp.value === null) return null;
        const levels = sp.ref.levels;
        const min = levels[0].min !== undefined ? levels[0].min : 0;
        const lastLevel = levels[levels.length - 1];
        const max = lastLevel.max === Infinity
            ? (lastLevel.min !== undefined ? lastLevel.min * 2 : sp.value * 2)
            : lastLevel.max;
        if (max === min) return 50;
        const pct = ((sp.value - min) / (max - min)) * 100;
        return Math.max(2, Math.min(98, pct));
    },

    /**
     * 게이지 바 세그먼트 정보 반환
     */
    getGaugeSegments(sp) {
        if (!sp.ref || !sp.ref.levels) return [];
        const levels = sp.ref.levels;
        const totalSegments = levels.length;
        return levels.map((lv, idx) => {
            const widthPct = 100 / totalSegments;
            const colorMap = {
                'text-blue-500': '#3b82f6',
                'text-green-600': '#22c55e',
                'text-yellow-600': '#eab308',
                'text-red-500': '#ef4444',
            };
            return {
                left: widthPct * idx + '%',
                width: widthPct + '%',
                color: colorMap[lv.color] || '#94a3b8',
            };
        });
    },

    /**
     * 스프레드 값의 부호 클래스 반환
     */
    getValueColorClass(sp) {
        if (sp.value === null) return 'text-gray-400';
        if (sp.ref && sp.ref.color) return sp.ref.color;
        if (sp.value > 0) return 'value-positive';
        if (sp.value < 0) return 'value-negative';
        return 'value-neutral';
    },

    /**
     * 스프레드 섹션 제목 반환
     */
    getSpreadSectionTitle() {
        const titles = {
            'INTEREST_RATE': '금리 스프레드',
            'MONEY_FINANCE': '유동성 / 신용 파생지표',
            'STOCK_BOND': '금융시장 파생지표',
            'GROWTH_INCOME': '성장 구조 파생지표',
            'PRODUCTION': '생산 구조 파생지표',
            'CONSUMPTION_INVESTMENT': '소비 / 투자 파생지표',
            'PRICE': '물가 구조 파생지표',
            'EMPLOYMENT_LABOR': '고용 / 노동 파생지표',
            'SENTIMENT': '경기 / 심리 파생지표',
            'EXTERNAL_ECONOMY': '대외경제 파생지표',
            'REAL_ESTATE': '부동산 파생지표',
            'CORPORATE_HOUSEHOLD': '가계 / 기업 / 분배 파생지표',
        };
        return titles[this.ecos.selectedCategory] || '';
    }
};