package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.koreaap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thlee.stock.market.stockmarket.economics.domain.exception.BondYieldParseException;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondCreditGrade;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYield;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 한국자산평가 응답(JSON 배열)을 도메인 스냅샷으로 바꾼다.
 * 국고채는 GMRI_CODE로, 공모 무보증 회사채는 분류(GMRI_TYPE)·보증 구분(GMRI_SUBTYPE)·등급(GMRI_BOND) 조합으로 고른다.
 * 같은 등급 문자열이 사모 회사채·금융채에도 있으므로 등급만으로 매핑하지 않는다.
 * 오류 메시지는 화면에 그대로 나가므로 출처의 필드명·원문 값은 넣지 않고 로그로만 남긴다.
 */
@Slf4j
@Component
public class KoreaApBondRateParser {

    private static final String TREASURY_CODE = "A101";
    private static final String PUBLIC_CORPORATE_TYPE = "회사채(공모)";
    private static final String UNSECURED_SUBTYPE = "무보증";
    private static final String NOT_PROVIDED = "-";
    /** 공모 무보증 회사채의 원본 만기(개월). 국고채는 도메인 조회 대상 만기만 뽑는다. */
    private static final List<Integer> CORPORATE_MATURITY_MONTHS =
            List.of(3, 6, 9, 12, 18, 24, 30, 36, 48, 60, 84, 120, 240, 360, 600);
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 행이 없으면(휴일·미발표) 빈 스냅샷이다. 형식이 다르거나, 대상 행이 없거나 중복되거나, 수익률이 숫자가 아니면 파싱 오류다.
     */
    public BondYieldSnapshot parse(LocalDate baseDate, String body) {
        JsonNode rows = readRows(body);
        if (rows.isEmpty()) {
            return BondYieldSnapshot.empty(baseDate);
        }

        List<BondYield> yields = new ArrayList<>();
        boolean treasuryFound = false;
        Set<BondCreditGrade> gradesFound = EnumSet.noneOf(BondCreditGrade.class);
        for (JsonNode row : rows) {
            if (isTreasury(row)) {
                if (treasuryFound) {
                    throw new BondYieldParseException("한국자산평가 응답에 국고채 행이 중복되었습니다.");
                }
                treasuryFound = true;
                for (int maturityMonths : BondYieldType.TREASURY_MATURITY_MONTHS) {
                    yields.add(new BondYield(BondYieldType.TREASURY, null, maturityMonths, rate(row, maturityMonths)));
                }
            } else if (isPublicUnsecuredCorporate(row)) {
                Optional<BondCreditGrade> grade = BondCreditGrade.fromLabel(row.path("GMRI_BOND").asText());
                if (grade.isEmpty()) {
                    continue;
                }
                if (!gradesFound.add(grade.get())) {
                    throw new BondYieldParseException("한국자산평가 응답에 공모 무보증 회사채 " + grade.get().getLabel() + " 행이 중복되었습니다.");
                }
                for (int maturityMonths : CORPORATE_MATURITY_MONTHS) {
                    yields.add(new BondYield(BondYieldType.CORPORATE_PUBLIC_UNSECURED, grade.get(), maturityMonths, rate(row, maturityMonths)));
                }
            }
        }

        if (!treasuryFound && gradesFound.isEmpty()) {
            throw new BondYieldParseException("한국자산평가 응답에 국고채·공모 무보증 회사채 행이 없습니다. 응답 구조가 바뀌었을 수 있습니다.");
        }
        return new BondYieldSnapshot(baseDate, yields);
    }

    private JsonNode readRows(String body) {
        if (body == null || body.isBlank()) {
            throw new BondYieldParseException("한국자산평가 응답 본문이 비어 있습니다.");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new BondYieldParseException("한국자산평가 응답 형식을 해석하지 못했습니다.", e);
        }
        if (!root.isArray()) {
            throw new BondYieldParseException("한국자산평가 응답이 목록 형식이 아닙니다.");
        }
        return root;
    }

    private static boolean isTreasury(JsonNode row) {
        return TREASURY_CODE.equals(row.path("GMRI_CODE").asText());
    }

    private static boolean isPublicUnsecuredCorporate(JsonNode row) {
        return PUBLIC_CORPORATE_TYPE.equals(withoutTagsAndSpaces(row.path("GMRI_TYPE").asText()))
                && UNSECURED_SUBTYPE.equals(row.path("GMRI_SUBTYPE").asText().strip());
    }

    /** 표시용 줄바꿈 태그(<br/>)와 공백을 지운다. 예: "회사채<br/>(공모)" → "회사채(공모)" */
    private static String withoutTagsAndSpaces(String text) {
        return WHITESPACE.matcher(HTML_TAG.matcher(text).replaceAll("")).replaceAll("");
    }

    /** 만기 키(M036 = 36개월)의 수익률(%). "-"·공백은 미제공(null)이다. */
    private static BigDecimal rate(JsonNode row, int maturityMonths) {
        String key = String.format("M%03d", maturityMonths);
        JsonNode value = row.get(key);
        if (value == null) {
            log.warn("한국자산평가 응답 만기 항목 누락: key={}", key);
            throw new BondYieldParseException("한국자산평가 응답에 필요한 만기 항목이 없습니다. 응답 구조가 바뀌었을 수 있습니다.");
        }
        if (value.isNull()) {
            return null;
        }
        String text = value.asText().strip();
        if (text.isEmpty() || NOT_PROVIDED.equals(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            log.warn("한국자산평가 수익률 값 해석 실패: key={}, value={}", key, text);
            throw new BondYieldParseException("한국자산평가 응답의 수익률 값을 해석하지 못했습니다.", e);
        }
    }
}
