package eye.on.the.money.service.shared;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class NetWorthSnapshotScheduler {

    private final NetWorthSnapshotService netWorthSnapshotService;

    @Scheduled(cron = "0 15 23 * * *", zone = "Europe/Budapest")
    public void captureSnapshots() {
        this.netWorthSnapshotService.captureAll();
    }
}
