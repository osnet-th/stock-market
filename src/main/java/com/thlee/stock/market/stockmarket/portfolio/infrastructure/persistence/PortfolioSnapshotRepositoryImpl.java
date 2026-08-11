package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioSnapshot;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PortfolioSnapshotRepositoryImpl implements PortfolioSnapshotRepository {

    private final PortfolioSnapshotJpaRepository jpaRepository;

    @Override
    public PortfolioSnapshot save(PortfolioSnapshot snapshot) {
        return toDomain(jpaRepository.save(toEntity(snapshot)));
    }

    @Override
    public Optional<PortfolioSnapshot> findByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate) {
        return jpaRepository.findByUserIdAndSnapshotDate(userId, snapshotDate).map(this::toDomain);
    }

    @Override
    public List<PortfolioSnapshot> findByUserIdSince(Long userId, LocalDate fromDate) {
        return jpaRepository
                .findByUserIdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(userId, fromDate)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private PortfolioSnapshotEntity toEntity(PortfolioSnapshot s) {
        return new PortfolioSnapshotEntity(
                s.getId(), s.getUserId(), s.getSnapshotDate(),
                s.getTotalEvaluated(), s.getTotalInvested(), s.getCreatedAt()
        );
    }

    private PortfolioSnapshot toDomain(PortfolioSnapshotEntity e) {
        return new PortfolioSnapshot(
                e.getId(), e.getUserId(), e.getSnapshotDate(),
                e.getTotalEvaluated(), e.getTotalInvested(), e.getCreatedAt()
        );
    }
}
