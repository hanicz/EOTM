package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.financial.TaxDetails;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString
@Table(name = "EOTM_STOCK_RSU_TAX")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class RSUTaxDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "price_date")
    private LocalDate priceDate;

    @Column(name = "currency")
    private String currency;

    @Embedded
    private TaxDetails taxDetails;

    @OneToOne(optional = false)
    @JoinColumn(name = "investment_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    @ToString.Exclude
    private Investment investment;
}
