package eye.on.the.money.service;


import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.model.User;

import java.util.List;

public interface AlertService {
    List<StockAlertDTO> getAllStockAlerts(Long userId);
    boolean deleteStockAlert(Long userid, Long id);
    StockAlertDTO createNewStockAlert(User user, StockAlertDTO stockAlert);
}
