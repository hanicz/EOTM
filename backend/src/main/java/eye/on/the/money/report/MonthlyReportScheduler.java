package eye.on.the.money.report;

import eye.on.the.money.exception.APIException;
import eye.on.the.money.exception.EmailException;
import eye.on.the.money.model.report.ReportSubscription;
import eye.on.the.money.repository.report.ReportSubscriptionRepository;
import eye.on.the.money.service.mail.EmailService;
import eye.on.the.money.service.report.MonthlyReportService;
import eye.on.the.money.service.report.ReportSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class MonthlyReportScheduler {

    public static final ZoneId ZONE = ZoneId.of("Europe/Budapest");

    private final ReportSubscriptionRepository reportSubscriptionRepository;
    private final ReportSubscriptionService reportSubscriptionService;
    private final MonthlyReportService monthlyReportService;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 7 10 * *", zone = "Europe/Budapest")
    public void sendMonthlyReports() {
        this.sendReportsFor(YearMonth.now(ZONE).minusMonths(1));
    }

    public void sendReportsFor(YearMonth period) {
        log.trace("Enter");
        if (!this.emailService.isEnabled()) {
            log.info("Email is not configured, skipping monthly reports.");
            return;
        }

        List<ReportSubscription> subscriptions = this.reportSubscriptionRepository.findByEnabledTrue();
        log.info("Sending monthly reports for {} to {} subscribers", period, subscriptions.size());

        for (ReportSubscription subscription : subscriptions) {
            this.sendFor(subscription, period);
        }
        log.trace("Exit");
    }

    private void sendFor(ReportSubscription subscription, YearMonth period) {
        String userEmail = subscription.getUser().getEmail();
        if (period.toString().equals(subscription.getLastSentPeriod())) {
            log.info("Monthly report for {} already sent to {}, skipping", period, userEmail);
            return;
        }
        try {
            List<String> recipients = this.reportSubscriptionService.recipientsOf(subscription);
            this.emailService.sendMonthlyReportMail(recipients,
                    this.monthlyReportService.build(userEmail, period, subscription.getCurrency()));
            this.reportSubscriptionService.markSent(subscription, period);
        } catch (APIException | EmailException | DataAccessException e) {
            log.error("Unable to send the {} monthly report to {}", period, userEmail, e);
        }
    }
}
