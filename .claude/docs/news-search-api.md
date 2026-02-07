# 뉴스 조회 API 정리

작성일: 2026-02-07

=== 국내 뉴스 === 
### naver(https://developers.naver.com/docs/serviceapi/search/news/news.md
요청 방법
- 엔드포인트: GET https://openapi.naver.com/v1/search/news.json
- 인증 헤더: X-Naver-Client-Id, X-Naver-Client-Secret
- 필수 파라미터: query(검색어, UTF-8 인코딩)
- 주요 옵션: display(기본 10, 최대 100), start(기본 1, 최대 1000), sort(sim/date)

요청 예시
- URL: https://openapi.naver.com/v1/search/news.json?query=현대차&sort=date&display=10
- 헤더: X-Naver-Client-Id=q1ckchPMg8u43uQzNtHS, X-Naver-Client-Secret=Zdj2T88ckX

응답 예시 (JSON)
{
    "lastBuildDate": "Sat, 07 Feb 2026 20:07:59 +0900",
    "total": 1883378,
    "start": 1,
    "display": 10,
    "items": [
        {
            "title": "美증시 흔들리자 ‘서학개미’ 이탈 가속도…‘AI 거품론’ 재점화 [투...",
            "originallink": "https://biz.heraldcorp.com/article/10671480?ref=naver",
            "link": "https://n.news.naver.com/mnews/article/016/0002597483?sid=101",
            "description": "하장권 <b>현대차</b> 증권 연구원은 “20년 만의 혁신 장세임에도 불구하고, 미국 증시는 이익 대비 차분한 흐름을 이어가고 있다”며 “과거 버블을 학습한 듯 노심초사하는 모습이 현 국면이라고 판단된다”고 밝혔다.... ",
            "pubDate": "Sat, 07 Feb 2026 19:40:00 +0900"
        },
        {
            "title": "[코스피 지수선물 옵션] 반도체·2차전지 흔들...금융은 버텼다",
            "originallink": "https://www.pinpointnews.co.kr/news/articleView.html?idxno=426897",
            "link": "https://www.pinpointnews.co.kr/news/articleView.html?idxno=426897",
            "description": "한국거래소에 따르면 삼성전자, SK하이닉스가 동반 하락했고, <b>현대차</b>와 기아, 현대모비스, 현대글로비스 등 자동차 관련 종목들도 조정을 받았다. LG에너지솔루션, 삼성SDI, LG화학, 포스코퓨처엠, 에코프로... ",
            "pubDate": "Sat, 07 Feb 2026 19:14:00 +0900"
        },
        {
            "title": "갈등 넘어 경쟁으로…아틀라스가 <b>현대차</b>에 남긴 숙제",
            "originallink": "https://www.econovill.com/news/articleView.html?idxno=728849",
            "link": "https://www.econovill.com/news/articleView.html?idxno=728849",
            "description": "실제로 <b>현대차</b>그룹의 휴머노이드 로봇 아틀라스는 기업에게는 혁신이었지만 노동 현장에서는 실업의... 자동차와 로보틱스 등 허물어진 기술 경계의 시대에서 <b>현대차</b>의 실행력이 미래 기술 경쟁의 키가 될 전망이다.... ",
            "pubDate": "Sat, 07 Feb 2026 17:00:00 +0900"
        },
        {
            "title": "메리츠證 “HL만도, 미국 로보택시 확산 수혜…목표주가 상향”",
            "originallink": "https://www.viva100.com/article/20260207500103",
            "link": "https://www.viva100.com/article/20260207500103",
            "description": "이 외에도 웨이모와 <b>현대차</b> 아이오닉5에는 브레이크와 서스펜션을 적용했다. 또 다른 로보택시 브랜드에도 스티어링(SbW)과 브레이크 시스템이 탑재된 것으로 전해졌다. 로보택시 업체들의 운영대수 확대 계획도... ",
            "pubDate": "Sat, 07 Feb 2026 14:00:00 +0900"
        },
        {
            "title": "&quot;테슬라 손잡이 보기엔 좋지만 불 나면 갇힌다&quot;…악재 터지나 [이슈+]",
            "originallink": "https://www.hankyung.com/article/202602069356g",
            "link": "https://n.news.naver.com/mnews/article/015/0005248202?sid=103",
            "description": "<b>현대차</b>는 매립형 도어를 채택했으나, 충돌 감지 시 도어 잠금 해제와 동시에 외부 손잡이가 튀어나오도록 설계됐다는 점에서 테슬라와 차별점이 있다. 테슬라의 매립형 도어 손잡이를 열지 못해 차량 사고 시 사망하는... ",
            "pubDate": "Sat, 07 Feb 2026 13:29:00 +0900"
        },
        {
            "title": "[트럼프 스톡커] 韓메모리 ‘무기화’ 걱정, 중국산도 찾는 빅테크",
            "originallink": "https://www.sedaily.com/article/20006157?ref=naver",
            "link": "https://n.news.naver.com/mnews/article/011/0004587811?sid=104",
            "description": "블룸버그통신도 코스피지수가 장중 5000을 처음 넘어선 지난달 22일 삼성전자와 SK하이닉스, <b>현대차</b> 등 대형주들이 한국의 증시 상승을 이끌었다고 진단했다. 그러면서 최근 코스피 랠리가 경기 변동 민감주 위주... ",
            "pubDate": "Sat, 07 Feb 2026 13:01:00 +0900"
        },
        {
            "title": "독일을 넘은 코스피…독일이 놓친 것, 한국이 잡은 것",
            "originallink": "http://weekly.chosun.com/news/articleView.html?idxno=48529",
            "link": "https://n.news.naver.com/mnews/article/053/0000055684?sid=104",
            "description": "한국 기업들의 주식 가치가 급등한 것은 인공지능(AI) 시대 삼성전자, SK하이닉스, <b>현대차</b>와 같은 주요... 655조원, <b>현대차</b>가 약 103조원, LG에너지솔루션이 약 94조원, 삼성바이오로직스가 약 81조원으로 집계된다. 혁신... ",
            "pubDate": "Sat, 07 Feb 2026 13:00:00 +0900"
        },
        {
            "title": "휴림로봇 상승 로봇테마주 전반 약세…뉴로메카·레인보우로보틱스 7% ...",
            "originallink": "https://www.cbci.co.kr/news/articleView.html?idxno=554626",
            "link": "https://www.cbci.co.kr/news/articleView.html?idxno=554626",
            "description": "대형주 가운데서는 <b>현대차</b>는 472,500원으로 16,000원 하락해 -3.28%, 현대모비스는 430,000원으로 3,500원 하락해 -0.81%를 나타냈다. 현대오토에버 역시 396,000원으로 20,000원 하락하며 -4.81%를 기록했다. 휴림로봇은... ",
            "pubDate": "Sat, 07 Feb 2026 12:10:00 +0900"
        },
        {
            "title": "<b>현대차</b>, 유럽 맞춤형 전기차 '아이오닉 3' 4월 첫 공개",
            "originallink": "https://www.ntoday.co.kr/news/articleView.html?idxno=125062",
            "link": "https://www.ntoday.co.kr/news/articleView.html?idxno=125062",
            "description": "[사진=현대자동차] <b>현대차</b>가 오는 4월 이탈리아 밀라노 디자인 위크에서 유럽 맞춤형 전기차 '아이오닉 3... 아이오닉 3 출시로 <b>현대차</b>는 유럽 B세그먼트(전장 3500~3850㎜) 전기차 시장을 본격 공략한다. B세그먼트는... ",
            "pubDate": "Sat, 07 Feb 2026 12:00:00 +0900"
        },
        {
            "title": "AI 중심의 미래사회, 유토피아일까? 디스토피아일까?",
            "originallink": "https://www.jejusori.net/news/articleView.html?idxno=443630",
            "link": "https://www.jejusori.net/news/articleView.html?idxno=443630",
            "description": "/ 편집자 주 아틀라스 로봇 [<b>현대차</b> 제공] 이재명 대통령이 1월 초 중국을 국빈 방문했다. 9년 만의 일이다.... 얼마 전 CES에서 가장 각광받았던 부스는 <b>현대차</b>였다. 아틀라스 로봇이 눈길을 사로잡았다. 부드러운 관절동작과... ",
            "pubDate": "Sat, 07 Feb 2026 11:54:00 +0900"
        }
    ]
}

