package eye.on.the.money.repository.etf;

import eye.on.the.money.model.etf.ETF;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ETFRepository extends JpaRepository<ETF, String> {
}
