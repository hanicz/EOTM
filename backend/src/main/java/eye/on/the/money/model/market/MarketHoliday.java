package eye.on.the.money.model.market;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@Slf4j
@Builder
@ToString
@Table(name = "EOTM_MARKET_HOLIDAY",
        uniqueConstraints = @UniqueConstraint(name = "UQ_MARKET_HOLIDAY_EXCHANGE_DATE",
                columnNames = {"exchange_id", "holiday_date"}),
        indexes = @Index(name = "IDX_MARKET_HOLIDAY_DATE", columnList = "holiday_date"))
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class MarketHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exchange_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private MarketExchange exchange;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(nullable = false)
    private String name;

    private LocalTime closeTime;
}
