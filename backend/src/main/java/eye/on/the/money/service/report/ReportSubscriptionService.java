package eye.on.the.money.service.report;

import eye.on.the.money.dto.in.ReportSubscriptionUpdateDTO;
import eye.on.the.money.dto.out.ReportSubscriptionDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.exception.CooldownException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.report.ReportSubscription;
import eye.on.the.money.report.MonthlyReportScheduler;
import eye.on.the.money.repository.report.ReportSubscriptionRepository;
import eye.on.the.money.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportSubscriptionService {

    private static final Duration MANUAL_SEND_COOLDOWN = Duration.ofHours(24);

    private final ReportSubscriptionRepository reportSubscriptionRepository;
    private final UserService userService;

    public ReportSubscriptionDTO get(Long userId) {
        return this.reportSubscriptionRepository.findByUserId(userId)
                .map(this::toDTO)
                .orElseGet(() -> ReportSubscriptionDTO.builder()
                        .enabled(false)
                        .currency(ReportSubscription.DEFAULT_CURRENCY)
                        .recipients(List.of())
                        .build());
    }

    @Transactional
    public ReportSubscriptionDTO update(Long userId, ReportSubscriptionUpdateDTO update) {
        log.trace("Enter");
        ReportSubscription subscription = this.reportSubscriptionRepository.findByUserId(userId)
                .orElseGet(() -> this.create(userId));

        List<String> recipients = this.normalise(subscription.getUser().getEmail(), update.recipients());

        subscription.setEnabled(Boolean.TRUE.equals(update.enabled()));
        subscription.setCurrency(update.currency().toUpperCase());
        subscription.getRecipients().clear();
        subscription.getRecipients().addAll(recipients);

        ReportSubscriptionDTO saved = this.toDTO(this.reportSubscriptionRepository.save(subscription));
        log.trace("Exit");
        return saved;
    }

    public List<String> recipientsOf(ReportSubscription subscription) {
        List<String> all = new ArrayList<>();
        all.add(subscription.getUser().getEmail());
        subscription.getRecipients().stream()
                .filter(email -> !email.equalsIgnoreCase(subscription.getUser().getEmail()))
                .forEach(all::add);
        return all;
    }

    @Transactional
    public ReportSubscription claimManualSend(Long userId) {
        log.trace("Enter");
        ReportSubscription subscription = this.reportSubscriptionRepository.findByUserId(userId)
                .orElseGet(() -> this.reportSubscriptionRepository.save(this.create(userId)));

        LocalDateTime now = LocalDateTime.now(MonthlyReportScheduler.ZONE);
        LocalDateTime claimableBefore = now.minus(ReportSubscriptionService.MANUAL_SEND_COOLDOWN);

        if (this.reportSubscriptionRepository.claimManualSend(subscription.getId(), now, claimableBefore) == 0) {
            throw this.cooldownException(userId, now);
        }
        log.trace("Exit");
        return subscription;
    }

    @Transactional
    public void markSent(ReportSubscription subscription, YearMonth period) {
        subscription.setLastSentPeriod(period.toString());
        this.reportSubscriptionRepository.save(subscription);
    }

    private CooldownException cooldownException(Long userId, LocalDateTime now) {
        LocalDateTime nextAllowed = this.reportSubscriptionRepository.findByUserId(userId)
                .map(ReportSubscription::getLastManualSendAt)
                .orElse(now)
                .plus(ReportSubscriptionService.MANUAL_SEND_COOLDOWN);
        Duration remaining = Duration.between(now, nextAllowed);
        if (remaining.isNegative()) remaining = Duration.ZERO;

        return new CooldownException("You have already sent this report. You can send it again in "
                + ReportSubscriptionService.describe(remaining) + ".", remaining);
    }

    private static String describe(Duration remaining) {
        long minutes = remaining.plusSeconds(59).toMinutes();
        long hours = minutes / 60;
        minutes = minutes % 60;

        if (hours == 0) return ReportSubscriptionService.plural(Math.max(minutes, 1), "minute");
        if (minutes == 0) return ReportSubscriptionService.plural(hours, "hour");
        return ReportSubscriptionService.plural(hours, "hour") + " " + ReportSubscriptionService.plural(minutes, "minute");
    }

    private static String plural(long value, String unit) {
        return value + " " + unit + (value == 1 ? "" : "s");
    }

    private ReportSubscription create(Long userId) {
        User user = this.userService.loadUserById(userId);
        return ReportSubscription.builder()
                .user(user)
                .enabled(false)
                .currency(ReportSubscription.DEFAULT_CURRENCY)
                .recipients(new ArrayList<>())
                .build();
    }

    private List<String> normalise(String userEmail, List<String> recipients) {
        if (recipients == null) return List.of();

        Set<String> unique = new LinkedHashSet<>();
        for (String recipient : recipients) {
            if (recipient == null || recipient.isBlank()) continue;
            String email = recipient.trim().toLowerCase();
            if (email.equalsIgnoreCase(userEmail)) continue;
            unique.add(email);
        }
        if (unique.size() > ReportSubscription.MAX_RECIPIENTS) {
            throw new APIException("At most " + ReportSubscription.MAX_RECIPIENTS + " extra recipients are allowed");
        }
        return List.copyOf(unique);
    }

    private ReportSubscriptionDTO toDTO(ReportSubscription subscription) {
        return ReportSubscriptionDTO.builder()
                .enabled(subscription.isEnabled())
                .currency(subscription.getCurrency())
                .recipients(List.copyOf(subscription.getRecipients()))
                .build();
    }
}
