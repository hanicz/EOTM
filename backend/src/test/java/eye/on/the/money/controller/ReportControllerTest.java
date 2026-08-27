package eye.on.the.money.controller;

import eye.on.the.money.dto.in.ReportSubscriptionUpdateDTO;
import eye.on.the.money.dto.out.MonthlyReportDTO;
import eye.on.the.money.dto.out.ReportSubscriptionDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.report.ReportSubscription;
import eye.on.the.money.repository.report.ReportSubscriptionRepository;
import eye.on.the.money.service.mail.EmailService;
import eye.on.the.money.service.report.MonthlyReportService;
import eye.on.the.money.service.report.ReportSubscriptionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@test.test";

    @Mock
    private ReportSubscriptionService reportSubscriptionService;

    @Mock
    private ReportSubscriptionRepository reportSubscriptionRepository;

    @Mock
    private MonthlyReportService monthlyReportService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReportController reportController;

    @Test
    void getSubscriptionReturnsTheStoredSettings() {
        ReportSubscriptionDTO dto = ReportSubscriptionDTO.builder()
                .enabled(true).currency("EUR").recipients(List.of("partner@test.test")).build();
        when(this.reportSubscriptionService.get(USER_ID)).thenReturn(dto);

        assertEquals(dto, this.reportController.getSubscription(USER_ID).getBody());
    }

    @Test
    void updateSubscriptionReturnsWhatWasSaved() {
        ReportSubscriptionUpdateDTO update =
                new ReportSubscriptionUpdateDTO(true, "HUF", List.of("partner@test.test"));
        ReportSubscriptionDTO saved = ReportSubscriptionDTO.builder()
                .enabled(true).currency("HUF").recipients(List.of("partner@test.test")).build();
        when(this.reportSubscriptionService.update(USER_ID, update)).thenReturn(saved);

        assertEquals(saved, this.reportController.updateSubscription(USER_ID, update).getBody());
    }

    @Test
    void sendNowConflictsWhenEmailIsNotConfigured() {
        when(this.emailService.isEnabled()).thenReturn(false);

        ResponseEntity<Object> response = this.reportController.sendNow(USER_ID, USER_EMAIL, null, null);

        Assertions.assertAll("Mail not configured",
                () -> assertEquals(HttpStatus.CONFLICT, response.getStatusCode()),
                () -> verify(this.monthlyReportService, never()).build(any(), any(), any()));
    }

    @Test
    void sendNowUsesTheRequestedPeriodAndTheStoredCurrency() {
        ReportSubscription subscription = ReportSubscription.builder()
                .user(User.builder().email(USER_EMAIL).build())
                .enabled(true).currency("HUF").recipients(new ArrayList<>()).build();
        MonthlyReportDTO report = MonthlyReportDTO.builder().year(2023).month(9).currency("HUF").build();

        when(this.emailService.isEnabled()).thenReturn(true);
        when(this.reportSubscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(subscription));
        when(this.reportSubscriptionService.recipientsOf(subscription)).thenReturn(List.of(USER_EMAIL));
        when(this.monthlyReportService.build(USER_ID, YearMonth.of(2023, 9), "HUF")).thenReturn(report);

        ResponseEntity<Object> response = this.reportController.sendNow(USER_ID, USER_EMAIL, 2023, 9);

        Assertions.assertAll("Manual send",
                () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
                () -> verify(this.emailService).sendMonthlyReportMail(List.of(USER_EMAIL), report));
    }

    @Test
    void sendNowFallsBackToTheOwnerWithoutASubscription() {
        when(this.emailService.isEnabled()).thenReturn(true);
        when(this.reportSubscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(this.monthlyReportService.build(eq(USER_ID), any(), eq(ReportSubscription.DEFAULT_CURRENCY)))
                .thenReturn(MonthlyReportDTO.builder().build());

        this.reportController.sendNow(USER_ID, USER_EMAIL, null, null);

        ArgumentCaptor<List<String>> recipients = ArgumentCaptor.captor();
        verify(this.emailService).sendMonthlyReportMail(recipients.capture(), any(MonthlyReportDTO.class));
        assertEquals(List.of(USER_EMAIL), recipients.getValue());
    }
}
