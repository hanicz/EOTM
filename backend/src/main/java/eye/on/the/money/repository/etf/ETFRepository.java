package eye.on.the.money.repository.etf;

import eye.on.the.money.model.etf.ETF;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ETFRepository extends CrudRepository<ETF, String> {
    public Optional<ETF> findByName(String name);

    public Optional<ETF> findByShortName(String shortName);

    public List<ETF> findAllByOrderByShortNameAsc();
}
