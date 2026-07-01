package eye.on.the.money.repository.security;

import eye.on.the.money.model.security.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterestRepository extends JpaRepository<Interest, Long> {
    List<Interest> findByUserEmailOrderByInterestDateDesc(String userEmail);

    void deleteByUserEmailAndIdIn(String userEmail, List<Long> ids);

    Optional<Interest> findByIdAndUserEmail(Long id, String userEmail);
}
