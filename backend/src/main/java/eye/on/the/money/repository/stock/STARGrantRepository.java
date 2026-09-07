package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.STARGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface STARGrantRepository extends JpaRepository<STARGrant, Long> {

    List<STARGrant> findByUserIdOrderByCommencementDateDescIdAsc(Long userId);

    Optional<STARGrant> findByIdAndUserId(Long id, Long userId);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update STARGrant g set g.currentValue = :currentValue "
            + "where g.user.id = :userId and upper(g.name) = upper(:name)")
    int updateCurrentValue(@Param("userId") Long userId, @Param("name") String name,
                           @Param("currentValue") BigDecimal currentValue);
}
