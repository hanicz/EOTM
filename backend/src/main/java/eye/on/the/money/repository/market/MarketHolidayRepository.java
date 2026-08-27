package eye.on.the.money.repository.market;

import eye.on.the.money.model.market.MarketHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MarketHolidayRepository extends JpaRepository<MarketHoliday, Long> {
    List<MarketHoliday> findByHolidayDateGreaterThanEqualOrderByHolidayDateAsc(LocalDate from);
}
