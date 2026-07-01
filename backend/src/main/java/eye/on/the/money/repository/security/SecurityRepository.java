package eye.on.the.money.repository.security;

import eye.on.the.money.model.security.Security;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SecurityRepository extends JpaRepository<Security, String> {
    Optional<Security> findByName(String name);

    List<Security> findAllByOrderByNameAsc();
}
