package eye.on.the.money.repository.alert;

import eye.on.the.money.model.alert.StockAlertSent;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StockAlertSentRepository extends CrudRepository<StockAlertSent, Long> {

    public List<StockAlertSent> findByStockAlert_Id(Long alertId);
}
