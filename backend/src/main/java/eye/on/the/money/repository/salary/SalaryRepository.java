package eye.on.the.money.repository.salary;

import eye.on.the.money.model.salary.Salary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalaryRepository extends JpaRepository<Salary, Long> {

    List<Salary> findByUserIdOrderByValidFromDesc(Long userId);

    Optional<Salary> findByIdAndUserId(Long id, Long userId);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);
}
