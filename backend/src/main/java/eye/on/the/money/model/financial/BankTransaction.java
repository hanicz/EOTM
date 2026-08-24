package eye.on.the.money.model.financial;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString
@Table(name = "EOTM_BANK_TRANSACTION",
        uniqueConstraints = @UniqueConstraint(name = "UK_BANK_TRANSACTION_NATURAL",
                columnNames = {"user_id", "bank_transaction_id", "booking_date", "type", "amount", "memo"}),
        indexes = {
                @Index(name = "IDX_BANK_TRANSACTION_USER_DATE", columnList = "user_id, booking_date"),
                @Index(name = "IDX_BANK_TRANSACTION_CURRENCY", columnList = "currency_id")})
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class BankTransaction {

    public static final int MEMO_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_transaction_id", nullable = false)
    private String bankTransactionId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "type", nullable = false)
    private String type;

    private String accountNumber;
    private String accountName;
    private String partnerAccount;
    private String partnerName;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "excluded", nullable = false)
    private boolean excluded;

    @Column(name = "taxable", nullable = false)
    private boolean taxable;

    @Column(name = "memo", nullable = false, length = MEMO_MAX_LENGTH)
    private String memo;

    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    @ManyToOne
    @JoinColumn(name = "currency_id", nullable = false)
    @JsonIgnore
    private Currency currency;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
