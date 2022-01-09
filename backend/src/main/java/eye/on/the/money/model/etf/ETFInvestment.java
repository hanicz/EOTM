package eye.on.the.money.model.etf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
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
@Table(name = "EOTM_ETF_INVESTMENT")
@AllArgsConstructor
@NoArgsConstructor
public class ETFInvestment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer quantity;
    private String buySell;
    private Date creationDate;
    private Date transactionDate;
    private Double fee;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "etf_id", nullable = false)
    private ETF etf;

    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JoinColumn(name = "etfPayment_id", nullable = false)
    private ETFPayment etfPayment;
}
