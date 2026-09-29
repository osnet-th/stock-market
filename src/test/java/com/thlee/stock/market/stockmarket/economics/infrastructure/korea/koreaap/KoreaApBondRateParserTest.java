package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.koreaap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.thlee.stock.market.stockmarket.economics.domain.exception.BondYieldParseException;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondCreditGrade;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYield;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldType.CORPORATE_PUBLIC_UNSECURED;
import static com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldType.TREASURY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KoreaApBondRateParserTest {

    private static final LocalDate WEEKDAY = LocalDate.of(2026, 9, 28);
    private static final LocalDate HOLIDAY = LocalDate.of(2026, 5, 5);

    private final KoreaApBondRateParser parser = new KoreaApBondRateParser();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // P1
    @Test
    void 평일_응답에서_국고채와_공모_무보증_회사채만_날짜_등급_만기별로_매핑한다() throws IOException {
        BondYieldSnapshot snapshot = parser.parse(WEEKDAY, fixture("bond-rates-20260928.json"));

        assertThat(snapshot.baseDate()).isEqualTo(WEEKDAY);
        assertThat(snapshot.isEmpty()).isFalse();
        assertThat(snapshot.yields().stream().filter(y -> y.type() == TREASURY).map(BondYield::maturityMonths))
                .containsExactly(12, 36, 60, 120, 240, 360);
        assertThat(rate(snapshot, TREASURY, null, 36)).isEqualTo("4.112");
        assertThat(snapshot.yields().stream().filter(y -> y.type() == CORPORATE_PUBLIC_UNSECURED).map(BondYield::grade).distinct())
                .containsExactlyInAnyOrder(BondCreditGrade.values());
        assertThat(snapshot.yields()).hasSize(6 + 10 * 15);
        // 공모 무보증 BBB- 값이어야 한다 (사모 무보증 BBB-, 기타금융채 BBB-와 섞이면 안 된다)
        assertThat(rate(snapshot, CORPORATE_PUBLIC_UNSECURED, BondCreditGrade.BBB_MINUS, 36)).isEqualTo("10.588");
        assertThat(rate(snapshot, CORPORATE_PUBLIC_UNSECURED, BondCreditGrade.BBB_MINUS, 60)).isEqualTo("10.986");
        // 원문에서 공모 무보증 30년은 '-' → 미제공
        assertThat(find(snapshot, CORPORATE_PUBLIC_UNSECURED, BondCreditGrade.AAA, 360).rate()).isNull();
    }

    // P2
    @Test
    void 대시와_공백은_미제공으로_0은_숫자_0으로_읽는다() throws IOException {
        ArrayNode rows = rows("bond-rates-20260928.json");
        row(rows, "F212").put("M003", "-");
        row(rows, "F212").put("M006", "   ");
        row(rows, "A101").put("M012", "0");

        BondYieldSnapshot snapshot = parser.parse(WEEKDAY, objectMapper.writeValueAsString(rows));

        assertThat(find(snapshot, CORPORATE_PUBLIC_UNSECURED, BondCreditGrade.AAA, 3).rate()).isNull();
        assertThat(find(snapshot, CORPORATE_PUBLIC_UNSECURED, BondCreditGrade.AAA, 6).rate()).isNull();
        assertThat(find(snapshot, TREASURY, null, 12).rate()).isEqualByComparingTo("0");
        assertThat(snapshot.isEmpty()).isFalse();
    }

    // P3
    @Test
    void 공휴일_응답은_예외_없이_빈_스냅샷이다() throws IOException {
        BondYieldSnapshot snapshot = parser.parse(HOLIDAY, fixture("bond-rates-20260505-holiday.json"));

        assertThat(snapshot.baseDate()).isEqualTo(HOLIDAY);
        assertThat(snapshot.isEmpty()).isTrue();
        assertThat(snapshot.yields()).isEmpty();
    }

    // P4
    @Test
    void JSON이_아닌_본문은_파싱_오류다() {
        String blockedPage = "<html><body>The request / response that are contrary to the Web firewall security policies have been blocked.</body></html>";

        assertThatThrownBy(() -> parser.parse(WEEKDAY, blockedPage)).isInstanceOf(BondYieldParseException.class);
    }

    // P5
    @Test
    void 행은_있지만_국고채와_공모_무보증_행이_없으면_파싱_오류다() throws IOException {
        ArrayNode rows = rows("bond-rates-20260928.json");
        ArrayNode privateOnly = objectMapper.createArrayNode();
        rows.forEach(row -> {
            if (row.path("GMRI_TYPE").asText().contains("사모")) {
                privateOnly.add(row);
            }
        });
        assertThat(privateOnly).isNotEmpty();

        assertThatThrownBy(() -> parser.parse(WEEKDAY, objectMapper.writeValueAsString(privateOnly)))
                .isInstanceOf(BondYieldParseException.class);
    }

    // P6
    @Test
    void 같은_등급의_공모_무보증_행이_두_개면_파싱_오류다() throws IOException {
        ArrayNode rows = rows("bond-rates-20260928.json");
        rows.add(row(rows, "F243").deepCopy());

        assertThatThrownBy(() -> parser.parse(WEEKDAY, objectMapper.writeValueAsString(rows)))
                .isInstanceOf(BondYieldParseException.class);
    }

    // P7
    @Test
    void 수익률_칸이_숫자가_아니면_파싱_오류다() throws IOException {
        ArrayNode rows = rows("bond-rates-20260928.json");
        row(rows, "A101").put("M036", "abc");

        assertThatThrownBy(() -> parser.parse(WEEKDAY, objectMapper.writeValueAsString(rows)))
                .isInstanceOf(BondYieldParseException.class);
    }

    // P8
    @Test
    void 대상_행은_있지만_값이_전부_미제공이면_빈_날짜로_판정한다() throws IOException {
        ArrayNode rows = rows("bond-rates-20260928.json");
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            List<String> maturityKeys = node.properties().stream()
                    .map(Map.Entry::getKey)
                    .filter(key -> key.matches("M\\d{3}"))
                    .toList();
            maturityKeys.forEach(key -> node.put(key, "-"));
        }

        BondYieldSnapshot snapshot = parser.parse(WEEKDAY, objectMapper.writeValueAsString(rows));

        assertThat(snapshot.isEmpty()).isTrue();
        assertThat(snapshot.yields()).hasSize(6 + 10 * 15);
    }

    // P9
    @Test
    void 만기_항목이_빠지면_파싱_오류다() throws IOException {
        ArrayNode rows = rows("bond-rates-20260928.json");
        row(rows, "A101").remove("M036");

        assertThatThrownBy(() -> parser.parse(WEEKDAY, objectMapper.writeValueAsString(rows)))
                .isInstanceOf(BondYieldParseException.class);
    }

    private String fixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/koreaap/" + name)) {
            assertThat(in).as("fixture " + name).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private ArrayNode rows(String name) throws IOException {
        return (ArrayNode) objectMapper.readTree(fixture(name));
    }

    private static ObjectNode row(ArrayNode rows, String gmriCode) {
        for (JsonNode row : rows) {
            if (gmriCode.equals(row.path("GMRI_CODE").asText())) {
                return (ObjectNode) row;
            }
        }
        throw new IllegalArgumentException("fixture에 없는 행: " + gmriCode);
    }

    private static String rate(BondYieldSnapshot snapshot, BondYieldType type, BondCreditGrade grade, int maturityMonths) {
        return find(snapshot, type, grade, maturityMonths).rate().toPlainString();
    }

    private static BondYield find(BondYieldSnapshot snapshot, BondYieldType type, BondCreditGrade grade, int maturityMonths) {
        List<BondYield> matches = snapshot.yields().stream()
                .filter(y -> y.type() == type && y.grade() == grade && y.maturityMonths() == maturityMonths)
                .toList();
        assertThat(matches).hasSize(1);
        return matches.get(0);
    }
}
