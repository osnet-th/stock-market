/**
 * DerivedIndicatorComponent - 사용자 커스텀 파생지표 (이슈 #36)
 * 소유 프로퍼티: derived
 *
 * XSS 정책(glossary.js 준수): name/unit/description/계산값 등 사용자 출처 렌더링은 x-text만 사용.
 * 2~3항 구조형 수식, 연산자 우선순위(×÷ 먼저)는 백엔드 평가. 같은 카테고리만 조합(R3, 사용자 정의).
 */
const DerivedIndicatorComponent = {
    derived: {
        list: [],
        presets: [],
        available: [],
        loading: false,
        loaded: false,
        showForm: false,
        editingId: null,
        error: '',
        form: {
            name: '',
            unit: '',
            description: '',
            category: '',
            termCount: 2,
            operands: [
                { type: 'INDICATOR', className: '', keystatName: '', value: null },
                { type: 'INDICATOR', className: '', keystatName: '', value: null },
                { type: 'INDICATOR', className: '', keystatName: '', value: null }
            ],
            operators: ['SUB', 'SUB']
        }
    },

    DERIVED_OPERATORS: [
        { value: 'ADD', symbol: '+' },
        { value: 'SUB', symbol: '−' },
        { value: 'MUL', symbol: '×' },
        { value: 'DIV', symbol: '÷' }
    ],

    async loadDerivedIndicators() {
        this.derived.loading = true;
        try {
            const [list, presets, available] = await Promise.all([
                API.getDerivedIndicators(),
                API.getDerivedPresets(),
                API.getDerivedAvailableIndicators(null)
            ]);
            this.derived.list = list || [];
            this.derived.presets = presets || [];
            this.derived.available = available || [];
            this.derived.loaded = true;
        } catch (e) {
            console.error('파생지표 로드 실패:', e);
        } finally {
            this.derived.loading = false;
        }
    },

    ensureDerivedLoaded() {
        if (!this.derived.loaded && !this.derived.loading) {
            this.loadDerivedIndicators();
        }
    },

    derivedCategories() {
        const seen = new Map();
        for (const ind of this.derived.available) {
            if (!seen.has(ind.category)) {
                seen.set(ind.category, ind.categoryLabel || ind.category);
            }
        }
        return Array.from(seen, ([name, label]) => ({ name, label }));
    },

    derivedAvailableForCategory() {
        if (!this.derived.form.category) return [];
        return this.derived.available.filter(i => i.category === this.derived.form.category);
    },

    derivedOperatorSymbol(op) {
        const found = this.DERIVED_OPERATORS.find(o => o.value === op);
        return found ? found.symbol : op;
    },

    openDerivedCreate() {
        this.derived.error = '';
        this.derived.editingId = null;
        this.derived.form = {
            name: '', unit: '', description: '', category: '', termCount: 2,
            operands: [
                { type: 'INDICATOR', className: '', keystatName: '', value: null },
                { type: 'INDICATOR', className: '', keystatName: '', value: null },
                { type: 'INDICATOR', className: '', keystatName: '', value: null }
            ],
            operators: ['SUB', 'SUB']
        };
        this.derived.showForm = true;
    },

    openDerivedEdit(item) {
        this.derived.error = '';
        this.derived.editingId = item.id;
        const ops = (item.formula && item.formula.operands) || [];
        const operators = (item.formula && item.formula.operators) || [];
        const operands = [0, 1, 2].map(i => {
            const o = ops[i];
            if (!o) return { type: 'INDICATOR', className: '', keystatName: '', value: null };
            return {
                type: o.type,
                className: o.className || '',
                keystatName: o.keystatName || '',
                value: o.value !== undefined ? o.value : null
            };
        });
        // 카테고리: 첫 지표 operand의 className으로 추정
        const firstIndicator = ops.find(o => o.type === 'INDICATOR');
        const cat = firstIndicator
            ? (this.derived.available.find(a => a.className === firstIndicator.className && a.keystatName === firstIndicator.keystatName) || {}).category || ''
            : '';
        this.derived.form = {
            name: item.name || '',
            unit: item.unit || '',
            description: item.description || '',
            category: cat,
            termCount: ops.length >= 3 ? 3 : 2,
            operands,
            operators: [operators[0] || 'SUB', operators[1] || 'SUB']
        };
        this.derived.showForm = true;
    },

    closeDerivedForm() {
        this.derived.showForm = false;
        this.derived.error = '';
    },

    setDerivedTermCount(n) {
        this.derived.form.termCount = n;
    },

    onDerivedCategoryChange() {
        // 카테고리 변경 시 지표 선택 초기화(다른 카테고리 지표가 남지 않도록)
        this.derived.form.operands.forEach(o => {
            if (o.type === 'INDICATOR') {
                o.className = '';
                o.keystatName = '';
            }
        });
    },

    onDerivedOperandSelect(operand, encoded) {
        // encoded = "className::keystatName"
        const idx = encoded.indexOf('::');
        if (idx < 0) { operand.className = ''; operand.keystatName = ''; return; }
        operand.className = encoded.slice(0, idx);
        operand.keystatName = encoded.slice(idx + 2);
    },

    derivedOperandEncoded(operand) {
        return operand.className && operand.keystatName ? operand.className + '::' + operand.keystatName : '';
    },

    async submitDerived() {
        this.derived.error = '';
        const f = this.derived.form;
        const count = f.termCount;
        const operands = f.operands.slice(0, count).map(o => {
            if (o.type === 'CONSTANT') {
                return { type: 'CONSTANT', value: o.value === null || o.value === '' ? null : Number(o.value) };
            }
            return { type: 'INDICATOR', className: o.className, keystatName: o.keystatName };
        });
        const body = {
            name: f.name,
            unit: f.unit,
            description: f.description,
            formula: { operands, operators: f.operators.slice(0, count - 1) }
        };
        try {
            if (this.derived.editingId) {
                await API.updateDerivedIndicator(this.derived.editingId, body);
            } else {
                await API.createDerivedIndicator(body);
            }
            this.derived.showForm = false;
            await this.loadDerivedIndicators();
        } catch (e) {
            this.derived.error = this.derivedErrorMessage(e);
        }
    },

    async removeDerived(id) {
        if (!confirm('이 파생지표를 삭제할까요?')) return;
        try {
            await API.deleteDerivedIndicator(id);
            await this.loadDerivedIndicators();
        } catch (e) {
            this.derived.error = this.derivedErrorMessage(e);
        }
    },

    async copyDerivedPreset(key) {
        this.derived.error = '';
        try {
            await API.copyDerivedPreset(key);
            await this.loadDerivedIndicators();
        } catch (e) {
            this.derived.error = this.derivedErrorMessage(e);
        }
    },

    derivedReasonText(reason) {
        const map = {
            INVALID_OPERAND_COUNT: '피연산자 개수가 올바르지 않습니다(2~3개).',
            INVALID_OPERATOR_COUNT: '연산자 개수가 올바르지 않습니다.',
            NO_INDICATOR: '지표가 최소 하나 필요합니다.',
            UNKNOWN_INDICATOR: '존재하지 않는 지표가 포함되어 있습니다.',
            CATEGORY_MISMATCH: '같은 카테고리의 지표만 조합할 수 있습니다.',
            DIVIDE_BY_ZERO: '0으로 나눌 수 없습니다.',
            MISSING_VALUE: '최신값이 없는 지표가 있습니다.',
            NON_FINITE: '계산 결과가 유효하지 않습니다.'
        };
        return map[reason] || null;
    },

    derivedErrorMessage(err) {
        const text = (err && err.message) || '';
        const m = text.match(/API Error \d+: (.*)$/);
        if (m) {
            try {
                const body = JSON.parse(m[1]);
                if (body.reason) return this.derivedReasonText(body.reason) || body.message || '요청을 처리할 수 없습니다.';
                if (body.message) return body.message;
            } catch (_) { /* not json */ }
        }
        return '요청을 처리할 수 없습니다. 입력을 확인해주세요.';
    },

    /** 수식을 사람이 읽는 문자열로: "국고채수익률(5년) − CD수익률(91일)". */
    derivedFormulaText(formula) {
        if (!formula || !formula.operands) return '';
        const parts = [];
        formula.operands.forEach((o, i) => {
            if (i > 0) parts.push(this.derivedOperatorSymbol(formula.operators[i - 1]));
            parts.push(o.type === 'CONSTANT' ? String(o.value) : o.keystatName);
        });
        return parts.join(' ');
    },

    /** 목록 항목 표시값: 계산 가능하면 값+단위, 아니면 사유 텍스트. */
    derivedDisplayValue(item) {
        if (item.computable && item.value !== null && item.value !== undefined) {
            return item.value + (item.unit ? ' ' + item.unit : '');
        }
        return this.derivedReasonText(item.reason) || '계산 불가';
    }
};
