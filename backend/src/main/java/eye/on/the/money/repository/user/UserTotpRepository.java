package eye.on.the.money.repository.user;

import eye.on.the.money.model.user.UserTotp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTotpRepository extends JpaRepository<UserTotp, Long> {

    Optional<UserTotp> findByUserId(Long userId);

    Optional<UserTotp> findByUserEmailAndConfirmedTrue(String email);

    boolean existsByUserIdAndConfirmedTrue(Long userId);

    void deleteByUserId(Long userId);
}
