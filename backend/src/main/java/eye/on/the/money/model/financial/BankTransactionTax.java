package eye.on.the.money.model.financial;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString
@Table(name = "EOTM_BANK_TRANSACTION_TAX")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class BankTransactionTax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private TaxDetails taxDetails;

    @OneToOne(optional = false)
    @JoinColumn(name = "bank_transaction_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    @ToString.Exclude
    private BankTransaction bankTransaction;
}
