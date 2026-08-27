package eye.on.the.money.report;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.MonthlyReportDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.report.ReportSubscription;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.report.ReportSubscriptionRepository;
import eye.on.the.money.service.mail.EmailService;
import eye.on.the.money.service.report.MonthlyReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class MonthlyReportSchedulerTest {

    private static final String USER = "test@test.test";
    private static final YearMonth PERIOD = YearMonth.of(2023, 9);

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private MonthlyReportService monthlyReportService;

    @Autowired
    private ReportSubscriptionRepository reportSubscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MonthlyReportScheduler monthlyReportScheduler;

    @BeforeEach
    public void setUpEach() {
        when(this.emailService.isEnabled()).thenReturn(true);
        when(this.monthlyReportService.build(anyLong(), any(), anyString()))
                .thenReturn(MonthlyReportDTO.builder().year(2023).month(9).currency("EUR").build());
    }

    @AfterEach
    public void cleanUpEach() {
        this.reportSubscriptionRepository.deleteAll();
    }

    @Test
    public void sendsToTheOwnerAndEveryExtraRecipient() {
        this.subscription(true, null, List.of("partner@test.test", "accountant@test.test"));

        this.monthlyReportScheduler.sendReportsFor(PERIOD);

        ArgumentCaptor<List<String>> recipients = ArgumentCaptor.captor();
        verify(this.emailService).sendMonthlyReportMail(recipients.capture(), any(MonthlyReportDTO.class));
        assertEquals(List.of(USER, "partner@test.test", "accountant@test.test"), recipients.getValue());
    }

    @Test
    public void marksThePeriodAsSent() {
        ReportSubscription subscription = this.subscription(true, null, List.of());

        this.monthlyReportScheduler.sendReportsFor(PERIOD);

        assertEquals("2023-09",
                this.reportSubscriptionRepository.findById(subscription.getId()).orElseThrow().getLastSentPeriod());
    }

    @Test
    public void sendsNothingWhenTheSubscriptionIsDisabled() {
        this.subscription(false, null, List.of());

        this.monthlyReportScheduler.sendReportsFor(PERIOD);

        verify(this.emailService, never()).sendMonthlyReportMail(any(), any());
    }

    @Test
    public void sendsNothingWhenThePeriodWasAlreadySent() {
        this.subscription(true, "2023-09", List.of());

        this.monthlyReportScheduler.sendReportsFor(PERIOD);

        verify(this.emailService, never()).sendMonthlyReportMail(any(), any());
    }

    @Test
    public void sendsNothingWhenEmailIsNotConfigured() {
        ReportSubscription subscription = this.subscription(true, null, List.of());
        when(this.emailService.isEnabled()).thenReturn(false);

        this.monthlyReportScheduler.sendReportsFor(PERIOD);

        verify(this.emailService, never()).sendMonthlyReportMail(any(), any());
        assertNull(this.reportSubscriptionRepository.findById(subscription.getId()).orElseThrow().getLastSentPeriod());
    }

    @Test
    public void oneFailureDoesNotStopTheOtherSubscribers() {
        ReportSubscription failing = this.subscription(true, null, List.of());
        User other = this.userRepository.save(User.builder().email("other@test.test").password("x").build());
        this.reportSubscriptionRepository.save(ReportSubscription.builder()
                .user(other).enabled(true).currency("EUR").recipients(new ArrayList<>()).build());

        when(this.monthlyReportService.build(eq(this.userRepository.findByEmail(USER).getId()), any(), anyString()))
                .thenThrow(new APIException("EODHD is down"));

        this.monthlyReportScheduler.sendReportsFor(PERIOD);

        Assertions.assertAll("Only the healthy subscriber is served",
                () -> verify(this.emailService, times(1))
                        .sendMonthlyReportMail(eq(List.of("other@test.test")), any(MonthlyReportDTO.class)),
                () -> assertNull(this.reportSubscriptionRepository.findById(failing.getId())
                        .orElseThrow().getLastSentPeriod()));

        this.reportSubscriptionRepository.deleteAll();
        this.userRepository.delete(other);
    }

    private ReportSubscription subscription(boolean enabled, String lastSentPeriod, List<String> recipients) {
        this.reportSubscriptionRepository.deleteAll();
        User user = this.userRepository.findByEmail(USER);
        return this.reportSubscriptionRepository.save(ReportSubscription.builder()
                .user(user)
                .enabled(enabled)
                .currency("EUR")
                .lastSentPeriod(lastSentPeriod)
                .recipients(new ArrayList<>(recipients))
                .build());
    }
}
