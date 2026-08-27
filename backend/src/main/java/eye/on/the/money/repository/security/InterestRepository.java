package eye.on.the.money.repository.security;

import eye.on.the.money.model.security.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InterestRepository extends JpaRepository<Interest, Long> {
    List<Interest> findByUserIdOrderByInterestDateDesc(Long userId);

    List<Interest> findByUserIdAndInterestDateBetweenOrderByInterestDate(
            Long userId, LocalDate from, LocalDate to);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);

    Optional<Interest> findByIdAndUserId(Long id, Long userId);
}
