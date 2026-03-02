# index.html 레이아웃 예시

```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Stock Market Dashboard</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script defer src="https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js"></script>
    <link rel="stylesheet" href="/css/custom.css">
</head>
<body class="bg-gray-50 min-h-screen" x-data="dashboard()">

    <!-- Header -->
    <header class="bg-white shadow-sm border-b border-gray-200 fixed top-0 w-full z-50">
        <div class="flex items-center justify-between px-6 h-14">
            <h1 class="text-lg font-bold text-gray-800">Stock Market</h1>
            <div class="flex items-center gap-4">
                <span class="text-sm text-gray-600" x-text="auth.userId ? `ID: ${auth.userId}` : ''"></span>
                <button @click="logout()" class="text-sm text-red-500 hover:text-red-700">로그아웃</button>
            </div>
        </div>
    </header>

    <div class="flex pt-14">
        <!-- Sidebar -->
        <aside class="w-56 bg-white border-r border-gray-200 min-h-[calc(100vh-3.5rem)] fixed">
            <nav class="p-4 space-y-1">
                <template x-for="menu in menus" :key="menu.key">
                    <button
                        @click="currentPage = menu.key"
                        :class="currentPage === menu.key
                            ? 'bg-blue-50 text-blue-700 border-l-4 border-blue-700'
                            : 'text-gray-600 hover:bg-gray-50'"
                        class="w-full text-left px-4 py-2.5 rounded-r text-sm font-medium transition"
                        x-text="menu.label">
                    </button>
                </template>
            </nav>
        </aside>

        <!-- Main Content -->
        <main class="ml-56 flex-1 p-6">
            <!-- Home -->
            <div x-show="currentPage === 'home'" x-cloak>
                <!-- 대시보드 요약 카드들 -->
            </div>

            <!-- Keywords -->
            <div x-show="currentPage === 'keywords'" x-cloak>
                <!-- 키워드 관리 컴포넌트 -->
            </div>

            <!-- ECOS -->
            <div x-show="currentPage === 'ecos'" x-cloak>
                <!-- ECOS 경제지표 컴포넌트 -->
            </div>

            <!-- Global -->
            <div x-show="currentPage === 'global'" x-cloak>
                <!-- 글로벌 경제지표 컴포넌트 -->
            </div>
        </main>
    </div>

    <!-- Scripts -->
    <script src="/js/api.js"></script>
    <script src="/js/utils/format.js"></script>
    <script src="/js/components/keyword.js"></script>
    <script src="/js/components/ecos.js"></script>
    <script src="/js/components/global.js"></script>
    <script src="/js/app.js"></script>
</body>
</html>
```