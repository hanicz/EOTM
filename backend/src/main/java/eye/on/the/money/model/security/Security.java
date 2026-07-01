package eye.on.the.money.model.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Entity
@Getter
@Setter
@Slf4j
@Builder
@EqualsAndHashCode
@ToString
@Table(name = "EOTM_SECURITY")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Security {
    @Id
    private String id;
    private String name;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "security")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<SecurityTransaction> transaction;
}
