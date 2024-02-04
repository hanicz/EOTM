package eye.on.the.money.repository.alert;

import eye.on.the.money.model.alert.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    List<StockAlert> findAll();

    List<StockAlert> findByUserEmailOrderByStockShortName(String userEmail);

    int deleteByIdAndUserEmail(Long id, String userEmail);
}
