package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.Dividend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DividendRepository extends JpaRepository<Dividend, Long> {
    List<Dividend> findByUserIdOrderByDividendDate(Long userId);

    List<Dividend> findByUserIdAndDividendDateBetweenOrderByDividendDate(
            Long userId, LocalDate from, LocalDate to);

    List<Dividend> findByUserIdOrderByDividendDateDesc(Long userId);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);

    Optional<Dividend> findByIdAndUserId(Long id, Long userId);
}
