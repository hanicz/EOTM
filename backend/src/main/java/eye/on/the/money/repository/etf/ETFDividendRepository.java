package eye.on.the.money.repository.etf;

import eye.on.the.money.model.etf.ETFDividend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ETFDividendRepository extends JpaRepository<ETFDividend, Long> {
    List<ETFDividend> findByUserEmailOrderByDividendDate(String userEmail);

    List<ETFDividend> findByUserEmailAndDividendDateBetweenOrderByDividendDate(
            String userEmail, LocalDate from, LocalDate to);

    List<ETFDividend> findByUserEmailOrderByDividendDateDesc(String userEmail);

    void deleteByUserEmailAndIdIn(String userEmail, List<Long> ids);

    Optional<ETFDividend> findByIdAndUserEmail(Long id, String userEmail);
}