CLIENT ID: q1ckchPMg8u43uQzNtHS
CLIENT Secret : Zdj2T88ckX



== 해외 뉴스 ==
### https://gnews.io/dashboard
API KEY : f08aef71b604738cd13a89890de2d174

요청 방법
- 엔드포인트: GET https://gnews.io/api/v4/search
- 필수 파라미터: q(검색어), apikey(API 키)
- 주요 옵션: lang(언어), country(국가), max(결과 수), in(검색 대상: title/description/content), from/to(ISO 8601), sortby(publishedAt/relevance), page(페이지)
  - max는 1~100 범위, page는 1부터 시작

요청 예시
- GET https://gnews.io/api/v4/search?q="PMI"&lang=ko&apikey=f08aef71b604738cd13a89890de2d174&in="title"&from=2026-02-02T00:00:00.000Z

응답 예시 (JSON)
{
  "articles": [
    {
      "id": "b961dade95c55b7f949ccd8e0234a356",
      "title": "M5 chip leak reveals Apple has big gains coming in key area",
      "description": "Apple’s forthcoming M5 chip has seemingly leaked as part of a new iPad Pro hardware leak. Here’s what its performance looks like in testing.",
      "content": "Today, Apple’s as-yet-unannounced M5 iPad Pro was seemingly leaked by the same YouTuber who last year leaked the M4 MacBook Pro. Thanks to the surprise reveal, we now have benchmarks for Apple’s forthcoming M5 chip, and they point to big gains coming... [1862 chars]",
      "url": "https://9to5mac.com/2025/09/30/m5-chip-leak-reveals-apple-has-big-gains-coming-in-key-area/",
      "image": "https://i0.wp.com/9to5mac.com/wp-content/uploads/sites/6/2024/12/M5-Pro-chip-could-separate-CPU-and-GPU-in-server-grade-chips.jpg?resize=1200%2C628&quality=82&strip=all&ssl=1",
      "publishedAt": "2025-09-30T19:38:25Z",
      "lang": "en",
      "source": {
        "id": "92f73865e835e33ed68c11447777c939",
        "name": "9to5Mac",
        "url": "https://9to5mac.com",
        "country": "us"
      }
    }
  ]
}

