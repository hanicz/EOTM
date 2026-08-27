package eye.on.the.money.repository.market;

import eye.on.the.money.model.market.MarketExchange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketExchangeRepository extends JpaRepository<MarketExchange, String> {
    List<MarketExchange> findAllByOrderByIdAsc();
}
