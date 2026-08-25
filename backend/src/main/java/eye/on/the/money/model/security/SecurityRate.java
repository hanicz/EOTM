package eye.on.the.money.model.security;

import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString
@Table(name = "EOTM_SECURITY_RATE",
        uniqueConstraints = @UniqueConstraint(name = "UQ_SECURITY_RATE_ISIN_PERIOD",
                columnNames = {"isin", "period_start"}),
        indexes = @Index(name = "IDX_SECURITY_RATE_ISIN_PAYMENT", columnList = "isin, payment_date"))
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class SecurityRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String isin;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    private Double rate;

    @Column(nullable = false)
    private Boolean zeroCoupon;

    private String convention;

    @Column(nullable = false)
    private LocalDateTime fetchedAt;
}
