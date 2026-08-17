/**
 * FavoriteComponent - 관심 지표 상태 관리
 * 소유 프로퍼티: favorites
 */
const FavoriteComponent = {
    favorites: {
        list: [],
        loading: false,
    },

    /**
     * 우선순위 편집 모드 상태.
     * 한 번에 한 컨테이너(sourceType)만 편집 가능. dirty가 true이면 새로고침 시 confirm 다이얼로그.
     * 표시 모드 폐지(#114)로 컨테이너 스코프가 sourceType 하나가 됐다.
     */
    favoriteEdit: {
        active: null,      // null | { sourceType, containerId }
        dirty: false,      // 드래그로 변경된 미저장 순서 존재 여부
        snapshotIds: [],   // 편집 진입 시점 카드 indicatorCode 순서 (취소용)
        snapshotKey: null, // snapshot이 어느 컨테이너용인지 검증
        sortable: null,    // SortableJS 인스턴스
        saving: false,
    },

    initFavorites() {
        Object.defineProperty(this.favorites, '_set', {
            value: new Set(), writable: true, enumerable: false
        });
        Object.defineProperty(this.favorites, '_togglePending', {
            value: false, writable: true, enumerable: false
        });
        // beforeunload 한 번만 등록.
        if (!this.favorites._beforeunloadAttached) {
            window.addEventListener('beforeunload', (e) => {
                if (this.favoriteEdit.dirty) {
                    e.preventDefault();
                    e.returnValue = '';
                }
            });
            Object.defineProperty(this.favorites, '_beforeunloadAttached', {
                value: true, writable: false, enumerable: false
            });
        }
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

    // ==================== 우선순위 편집 모드 ====================
    isEditing(sourceType) {
        const a = this.favoriteEdit.active;
        return a !== null && a.sourceType === sourceType;
    },

    canEnterEditMode(sourceType) {
        if (this.favoriteEdit.active !== null) return false;
        return this.containerCards(sourceType).length >= 2;
    },

    /**
     * 컨테이너 = (sourceType) 버킷 전체. 표시 모드 폐지(#114)로 하위 분기가 사라졌다.
     * 조회 실패한 글로벌 카드도 자리를 차지하므로 순서 대상에 포함한다.
     */
    containerCards(sourceType) {
        const enriched = this.homeSummary?.enrichedFavorites;
        if (!enriched) return [];
        const bucket = sourceType === 'ECOS' ? enriched.ecos : enriched.global;
        return Array.isArray(bucket) ? bucket : [];
    },

    enterEditMode(sourceType, containerId) {
        if (!this.canEnterEditMode(sourceType)) return;
        this.favoriteEdit.active = { sourceType, containerId };
        this.favoriteEdit.dirty = false;
        this.favoriteEdit.snapshotIds = this.containerCards(sourceType).map(c => c.indicatorCode);
        this.favoriteEdit.snapshotKey = sourceType;
        this.$nextTick(() => this.attachSortable(containerId));
    },

    attachSortable(containerId) {
        const container = document.getElementById(containerId);
        if (!container || typeof Sortable === 'undefined') return;
        if (this.favoriteEdit.sortable) {
            try { this.favoriteEdit.sortable.destroy(); } catch (_) { /* noop */ }
        }
        this.favoriteEdit.sortable = Sortable.create(container, {
            animation: 150,
            delay: 200,
            delayOnTouchOnly: true,
            touchStartThreshold: 5,
            ghostClass: 'opacity-40',
            onEnd: (evt) => this.handleSortEnd(evt),
        });
    },

    /**
     * SortableJS onEnd 콜백.
     *
     * Alpine x-for + reactive splice 충돌 회피를 위해 revert-then-splice 패턴 사용.
     * 알고리즘:
     *  (1) SortableJS가 옮긴 DOM을 즉시 되돌려 Alpine이 reactive array를 source-of-truth로 다시 그리게 한다.
     *  (2) `containerCards` 결과(현재 컨테이너 카드들의 *pre-revert* 순서)에서 oldIndex → newIndex 로 항목을 재배치한 새 컨테이너 순서를 만든다.
     *  (3) bucket 내 컨테이너 카드 위치(절대 인덱스)는 그대로 두고, 그 슬롯들에 새 컨테이너 순서를 차례로 다시 채워 넣는다.
     *      → 인덱스 보정이 필요 없는 안전한 슬롯-기반 갱신.
     */
    handleSortEnd(evt) {
        if (evt.oldIndex === evt.newIndex) return;
        const a = this.favoriteEdit.active;
        if (!a) return;
        const parent = evt.from;
        const children = Array.from(parent.children);
        if (evt.oldIndex < children.length) {
            parent.insertBefore(evt.item, children[evt.oldIndex] === evt.item ? children[evt.oldIndex + 1] : children[evt.oldIndex]);
        }
        const enriched = this.homeSummary?.enrichedFavorites;
        if (!enriched) return;
        const bucket = a.sourceType === 'ECOS' ? enriched.ecos : enriched.global;
        const items = this.containerCards(a.sourceType);
        if (evt.oldIndex >= items.length || evt.newIndex >= items.length) return;
        // (2) 컨테이너 카드들의 새 순서 계산 — splice on copy.
        const reordered = items.slice();
        const [moving] = reordered.splice(evt.oldIndex, 1);
        reordered.splice(evt.newIndex, 0, moving);
        // (3) bucket 내 컨테이너 슬롯 절대 인덱스 수집 후 새 순서로 채워 넣기.
        const slots = items.map(card => bucket.indexOf(card)).filter(idx => idx >= 0);
        if (slots.length !== items.length) return;
        slots.forEach((slot, i) => { bucket[slot] = reordered[i]; });
        this.favoriteEdit.dirty = true;
    },

    async saveOrder() {
        const a = this.favoriteEdit.active;
        if (!a || this.favoriteEdit.saving) return;
        this.favoriteEdit.saving = true;
        const codes = this.containerCards(a.sourceType).map(c => c.indicatorCode);
        try {
            await API.reorderFavorites(a.sourceType, codes);
            this.exitEditMode(true);
        } catch (e) {
            console.error('관심지표 순서 저장 실패:', e);
            alert('순서 저장에 실패했어요. 잠시 후 다시 시도해주세요');
        } finally {
            this.favoriteEdit.saving = false;
        }
    },

    cancelOrder() {
        const a = this.favoriteEdit.active;
        if (!a) return;
        this.restoreSnapshot();
        this.exitEditMode(false);
    },

    /**
     * 편집 진입 시점의 컨테이너 카드 순서로 되돌린다.
     *
     * Slot-based 갱신: 컨테이너 카드들이 점유한 bucket 절대 인덱스 슬롯들에 snapshot 순서로 다시 채워 넣는다.
     * handleSortEnd와 동일 패턴이라 cancel/save 동작 일관성이 확보된다.
     * snapshot에 있던 카드가 그 사이 다른 탭/액션으로 사라졌으면 silent skip(R8 즉시 반영 정책과 정합).
     */
    restoreSnapshot() {
        const a = this.favoriteEdit.active;
        if (!a) return;
        if (this.favoriteEdit.snapshotKey !== a.sourceType) return;
        const enriched = this.homeSummary?.enrichedFavorites;
        if (!enriched) return;
        const bucket = a.sourceType === 'ECOS' ? enriched.ecos : enriched.global;
        const items = this.containerCards(a.sourceType);
        const slots = items.map(card => bucket.indexOf(card)).filter(idx => idx >= 0);
        if (slots.length !== items.length) return;
        const restored = this.favoriteEdit.snapshotIds
            .map(code => bucket.find(c => c.indicatorCode === code))
            .filter(c => c !== undefined);
        // snapshot보다 현재 컨테이너에 항목이 더 적거나 많으면(추가/해제 발생) 가능한 만큼만 복원.
        const fillCount = Math.min(slots.length, restored.length);
        for (let i = 0; i < fillCount; i++) {
            bucket[slots[i]] = restored[i];
        }
    },

    exitEditMode(saved) {
        if (this.favoriteEdit.sortable) {
            try { this.favoriteEdit.sortable.destroy(); } catch (_) { /* noop */ }
            this.favoriteEdit.sortable = null;
        }
        this.favoriteEdit.active = null;
        this.favoriteEdit.dirty = false;
        this.favoriteEdit.snapshotIds = [];
        this.favoriteEdit.snapshotKey = null;
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
    }

};