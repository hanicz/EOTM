package eye.on.the.money.model.etf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.forex.Currency;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;

@Entity
@Data
@Builder
@Slf4j
@ToString
@Table(name = "EOTM_ETF_PAYMENT")
@AllArgsConstructor
@NoArgsConstructor
public class ETFPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double amount;

    @OneToOne(mappedBy = "etfPayment")
    @JsonIgnore
    private ETFInvestment etfinvestment;

    @ManyToOne
    @JoinColumn(name = "currency_id", nullable = false)
    @JsonIgnore
    private Currency currency;
}
