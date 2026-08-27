package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.RSUTaxDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RSUTaxDetailsRepository extends JpaRepository<RSUTaxDetails, Long> {

    @Query("select r from RSUTaxDetails r join fetch r.investment i join fetch i.stock "
            + "where i.user.id = :userId and i.rsu = true order by i.transactionDate desc")
    List<RSUTaxDetails> findFlaggedByUserId(@Param("userId") Long userId);

    @Query("select r from RSUTaxDetails r join fetch r.investment i join fetch i.stock "
            + "where i.user.id = :userId and i.id in :ids")
    List<RSUTaxDetails> findByUserIdAndInvestmentIdIn(@Param("userId") Long userId,
                                                         @Param("ids") List<Long> ids);

    void deleteByInvestmentIdIn(List<Long> ids);
}
