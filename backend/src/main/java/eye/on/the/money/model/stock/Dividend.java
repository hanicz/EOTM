package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.AuditedEntity;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@ToString
@Table(name = "EOTM_STOCK_DIVIDEND", indexes = {
        @Index(name = "IDX_STOCK_DIVIDEND_USER_DATE", columnList = "user_id, dividend_date"),
        @Index(name = "IDX_STOCK_DIVIDEND_STOCK", columnList = "stock_id"),
        @Index(name = "IDX_STOCK_DIVIDEND_CURRENCY", columnList = "currency_id")})
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Dividend extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate dividendDate;

    @ManyToOne
    @JoinColumn(name = "currency_id", nullable = false)
    @JsonIgnore
    private Currency currency;

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
