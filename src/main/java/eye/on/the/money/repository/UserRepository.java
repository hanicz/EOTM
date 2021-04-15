package eye.on.the.money.repository;

import eye.on.the.money.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
    public User findByEmail(String email);
}
