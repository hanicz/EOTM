package eye.on.the.money.model.market;

import eye.on.the.money.util.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;

@Entity
@Getter
@Setter
@Slf4j
@Builder
@ToString
@EqualsAndHashCode
@Table(name = "EOTM_EXCHANGE")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class MarketExchange {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String timeZone;

    @Column(nullable = false)
    private LocalTime openTime;

    @Column(nullable = false)
    private LocalTime closeTime;

    @Column(nullable = false)
    private String currency;

    @Column(name = "country_iso2")
    private String countryISO2;
}
