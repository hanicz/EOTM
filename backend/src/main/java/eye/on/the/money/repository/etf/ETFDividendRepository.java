package eye.on.the.money.repository.etf;

import eye.on.the.money.model.etf.ETFDividend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ETFDividendRepository extends JpaRepository<ETFDividend, Long> {
    List<ETFDividend> findByUserIdOrderByDividendDate(Long userId);

    List<ETFDividend> findByUserIdAndDividendDateBetweenOrderByDividendDate(
            Long userId, LocalDate from, LocalDate to);

    List<ETFDividend> findByUserIdOrderByDividendDateDesc(Long userId);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);

    Optional<ETFDividend> findByIdAndUserId(Long id, Long userId);
}
