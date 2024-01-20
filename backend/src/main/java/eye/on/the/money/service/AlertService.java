package eye.on.the.money.service;


import eye.on.the.money.dto.out.StockAlertDTO;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface AlertService {
    List<StockAlertDTO> getAllStockAlerts(String userEmail);

    public StockAlertDTO createNewStockAlert(UserDetails userDetails, StockAlertDTO stockAlertDTO);

    boolean deleteStockAlert(String userEmail, Long id);
}
