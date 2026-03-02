# keyword.js 컴포넌트 예시

```javascript
// js/components/keyword.js
// Alpine.js 키워드 관리 컴포넌트 로직

const keywordComponent = {
    async loadKeywords() {
        const active = this.keywords.filter === 'active' ? true
            : this.keywords.filter === 'inactive' ? false : null;
        this.keywords.list = await API.getKeywords(this.auth.userId, active);
    },

    get filteredKeywords() {
        let list = this.keywords.list;
        if (this.keywords.regionFilter !== 'all') {
            list = list.filter(k => k.region === this.keywords.regionFilter);
        }
        return list;
    },

    async addKeyword() {
        const { keyword, region } = this.keywords.newKeyword;
        if (!keyword.trim()) return;

        await API.registerKeyword(keyword.trim(), this.auth.userId, region);
        this.keywords.showAddModal = false;
        this.keywords.newKeyword = { keyword: '', region: 'KOREA' };
        await this.loadKeywords();
    },

    async toggleKeyword(kw) {
        if (kw.active) {
            await API.deactivateKeyword(kw.id);
        } else {
            await API.activateKeyword(kw.id);
        }
        await this.loadKeywords();
    },

    async removeKeyword(id) {
        if (!confirm('키워드를 삭제하시겠습니까?')) return;
        await API.deleteKeyword(id);
        await this.loadKeywords();
    }
};
```

## 키워드 관리 HTML 템플릿

```html
<!-- index.html 내 keywords 섹션 -->
<div x-show="currentPage === 'keywords'" x-cloak>
    <div class="flex items-center justify-between mb-6">
        <h2 class="text-xl font-bold text-gray-800">키워드 관리</h2>
        <button @click="keywords.showAddModal = true"
            class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-blue-700">
            + 키워드 등록
        </button>
    </div>

    <!-- 필터 -->
    <div class="flex gap-2 mb-4">
        <template x-for="f in [{key:'all',label:'전체'},{key:'active',label:'활성'},{key:'inactive',label:'비활성'}]">
            <button @click="keywords.filter = f.key; loadKeywords()"
                :class="keywords.filter === f.key ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-600'"
                class="px-3 py-1.5 rounded-full text-xs font-medium" x-text="f.label">
            </button>
        </template>
        <span class="border-l border-gray-300 mx-2"></span>
        <template x-for="r in [{key:'all',label:'전체'},{key:'KOREA',label:'국내'},{key:'GLOBAL',label:'해외'}]">
            <button @click="keywords.regionFilter = r.key"
                :class="keywords.regionFilter === r.key ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'"
                class="px-3 py-1.5 rounded-full text-xs font-medium" x-text="r.label">
            </button>
        </template>
    </div>

    <!-- 키워드 목록 -->
    <div class="space-y-2">
        <template x-for="kw in filteredKeywords" :key="kw.id">
            <div class="bg-white rounded-lg border border-gray-200 p-4 flex items-center justify-between">
                <div class="flex items-center gap-3">
                    <span class="font-medium text-gray-800" x-text="kw.keyword"></span>
                    <span :class="kw.region === 'KOREA' ? 'bg-blue-100 text-blue-700' : 'bg-purple-100 text-purple-700'"
                        class="text-xs px-2 py-0.5 rounded-full"
                        x-text="kw.region === 'KOREA' ? '국내' : '해외'">
                    </span>
                    <span :class="kw.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'"
                        class="text-xs px-2 py-0.5 rounded-full"
                        x-text="kw.active ? '활성' : '비활성'">
                    </span>
                </div>
                <div class="flex gap-2">
                    <button @click="toggleKeyword(kw)"
                        :class="kw.active ? 'text-yellow-600 hover:text-yellow-800' : 'text-green-600 hover:text-green-800'"
                        class="text-sm" x-text="kw.active ? '비활성화' : '활성화'">
                    </button>
                    <button @click="removeKeyword(kw.id)"
                        class="text-sm text-red-500 hover:text-red-700">삭제</button>
                </div>
            </div>
        </template>
    </div>

    <!-- 등록 모달 -->
    <div x-show="keywords.showAddModal" x-cloak
        class="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
        <div class="bg-white rounded-xl p-6 w-96 shadow-xl" @click.outside="keywords.showAddModal = false">
            <h3 class="text-lg font-bold mb-4">키워드 등록</h3>
            <input x-model="keywords.newKeyword.keyword" placeholder="키워드 입력"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 mb-3 text-sm">
            <select x-model="keywords.newKeyword.region"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 mb-4 text-sm">
                <option value="KOREA">국내</option>
                <option value="GLOBAL">해외</option>
            </select>
            <div class="flex justify-end gap-2">
                <button @click="keywords.showAddModal = false"
                    class="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">취소</button>
                <button @click="addKeyword()"
                    class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-blue-700">등록</button>
            </div>
        </div>
    </div>
</div>
```
