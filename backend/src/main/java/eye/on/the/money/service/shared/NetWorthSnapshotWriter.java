package eye.on.the.money.service.shared;

import eye.on.the.money.dto.out.AssetClassValueDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.networth.NetWorthSnapshot;
import eye.on.the.money.repository.networth.NetWorthSnapshotRepository;
import eye.on.the.money.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NetWorthSnapshotWriter {

    private final NetWorthSnapshotRepository netWorthSnapshotRepository;
    private final UserService userService;

    @Transactional
    public void replace(Long userId, LocalDate date, List<AssetClassValueDTO> assets) {
        this.netWorthSnapshotRepository.deleteByUserIdAndSnapshotDate(userId, date);
        this.netWorthSnapshotRepository.saveAll(this.rows(userId, date, assets));
    }

    @Transactional
    public void insertIfMissing(Long userId, LocalDate date, List<AssetClassValueDTO> assets) {
        if (this.netWorthSnapshotRepository.existsByUserIdAndSnapshotDate(userId, date)) {
            return;
        }
        this.netWorthSnapshotRepository.saveAll(this.rows(userId, date, assets));
    }

    private List<NetWorthSnapshot> rows(Long userId, LocalDate date, List<AssetClassValueDTO> assets) {
        User user = this.userService.getReference(userId);
        return assets.stream()
                .map(asset -> NetWorthSnapshot.builder()
                        .user(user)
                        .snapshotDate(date)
                        .assetClass(asset.getAssetClass())
                        .spent(asset.getSpent().doubleValue())
                        .worth(asset.getWorth().doubleValue())
                        .build())
                .toList();
    }
}
