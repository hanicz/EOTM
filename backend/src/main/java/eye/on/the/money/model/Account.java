package eye.on.the.money.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.stock.Investment;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Set;

@Entity
@Getter
@Setter
@Slf4j
@Builder
@ToString
@Table(name = "EOTM_USER_ACCOUNT")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountName;
    private LocalDate creationDate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "account")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Investment> investment;
}
