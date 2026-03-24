-- ============================================================
-- 경제지표 메타데이터 description 업데이트 + 누락 지표 추가 (PostgreSQL)
-- ============================================================

-- === 기존 지표 description 업데이트 ===

UPDATE ecos_indicator_metadata SET description = '한국은행이 정하는 기본 금리. 이 금리가 오르면 대출이자가 오르고, 내리면 대출이자가 내려감'
WHERE class_name = '시장금리' AND keystat_name = '한국은행 기준금리';

UPDATE ecos_indicator_metadata SET description = '은행이 발행하는 3개월짜리 예금증서의 금리. 주택담보대출 변동금리의 기준으로 많이 쓰임'
WHERE class_name = '시장금리' AND keystat_name = 'CD(91일)';

UPDATE ecos_indicator_metadata SET description = '시중에 풀린 돈의 총량. 이 수치가 빠르게 늘면 물가가 오를 수 있음'
WHERE class_name = '통화량' AND keystat_name = 'M2(광의통화)(평잔)';

UPDATE ecos_indicator_metadata SET description = '한국 대표 주가지수. 삼성전자 등 대기업 주식의 전체 흐름을 보여줌'
WHERE class_name = '주식' AND keystat_name = 'KOSPI';

UPDATE ecos_indicator_metadata SET description = '중소·벤처기업 중심 주가지수. IT·바이오 등 성장주 흐름을 보여줌'
WHERE class_name = '주식' AND keystat_name = 'KOSDAQ';

UPDATE ecos_indicator_metadata SET description = '1달러를 사는 데 필요한 원화. 숫자가 오르면 원화 가치가 떨어진 것, 내리면 원화 가치가 오른 것'
WHERE class_name = '환율' AND keystat_name = '원/달러';

UPDATE ecos_indicator_metadata SET description = '우리나라 경제가 지난 분기 대비 얼마나 성장했는지를 나타내는 비율'
WHERE class_name = '성장률' AND keystat_name = 'GDP성장률(전기대비)';

UPDATE ecos_indicator_metadata SET description = '공장에서 물건을 얼마나 만들고 있는지를 나타내는 지수. 경기가 좋으면 올라감'
WHERE class_name = '생산' AND keystat_name = '광공업생산지수';

UPDATE ecos_indicator_metadata SET description = '사람들이 물건을 얼마나 사고 있는지를 나타내는 지수. 소비가 활발하면 올라감'
WHERE class_name = '소비' AND keystat_name = '소매판매액지수';

UPDATE ecos_indicator_metadata SET description = '생활물가가 얼마나 올랐는지를 나타내는 지수. 숫자가 빠르게 오르면 장바구니 부담이 커짐'
WHERE class_name = '소비자/생산자 물가' AND keystat_name = '소비자물가지수';

UPDATE ecos_indicator_metadata SET description = '일하고 싶은데 일자리를 못 구한 사람의 비율. 낮을수록 고용 상황이 좋음'
WHERE class_name = '고용' AND keystat_name = '실업률';

UPDATE ecos_indicator_metadata SET description = '현재 일하고 있는 사람의 수. 많을수록 고용 상황이 좋음'
WHERE class_name = '고용' AND keystat_name = '취업자수';

UPDATE ecos_indicator_metadata SET description = '사람들이 경제 상황을 어떻게 느끼는지를 숫자로 표현. 100 이상이면 낙관적, 미만이면 비관적'
WHERE class_name = '심리지표' AND keystat_name = '소비자심리지수(CSI)';

UPDATE ecos_indicator_metadata SET description = '앞으로 경기가 좋아질지 나빠질지 미리 알려주는 지표. 100 위면 호황, 아래면 불황 신호'
WHERE class_name = '경기순환지표' AND keystat_name = '경기선행지수 순환변동치';

UPDATE ecos_indicator_metadata SET description = '우리나라가 해외에 물건을 판 금액. 수출이 늘면 경제에 좋은 신호'
WHERE class_name = '통관수출입' AND keystat_name = '수출(통관기준)';

