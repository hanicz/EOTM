package eye.on.the.money.repository.watchlist;

import eye.on.the.money.model.watchlist.CryptoWatch;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CryptoWatchRepository extends CrudRepository<CryptoWatch, Long> {
    List<CryptoWatch> findByUserEmailOrderByCoin_Symbol(String userEMail);
    void deleteByIdAndUserEmail(Long id, String userEmail);
}