== 해외 뉴스 ==
### https://newsapi.org/docs
API KEY : 7c40f00c0e5d4c12a0de63c6f2cba992

요청 방법
- 엔드포인트: GET https://newsapi.org/v2/everything
- 필수 파라미터: apiKey(쿼리) 또는 X-Api-Key(헤더)
- 주요 옵션: q(검색어), from/to(ISO 8601), language, sortBy(relevancy/popularity/publishedAt), pageSize(최대 100), page

요청 예시
- GET https://newsapi.org/v2/everything?q=bitcoin&apiKey=API_KEY

응답 예시 (필드 구조)
{
  "status": "ok",
  "totalResults": 1234,
  "articles": [
    {
      "source": { "id": "the-verge", "name": "The Verge" },
      "author": "Author Name",
      "title": "Example headline",
      "description": "Example description",
      "url": "https://example.com/article",
      "urlToImage": "https://example.com/image.jpg",
      "publishedAt": "2026-02-07T10:33:59Z",
      "content": "Example content (truncated)"
    }
  ]
}

### https://open-platform.theguardian.com/documentation/search
API KEY : a3501c45-3af9-44ca-96db-5a10a1a72990

요청 방법
- 엔드포인트: GET https://content.guardianapis.com/search
- 인증: API 키 필요(등록 후 발급)
- 주요 옵션: q(검색어), tag(태그/섹션 필터), order-by(newest/oldest/relevance), page, page-size, from-date/to-date(YYYY-MM-DD)

요청 예시
- GET https://content.guardianapis.com/search?api-key=test

응답 예시 (JSON)
{
  "response": {
    "status": "ok",
    "userTier": "developer",
    "total": 2640755,
    "startIndex": 1,
    "pageSize": 10,
    "currentPage": 1,
    "pages": 264076,
    "orderBy": "newest",
    "results": [
      {
        "id": "sport/live/2026/feb/07/winter-olympics-2026-first-gold-medals-mens-downhill-figure-skating-live",
        "type": "liveblog",
        "sectionId": "sport",
        "sectionName": "Sport",
        "webPublicationDate": "2026-02-07T10:37:15Z",
        "webTitle": "Winter Olympics 2026: first gold medal up for grabs in men’s downhill – live",
        "webUrl": "https://www.theguardian.com/sport/live/2026/feb/07/winter-olympics-2026-first-gold-medals-mens-downhill-figure-skating-live",
        "apiUrl": "https://content.guardianapis.com/sport/live/2026/feb/07/winter-olympics-2026-first-gold-medals-mens-downhill-figure-skating-live",
        "isHosted": false,
        "pillarId": "pillar/sport",
        "pillarName": "Sport"
      }
    ]
  }
}
