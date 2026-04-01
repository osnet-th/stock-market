/**
 * FinancialComponent - 재무제표, 배당, 소송, config 데이터
 * 소유 프로퍼티: financialColumns, financialSummaryConfig, financialDescriptions, assetTypeConfig
 * 상태 없음 - 모든 상태는 PortfolioComponent의 portfolio 객체 내에 존재
 */
const FinancialComponent = {
    financialColumns: {
        accounts: [
            { key: 'accountName', label: '계정명', type: 'text', tooltip: true },
            { key: 'fsName', label: '재무제표', type: 'text' },
            { key: 'currentTermAmount', label: '당기', type: 'amount' },
            { key: 'previousTermAmount', label: '전기', type: 'amount' },
            { key: 'beforePreviousTermAmount', label: '전전기', type: 'amount' }
        ],
        indices: [
            { key: 'indexName', label: '지표명', type: 'text', tooltip: true },
            { key: 'indexValue', label: '값', type: 'number' }
        ],
        'full-statements': [
            { key: 'statementName', label: '제표종류', type: 'text' },
            { key: 'accountName', label: '계정명', type: 'text', tooltip: true },
            { key: 'accountDetail', label: '상세', type: 'text' },
            { key: 'currentTermAmount', label: '당기', type: 'amount' },
            { key: 'previousTermAmount', label: '전기', type: 'amount' }
        ],
        'stock-quantities': [
            { key: 'category', label: '구분', type: 'text', tooltip: true },
            { key: 'issuedTotalQuantity', label: '발행주식총수', type: 'amount' },
            { key: 'treasuryStockCount', label: '자기주식수', type: 'amount' },
            { key: 'distributedStockCount', label: '유통주식수', type: 'amount' }
        ],
        dividends: [
            { key: 'category', label: '구분', type: 'text', tooltip: true },
            { key: 'stockKind', label: '주식종류', type: 'text' },
            { key: 'currentTerm', label: '당기', type: 'number' },
            { key: 'previousTerm', label: '전기', type: 'number' },
            { key: 'beforePreviousTerm', label: '전전기', type: 'number' }
        ],
        lawsuits: [
            { key: 'plaintiffName', label: '원고', type: 'text' },
            { key: 'lawsuitAmount', label: '소송금액', type: 'amount' },
            { key: 'claimContent', label: '청구내용', type: 'text' },
            { key: 'currentProgress', label: '현재진행', type: 'text' },
            { key: 'litigationDate', label: '소송제기일', type: 'text' }
        ],
        'private-fund': [
            { key: 'category', label: '구분', type: 'text', tooltip: true },
            { key: 'usePurpose', label: '사용목적', type: 'text' },
            { key: 'planAmount', label: '계획금액', type: 'amount' },
            { key: 'actualAmount', label: '실제금액', type: 'amount' },
            { key: 'differenceReason', label: '차이사유', type: 'text' }
        ],
        'public-fund': [
            { key: 'category', label: '구분', type: 'text', tooltip: true },
            { key: 'usePurpose', label: '사용목적', type: 'text' },
            { key: 'planAmount', label: '계획금액', type: 'amount' },
            { key: 'actualAmount', label: '실제금액', type: 'amount' },
            { key: 'differenceReason', label: '차이사유', type: 'text' }
        ]
    },

    financialSummaryConfig: {
        accounts: [
            { match: '매출액', label: '매출액' },
            { match: '영업이익', label: '영업이익' },
            { match: '당기순이익', label: '당기순이익' },
            { match: '자산총계', label: '자산총계' },
            { match: '부채총계', label: '부채총계' },
            { match: '자본총계', label: '자본총계' }
        ],
        dividends: [
            { match: '주당 현금배당금', label: '주당배당금' },
            { match: '현금배당수익률', label: '배당수익률' },
            { match: '현금배당성향', label: '배당성향' }
        ]
    },

    financialDescriptions: {
        accounts: {
            '매출액': '기업이 제품이나 서비스를 판매하여 벌어들인 총 수입입니다.',
            '영업이익': '매출에서 원가와 판매·관리비를 뺀 금액으로, 본업에서 얼마나 벌었는지 보여줍니다.',
            '영업이익(손실)': '매출에서 원가와 판매·관리비를 뺀 금액입니다. 음수면 본업에서 손실이 난 것입니다.',
            '당기순이익': '모든 수익에서 모든 비용을 뺀 최종 이익입니다. 기업의 실질적 수익력을 나타냅니다.',
            '당기순이익(손실)': '모든 수익에서 모든 비용을 뺀 최종 결과입니다. 음수면 순손실이 발생한 것입니다.',
            '매출원가': '제품을 만들거나 서비스를 제공하는 데 직접 들어간 비용입니다.',
            '매출총이익': '매출에서 매출원가를 뺀 금액입니다. 제품 자체의 수익력을 보여줍니다.',
            '판매비와관리비': '영업활동에 필요한 인건비, 임차료, 광고비 등 간접 비용의 합계입니다.',
            '영업외수익': '본업이 아닌 활동(이자, 배당, 환차익 등)에서 발생한 수익입니다.',
            '영업외비용': '본업이 아닌 활동(이자비용, 환차손 등)에서 발생한 비용입니다.',
            '법인세비용': '기업이 이익에 대해 납부하는 세금입니다.',
            '법인세차감전 순이익': '세금을 내기 전의 이익입니다. 세전이익이라고도 합니다.',
            '법인세차감전 순손익': '세금을 내기 전의 손익입니다. 음수면 세전 단계에서 이미 손실입니다.',
            '총포괄손익': '당기순이익에 환율변동, 자산 재평가 등 기타포괄손익을 더한 총 성과입니다.',
            '이자수익': '예금, 대출, 채권 등에서 받는 이자 수입입니다.',
            '이자비용': '차입금, 사채 등에 대해 지급하는 이자 비용입니다.',
            '수수료수익': '서비스 제공 대가로 받는 수수료 수입입니다.',
            '수수료비용': '서비스 이용 대가로 지급하는 수수료 비용입니다.',
            '자산총계': '기업이 보유한 모든 재산(현금, 건물, 설비 등)의 합계입니다.',
            '유동자산': '1년 이내에 현금화할 수 있는 자산(현금, 매출채권, 재고 등)입니다.',
            '비유동자산': '1년 이상 장기 보유하는 자산(건물, 토지, 설비, 특허 등)입니다.',
            '현금및현금성자산': '즉시 사용할 수 있는 현금과 현금에 준하는 자산입니다.',
            '단기금융상품': '1년 이내 만기의 예금, CD 등 금융상품입니다.',
            '매출채권': '제품을 팔고 아직 받지 못한 대금입니다. 외상 매출금이라고도 합니다.',
            '재고자산': '판매를 위해 보유 중인 제품, 원재료, 반제품 등입니다.',
            '유형자산': '건물, 토지, 기계장치 등 물리적 형태가 있는 자산입니다.',
            '무형자산': '특허권, 영업권 등 물리적 형태가 없는 자산입니다.',
            '투자부동산': '임대수익이나 시세차익을 위해 보유하는 부동산입니다.',
            '사용권자산': '리스 계약으로 사용할 권리를 가진 자산입니다. (IFRS 16 기준)',
            '당기손익-공정가치측정금융자산': '시세 변동에 따라 평가하고 그 손익을 바로 당기이익에 반영하는 금융자산입니다.',
            '기타포괄손익-공정가치측정금융자산': '시세 변동 손익을 당기이익이 아닌 기타포괄손익으로 따로 모아두는 금융자산입니다.',
            '상각후원가측정금융자산': '만기까지 보유하며 이자수익을 인식하는 금융자산(대출, 채권 등)입니다.',
            '관계기업투자': '지분 20~50%를 보유하여 경영에 영향력을 행사하는 기업에 대한 투자입니다.',
            '파생상품자산': '선물, 옵션 등 파생상품 계약에서 발생한 자산(미실현 이익)입니다.',
            '보험계약자산': '보험료 수입이 보험금 지급 의무보다 큰 경우 인식하는 자산입니다.',
            '재보험계약자산': '보험사가 다른 보험사에 위험을 넘긴 재보험 계약에서 발생한 자산입니다.',
            '부채총계': '기업이 갚아야 할 모든 빚의 합계입니다. 자산 대비 비율이 중요합니다.',
            '유동부채': '1년 이내에 갚아야 하는 빚(단기차입금, 미지급금 등)입니다.',
            '비유동부채': '1년 이후에 갚아야 하는 빚(장기차입금, 사채 등)입니다.',
            '차입부채': '은행 대출, 사채 등 이자를 내고 빌린 빚의 총액입니다.',
            '단기차입금': '1년 이내에 갚아야 하는 은행 대출 등 단기 빚입니다.',
            '장기차입금': '1년 이후에 갚아야 하는 장기 대출입니다.',
            '사채': '기업이 자금 조달을 위해 발행한 채권입니다.',
            '리스부채': '리스 계약에 따라 앞으로 지급해야 할 금액입니다.',
            '충당부채': '미래에 지출이 예상되지만 정확한 시기나 금액이 불확실한 부채입니다.',
            '파생상품부채': '선물, 옵션 등 파생상품 계약에서 발생한 부채(미실현 손실)입니다.',
            '보험계약부채': '보험 가입자에게 보험금을 지급해야 할 의무입니다. 보험사의 핵심 부채입니다.',
            '재보험계약부채': '재보험 계약에서 발생한 지급 의무입니다.',
            '자본총계': '자산에서 부채를 뺀 금액으로, 기업의 순수한 재산입니다.',
            '자본금': '주식 발행으로 주주가 납입한 금액입니다. 액면가 × 발행주식수.',
            '자본잉여금': '주식 발행 시 액면가를 초과하여 받은 금액(주식발행초과금 등)입니다.',
            '이익잉여금': '기업이 벌어들인 이익 중 배당하지 않고 쌓아둔 금액입니다.',
            '기타포괄손익누계액': '환율변동, 자산 재평가 등으로 발생한 미실현 손익의 누적액입니다.',
            '기타자본항목': '자기주식, 주식선택권 등 위 항목에 속하지 않는 자본 항목입니다.',
            '비지배지분': '연결 자회사 중 모회사가 소유하지 않은 지분(소수주주 몫)입니다.',
        },
        indices: {
            'PER': '주가를 주당순이익으로 나눈 값으로, 투자금 회수에 걸리는 연수를 뜻합니다. 낮을수록 저평가.',
            'PBR': '주가를 주당순자산으로 나눈 값으로, 1 미만이면 기업 청산가치보다 싸게 거래되는 것입니다.',
            'EPS': '주당순이익. 주식 1주가 벌어들이는 순이익으로, 높을수록 수익성이 좋습니다.',
            'BPS': '주당순자산. 주식 1주에 해당하는 순자산으로, 기업의 내재가치를 나타냅니다.',
            'ROE': '자기자본이익률. 주주가 투자한 돈으로 얼마나 이익을 냈는지 보여줍니다. 높을수록 효율적.',
            'ROA': '총자산이익률. 기업 전체 자산을 활용해 얼마나 이익을 냈는지 보여줍니다.',
            'SPS': '주당매출액. 주식 1주당 매출액으로, 기업의 외형 성장을 확인할 수 있습니다.',
            'CPS': '주당현금흐름. 주식 1주당 영업활동으로 벌어들인 현금입니다.',
            'CFPS': '주당잉여현금흐름. 영업현금에서 투자를 뺀 자유현금을 주당으로 나눈 값입니다.',
            'DPS': '주당배당금. 주식 1주당 지급되는 배당금입니다.',
            '영업이익률': '매출 대비 영업이익 비율입니다. 높을수록 본업의 수익성이 좋습니다.',
            '순이익률': '매출 대비 순이익 비율입니다. 기업의 최종 수익성을 나타냅니다.',
            '매출총이익률': '매출에서 매출원가를 뺀 비율입니다. 제품·서비스 자체의 마진을 보여줍니다.',
            '세전계속사업이익률': '계속되는 사업에서 세금 내기 전 이익의 매출 대비 비율입니다.',
            '총포괄이익률': '당기순이익에 기타포괄손익을 더한 총포괄이익의 매출 대비 비율입니다.',
            '판관비율': '매출 대비 판매비와관리비 비율입니다. 낮을수록 비용 관리가 효율적입니다.',
            '총자산영업이익률': '총자산 대비 영업이익 비율입니다. 자산을 활용해 본업에서 얼마나 벌었는지 봅니다.',
            '총자산세전계속사업이익률': '총자산 대비 세전계속사업이익 비율입니다.',
            '자기자본영업이익률': '자기자본 대비 영업이익 비율입니다. 주주 자본으로 본업에서 얼마나 벌었는지 봅니다.',
            '자기자본세전계속사업이익률': '자기자본 대비 세전계속사업이익 비율입니다.',
            '자본금영업이익률': '납입 자본금 대비 영업이익 비율입니다. 자본금 규모 대비 수익성을 봅니다.',
            '자본금세전계속사업이익률': '납입 자본금 대비 세전계속사업이익 비율입니다.',
            '납입자본이익률': '납입자본(자본금+자본잉여금) 대비 순이익 비율입니다.',
            '영업수익경비율': '영업수익 중 경비가 차지하는 비율입니다. 낮을수록 비용 효율이 높습니다.',
            '부채비율': '자본 대비 부채의 비율입니다. 높을수록 빚에 대한 의존도가 크다는 뜻입니다.',
            '유동비율': '유동부채 대비 유동자산 비율입니다. 100% 이상이면 단기 지급능력이 양호합니다.',
            '당좌비율': '유동자산에서 재고를 뺀 당좌자산의 유동부채 대비 비율입니다.',
            '자기자본비율': '총자산 중 자기자본 비율입니다. 높을수록 재무 안정성이 높습니다.',
            '이자보상배율': '영업이익이 이자비용의 몇 배인지 보여줍니다. 1 미만이면 이자도 못 갚는 상태입니다.',
            '비유동비율': '자기자본 대비 비유동자산 비율입니다.',
            '비유동장기적합률': '(자기자본+비유동부채) 대비 비유동자산 비율입니다. 100% 이하가 안정적입니다.',
            '차입금의존도': '총자산 중 차입금(이자부부채)이 차지하는 비율입니다. 낮을수록 안정적입니다.',
            '순차입금비율': '(차입금−현금성자산)을 자기자본으로 나눈 비율입니다.',
            '영업이익대비이자비용비율': '영업이익 중 이자비용이 차지하는 비율입니다.',
            '매출액증가율': '전기 대비 매출액이 얼마나 늘었는지 보여줍니다.',
            '영업이익증가율': '전기 대비 영업이익 증가율입니다.',
            '순이익증가율': '전기 대비 순이익 증가율입니다.',
            '총자산증가율': '전기 대비 총자산 증가율입니다.',
            '자기자본증가율': '전기 대비 자기자본 증가율입니다.',
            '유형자산증가율': '전기 대비 유형자산 증가율입니다.',
            '부채증가율': '전기 대비 부채 증가율입니다.',
            '총자산회전율': '매출을 총자산으로 나눈 값입니다.',
            '매출채권회전율': '매출을 매출채권으로 나눈 값입니다.',
            '재고자산회전율': '매출원가를 재고자산으로 나눈 값입니다.',
            '매입채무회전율': '매출원가를 매입채무로 나눈 값입니다.',
            '유형자산회전율': '매출을 유형자산으로 나눈 값입니다.',
            '매출채권회전기간': '매출채권을 현금으로 회수하는 데 걸리는 평균 일수입니다.',
            '재고자산회전기간': '재고가 판매되기까지 걸리는 평균 일수입니다.',
            '매입채무회전기간': '매입대금을 지급하기까지 걸리는 평균 일수입니다.',
            '순운전자본회전율': '매출을 순운전자본(유동자산−유동부채)으로 나눈 값입니다.',
        },
        'full-statements': {
            '금융수익': '이자수익, 배당수익, 금융자산 처분이익 등 금융활동에서 발생한 수익입니다.',
            '금융비용': '이자비용, 금융자산 처분손실 등 금융활동에서 발생한 비용입니다.',
            '기타수익': '유형자산 처분이익 등 영업·금융 외의 기타 수익입니다.',
            '기타비용': '유형자산 처분손실, 기부금 등 영업·금융 외의 기타 비용입니다.',
            '금융원가': '차입금 이자, 사채 이자 등 자금 조달에 드는 비용입니다.',
            '감가상각비': '건물, 기계 등 유형자산의 가치 감소를 비용으로 인식한 금액입니다.',
            '대손상각비': '회수 불가능한 매출채권을 비용으로 처리한 금액입니다.',
            '연구개발비': '신제품·기술 개발을 위해 투입한 비용입니다.',
            '지급수수료': '외부 서비스 이용에 대해 지급한 수수료입니다.',
            '급여': '임직원에게 지급한 급여·상여금 등 인건비입니다.',
            '복리후생비': '직원 복지를 위한 식대, 보험료, 건강검진비 등입니다.',
            '임차료': '사무실, 공장 등 임대 공간 사용 대가입니다.',
            '광고선전비': '제품·서비스 홍보를 위한 광고비입니다.',
            '경상연구개발비': '지속적인 연구개발 활동에 투입되는 비용입니다.',
            '외환차익': '환율 변동으로 인해 실현된 이익입니다.',
            '외환차손': '환율 변동으로 인해 실현된 손실입니다.',
            '외화환산이익': '결산 시점 환율 변동으로 발생한 미실현 이익입니다.',
            '외화환산손실': '결산 시점 환율 변동으로 발생한 미실현 손실입니다.',
            '선급금': '물건이나 서비스를 받기 전에 미리 지급한 대금입니다.',
            '선급비용': '이미 지급했지만 아직 비용으로 인식하지 않은 금액입니다.',
            '미수금': '영업 외 거래에서 아직 받지 못한 대금입니다.',
            '미수수익': '이미 발생했지만 아직 받지 못한 수익(미수이자 등)입니다.',
            '미지급금': '물건이나 서비스를 받고 아직 지급하지 않은 대금입니다.',
            '미지급비용': '이미 발생했지만 아직 지급하지 않은 비용(미지급이자 등)입니다.',
            '선수금': '물건이나 서비스를 제공하기 전에 미리 받은 대금입니다.',
            '선수수익': '이미 받았지만 아직 수익으로 인식하지 않은 금액입니다.',
            '예수금': '일시적으로 보관 중인 타인의 자금(원천징수세 등)입니다.',
            '퇴직급여충당부채': '직원 퇴직 시 지급해야 할 퇴직금의 추정 금액입니다.',
            '확정급여부채': '확정급여형 퇴직연금에서 미래 지급할 퇴직급여의 현재가치입니다.',
            '매입채무': '원재료 등을 구매하고 아직 지급하지 않은 대금입니다.',
            '이연법인세자산': '미래에 세금을 줄여줄 수 있는 일시적 차이에서 발생한 자산입니다.',
            '이연법인세부채': '미래에 추가 세금을 내야 하는 일시적 차이에서 발생한 부채입니다.',
            '영업권': '기업 인수 시 순자산 공정가치를 초과하여 지급한 프리미엄입니다.',
            '영업활동현금흐름': '본업에서 실제로 들어오고 나간 현금의 순액입니다.',
            '투자활동현금흐름': '설비 투자, 자산 매각 등에 사용된 현금입니다.',
            '재무활동현금흐름': '차입, 상환, 배당 등 자금 조달·상환에 사용된 현금입니다.',
            '보험수익': '보험료 수입 등 보험 계약에서 발생한 수익입니다.',
            '보험서비스비용': '보험금 지급, 사업비 등 보험 서비스 제공에 들어간 비용입니다.',
            '보험금융수익': '보험부채 관련 이자수익, 투자수익 등입니다.',
            '보험금융비용': '보험부채 관련 이자비용, 투자손실 등입니다.',
            '순보험금융손익': '보험금융수익에서 보험금융비용을 뺀 순 결과입니다.',
            '투자손익': '금융자산 매매, 평가 등에서 발생한 투자 관련 순 손익입니다.',
        },
        'stock-quantities': {
            '보통주': '의결권이 있는 일반 주식입니다.',
            '우선주': '배당을 먼저 받는 대신 의결권이 없는 주식입니다.',
        },
        dividends: {
            '주당액면가액(원)': '주식 1주의 액면가입니다.',
            '(연결)당기순이익(백만원)': '연결 재무제표 기준 당기순이익입니다.',
            '(별도)당기순이익(백만원)': '별도 재무제표 기준 당기순이익입니다.',
            '(연결)주당순이익(원)': '연결 기준 당기순이익을 발행주식수로 나눈 값입니다.',
            '현금배당금총액(백만원)': '전체 주주에게 지급한 현금 배당금의 합계입니다.',
            '주식배당금총액(백만원)': '현금 대신 주식으로 지급한 배당의 총액입니다.',
            '(연결)현금배당성향(%)': '연결 기준 순이익 중 현금배당으로 지급한 비율입니다.',
            '현금배당수익률(%)': '주가 대비 현금배당금 비율입니다.',
            '주식배당수익률(%)': '주가 대비 주식배당의 가치 비율입니다.',
            '주당 현금배당금(원)': '주식 1주당 지급되는 현금 배당금입니다.',
            '주당 주식배당(주)': '주식 1주당 지급되는 주식 배당의 주수입니다.',
            '주당 주식배당금': '현금 대신 주식으로 지급되는 배당입니다.',
            '현금배당수익률': '주가 대비 배당금 비율입니다.',
            '현금배당성향': '순이익 중 배당으로 지급한 비율입니다.',
            '주당 현금배당금': '주식 1주당 지급되는 현금 배당금입니다.',
        },
        'private-fund': {
            '시설자금': '공장, 설비 등 생산시설 투자를 위해 조달한 자금입니다.',
            '영업양수자금': '다른 기업의 영업을 인수하기 위해 조달한 자금입니다.',
            '운영자금': '일상적인 경영활동(원재료 구매, 인건비 등)에 사용하는 자금입니다.',
            '채무상환자금': '기존 빚을 갚기 위해 조달한 자금입니다.',
            '타법인증권취득자금': '다른 기업의 주식이나 채권을 사기 위해 조달한 자금입니다.',
            '기타': '위 항목에 해당하지 않는 기타 용도의 자금입니다.',
        },
        'public-fund': {
            '시설자금': '공장, 설비 등 생산시설 투자를 위해 공모로 조달한 자금입니다.',
            '영업양수자금': '다른 기업의 영업을 인수하기 위해 공모로 조달한 자금입니다.',
            '운영자금': '일상적인 경영활동에 사용하기 위해 공모로 조달한 자금입니다.',
            '채무상환자금': '기존 빚을 갚기 위해 공모로 조달한 자금입니다.',
            '타법인증권취득자금': '다른 기업의 주식이나 채권을 사기 위해 공모로 조달한 자금입니다.',
            '기타': '위 항목에 해당하지 않는 기타 용도의 자금입니다.',
        },
    },

    assetTypeConfig: {
        STOCK:       { label: '주식',     color: 'blue',   barColor: 'bg-blue-500',   chartColor: '#3B82F6' },
        BOND:        { label: '채권',     color: 'green',  barColor: 'bg-green-500',  chartColor: '#22C55E' },
        REAL_ESTATE: { label: '부동산',   color: 'yellow', barColor: 'bg-yellow-500', chartColor: '#EAB308' },
        FUND:        { label: '펀드',     color: 'purple', barColor: 'bg-purple-500', chartColor: '#A855F7' },
        CRYPTO:      { label: '암호화폐', color: 'orange', barColor: 'bg-orange-500', chartColor: '#F97316' },
        GOLD:        { label: '금',       color: 'amber',  barColor: 'bg-amber-500',  chartColor: '#F59E0B' },
        COMMODITY:   { label: '원자재',   color: 'red',    barColor: 'bg-red-500',    chartColor: '#EF4444' },
        CASH:        { label: '현금',     color: 'gray',   barColor: 'bg-gray-500',   chartColor: '#6B7280' },
        OTHER:       { label: '기타',     color: 'slate',  barColor: 'bg-slate-500',  chartColor: '#64748B' }
    },

    getYearOptions() {
        const currentYear = new Date().getFullYear();
        return Array.from({ length: 6 }, (_, i) => String(currentYear - i));
    },

    getDefaultYear() {
        return String(new Date().getFullYear());
    },

    async loadFinancialOptions() {
        if (this.portfolio.financialOptions) return;
        try {
            this.portfolio.financialOptions = await API.getFinancialOptions();
        } catch (e) {
            console.error('재무 옵션 로드 실패:', e);
        }
    },

    async openStockDetail(item) {
        const stockCode = item.stockDetail?.stockCode;
        const country = item.stockDetail?.country;
        if (!stockCode || item.stockDetail?.subType === 'ETF') return;

        // 지원 국가: KR, US. 기타 해외는 미지원
        if (country !== 'KR' && country !== 'US') return;

        this.portfolio.selectedStockItem = item;
        this.portfolio.selectedFinancialMenu = null;
        this.portfolio.financialResult = null;
        this.portfolio.secFinancialData = null;
        this.portfolio.secQuarterlyData = null;
        this.portfolio.secQuarterlyPeriod = 'annual';
        this.portfolio.secMetricsData = null;
        this.portfolio.secFinancialError = null;
        if (this.portfolio._secChartInstance) {
            this.portfolio._secChartInstance.destroy();
            this.portfolio._secChartInstance = null;
        }

        if (country === 'US') {
            // 해외주식: SEC 4개 탭 메뉴
            this.portfolio.financialMenus = this.portfolio._secFinancialMenus;
        } else {
            // 국내주식: 기존 8개 메뉴
            this.portfolio.financialMenus = this.portfolio._krFinancialMenus;
            this.portfolio.financialYear = this.getDefaultYear();
            this.portfolio.financialReportCode = 'ANNUAL';
            await this.loadFinancialOptions();
        }
    },

    closeStockDetail() {
        if (this.portfolio.financialChartInstance) {
            this.portfolio.financialChartInstance.destroy();
            this.portfolio.financialChartInstance = null;
        }
        if (this.portfolio._secChartInstance) {
            this.portfolio._secChartInstance.destroy();
            this.portfolio._secChartInstance = null;
        }
        this.portfolio.selectedStockItem = null;
        this.portfolio.selectedFinancialMenu = null;
        this.portfolio.financialResult = null;
        this.portfolio.secFinancialData = null;
        this.portfolio.secQuarterlyData = null;
        this.portfolio.secQuarterlyPeriod = 'annual';
        this.portfolio.secMetricsData = null;
        this.portfolio.secFinancialError = null;
        this.portfolio.financialMenus = this.portfolio._krFinancialMenus;
    },

    getFinancialColumns() {
        const menu = this.portfolio.selectedFinancialMenu;
        if (menu && menu.startsWith('sec-')) {
            return this.getSecTableColumns(menu);
        }
        return this.financialColumns[menu] || null;
    },

    getFinancialDescription(menuKey, itemName) {
        const menuDescriptions = this.financialDescriptions[menuKey];
        if (menuDescriptions && menuDescriptions[itemName]) return menuDescriptions[itemName];
        if (menuKey === 'full-statements') {
            const accountDescriptions = this.financialDescriptions['accounts'];
            if (accountDescriptions && accountDescriptions[itemName]) return accountDescriptions[itemName];
        }
        return null;
    },

    getFilteredFinancialResult() {
        const result = this.portfolio.financialResult;
        if (!result || result.length === 0) return [];
        const menu = this.portfolio.selectedFinancialMenu;

        if (menu === 'accounts' && this.portfolio.financialAccountFsFilter) {
            const fsFilter = this.portfolio.financialAccountFsFilter;
            return result.filter(row => row.fsName === fsFilter);
        }
        if (menu === 'full-statements' && this.portfolio.financialStatementFilter) {
            const stFilter = this.portfolio.financialStatementFilter;
            return result.filter(row => row.statementName === stFilter);
        }
        return result;
    },

    getFilterOptions(fieldName) {
        const result = this.portfolio.financialResult;
        if (!result || result.length === 0) return [];
        const seen = {};
        const options = [];
        for (let i = 0; i < result.length; i++) {
            const name = result[i][fieldName];
            if (name && !seen[name]) {
                seen[name] = true;
                options.push(name);
            }
        }
        return options;
    },

    getFinancialSummaryCards() {
        const menu = this.portfolio.selectedFinancialMenu;
        const result = this.getFilteredFinancialResult();
        if (!result || result.length === 0) return [];

        const config = this.financialSummaryConfig[menu];
        if (!config) return [];

        const cards = [];
        for (let i = 0; i < config.length; i++) {
            const cfg = config[i];
            for (let j = 0; j < result.length; j++) {
                const row = result[j];
                const name = row.accountName || row.category || '';
                if (name.indexOf(cfg.match) !== -1) {
                    const current = row.currentTermAmount || row.currentTerm || '';
                    const previous = row.previousTermAmount || row.previousTerm || '';
                    const currentNum = parseFloat(String(current).replace(/,/g, '')) || 0;
                    const previousNum = parseFloat(String(previous).replace(/,/g, '')) || 0;
                    const changeRate = previousNum !== 0 ? ((currentNum - previousNum) / Math.abs(previousNum) * 100) : null;
                    cards.push({
                        label: cfg.label,
                        value: Format.compactNumber(current),
                        changeRate: changeRate
                    });
                    break;
                }
            }
        }
        return cards;
    },

    formatFinancialCell(value, type) {
        if (value == null || value === '') return '-';
        if (type === 'amount') {
            return Format.compactNumber(value);
        }
        if (type === 'number') {
            const n = parseFloat(String(value).replace(/,/g, ''));
            return isNaN(n) ? value : Format.number(n);
        }
        return value;
    },

    isAmountColumn(type) {
        return type === 'amount' || type === 'number';
    },

    async selectFinancialMenu(menuKey) {
        this.portfolio.selectedFinancialMenu = menuKey;
        this.portfolio.financialResult = null;
        this.portfolio.financialAccountFsFilter = '';
        this.portfolio.financialStatementFilter = '';

        // 탭 전환 시 차트 인스턴스 정리
        if (this.portfolio._secChartInstance) {
            this.portfolio._secChartInstance.destroy();
            this.portfolio._secChartInstance = null;
        }

        if (menuKey.startsWith('sec-')) {
            await this.loadSecFinancial(menuKey);
        } else {
            await this.loadSelectedFinancial();
        }
    },

    async onFinancialFilterChange() {
        if (!this.portfolio.selectedFinancialMenu) return;
        await this.loadSelectedFinancial();
    },

    async loadSelectedFinancial() {
        const stockCode = this.portfolio.selectedStockItem?.stockDetail?.stockCode;
        const menu = this.portfolio.selectedFinancialMenu;
        if (!stockCode || !menu) return;

        const year = this.portfolio.financialYear;
        const reportCode = this.portfolio.financialReportCode;

        const thisGeneration = ++this.portfolio._financialRequestGeneration;
        this.portfolio.financialLoading = true;
        try {
            let result;
            switch (menu) {
                case 'accounts':
                    result = await API.getFinancialAccounts(stockCode, year, reportCode);
                    break;
                case 'indices':
                    result = await API.getFinancialIndices(stockCode, year, reportCode, this.portfolio.financialIndexClass);
                    break;
                case 'full-statements':
                    result = await API.getFullFinancialStatements(stockCode, year, reportCode, this.portfolio.financialFsDiv);
                    break;
                case 'stock-quantities':
                    result = await API.getFinancialStockQuantities(stockCode, year, reportCode);
                    break;
                case 'dividends':
                    result = await API.getFinancialDividends(stockCode, year, reportCode);
                    break;
                case 'lawsuits':
                    result = await API.getLawsuits(stockCode, year + '0101', year + '1231');
                    break;
                case 'private-fund':
                    result = await API.getPrivateFundUsages(stockCode, year, reportCode);
                    break;
                case 'public-fund':
                    result = await API.getPublicFundUsages(stockCode, year, reportCode);
                    break;
            }
            if (thisGeneration !== this.portfolio._financialRequestGeneration) return;
            this.portfolio.financialResult = result || [];
        } catch (e) {
            if (thisGeneration !== this.portfolio._financialRequestGeneration) return;
            console.error('재무정보 조회 실패:', e);
            this.portfolio.financialResult = [];
        } finally {
            if (thisGeneration === this.portfolio._financialRequestGeneration) {
                this.portfolio.financialLoading = false;
                if (menu === 'accounts' && this.portfolio.financialResult && this.portfolio.financialResult.length > 0) {
                    this.$nextTick(() => {
                        this.renderFinancialBarChart();
                    });
                }
            }
        }
    },

    // === SEC (해외주식) 재무제표 ===

    secFinancialColumns: {
        'sec-income': [
            { key: 'label', label: '항목', type: 'text' }
        ],
        'sec-balance': [
            { key: 'label', label: '항목', type: 'text' }
        ],
        'sec-cashflow': [
            { key: 'label', label: '항목', type: 'text' }
        ],
        'sec-metrics': [
            { key: 'name', label: '지표', type: 'text' },
            { key: 'formattedValue', label: '값', type: 'text' },
            { key: 'description', label: '설명', type: 'text' }
        ]
    },

    _secStatementTypeMap: {
        'sec-income': 'INCOME',
        'sec-balance': 'BALANCE',
        'sec-cashflow': 'CASHFLOW'
    },

    async loadSecFinancial(menuKey) {
        const ticker = this.portfolio.selectedStockItem?.stockDetail?.stockCode;
        if (!ticker) return;

        const thisGeneration = ++this.portfolio._financialRequestGeneration;
        this.portfolio.financialLoading = true;
        this.portfolio.secFinancialError = null;

        try {
            if (menuKey === 'sec-metrics') {
                if (!this.portfolio.secMetricsData) {
                    this.portfolio.secMetricsData = await API.getSecInvestmentMetrics(ticker);
                }
                if (thisGeneration !== this.portfolio._financialRequestGeneration) return;
                this.portfolio.financialResult = this.portfolio.secMetricsData.map(m => ({
                    name: m.name,
                    formattedValue: this.formatSecMetricValue(m.value, m.unit),
                    description: m.description
                }));
            } else {
                const isQuarterly = this.portfolio.secQuarterlyPeriod === 'quarterly';

                if (isQuarterly) {
                    if (!this.portfolio.secQuarterlyData) {
                        this.portfolio.secQuarterlyData = await API.getSecQuarterlyStatements(ticker);
                    }
                    if (thisGeneration !== this.portfolio._financialRequestGeneration) return;
                    const targetType = this._secStatementTypeMap[menuKey];
                    const statement = this.portfolio.secQuarterlyData.find(s => s.statementType === targetType);
                    this.portfolio.financialResult = statement?.items
                        ? this.buildSecTableRows(statement.items) : [];
                } else {
                    if (!this.portfolio.secFinancialData) {
                        this.portfolio.secFinancialData = await API.getSecFinancialStatements(ticker);
                    }
                    if (thisGeneration !== this.portfolio._financialRequestGeneration) return;
                    const targetType = this._secStatementTypeMap[menuKey];
                    const statement = this.portfolio.secFinancialData.find(s => s.statementType === targetType);
                    this.portfolio.financialResult = statement?.items
                        ? this.buildSecTableRows(statement.items) : [];
                }
            }
        } catch (e) {
            if (thisGeneration !== this.portfolio._financialRequestGeneration) return;
            console.error('SEC 재무정보 조회 실패:', e);
            this.portfolio.financialResult = [];
            this.portfolio.secFinancialError = this.getSecErrorMessage(e);
        } finally {
            if (thisGeneration === this.portfolio._financialRequestGeneration) {
                this.portfolio.financialLoading = false;
                if (menuKey !== 'sec-metrics' && this.portfolio.financialResult && this.portfolio.financialResult.length > 0) {
                    this.$nextTick(() => {
                        this.renderSecBarChart();
                    });
                }
            }
        }
    },

    async toggleSecPeriod(period) {
        this.portfolio.secQuarterlyPeriod = period;
        const menu = this.portfolio.selectedFinancialMenu;
        if (menu && menu.startsWith('sec-') && menu !== 'sec-metrics') {
            await this.loadSecFinancial(menu);
        }
    },

    buildSecTableRows(items) {
        if (!items || items.length === 0) return [];

        // 키: "2024" (연간) 또는 "2024Q1" (분기)
        const periods = Object.keys(items[0].values || {}).sort().reverse();

        return items.map(item => {
            const row = { label: item.label };
            for (const period of periods) {
                const val = item.values ? item.values[period] : null;
                row['p' + period] = val !== null && val !== undefined ? Format.usd(val) : '-';
            }
            return row;
        });
    },

    getSecTableColumns(menuKey) {
        if (menuKey === 'sec-metrics') {
            return this.secFinancialColumns['sec-metrics'];
        }

        const result = this.portfolio.financialResult;
        if (!result || result.length === 0) return this.secFinancialColumns[menuKey];

        // 동적으로 기간 컬럼 생성
        const firstRow = result[0];
        const cols = [{ key: 'label', label: '항목', type: 'text' }];
        const periodKeys = Object.keys(firstRow).filter(k => k.startsWith('p')).sort().reverse();
        for (const pk of periodKeys) {
            const period = pk.substring(1);
            const label = period.includes('Q') ? period.replace('Q', ' Q') : period + '년';
            cols.push({ key: pk, label: label, type: 'text' });
        }
        return cols;
    },

    formatSecMetricValue(value, unit) {
        if (value === null || value === undefined) return '-';
        if (unit === '$') return '$' + Format.number(value);
        if (unit === '%') return Format.percent(value);
        if (unit === 'x') return Format.multiple(value);
        return String(value);
    },

    getSecErrorMessage(error) {
        const msg = error?.message || '';
        if (msg.includes('404') || msg.includes('SEC_NOT_FOUND')) {
            return '해당 종목의 SEC 재무 데이터를 찾을 수 없습니다';
        }
        if (msg.includes('429') || msg.includes('RATE_LIMIT')) {
            return 'SEC 데이터 요청이 제한되었습니다. 잠시 후 다시 시도해주세요';
        }
        return 'SEC 데이터 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요';
    },

    isSecMenu() {
        const menu = this.portfolio.selectedFinancialMenu;
        return menu && menu.startsWith('sec-');
    },

    // === SEC 요약 카드 ===

    _secSummaryConfig: {
        'sec-income': [
            { label: '매출', labelEn: 'Revenue' },
            { label: '영업이익', labelEn: 'Operating Income' },
            { label: '순이익', labelEn: 'Net Income' }
        ],
        'sec-balance': [
            { label: '총자산', labelEn: 'Total Assets' },
            { label: '총부채', labelEn: 'Total Liabilities' },
            { label: '자기자본', labelEn: 'Stockholders Equity' }
        ],
        'sec-cashflow': [
            { label: '영업활동 현금흐름', labelEn: 'Operating Cash Flow' },
            { label: '잉여현금흐름(FCF)', labelEn: 'Free Cash Flow' },
            { label: '설비투자(CapEx)', labelEn: 'Capital Expenditure' }
        ]
    },

    getSecSummaryCards() {
        const menu = this.portfolio.selectedFinancialMenu;
        const result = this.portfolio.financialResult;
        if (!result || result.length === 0) return [];

        const config = this._secSummaryConfig[menu];
        if (!config) return [];

        // 기간 키 추출 (p2024, p2024Q1 등)
        const firstRow = result[0];
        const periodKeys = Object.keys(firstRow).filter(k => k.startsWith('p')).sort().reverse();
        if (periodKeys.length === 0) return [];

        const latestKey = periodKeys[0];
        const prevKey = periodKeys.length > 1 ? periodKeys[1] : null;

        const cards = [];
        for (const cfg of config) {
            const row = result.find(r => r.label === cfg.label);
            if (!row) continue;

            const currentRaw = row[latestKey];
            const prevRaw = prevKey ? row[prevKey] : null;
            const currentNum = this._parseUsdValue(currentRaw);
            const prevNum = this._parseUsdValue(prevRaw);
            const changeRate = (prevNum !== null && prevNum !== 0 && currentNum !== null)
                ? ((currentNum - prevNum) / Math.abs(prevNum) * 100) : null;

            cards.push({
                label: cfg.label,
                value: currentRaw || '-',
                changeRate: changeRate
            });
        }
        return cards;
    },

    _parseUsdValue(formatted) {
        if (!formatted || formatted === '-') return null;
        const str = String(formatted).replace(/[$,]/g, '');
        const multipliers = { 'T': 1e12, 'B': 1e9, 'M': 1e6, 'K': 1e3 };
        const lastChar = str.charAt(str.length - 1);
        if (multipliers[lastChar]) {
            return parseFloat(str.slice(0, -1)) * multipliers[lastChar];
        }
        const num = parseFloat(str);
        return isNaN(num) ? null : num;
    },

    // === SEC 바 차트 ===

    renderSecBarChart() {
        const canvas = document.getElementById('secBarChart');
        if (!canvas) return;

        if (this.portfolio._secChartInstance) {
            this.portfolio._secChartInstance.destroy();
            this.portfolio._secChartInstance = null;
        }

        const menu = this.portfolio.selectedFinancialMenu;
        const result = this.portfolio.financialResult;
        const config = this._secSummaryConfig[menu];
        if (!result || result.length === 0 || !config) return;

        // 기간 키 추출
        const firstRow = result[0];
        const periodKeys = Object.keys(firstRow).filter(k => k.startsWith('p')).sort();

        const colors = ['#3B82F6', '#93C5FD', '#DBEAFE', '#60A5FA', '#2563EB', '#1D4ED8', '#BFDBFE', '#EFF6FF'];
        const datasets = periodKeys.map((pk, idx) => {
            const period = pk.substring(1);
            const label = period.includes('Q') ? period.replace('Q', ' Q') : period + '년';
            const data = config.map(cfg => {
                const row = result.find(r => r.label === cfg.label);
                return row ? (this._parseUsdValue(row[pk]) || 0) : 0;
            });
            return {
                label: label,
                data: data,
                backgroundColor: colors[idx % colors.length],
                borderRadius: 4
            };
        });

        const labels = config.map(cfg => cfg.label);

        this.portfolio._secChartInstance = new Chart(canvas, {
            type: 'bar',
            data: { labels, datasets },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                        labels: { usePointStyle: true, pointStyle: 'rect', font: { size: 11 } }
                    },
                    tooltip: {
                        callbacks: {
                            label: (ctx) => ctx.dataset.label + ': ' + Format.usd(ctx.parsed.y)
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: (value) => Format.usd(value),
                            font: { size: 11 }
                        },
                        grid: { color: '#F3F4F6' }
                    },
                    x: {
                        ticks: { font: { size: 11 } },
                        grid: { display: false }
                    }
                }
            }
        });
    },

    // === SEC 투자지표 카드 ===

    getSecMetricCards() {
        const metrics = this.portfolio.secMetricsData;
        if (!metrics || metrics.length === 0) return [];

        return metrics.map(m => ({
            name: m.name,
            value: this.formatSecMetricValue(m.value, m.unit),
            description: m.description,
            unit: m.unit
        }));
    },

    parseAmount(value) {
        if (!value) return 0;
        const num = parseFloat(String(value).replace(/,/g, ''));
        return isNaN(num) ? 0 : num;
    },

    renderFinancialBarChart() {
        const canvas = document.getElementById('financialBarChart');
        if (!canvas) return;

        if (this.portfolio.financialChartInstance) {
            this.portfolio.financialChartInstance.destroy();
            this.portfolio.financialChartInstance = null;
        }

        const result = this.getFilteredFinancialResult();
        const config = this.financialSummaryConfig.accounts;
        if (!result || result.length === 0 || !config) return;

        const labels = [];
        const currentData = [];
        const previousData = [];
        const beforePreviousData = [];

        for (let i = 0; i < config.length; i++) {
            const cfg = config[i];
            for (let j = 0; j < result.length; j++) {
                const row = result[j];
                const name = row.accountName || '';
                if (name.indexOf(cfg.match) !== -1) {
                    labels.push(cfg.label);
                    currentData.push(this.parseAmount(row.currentTermAmount));
                    previousData.push(this.parseAmount(row.previousTermAmount));
                    beforePreviousData.push(this.parseAmount(row.beforePreviousTermAmount));
                    break;
                }
            }
        }

        if (labels.length === 0) return;

        this.portfolio.financialChartInstance = new Chart(canvas, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [
                    { label: '당기', data: currentData, backgroundColor: '#3B82F6', borderRadius: 4 },
                    { label: '전기', data: previousData, backgroundColor: '#93C5FD', borderRadius: 4 },
                    { label: '전전기', data: beforePreviousData, backgroundColor: '#DBEAFE', borderRadius: 4 }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                        labels: { usePointStyle: true, pointStyle: 'rect', font: { size: 11 } }
                    },
                    tooltip: {
                        callbacks: {
                            label: (context) => context.dataset.label + ': ' + Format.compactNumber(context.parsed.y)
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: (value) => Format.compactNumber(value),
                            font: { size: 11 }
                        },
                        grid: { color: '#F3F4F6' }
                    },
                    x: {
                        ticks: { font: { size: 11 } },
                        grid: { display: false }
                    }
                }
            }
        });
    }
};