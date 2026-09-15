package eye.on.the.money.repository.networth;

import eye.on.the.money.model.networth.NetWorthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface NetWorthSnapshotRepository extends JpaRepository<NetWorthSnapshot, Long> {

    List<NetWorthSnapshot> findByUserIdOrderBySnapshotDate(Long userId);

    List<NetWorthSnapshot> findByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);

    boolean existsByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from NetWorthSnapshot s where s.user.id = :userId and s.snapshotDate = :snapshotDate")
    int deleteByUserIdAndSnapshotDate(@Param("userId") Long userId, @Param("snapshotDate") LocalDate snapshotDate);
}
