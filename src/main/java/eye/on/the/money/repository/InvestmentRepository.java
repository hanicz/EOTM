package eye.on.the.money.repository;

import eye.on.the.money.model.stock.Investment;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface InvestmentRepository extends CrudRepository<Investment, String> {
    public List<Investment> findByUser_Id(Long userId);

    public Optional<Investment> findByIdAndUser_Id(Long id, Long userId);
}
