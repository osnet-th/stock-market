/**
 * partial-loader: 메뉴 partial HTML 을 fetch + 주입 + Alpine.initTree 처리.
 *
 * api.js 는 JSON 전용이므로(Content-Type/response.json 강제) HTML partial 용 별도 헬퍼.
 * dashboard().init() 에서 mountAllPartials 1회 호출, 실패 placeholder 는 inline 에러 카드.
 *
 * 보안: name allow-list regex 로 path traversal 차단. 보호 partial 은 /secured-partials/ 경로.
 * 캐시: window.__PARTIAL_HASH__ 가 valid 하면 ?v=<hash> 쿼리 부착(Unit 8 에서 주입).
 */
const PartialLoader = {
    ALLOWED_NAME_REGEX: /^[a-z_][a-z0-9_-]*$/,
    RETRY_LIMIT: 3,

    ERROR_COPY: '메뉴를 불러오지 못했어요. 잠시 후 다시 시도해주세요.',
    PERMANENT_COPY: '여러 번 시도했지만 실패했어요. 새로고침해주세요.',
    RETRY_LABEL: '다시 시도',
    RETRYING_LABEL: '다시 시도 중...',
    REFRESH_LABEL: '새로고침',

    _retryCount: new Map(),
    _cleanupRegistry: null,
    _dashboard: null,

    _buildUrl(name, secured) {
        if (!this.ALLOWED_NAME_REGEX.test(name)) {
            throw new Error('Invalid partial name: ' + name);
        }
        const base = secured ? '/secured-partials' : '/partials';
        const hash = window.__PARTIAL_HASH__;
        const validHash = typeof hash === 'string' && hash.length > 0 && !hash.startsWith('${');
        const query = validHash ? '?v=' + encodeURIComponent(hash) : '';
        return base + '/' + name + '.html' + query;
    },

    async fetchPartial(name, opts = {}) {
        const url = this._buildUrl(name, opts.secured);
        // /secured-partials/** 는 hasRole(ADMIN) 필요 — STATELESS JWT 인증이라 Authorization 헤더 누락 시 403.
        const headers = { 'Accept': 'text/html' };
        const token = typeof localStorage !== 'undefined' ? localStorage.getItem('accessToken') : null;
        if (token) headers['Authorization'] = 'Bearer ' + token;
        const response = await fetch(url, {
            headers,
            credentials: 'same-origin'
        });
        if (response.status === 401 || response.status === 403) {
            const err = new Error('Unauthorized partial: ' + name);
            err.code = 'UNAUTHORIZED';
            throw err;
        }
        if (!response.ok) {
            throw new Error('Partial fetch failed ' + response.status + ': ' + name);
        }
        return response.text();
    },

    async mountPartial(name, host, opts = {}) {
        if (!host) throw new Error('Host element missing for ' + name);

        if (typeof Alpine !== 'undefined' && typeof Alpine.destroyTree === 'function') {
            try { Alpine.destroyTree(host); } catch (e) { /* first mount: no-op */ }
        }

        if (this._cleanupRegistry && typeof this._cleanupRegistry[name] === 'function') {
            try { this._cleanupRegistry[name](this._dashboard); } catch (e) {
                console.warn('[partial-loader] cleanup failed for ' + name, e);
            }
        }

        try {
            const html = await this.fetchPartial(name, opts);
            host.innerHTML = html;
            if (typeof Alpine !== 'undefined' && typeof Alpine.initTree === 'function') {
                Alpine.initTree(host);
            }
            host.setAttribute('aria-busy', 'false');
            this._retryCount.delete(name);

            const isActive = this._dashboard
                && this._dashboard.currentPage === name
                && this._dashboard.bootReady === true;
            if (isActive && typeof this._dashboard.navigateTo === 'function') {
                try { await this._dashboard.navigateTo(name); } catch (e) {
                    console.warn('[partial-loader] navigateTo dispatch after remount failed for ' + name, e);
                }
            }
        } catch (err) {
            if (err && err.code === 'UNAUTHORIZED') {
                host.innerHTML = '';
                host.setAttribute('aria-busy', 'false');
                return;
            }
            const attempt = (this._retryCount.get(name) || 0) + 1;
            this._retryCount.set(name, attempt);
            console.warn('[partial-loader] mount failed for ' + name + ' (attempt ' + attempt + ')', err);
            this._renderErrorCard(host, name, attempt);
        }
    },

    async mountAllPartials(dashboard, names, cleanupRegistry, securedNames) {
        this._dashboard = dashboard;
        this._cleanupRegistry = cleanupRegistry || {};
        const securedSet = new Set(securedNames || []);

        const tasks = names.map(name => {
            const host = document.querySelector('[data-partial="' + name + '"]');
            if (!host) {
                console.warn('[partial-loader] placeholder missing for ' + name);
                return Promise.resolve();
            }
            return this.mountPartial(name, host, { secured: securedSet.has(name) });
        });

        await Promise.allSettled(tasks);
    },

    async retryPartial(name) {
        const host = document.querySelector('[data-partial="' + name + '"]');
        if (!host) return;
        host.setAttribute('aria-busy', 'true');
        const secured = host.getAttribute('data-secured') === 'true';
        await this.mountPartial(name, host, { secured });
    },

    _renderErrorCard(host, name, attempt) {
        const isPermanent = attempt > this.RETRY_LIMIT;
        const copy = isPermanent ? this.PERMANENT_COPY : this.ERROR_COPY;
        const buttonLabel = isPermanent ? this.REFRESH_LABEL : this.RETRY_LABEL;
        const buttonAttrs = isPermanent
            ? 'onclick="window.location.reload()"'
            : 'data-retry-partial="' + name + '"';

        host.innerHTML =
            '<div role="alert" class="border-l-4 border-red-400 bg-red-50 p-4 my-2 rounded">' +
            '<p class="text-sm text-red-800 mb-2">' + copy + '</p>' +
            '<button type="button" ' + buttonAttrs + ' class="px-3 py-1 text-sm bg-red-600 text-white rounded hover:bg-red-700">' + buttonLabel + '</button>' +
            '</div>';
        host.setAttribute('aria-busy', 'false');
    }
};

// 다시 시도 버튼: delegated DOM listener (Alpine 실패 모드에서도 작동)
document.addEventListener('click', function (e) {
    const target = e.target;
    if (!target || typeof target.matches !== 'function') return;
    if (!target.matches('[data-retry-partial]')) return;
    const name = target.getAttribute('data-retry-partial');
    if (!name) return;
    target.disabled = true;
    target.textContent = PartialLoader.RETRYING_LABEL;
    PartialLoader.retryPartial(name);
});
