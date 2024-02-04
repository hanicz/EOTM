package eye.on.the.money.repository.watchlist;

import eye.on.the.money.model.watchlist.CryptoWatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CryptoWatchRepository extends JpaRepository<CryptoWatch, Long> {
    List<CryptoWatch> findByUserEmailOrderByCoin_Symbol(String userEMail);

    void deleteByIdAndUserEmail(Long id, String userEmail);
}
