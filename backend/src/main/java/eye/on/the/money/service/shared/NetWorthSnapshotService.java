package eye.on.the.money.service.shared;

import eye.on.the.money.dto.out.AssetClassValueDTO;
import eye.on.the.money.dto.out.MonthlyPerformanceDTO;
import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.dto.out.NetWorthHistoryDTO;
import eye.on.the.money.dto.out.NetWorthPointDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.networth.NetWorthSnapshot;
import eye.on.the.money.report.MonthlyReportScheduler;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.networth.NetWorthSnapshotRepository;
import eye.on.the.money.service.user.UserService;
import eye.on.the.money.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NetWorthSnapshotService {

    public static final String CURRENCY = User.DEFAULT_CURRENCY;

    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final NetWorthSnapshotRepository netWorthSnapshotRepository;
    private final NetWorthService netWorthService;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public void capture(Long userId, LocalDate date, boolean refresh) {
        NetWorthDTO netWorth = this.netWorthService.getNetWorth(userId, CURRENCY, refresh);
        Map<String, NetWorthSnapshot> existing = this.netWorthSnapshotRepository
                .findByUserIdAndSnapshotDate(userId, date).stream()
                .collect(Collectors.toMap(NetWorthSnapshot::getAssetClass, Function.identity()));

        List<NetWorthSnapshot> rows = new ArrayList<>();
        for (AssetClassValueDTO asset : netWorth.getAssets()) {
            NetWorthSnapshot row = existing.get(asset.getAssetClass());
            if (row == null) {
                row = NetWorthSnapshot.builder()
                        .user(this.userService.getReference(userId))
                        .snapshotDate(date)
                        .assetClass(asset.getAssetClass())
                        .build();
            }
            row.setSpent(asset.getSpent().doubleValue());
            row.setWorth(asset.getWorth().doubleValue());
            rows.add(row);
        }
        this.netWorthSnapshotRepository.saveAll(rows);
    }

    public void captureAll() {
        this.captureAll(this.today());
    }

    void captureAll(LocalDate today) {
        List<User> users = this.userRepository.findAll();
        log.info("Capturing the {} net worth snapshot for {} users", today, users.size());

        int failed = 0;
        for (User user : users) {
            try {
                this.capture(user.getId(), today, true);
            } catch (APIException | DataAccessException e) {
                failed++;
                log.error("Unable to capture the {} net worth snapshot for {}", today,
                        LogSanitizer.maskEmail(user.getEmail()), e);
            }
        }

        int captured = users.size() - failed;
        if (failed == 0) {
            log.info("Captured the {} net worth snapshot for all {} users", today, captured);
        } else {
            log.warn("Captured the {} net worth snapshot for {} of {} users, {} failed", today, captured,
                    users.size(), failed);
        }
    }

    public NetWorthHistoryDTO getHistory(Long userId) {
        return this.getHistory(userId, this.today());
    }

    NetWorthHistoryDTO getHistory(Long userId, LocalDate today) {
        if (!this.netWorthSnapshotRepository.existsByUserIdAndSnapshotDate(userId, today)) {
            try {
                this.capture(userId, today, false);
            } catch (APIException | DataAccessException e) {
                log.warn("Unable to capture today's net worth snapshot, returning the stored history", e);
            }
        }

        List<NetWorthPointDTO> points = this.points(
                this.netWorthSnapshotRepository.findByUserIdOrderBySnapshotDate(userId));

        return NetWorthHistoryDTO.builder()
                .currency(CURRENCY)
                .points(points)
                .months(this.months(points))
                .build();
    }

    private List<NetWorthPointDTO> points(List<NetWorthSnapshot> snapshots) {
        Map<LocalDate, List<NetWorthSnapshot>> byDate = snapshots.stream()
                .collect(Collectors.groupingBy(NetWorthSnapshot::getSnapshotDate, TreeMap::new, Collectors.toList()));

        return byDate.entrySet().stream()
                .map(entry -> this.point(entry.getKey(), entry.getValue()))
                .toList();
    }

    private NetWorthPointDTO point(LocalDate date, List<NetWorthSnapshot> rows) {
        double spent = 0;
        double worth = 0;
        Map<String, BigDecimal> assetWorth = new LinkedHashMap<>();
        for (NetWorthSnapshot row : rows) {
            spent += row.getSpent();
            worth += row.getWorth();
            assetWorth.put(row.getAssetClass(), this.scaled(row.getWorth()));
        }

        return NetWorthPointDTO.builder()
                .date(date)
                .totalSpent(this.scaled(spent))
                .totalWorth(this.scaled(worth))
                .assetWorth(assetWorth)
                .build();
    }

    private List<MonthlyPerformanceDTO> months(List<NetWorthPointDTO> points) {
        Map<YearMonth, List<NetWorthPointDTO>> byMonth = points.stream()
                .collect(Collectors.groupingBy(point -> YearMonth.from(point.getDate()), TreeMap::new,
                        Collectors.toList()));

        List<MonthlyPerformanceDTO> months = new ArrayList<>();
        NetWorthPointDTO previousEnd = null;
        for (Map.Entry<YearMonth, List<NetWorthPointDTO>> entry : byMonth.entrySet()) {
            NetWorthPointDTO start = (previousEnd == null) ? entry.getValue().getFirst() : previousEnd;
            NetWorthPointDTO end = entry.getValue().getLast();
            months.add(this.month(entry.getKey(), start, end));
            previousEnd = end;
        }
        return months.reversed();
    }

    private MonthlyPerformanceDTO month(YearMonth month, NetWorthPointDTO start, NetWorthPointDTO end) {
        BigDecimal change = end.getTotalWorth().subtract(start.getTotalWorth());
        BigDecimal contributions = end.getTotalSpent().subtract(start.getTotalSpent());

        return MonthlyPerformanceDTO.builder()
                .month(month.toString())
                .endWorth(end.getTotalWorth())
                .endSpent(end.getTotalSpent())
                .change(change)
                .changePct(this.pct(change, start.getTotalWorth()))
                .contributions(contributions)
                .marketGain(change.subtract(contributions))
                .build();
    }

    private BigDecimal pct(BigDecimal change, BigDecimal base) {
        if (base.signum() == 0) return null;
        return change.multiply(HUNDRED).divide(base, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scaled(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private LocalDate today() {
        return LocalDate.now(MonthlyReportScheduler.ZONE);
    }
}
