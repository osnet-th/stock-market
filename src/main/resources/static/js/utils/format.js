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

    changeRate(current, previous) {
        if (current === null || previous === null || current === undefined || previous === undefined) return '-';
        const curr = parseFloat(current);
        const prev = parseFloat(previous);
        if (isNaN(curr) || isNaN(prev) || prev === 0) return '-';
        const rate = ((curr - prev) / Math.abs(prev) * 100).toFixed(2);
        return rate > 0 ? `+${rate}%` : `${rate}%`;
    }
};
