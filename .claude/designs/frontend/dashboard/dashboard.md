# 프론트엔드 대시보드 설계

## 기술 스택

- **HTML5** + **Alpine.js** (CDN) + **Tailwind CSS** (CDN)
- Spring Boot `src/main/resources/static/` 디렉토리에 배치
- SPA 방식 (단일 페이지에서 탭 전환)

## 디렉토리 구조

```
src/main/resources/static/
├── index.html              # 메인 대시보드 (SPA)
├── kakao-login.html        # 기존 로그인 페이지
├── js/
│   ├── app.js              # Alpine.js 메인 앱 상태/라우팅
│   ├── api.js              # API 호출 모듈 (fetch 래퍼)
│   ├── components/
│   │   ├── keyword.js      # 키워드 관리 컴포넌트
│   │   ├── ecos.js         # ECOS 경제지표 컴포넌트
│   │   └── global.js       # 글로벌 경제지표 컴포넌트
│   └── utils/
│       └── format.js       # 숫자/날짜 포맷 유틸
└── css/
    └── custom.css          # Tailwind 보충 커스텀 스타일
```

## 화면 구성

### 레이아웃

```
┌─────────────────────────────────────────────────────┐
│  Header: 로고 + 사용자 정보 + 로그아웃              │
├──────────┬──────────────────────────────────────────┤
│          │                                          │
│ Sidebar  │         Main Content Area                │
│          │                                          │
│ - 대시보드│  (선택된 메뉴에 따라 콘텐츠 전환)         │
│ - 키워드 │                                          │
│ - 국내지표│                                          │
│ - 글로벌 │                                          │
│          │                                          │
├──────────┴──────────────────────────────────────────┤
│  Footer (optional)                                  │
└─────────────────────────────────────────────────────┘
```

### 페이지별 상세

#### 1. 대시보드 (홈)
- 키워드 요약 카드 (등록된 키워드 수, 활성/비활성)
- ECOS 주요 지표 요약 (금리, 환율, 물가 핵심 수치)
- 글로벌 지표 하이라이트 (주요 국가 GDP, CPI 등)

#### 2. 키워드 관리
```
┌──────────────────────────────────────────┐
│ [+ 키워드 등록] 버튼                      │
├──────────────────────────────────────────┤
│ 필터: [전체 | 활성 | 비활성]  [국내|해외]  │
├──────────────────────────────────────────┤
│ 키워드 목록 (카드 or 테이블)              │
│ ┌────────────────────────────────┐       │
│ │ 삼성전자  |  국내  | 활성      │       │
│ │         [비활성화] [삭제]      │       │
│ └────────────────────────────────┘       │
│ ┌────────────────────────────────┐       │
│ │ Apple    |  해외  | 활성       │       │
│ │         [비활성화] [삭제]      │       │
│ └────────────────────────────────┘       │
└──────────────────────────────────────────┘
```

- 등록 모달: keyword 입력 + region 선택 (KOREA/GLOBAL)
- 활성화/비활성화 토글
- 삭제 확인 다이얼로그

#### 3. 국내 경제지표 (ECOS)
```
┌──────────────────────────────────────────┐
│ 카테고리 탭: [금리] [물가] [환율] ...     │
├──────────────────────────────────────────┤
│ 지표 테이블                              │
│ ┌─────────┬──────────┬──────┬─────┐     │
│ │ 분류명   │ 지표명    │ 값   │ 단위│     │
│ ├─────────┼──────────┼──────┼─────┤     │
│ │ 시장금리 │ CD(91일) │ 2.85 │ %  │     │
│ │ 시장금리 │ CP(91일) │ 3.12 │ %  │     │
│ └─────────┴──────────┴──────┴─────┘     │
└──────────────────────────────────────────┘
```

- 카테고리 탭으로 전환
- 테이블 형태로 지표 표시
- 주기(cycle) 배지 표시

