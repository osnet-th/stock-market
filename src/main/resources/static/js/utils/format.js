const Format = {
    number(value, decimals = 2) {
        if (value === null || value === undefined || value === '') return '-';
        const num = parseFloat(value);
        if (isNaN(num)) return value;
        return num.toLocaleString('ko-KR', {
            minimumFractionDigits: 0,
            maximumFractionDigits: decimals
        });
    },

    change(current, previous) {
        if (current === null || previous === null || current === undefined || previous === undefined) {
            return { direction: 'none', symbol: '', className: 'text-gray-500' };
        }
        const curr = parseFloat(current);
        const prev = parseFloat(previous);
        if (isNaN(curr) || isNaN(prev)) {
            return { direction: 'none', symbol: '', className: 'text-gray-500' };
        }
        if (curr > prev) {
            return { direction: 'up', symbol: '\u25B2', className: 'text-red-500' };
        } else if (curr < prev) {
            return { direction: 'down', symbol: '\u25BC', className: 'text-blue-500' };
        }
        return { direction: 'same', symbol: '-', className: 'text-gray-500' };
    },

    date(dateString) {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        });
    },

    compactNumber(value) {
        if (!value) return '-';
        var num = typeof value === 'string' ? parseInt(value.replace(/,/g, '')) : value;
        if (isNaN(num)) return value;

        var absNum = Math.abs(num);
        var sign = num < 0 ? '-' : '';

        if (absNum >= 1_0000_0000_0000) {
            return sign + (absNum / 1_0000_0000_0000).toFixed(1) + '조';
        }
        if (absNum >= 1_0000_0000) {
            return sign + (absNum / 1_0000_0000).toFixed(1) + '억';
        }
        if (absNum >= 1_0000) {
            return sign + (absNum / 1_0000).toFixed(0) + '만';
        }
        return sign + Format.number(absNum);
    },

    /**
     * 값과 단위를 통합하여 사람이 읽기 쉬운 형태로 변환
     * 예: (2148225.3, "십억원") → "2,148.2조원"
     */
    withUnit(value, unitName) {
        if (value === null || value === undefined || value === '') return '-';
        const num = parseFloat(value);
        if (isNaN(num)) return value;

        if (!unitName || !unitName.trim()) return Format.number(num);

        const unit = unitName.trim();

        // 지수류 (2020=100, 1980.01.04=100 등) — 값만 표시
        if (/^\d.*=\d+/.test(unit)) return Format.number(num);

        // 배율 단위 매핑: [배율, 기본단위]
        const scaleUnits = {
            '십억원': [1e9, '원'],
            '백만원': [1e6, '원'],
            '백만달러': [1e6, '달러'],
            '천달러': [1e3, '달러'],
            '천명': [1e3, '명'],
        };

        const scaleEntry = scaleUnits[unit];
        if (scaleEntry) {
            const [scale, baseUnit] = scaleEntry;
            const realValue = num * scale;
            return Format._compactWithUnit(realValue, baseUnit);
        }

        // %, 조원, 원, 달러, 달러/배럴 등 — 값 + 단위 그대로
        if (unit === '%') return Format.number(num) + '%';

        return Format.number(num) + unit;
    },

    /** 실제 값을 조/억/만 단위로 축약하여 기본단위를 붙임 */
    _compactWithUnit(realValue, baseUnit) {
        const abs = Math.abs(realValue);
        const sign = realValue < 0 ? '-' : '';

        if (abs >= 1_0000_0000_0000) {
            return sign + (abs / 1_0000_0000_0000).toFixed(1) + '조' + baseUnit;
        }
        if (abs >= 1_0000_0000) {
            return sign + (abs / 1_0000_0000).toFixed(1) + '억' + baseUnit;
        }
        if (abs >= 1_0000) {
            return sign + (abs / 1_0000).toFixed(1) + '만' + baseUnit;
        }
        return sign + Format.number(abs) + baseUnit;
    },

    usd(value) {
        if (value === null || value === undefined || value === '') return '-';
        var num = typeof value === 'string' ? parseFloat(value.replace(/,/g, '')) : value;
        if (isNaN(num)) return value;

        var absNum = Math.abs(num);
        var sign = num < 0 ? '-' : '';

        if (absNum >= 1_000_000_000_000) {
            return sign + '$' + (absNum / 1_000_000_000_000).toFixed(1) + 'T';
        }
        if (absNum >= 1_000_000_000) {
            return sign + '$' + (absNum / 1_000_000_000).toFixed(1) + 'B';
        }
        if (absNum >= 1_000_000) {
            return sign + '$' + (absNum / 1_000_000).toFixed(1) + 'M';
        }
        if (absNum >= 1_000) {
            return sign + '$' + (absNum / 1_000).toFixed(1) + 'K';
        }
        return sign + '$' + Format.number(absNum);
    },

    percent(value) {
        if (value === null || value === undefined || value === '') return '-';
        var num = typeof value === 'number' ? value : parseFloat(value);
        if (isNaN(num)) return value;
        return num.toFixed(1) + '%';
    },

    multiple(value) {
        if (value === null || value === undefined || value === '') return '-';
        var num = typeof value === 'number' ? value : parseFloat(value);
        if (isNaN(num)) return value;
        return num.toFixed(1) + 'x';
    },

    changeRate(current, previous) {
        if (current === null || previous === null || current === undefined || previous === undefined) return '-';
        const curr = parseFloat(current);
        const prev = parseFloat(previous);
        if (isNaN(curr) || isNaN(prev) || prev === 0) return '-';
        const rate = ((curr - prev) / Math.abs(prev) * 100).toFixed(2);
        return rate > 0 ? `+${rate}%` : `${rate}%`;
    }
};
