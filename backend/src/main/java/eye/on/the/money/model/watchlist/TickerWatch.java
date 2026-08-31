package eye.on.the.money.model.watchlist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString
@Table(name = "EOTM_STOCK_WATCH",
        uniqueConstraints = @UniqueConstraint(name = "UK_STOCK_WATCH_USER_STOCK",
                columnNames = {"user_id", "stock_id"}),
        indexes = @Index(name = "IDX_STOCK_WATCH_USER", columnList = "user_id"))
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class TickerWatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @ManyToOne
    @JoinColumn(name = "group_id")
    @JsonIgnore
    private WatchGroup group;
}