#### 4. 글로벌 경제지표
```
┌──────────────────────────────────────────┐
│ 카테고리: [무역/GDP] [고용] [물가] ...    │
├──────────────────────────────────────────┤
│ 지표 선택: [핵심소비자물가] [GDP성장률]... │
├──────────────────────────────────────────┤
│ G20 국가 데이터 테이블                    │
│ ┌────────┬────────┬────────┬─────┬────┐ │
│ │ 국가    │ 최신값  │ 이전값  │ 단위│참고│ │
│ ├────────┼────────┼────────┼─────┼────┤ │
│ │ 한국    │  2.1   │  2.3   │  %  │    │ │
│ │ 미국    │  3.2   │  3.0   │  %  │    │ │
│ │ 일본    │  1.8   │  1.5   │  %  │    │ │
│ └────────┴────────┴────────┴─────┴────┘ │
│                                          │
│ 변화율 표시: ▲ 상승(녹색) ▼ 하락(적색)    │
└──────────────────────────────────────────┘
```

- 2단계 네비게이션: 카테고리 → 지표 선택
- 국가별 데이터 테이블 + 변화 방향 표시
- 카테고리 전체 조회 모드 지원

## API 연동 매핑

| 화면 | API | 메서드 |
|------|-----|--------|
| 로그인 | `GET /oauth/kakao?code=` | 카카오 OAuth |
| 회원가입 | `POST /signup` | 가입 완료 |
| 키워드 목록 | `GET /api/keywords?userId=&active=` | 조회 |
| 키워드 등록 | `POST /api/keywords` | 등록 |
| 키워드 활성화 | `PATCH /api/keywords/{id}/activate` | 활성화 |
| 키워드 비활성화 | `PATCH /api/keywords/{id}/deactivate` | 비활성화 |
| 키워드 삭제 | `DELETE /api/keywords/{id}` | 삭제 |
| ECOS 카테고리 | `GET /api/economics/indicators/categories` | 카테고리 목록 |
| ECOS 지표 | `GET /api/economics/indicators?category=` | 지표 조회 |
| 글로벌 카테고리 | `GET /api/economics/global-indicators/categories` | 카테고리 목록 |
| 글로벌 지표 | `GET /api/economics/global-indicators/{type}` | 지표별 조회 |
| 글로벌 카테고리 전체 | `GET /api/economics/global-indicators/categories/{cat}` | 카테고리별 전체 |

## Alpine.js 상태 설계

```javascript
// app.js - 메인 앱 상태
Alpine.data('dashboard', () => ({
    // 라우팅
    currentPage: 'home',        // home | keywords | ecos | global

    // 인증
    auth: {
        token: localStorage.getItem('accessToken'),
        userId: localStorage.getItem('userId'),
        role: null,
        isLoggedIn: false
    },

    // 키워드
    keywords: {
        list: [],
        filter: 'all',          // all | active | inactive
        regionFilter: 'all',    // all | KOREA | GLOBAL
        showAddModal: false,
        newKeyword: { keyword: '', region: 'KOREA' }
    },

    // ECOS 경제지표
    ecos: {
        categories: [],
        selectedCategory: null,
        indicators: [],
        loading: false
    },

    // 글로벌 경제지표
    global: {
        categories: [],
        selectedCategory: null,
        selectedIndicator: null,
        indicatorData: null,
        loading: false
    }
}));
```

## 작업 리스트

1. `api.js` - API 호출 모듈 (fetch 래퍼, JWT 헤더, 에러 처리)
2. `format.js` - 숫자/날짜 포맷 유틸
3. `index.html` - 메인 레이아웃 (Header + Sidebar + Content Area)
4. `app.js` - Alpine.js 메인 앱 상태 및 라우팅
5. `keyword.js` - 키워드 관리 컴포넌트 (CRUD + 필터)
6. `ecos.js` - ECOS 경제지표 컴포넌트 (카테고리 탭 + 테이블)
7. `global.js` - 글로벌 경제지표 컴포넌트 (2단계 네비 + 테이블)
8. `custom.css` - 커스텀 스타일

## 코드 예시

- [api.js 예시](examples/api-example.md)
- [index.html 레이아웃 예시](examples/layout-example.md)
- [keyword 컴포넌트 예시](examples/keyword-example.md)
