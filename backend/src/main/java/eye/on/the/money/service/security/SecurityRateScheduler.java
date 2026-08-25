package eye.on.the.money.service.security;

import eye.on.the.money.exception.APIException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class SecurityRateScheduler {

    private final SecurityRateService securityRateService;

    @Scheduled(cron = "0 30 6 * * *", zone = "Europe/Budapest")
    public void refreshWhenStale() {
        log.trace("Enter refreshWhenStale");
        try {
            this.securityRateService.refreshIfStale();
        } catch (APIException | NoSuchElementException e) {
            log.error("Failed to refresh security rates, keeping stored rates", e);
        }
        log.trace("Exit refreshWhenStale");
    }
}
