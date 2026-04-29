/**
 * DashboardSummaryComponent — 메인 대시보드 4개 요약 카드 mixin.
 *
 * 소유 프로퍼티: dashboardSummary
 * 메서드: loadDashboardSummary, destroyDashboardSummaryChart
 *
 * - mixin 객체 패턴(nested x-data 사용 안 함). app.js 의 dashboard() 에 spread.
 * - top-level 키는 dashboardSummary 객체 + loadDashboardSummary/destroyDashboardSummaryChart
 *   메서드만 노출 — HomeComponent 와의 키 충돌 회피.
 * - 4개 fetch 를 Promise.allSettled 로 병렬. admin 이 아닌 경우 incident 호출은 발사 안 함.
 * - 차트는 매 loadDashboardSummary 마다 destroy + recreate. canvas id: dashboardSalaryDonut.
 */
const DashboardSummaryComponent = {
    dashboardSummary: {
        news: null,        // { recentEvents: [...], categoryCounts: [...] }
        note: null,        // StockNoteListResponse — items[]
        salary: null,      // MonthlySalaryResponse
        incident: null,    // { count, asOf, available }

        loading: {
            news: false,
            note: false,
            salary: false,
            incident: false
        },
        error: {
            news: null,
            note: null,
            salary: null,
            incident: null
        },

        // 내부: Chart.js 인스턴스 보관(파괴 추적). UI 에는 노출되지 않음.
        _chartInstance: null
    },

    /**
     * 4개 카드 데이터 페치(home 진입/재진입 시 1회). admin 일 때만 incident 호출.
     * 부분 실패는 카드별 error 로 흡수 — 다른 카드 렌더 영향 없음.
     */
    async loadDashboardSummary() {
        const ds = this.dashboardSummary;
        // 진입 시 상태 초기화 (재진입 시 이전 에러/로딩 잔재 제거)
        ds.loading.news = true;
        ds.loading.note = true;
        ds.loading.salary = true;
        ds.error.news = null;
        ds.error.note = null;
        ds.error.salary = null;

        const tasks = [
            API.getNewsJournalDashboardSummary()
                .then(r => { ds.news = r; })
                .catch(e => {
                    console.error('dashboardSummary:news:fetch-failed');
                    ds.error.news = '데이터를 불러올 수 없습니다';
                })
                .finally(() => { ds.loading.news = false; }),

            API.getStockNoteList({ size: 3 })
                .then(r => { ds.note = r; })
                .catch(e => {
                    console.error('dashboardSummary:note:fetch-failed');
                    ds.error.note = '데이터를 불러올 수 없습니다';
                })
                .finally(() => { ds.loading.note = false; }),

            (async () => {
                try {
                    const yearMonth = this._currentYearMonthKst();
                    ds.salary = await API.getSalaryMonthly(this.auth.userId, yearMonth);
                } catch (e) {
                    console.error('dashboardSummary:salary:fetch-failed');
                    ds.error.salary = '데이터를 불러올 수 없습니다';
                } finally {
                    ds.loading.salary = false;
                }
            })()
        ];

        if (this.auth.isAdmin) {
            ds.loading.incident = true;
            ds.error.incident = null;
            tasks.push(
                API.getTodayIncidentCount()
                    .then(r => { ds.incident = r; })
                    .catch(e => {
                        console.error('dashboardSummary:incident:fetch-failed');
                        ds.error.incident = '데이터를 불러올 수 없습니다';
                    })
                    .finally(() => { ds.loading.incident = false; })
            );
        } else {
            // admin 아님 — 페치 자체를 발사하지 않음. 상태도 초기화로 리셋.
            ds.incident = null;
            ds.error.incident = null;
            ds.loading.incident = false;
        }

        await Promise.allSettled(tasks);

        // 차트는 데이터 로드 완료 후 + DOM 마운트 완료 후 그림.
        this.$nextTick(() => this._renderDashboardSalaryDonut());
    },

    /**
     * SPA 페이지 이동/재진입 시 누적 차트 인스턴스 정리. navigateTo 에서 호출.
     */
    destroyDashboardSummaryChart() {
        if (this.dashboardSummary._chartInstance) {
            this.dashboardSummary._chartInstance.destroy();
            this.dashboardSummary._chartInstance = null;
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
    },

    /**
     * 월급 카드 도넛 차트 렌더. 데이터/canvas/totalSpending 가드 후 그림.
     * prefers-reduced-motion 사용자에게는 애니메이션 비활성.
     */
    _renderDashboardSalaryDonut() {
        const ds = this.dashboardSummary;
        const canvas = document.getElementById('dashboardSalaryDonut');
        if (!canvas) return;

        // 기존 인스턴스 정리(재진입 가드)
        if (ds._chartInstance) {
            ds._chartInstance.destroy();
            ds._chartInstance = null;
        }

        const data = ds.salary;
        if (!data || !Array.isArray(data.spendings) || !data.totalSpending || Number(data.totalSpending) === 0) {
            // 데이터 없음/total=0 가드: 차트 미렌더. UI 측 placeholder 분기는 템플릿이 처리.
            return;
        }

        const positives = data.spendings.filter(s => s && Number(s.amount) > 0);
        if (positives.length === 0) return;

        const labels = positives.map(s => s.categoryLabel || s.category || '기타');
        const values = positives.map(s => Number(s.amount));

        const reduceMotion = window.matchMedia &&
            window.matchMedia('(prefers-reduced-motion: reduce)').matches;

        ds._chartInstance = new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels,
                datasets: [{
                    data: values,
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: reduceMotion ? false : undefined,
                plugins: {
                    legend: { position: 'bottom' },
                    tooltip: {
                        callbacks: {
                            label: (ctx) => {
                                const total = Number(data.totalSpending);
                                const v = ctx.parsed;
                                const pct = total > 0 ? ((v / total) * 100).toFixed(1) : '0.0';
                                return `${ctx.label}: ${pct}%`;
                            }
                        }
                    }
                }
            }
        });
    }
};