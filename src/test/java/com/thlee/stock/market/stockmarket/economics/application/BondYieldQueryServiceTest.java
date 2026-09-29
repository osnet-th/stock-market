package com.thlee.stock.market.stockmarket.economics.application;

import com.github.benmanes.caffeine.cache.Ticker;
import com.thlee.stock.market.stockmarket.economics.domain.exception.BondYieldFetchException;
import com.thlee.stock.market.stockmarket.economics.domain.exception.BondYieldParseException;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondCreditGrade;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYield;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldLookup;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldType;
import com.thlee.stock.market.stockmarket.economics.domain.service.BondYieldPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import static com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldLookup.Status.FALLBACK;
import static com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldLookup.Status.FOUND;
import static com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldLookup.Status.NOT_FOUND;
import static com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldType.CORPORATE_PUBLIC_UNSECURED;
import static com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldType.TREASURY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BondYieldQueryServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 29);
    private static final String SOURCE = "테스트 출처";

    private FakeTime time;
    private StubPort port;
    private BondYieldQueryService service;

    @BeforeEach
    void setUp() {
        time = new FakeTime(TODAY.atTime(10, 0).atZone(KST).toInstant());
        port = new StubPort(time);
        BondYieldQueryService.CachePolicy policy = new BondYieldQueryService.CachePolicy(
                Duration.ofHours(24), Duration.ofMinutes(10), 100, Duration.ofSeconds(20));
        service = new BondYieldQueryService(port, policy, time, time.clock());
    }

    // S1
    @Test
    void 요청일_데이터가_있으면_요청일을_기준일로_돌려준다() {
        LocalDate date = LocalDate.of(2026, 9, 25);
        port.data(date, "3.100");

        BondYieldLookup result = service.lookup(date);

        assertThat(result.status()).isEqualTo(FOUND);
        assertThat(result.requestedDate()).isEqualTo(date);
        assertThat(result.baseDate()).isEqualTo(date);
        assertThat(result.source()).isEqualTo(SOURCE);
        assertThat(treasury3y(result)).isEqualByComparingTo("3.100");
    }

    // S2
    @Test
    void 요청일_전체가_비면_데이터가_있는_이전_날짜로_폴백한다() {
        LocalDate sunday = LocalDate.of(2026, 9, 27);
        LocalDate friday = LocalDate.of(2026, 9, 25);
        port.data(friday, "3.200");

        BondYieldLookup result = service.lookup(sunday);

        assertThat(result.status()).isEqualTo(FALLBACK);
        assertThat(result.requestedDate()).isEqualTo(sunday);
        assertThat(result.baseDate()).isEqualTo(friday);
        assertThat(treasury3y(result)).isEqualByComparingTo("3.200");
    }

    // S3
    @Test
    void 요청일로부터_10일_전_데이터까지_폴백한다() {
        LocalDate requested = LocalDate.of(2026, 9, 20);
        port.data(requested.minusDays(10), "3.300");

        BondYieldLookup result = service.lookup(requested);

        assertThat(result.status()).isEqualTo(FALLBACK);
        assertThat(result.baseDate()).isEqualTo(requested.minusDays(10));
    }

    // S4
    @Test
    void 요청일_포함_11개_날짜가_모두_비면_한도_초과다() {
        LocalDate requested = LocalDate.of(2026, 9, 20);
        port.data(requested.minusDays(11), "3.400");

        BondYieldLookup result = service.lookup(requested);

        assertThat(result.status()).isEqualTo(NOT_FOUND);
        assertThat(result.requestedDate()).isEqualTo(requested);
        assertThat(result.baseDate()).isNull();
        assertThat(result.yields()).isEmpty();
    }

    // S5
    @Test
    void 일부_등급_만기만_결측이면_폴백하지_않고_미제공으로_둔다() {
        LocalDate date = LocalDate.of(2026, 9, 25);
        port.respond(date, call -> new BondYieldSnapshot(date, List.of(
                new BondYield(TREASURY, null, 36, new BigDecimal("3.100")),
                new BondYield(CORPORATE_PUBLIC_UNSECURED, BondCreditGrade.AAA, 600, null))));
        port.data(date.minusDays(1), "9.999");

        BondYieldLookup result = service.lookup(date);

        assertThat(result.status()).isEqualTo(FOUND);
        assertThat(result.baseDate()).isEqualTo(date);
        assertThat(find(result, CORPORATE_PUBLIC_UNSECURED, BondCreditGrade.AAA, 600).rate()).isNull();
    }

    // S6
    @Test
    void 통신_오류는_빈_날짜로_보지_않고_실패로_전파한다() {
        LocalDate date = LocalDate.of(2026, 9, 25);
        port.fail(date, new BondYieldFetchException("통신 실패"));
        port.data(date.minusDays(1), "3.100");

        assertThatThrownBy(() -> service.lookup(date)).isInstanceOf(BondYieldFetchException.class);
    }

    // S7
    @Test
    void 폴백_중_파싱_오류가_나면_다음_날짜로_건너뛰지_않고_실패한다() {
        LocalDate date = LocalDate.of(2026, 9, 27);
        port.fail(date.minusDays(1), new BondYieldParseException("형식 오류"));
        port.data(date.minusDays(2), "3.100");

        assertThatThrownBy(() -> service.lookup(date)).isInstanceOf(BondYieldParseException.class);
    }

    // S8
    @Test
    void 실패한_조회는_캐시되지_않아_다음_조회에서_다시_시도한다() {
        LocalDate date = LocalDate.of(2026, 9, 25);
        port.respond(date, call -> {
            if (call == 1) {
                throw new BondYieldFetchException("일시 장애");
            }
            return snapshot(date, "3.100");
        });

        assertThatThrownBy(() -> service.lookup(date)).isInstanceOf(BondYieldFetchException.class);
        BondYieldLookup retried = service.lookup(date);

        assertThat(retried.status()).isEqualTo(FOUND);
        assertThat(treasury3y(retried)).isEqualByComparingTo("3.100");
    }

    // S9
    @Test
    void 같은_날짜_재조회는_캐시된_금리를_돌려준다() {
        LocalDate date = LocalDate.of(2026, 9, 25);
        port.respond(date, call -> snapshot(date, "3.10" + call));

        BondYieldLookup first = service.lookup(date);
        BondYieldLookup second = service.lookup(date);

        assertThat(treasury3y(first)).isEqualByComparingTo("3.101");
        assertThat(treasury3y(second)).isEqualByComparingTo("3.101");
    }

    // S10
    @Test
    void 과거_기준일_캐시는_24시간이_지나면_다시_조회한다() {
        LocalDate date = LocalDate.of(2026, 9, 25);
        port.respond(date, call -> snapshot(date, "3.10" + call));
        BondYieldLookup first = service.lookup(date);

        time.advance(Duration.ofHours(24).plusMinutes(1));
        BondYieldLookup second = service.lookup(date);

        assertThat(treasury3y(first)).isEqualByComparingTo("3.101");
        assertThat(treasury3y(second)).isEqualByComparingTo("3.102");
    }

    // S11
    @Test
    void 오늘_폴백은_10분_뒤_다시_해석해_당일_공시를_반영한다() {
        LocalDate yesterday = TODAY.minusDays(1);
        port.data(yesterday, "3.100");
        BondYieldLookup beforePublish = service.lookup(TODAY);
        port.data(TODAY, "3.200");

        time.advance(Duration.ofMinutes(9));
        BondYieldLookup after9Minutes = service.lookup(TODAY);
        time.advance(Duration.ofMinutes(2));
        BondYieldLookup after11Minutes = service.lookup(TODAY);

        assertThat(beforePublish.status()).isEqualTo(FALLBACK);
        assertThat(after9Minutes.status()).isEqualTo(FALLBACK);
        assertThat(after9Minutes.baseDate()).isEqualTo(yesterday);
        assertThat(after11Minutes.status()).isEqualTo(FOUND);
        assertThat(after11Minutes.baseDate()).isEqualTo(TODAY);
        assertThat(treasury3y(after11Minutes)).isEqualByComparingTo("3.200");
    }

    // S12
    @Test
    void 미래_날짜는_거부한다() {
        assertThatThrownBy(() -> service.lookup(TODAY.plusDays(1))).isInstanceOf(IllegalArgumentException.class);
    }

    // S13
    @Test
    void 날짜를_생략하면_오늘_KST로_조회한다() {
        port.data(TODAY, "3.100");

        BondYieldLookup result = service.lookup(null);

        assertThat(result.requestedDate()).isEqualTo(TODAY);
        assertThat(result.status()).isEqualTo(FOUND);
    }

    // S14
    @Test
    void 전체_조회가_20초_상한을_넘기면_시간_초과로_실패한다() {
        port.delayPerFetch(Duration.ofSeconds(8));
        LocalDate date = LocalDate.of(2026, 9, 20);

        assertThatThrownBy(() -> service.lookup(date)).isInstanceOf(BondYieldFetchException.class);
    }

    // S15
    @Test
    void 같은_날짜_동시_요청은_한_번의_조회_결과를_함께_받는다() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 25);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        port.respond(date, call -> {
            if (call == 1) {
                entered.countDown();
                await(release);
            }
            return snapshot(date, "3.10" + call);
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<BondYieldLookup> first = executor.submit(() -> service.lookup(date));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            AtomicReference<Thread> secondThread = new AtomicReference<>();
            Future<BondYieldLookup> second = executor.submit(() -> {
                secondThread.set(Thread.currentThread());
                return service.lookup(date);
            });
            waitUntilWaiting(secondThread);
            release.countDown();

            assertThat(treasury3y(first.get(5, TimeUnit.SECONDS))).isEqualByComparingTo("3.101");
            assertThat(treasury3y(second.get(5, TimeUnit.SECONDS))).isEqualByComparingTo("3.101");
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private static BondYieldSnapshot snapshot(LocalDate date, String rate) {
        return new BondYieldSnapshot(date, List.of(
                new BondYield(TREASURY, null, 36, new BigDecimal(rate)),
                new BondYield(CORPORATE_PUBLIC_UNSECURED, BondCreditGrade.BBB_MINUS, 36, new BigDecimal(rate))));
    }

    private static BigDecimal treasury3y(BondYieldLookup lookup) {
        return find(lookup, TREASURY, null, 36).rate();
    }

    private static BondYield find(BondYieldLookup lookup, BondYieldType type, BondCreditGrade grade, int maturityMonths) {
        return lookup.yields().stream()
                .filter(y -> y.type() == type && y.grade() == grade && y.maturityMonths() == maturityMonths)
                .findFirst()
                .orElseThrow();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("대기 시간 초과");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void waitUntilWaiting(AtomicReference<Thread> threadRef) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            Thread thread = threadRef.get();
            if (thread != null && (thread.getState() == Thread.State.BLOCKED
                    || thread.getState() == Thread.State.WAITING
                    || thread.getState() == Thread.State.TIMED_WAITING)) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("두 번째 요청이 대기 상태에 들어가지 않았습니다.");
    }

    /** 날짜별 응답을 정해 두는 테스트용 포트. 정하지 않은 날짜는 빈 날짜다. 응답 함수는 해당 날짜의 몇 번째 호출인지 받는다. */
    private static final class StubPort implements BondYieldPort {
        private final FakeTime time;
        private final Map<LocalDate, IntFunction<BondYieldSnapshot>> responses = new ConcurrentHashMap<>();
        private final Map<LocalDate, AtomicInteger> calls = new ConcurrentHashMap<>();
        private volatile Duration delayPerFetch = Duration.ZERO;

        private StubPort(FakeTime time) {
            this.time = time;
        }

        void respond(LocalDate date, IntFunction<BondYieldSnapshot> response) {
            responses.put(date, response);
        }

        void data(LocalDate date, String rate) {
            respond(date, call -> snapshot(date, rate));
        }

        void fail(LocalDate date, RuntimeException failure) {
            respond(date, call -> {
                throw failure;
            });
        }

        void delayPerFetch(Duration delay) {
            this.delayPerFetch = delay;
        }

        @Override
        public BondYieldSnapshot fetch(LocalDate baseDate) {
            time.advance(delayPerFetch);
            int call = calls.computeIfAbsent(baseDate, d -> new AtomicInteger()).incrementAndGet();
            return responses.getOrDefault(baseDate, c -> BondYieldSnapshot.empty(baseDate)).apply(call);
        }

        @Override
        public String sourceName() {
            return SOURCE;
        }
    }

    /** Caffeine 만료와 벽시계 상한, 오늘(KST) 판단이 같은 가짜 시간을 쓰게 한다. */
    private static final class FakeTime implements Ticker {
        private final Instant start;
        private final AtomicLong elapsedNanos = new AtomicLong();

        private FakeTime(Instant start) {
            this.start = start;
        }

        @Override
        public long read() {
            return elapsedNanos.get();
        }

        void advance(Duration duration) {
            elapsedNanos.addAndGet(duration.toNanos());
        }

        Clock clock() {
            return new Clock() {
                @Override
                public ZoneId getZone() {
                    return KST;
                }

                @Override
                public Clock withZone(ZoneId zone) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Instant instant() {
                    return start.plusNanos(elapsedNanos.get());
                }
            };
        }
    }
}
