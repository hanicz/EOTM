package eye.on.the.money.repository.alert;

import eye.on.the.money.model.alert.CryptoAlert;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CryptoAlertRepository extends CrudRepository<CryptoAlert, Long> {

    List<CryptoAlert> findAll();

    List<CryptoAlert> findByUser_IdOrderByCoinSymbol(Long userId);

    int deleteByIdAndUser_Id(Long id, Long userId);
}
