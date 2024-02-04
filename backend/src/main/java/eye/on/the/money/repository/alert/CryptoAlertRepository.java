package eye.on.the.money.repository.alert;

import eye.on.the.money.model.alert.CryptoAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CryptoAlertRepository extends JpaRepository<CryptoAlert, Long> {

    List<CryptoAlert> findAll();

    List<CryptoAlert> findByUser_IdOrderByCoinSymbol(Long userId);

    int deleteByIdAndUser_Id(Long id, Long userId);
}
