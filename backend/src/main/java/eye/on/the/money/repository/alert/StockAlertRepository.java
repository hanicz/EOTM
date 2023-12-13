package eye.on.the.money.repository.alert;

import eye.on.the.money.model.alert.StockAlert;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StockAlertRepository extends CrudRepository<StockAlert, Long> {
    List<StockAlert> findAll();

    List<StockAlert> findByUser_IdOrderByStockShortName(Long userId);

    int deleteByIdAndUser_Id(Long id, Long userId);
}
