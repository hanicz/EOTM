package eye.on.the.money.controller;

import eye.on.the.money.dto.in.ReportSubscriptionUpdateDTO;
import eye.on.the.money.dto.out.ReportSubscriptionDTO;
import eye.on.the.money.exception.dto.ErrorResponse;
import eye.on.the.money.model.report.ReportSubscription;
import eye.on.the.money.report.MonthlyReportScheduler;
import eye.on.the.money.repository.report.ReportSubscriptionRepository;
import eye.on.the.money.security.CurrentUserEmail;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.mail.EmailService;
import eye.on.the.money.service.report.MonthlyReportService;
import eye.on.the.money.service.report.ReportSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/v1/report")
@Slf4j
@RequiredArgsConstructor
public class ReportController {

    private final ReportSubscriptionService reportSubscriptionService;
    private final ReportSubscriptionRepository reportSubscriptionRepository;
    private final MonthlyReportService monthlyReportService;
    private final EmailService emailService;

    @GetMapping("monthly/subscription")
    public ResponseEntity<ReportSubscriptionDTO> getSubscription(@CurrentUserId Long userId) {
        log.trace("Enter");
        return ResponseEntity.ok(this.reportSubscriptionService.get(userId));
    }

    @PutMapping("monthly/subscription")
    public ResponseEntity<ReportSubscriptionDTO> updateSubscription(@CurrentUserId Long userId,
                                                                    @Valid @RequestBody ReportSubscriptionUpdateDTO update) {
        log.trace("Enter");
        return ResponseEntity.ok(this.reportSubscriptionService.update(userId, update));
    }

    @PostMapping("monthly/send")
    public ResponseEntity<Object> sendNow(@CurrentUserId Long userId,
                                          @CurrentUserEmail String userEmail,
                                          @RequestParam(required = false) Integer year,
                                          @RequestParam(required = false) Integer month) {
        log.trace("Enter");
        if (!this.emailService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Email is not configured on the server"));
        }

        YearMonth period = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now(MonthlyReportScheduler.ZONE).minusMonths(1);

        Optional<ReportSubscription> subscription = this.reportSubscriptionRepository.findByUserId(userId);
        String currency = subscription.map(ReportSubscription::getCurrency)
                .orElse(ReportSubscription.DEFAULT_CURRENCY);
        List<String> recipients = subscription.map(this.reportSubscriptionService::recipientsOf)
                .orElse(List.of(userEmail));

        this.emailService.sendMonthlyReportMail(recipients,
                this.monthlyReportService.build(userId, period, currency));
        return ResponseEntity.ok().build();
    }
}
