package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.AuditedEntity;
import eye.on.the.money.model.financial.TaxDetails;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@ToString
@Table(name = "EOTM_RSU_VEST_TAX", uniqueConstraints = {
        @UniqueConstraint(name = "UK_RSU_VEST_TAX_GRANT_DATE", columnNames = {"grant_id", "vest_date"})})
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class RSUVestTaxDetails extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vest_date", nullable = false)
    private LocalDate vestDate;

    @Column(name = "vest_sequence", nullable = false)
    private int sequence;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "price_date")
    private LocalDate priceDate;

    @Column(name = "currency")
    private String currency;

    @Embedded
    private TaxDetails taxDetails;

    @ManyToOne(optional = false)
    @JoinColumn(name = "grant_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    @ToString.Exclude
    private RSUGrant grant;
}
