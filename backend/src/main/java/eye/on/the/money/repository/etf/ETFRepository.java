package eye.on.the.money.repository.etf;

import eye.on.the.money.model.etf.ETF;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ETFRepository extends JpaRepository<ETF, String> {
    Optional<ETF> findByName(String name);

    Optional<ETF> findByShortName(String shortName);

    Optional<ETF> findByShortNameAndExchange(String shortName, String exhange);

    List<ETF> findAllByOrderByShortNameAsc();
}
