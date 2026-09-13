package eye.on.the.money.repository.networth;

import eye.on.the.money.model.networth.NetWorthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface NetWorthSnapshotRepository extends JpaRepository<NetWorthSnapshot, Long> {

    List<NetWorthSnapshot> findByUserIdOrderBySnapshotDate(Long userId);

    List<NetWorthSnapshot> findByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);

    boolean existsByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);
}
