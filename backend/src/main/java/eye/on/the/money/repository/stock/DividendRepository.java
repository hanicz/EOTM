package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.Dividend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DividendRepository extends JpaRepository<Dividend, Long> {
    List<Dividend> findByUserEmailOrderByDividendDate(String userEmail);

    List<Dividend> findByUserEmailAndDividendDateBetweenOrderByDividendDate(
            String userEmail, LocalDate from, LocalDate to);

    List<Dividend> findByUserEmailOrderByDividendDateDesc(String userEmail);

    void deleteByUserEmailAndIdIn(String userEmail, List<Long> ids);

    Optional<Dividend> findByIdAndUserEmail(Long id, String userEmail);
}
