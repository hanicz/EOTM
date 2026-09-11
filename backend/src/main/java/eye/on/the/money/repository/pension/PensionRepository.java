package eye.on.the.money.repository.pension;

import eye.on.the.money.model.pension.Pension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PensionRepository extends JpaRepository<Pension, Long> {

    Optional<Pension> findByUserId(Long userId);
}