UPDATE ecos_indicator_metadata SET description = '해외와 주고받은 돈의 차이. 플러스면 벌어들인 돈이 더 많은 것, 마이너스면 나간 돈이 더 많은 것'
WHERE class_name = '국제수지' AND keystat_name = '경상수지';

UPDATE ecos_indicator_metadata SET description = '가계가 은행 등에서 빌린 돈의 총액. 빠르게 늘면 가계 부채 부담이 커지는 신호'
WHERE class_name = '가계' AND keystat_name = '가계신용(잔액)';

UPDATE ecos_indicator_metadata SET description = '전국 집값이 오르고 있는지 내리고 있는지를 보여주는 지수'
WHERE class_name = '부동산 가격' AND keystat_name = '주택매매가격지수';

UPDATE ecos_indicator_metadata SET description = '대한민국에 살고 있는 전체 인구수'
WHERE class_name = '인구' AND keystat_name = '총인구';

UPDATE ecos_indicator_metadata SET description = '국제 원유 가격. 오르면 기름값·물가가 올라가고, 내리면 생활비 부담이 줄어듦'
WHERE class_name = '국제원자재가격' AND keystat_name = '두바이유';

-- === 누락 지표 추가 ===

INSERT INTO ecos_indicator_metadata (class_name, keystat_name, description)
VALUES ('시장금리', '콜금리(익일물)', '은행끼리 하루만 빌려줄 때 적용하는 금리. 기준금리가 바뀌면 가장 먼저 움직임')
ON CONFLICT (class_name, keystat_name) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO ecos_indicator_metadata (class_name, keystat_name, description)
VALUES ('시장금리', 'KORIBOR(3개월)', '은행끼리 3개월간 빌려줄 때의 금리. 변동금리 대출 이자를 정할 때 기준이 됨')
ON CONFLICT (class_name, keystat_name) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO ecos_indicator_metadata (class_name, keystat_name, description)
VALUES ('시장금리', 'CD수익률(91일)', '은행이 발행하는 3개월짜리 예금증서의 금리. 주택담보대출 변동금리의 기준으로 많이 쓰임')
ON CONFLICT (class_name, keystat_name) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO ecos_indicator_metadata (class_name, keystat_name, description)
VALUES ('시장금리', '통안증권수익률(364일)', '한국은행이 발행하는 1년짜리 채권의 금리. 1년 정도 돈을 묶어둘 때 받을 수 있는 수익률 기준')
ON CONFLICT (class_name, keystat_name) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO ecos_indicator_metadata (class_name, keystat_name, description)
VALUES ('시장금리', '국고채수익률(3년)', '나라에서 발행하는 3년짜리 채권의 금리. 적금·예금 금리와 비교할 때 참고하는 대표 지표')
ON CONFLICT (class_name, keystat_name) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO ecos_indicator_metadata (class_name, keystat_name, description)
VALUES ('시장금리', '국고채수익률(5년)', '나라에서 발행하는 5년짜리 채권의 금리. 장기 금리 흐름을 볼 때 참고')
ON CONFLICT (class_name, keystat_name) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO ecos_indicator_metadata (class_name, keystat_name, description)
VALUES ('시장금리', '회사채수익률(3년,AA-)', '신용이 좋은 기업이 발행한 3년짜리 채권의 금리. 국고채보다 높으면 기업 투자심리가 위축된 것')
ON CONFLICT (class_name, keystat_name) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO ecos_indicator_metadata (class_name, keystat_name, description)
VALUES ('여수신금리', '예금은행 수신금리', '은행에 돈을 맡길 때 받는 평균 이자율. 높을수록 예금·적금 이자를 많이 받음')
ON CONFLICT (class_name, keystat_name) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO ecos_indicator_metadata (class_name, keystat_name, description)
VALUES ('여수신금리', '예금은행 대출금리', '은행에서 돈을 빌릴 때 내는 평균 이자율. 높을수록 대출 이자 부담이 큼')
ON CONFLICT (class_name, keystat_name) DO UPDATE SET description = EXCLUDED.description;