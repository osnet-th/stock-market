/**
 * DashboardSummaryComponent — 홈 대시보드의 월급 데이터 mixin.
 *
 * 소유 프로퍼티: dashboardSummary
 * 메서드: loadDashboardSummary
 *
 * - mixin 객체 패턴(nested x-data 사용 안 함). app.js 의 dashboard() 에 spread.
 * - top-level 키는 dashboardSummary 객체 + loadDashboardSummary 만 노출 — HomeComponent 와의 키 충돌 회피.
 *
 * #114 로 홈이 재설계되며 축소됐다.
 * - 뉴스 기록 요약 카드·운영자 로그 카드가 사라져 해당 페치를 제거했다(홈 진입 호출 2건 감소).
 *   두 화면은 사이드바 메뉴로 그대로 들어간다.
 * - 월급은 도넛 차트 → 우측 패널 스택 바로 바뀌어 Chart.js 렌더/파괴 로직도 제거했다.
 *   스택 바는 `HomeComponent.getHomeSalaryBreakdown()` 이 계산한다.
 */
const DashboardSummaryComponent = {
    dashboardSummary: {
        salary: null,      // MonthlySalaryResponse

        loading: {
            salary: false
        },
        error: {
            salary: null
        }
    },

    /**
     * 월급 데이터 페치(home 진입/재진입 시 1회).
     * 실패는 error 로 흡수 — 우측 패널만 빈 상태가 된다.
     */
    async loadDashboardSummary() {
        const ds = this.dashboardSummary;
        // 진입 시 상태 초기화 (재진입 시 이전 에러/로딩 잔재 제거)
        ds.loading.salary = true;
        ds.error.salary = null;

        try {
            const yearMonth = this._currentYearMonthKst();
            ds.salary = await API.getSalaryMonthly(this.auth.userId, yearMonth);
        } catch (e) {
            console.error('dashboardSummary:salary:fetch-failed');
            ds.error.salary = '데이터를 불러올 수 없습니다';
        } finally {
            ds.loading.salary = false;
        }
    },

    // ──────────────────────────────────────────────────────────────────
    // 내부 헬퍼 (인스턴스 메서드 — Alpine spread 후 this 바인딩 의존)
    // ──────────────────────────────────────────────────────────────────

    /**
     * KST 기준 현재 yearMonth(YYYY-MM). cross-timezone 클라이언트(예: 해외 거주) 도
     * 한국 시간 기준으로 일관되게 산출되도록 Intl.DateTimeFormat 명시 사용.
     */
    _currentYearMonthKst() {
        const parts = new Intl.DateTimeFormat('en-CA', {
            timeZone: 'Asia/Seoul',
            year: 'numeric',
            month: '2-digit'
        }).formatToParts(new Date());
        const year = parts.find(p => p.type === 'year').value;
        const month = parts.find(p => p.type === 'month').value;
        return `${year}-${month}`;
    }
};
