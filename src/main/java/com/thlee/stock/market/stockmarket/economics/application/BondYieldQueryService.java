package com.thlee.stock.market.stockmarket.economics.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Ticker;
import com.thlee.stock.market.stockmarket.economics.domain.exception.BondYieldFetchException;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldLookup;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.service.BondYieldPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 채권 기준수익률 날짜별 조회.
 * 요청일 전체 데이터가 없으면 하루씩 최대 10일 전까지 거슬러 적용일을 찾는다.
 * 캐시는 둘로 나눈다.
 * - 금리 캐시: 실제 기준일 → 하루치 스냅샷. 빈 날짜도 짧게 담는다.
 * - 연결 캐시: 요청일 → 적용일(또는 한도 초과). 폴백 연결은 짧게 담는다.
 * 같은 key 동시 요청은 Caffeine get(key, 로더)의 key별 원자 계산으로 한 번만 조회한다.
 * 로더 예외는 캐시에 남지 않으므로 통신·파싱 실패는 다음 요청에서 다시 시도된다.
 */
@Service
public class BondYieldQueryService {

    /** 요청일 이전으로 거슬러 조회하는 최대 일수. 요청일을 포함해 최대 11개 날짜를 본다. */
    public static final int MAX_FALLBACK_DAYS = 10;

    /** 연결 뒤 적용일 금리가 비었을 때 다시 해석하는 것까지 포함한 최대 해석 횟수 */
    private static final int MAX_RESOLVE_ATTEMPTS = 2;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final BondYieldPort bondYieldPort;
    private final CachePolicy policy;
    private final Ticker ticker;
    private final Clock clock;
    private final Cache<LocalDate, BondYieldSnapshot> rateCache;
    private final Cache<LocalDate, Link> linkCache;

    @Autowired
    public BondYieldQueryService(BondYieldPort bondYieldPort,
                                 @Value("${economics.bond-yield.cache.ttl-hours:24}") long ttlHours,
                                 @Value("${economics.bond-yield.cache.short-ttl-minutes:10}") long shortTtlMinutes,
                                 @Value("${economics.bond-yield.cache.max-size:100}") long maxSize,
                                 @Value("${economics.bond-yield.cache.lookup-timeout-seconds:20}") long lookupTimeoutSeconds) {
        this(bondYieldPort,
                new CachePolicy(Duration.ofHours(ttlHours), Duration.ofMinutes(shortTtlMinutes), maxSize,
                        Duration.ofSeconds(lookupTimeoutSeconds)),
                Ticker.systemTicker(),
                Clock.system(KST));
    }

    BondYieldQueryService(BondYieldPort bondYieldPort, CachePolicy policy, Ticker ticker, Clock clock) {
        this.bondYieldPort = bondYieldPort;
        this.policy = policy;
        this.ticker = ticker;
        this.clock = clock;
        this.rateCache = Caffeine.newBuilder()
                .ticker(ticker)
                .maximumSize(policy.maxSize())
                .expireAfter(Expiry.creating(this::rateTtl))
                .build();
        this.linkCache = Caffeine.newBuilder()
                .ticker(ticker)
                .maximumSize(policy.maxSize())
                .expireAfter(Expiry.creating(this::linkTtl))
                .build();
    }

    /**
     * 요청일 기준수익률을 조회한다. 요청일이 없으면 오늘(KST)이다.
     *
     * @throws IllegalArgumentException 미래 날짜
     * @throws BondYieldFetchException  출처 통신 실패 또는 조회 시간 초과
     */
    public BondYieldLookup lookup(LocalDate requestedDate) {
        LocalDate today = LocalDate.now(clock);
        LocalDate requested = requestedDate == null ? today : requestedDate;
        if (requested.isAfter(today)) {
            throw new IllegalArgumentException("미래 날짜의 금리는 조회할 수 없습니다: " + requested);
        }
        String source = bondYieldPort.sourceName();
        // 연결을 만든 뒤 적용일 금리를 다시 불러온 결과가 비었으면 연결을 버리고 한 번만 다시 해석한다
        for (int attempt = 0; attempt < MAX_RESOLVE_ATTEMPTS; attempt++) {
            Link link = linkCache.get(requested, this::resolve);
            if (link.isNotFound()) {
                return BondYieldLookup.notFound(requested, source);
            }
            BondYieldSnapshot snapshot = rateCache.get(link.appliedDate(), this::load);
            if (!snapshot.isEmpty()) {
                return BondYieldLookup.applied(requested, source, snapshot);
            }
            linkCache.invalidate(requested);
        }
        return BondYieldLookup.notFound(requested, source);
    }

    /** 요청일부터 하루씩 거슬러 데이터가 있는 첫 날짜를 찾는다. 도중 실패는 건너뛰지 않고 그대로 전파한다. */
    private Link resolve(LocalDate requested) {
        long startNanos = ticker.read();
        for (int daysBack = 0; daysBack <= MAX_FALLBACK_DAYS; daysBack++) {
            if (daysBack > 0 && ticker.read() - startNanos >= policy.lookupTimeout().toNanos()) {
                throw new BondYieldFetchException("금리 조회 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.");
            }
            LocalDate date = requested.minusDays(daysBack);
            if (!rateCache.get(date, this::load).isEmpty()) {
                return new Link(date);
            }
        }
        return Link.NOT_FOUND;
    }

    private BondYieldSnapshot load(LocalDate baseDate) {
        BondYieldSnapshot snapshot = bondYieldPort.fetch(baseDate);
        return snapshot == null ? BondYieldSnapshot.empty(baseDate) : snapshot;
    }

    /** 지난 날짜의 공시 금리는 바뀌지 않으므로 길게, 오늘 데이터와 빈 날짜는 공시 반영을 위해 짧게 둔다. */
    private Duration rateTtl(LocalDate baseDate, BondYieldSnapshot snapshot) {
        if (snapshot.isEmpty() || !baseDate.isBefore(LocalDate.now(clock))) {
            return policy.shortTtl();
        }
        return policy.ttl();
    }

    /** 폴백·한도 초과·오늘 연결은 짧게, 지난 날짜의 요청일 = 적용일 연결만 길게 둔다. */
    private Duration linkTtl(LocalDate requested, Link link) {
        if (link.isNotFound() || !link.appliedDate().equals(requested) || !requested.isBefore(LocalDate.now(clock))) {
            return policy.shortTtl();
        }
        return policy.ttl();
    }

    /** 캐시 정책 값 (application.yml economics.bond-yield.cache.*) */
    record CachePolicy(Duration ttl, Duration shortTtl, long maxSize, Duration lookupTimeout) {
    }

    /** 요청일 → 적용일 연결. 적용일이 없으면 한도 초과다. */
    private record Link(LocalDate appliedDate) {
        private static final Link NOT_FOUND = new Link(null);

        boolean isNotFound() {
            return appliedDate == null;
        }
    }
}
