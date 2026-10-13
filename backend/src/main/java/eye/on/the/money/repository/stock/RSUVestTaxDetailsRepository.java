package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.RSUVestTaxDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RSUVestTaxDetailsRepository extends JpaRepository<RSUVestTaxDetails, Long> {

    @Query("select v from RSUVestTaxDetails v join fetch v.grant g "
            + "where g.user.id = :userId order by v.vestDate desc, v.id asc")
    List<RSUVestTaxDetails> findByUserId(@Param("userId") Long userId);

    @Query("select v from RSUVestTaxDetails v join fetch v.grant g "
            + "where g.user.id = :userId and v.id in :ids")
    List<RSUVestTaxDetails> findByUserIdAndIdIn(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}
