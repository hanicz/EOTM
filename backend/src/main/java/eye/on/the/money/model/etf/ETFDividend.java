package eye.on.the.money.model.etf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
import eye.on.the.money.model.forex.Currency;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString
@Table(name = "EOTM_ETF_DIVIDEND")
@AllArgsConstructor
@NoArgsConstructor
public class ETFDividend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double amount;
    private Date dividendDate;

    @ManyToOne
    @JoinColumn(name = "currency_id", nullable = false)
    @JsonIgnore
    private Currency currency;

    @ManyToOne
    @JoinColumn(name = "etf_id", nullable = false)
    private ETF etf;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
