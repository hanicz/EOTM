package eye.on.the.money.report;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.dto.in.ReportSubscriptionUpdateDTO;
import eye.on.the.money.dto.out.ReportSubscriptionDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.exception.CooldownException;
import eye.on.the.money.model.report.ReportSubscription;
import eye.on.the.money.repository.report.ReportSubscriptionRepository;
import eye.on.the.money.service.report.ReportSubscriptionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class ReportSubscriptionServiceTest {

    private static final String USER_EMAIL = "test@test.test";

    @Autowired
    private UserRepository userRepository;

    private Long user;

    @Autowired
    private ReportSubscriptionService reportSubscriptionService;

    @Autowired
    private ReportSubscriptionRepository reportSubscriptionRepository;

    @BeforeEach
    public void setUpEach() {
        this.user = this.userRepository.findByEmail(USER_EMAIL).getId();
    }

    @AfterEach
    public void cleanUpEach() {
        this.reportSubscriptionRepository.deleteAll();
    }

    @Test
    public void updateCreatesTheRowOnFirstUse() {
        this.reportSubscriptionRepository.deleteAll();

        ReportSubscriptionDTO saved = this.reportSubscriptionService.update(this.user,
                new ReportSubscriptionUpdateDTO(true, "huf", List.of("partner@test.test")));

        Assertions.assertAll("First save",
                () -> assertTrue(saved.isEnabled()),
                () -> assertEquals("HUF", saved.getCurrency()),
                () -> assertEquals(List.of("partner@test.test"), saved.getRecipients()),
                () -> assertTrue(this.reportSubscriptionRepository.findByUserId(this.user).isPresent()));
    }

    @Test
    public void updateNormalisesAndDeduplicatesRecipients() {
        ReportSubscriptionDTO saved = this.reportSubscriptionService.update(this.user,
                new ReportSubscriptionUpdateDTO(true, "EUR",
                        List.of("  Partner@Test.test ", "partner@test.test", "", "second@test.test")));

        assertEquals(List.of("partner@test.test", "second@test.test"), saved.getRecipients());
    }

    @Test
    public void updateDropsTheOwnersOwnAddress() {
        ReportSubscriptionDTO saved = this.reportSubscriptionService.update(this.user,
                new ReportSubscriptionUpdateDTO(true, "EUR", List.of(USER_EMAIL, "partner@test.test")));

        assertEquals(List.of("partner@test.test"), saved.getRecipients());
    }

    @Test
    public void updateRejectsMoreRecipientsThanTheCap() {
        List<String> tooMany = List.of("a@test.test", "b@test.test", "c@test.test",
                "d@test.test", "e@test.test", "f@test.test");

        assertThrows(APIException.class, () -> this.reportSubscriptionService.update(this.user,
                new ReportSubscriptionUpdateDTO(true, "EUR", tooMany)));
    }

    @Test
    public void recipientsAlwaysStartWithTheOwner() {
        this.reportSubscriptionService.update(this.user,
                new ReportSubscriptionUpdateDTO(true, "EUR", List.of("partner@test.test")));

        List<String> recipients = this.reportSubscriptionService.recipientsOf(
                this.reportSubscriptionRepository.findByUserId(this.user).orElseThrow());

        assertEquals(List.of(USER_EMAIL, "partner@test.test"), recipients);
    }

    @Test
    public void claimManualSendStampsTheTimeOnTheFirstSend() {
        ReportSubscription claimed = this.reportSubscriptionService.claimManualSend(this.user);

        Assertions.assertAll("First claim",
                () -> assertEquals(this.user, claimed.getUser().getId()),
                () -> assertNotNull(this.reportSubscriptionRepository.findByUserId(this.user)
                        .orElseThrow().getLastManualSendAt()));
    }

    @Test
    public void claimManualSendCreatesTheRowForAUserWithoutASubscription() {
        this.reportSubscriptionRepository.deleteAll();

        ReportSubscription claimed = this.reportSubscriptionService.claimManualSend(this.user);

        Assertions.assertAll("Created on claim",
                () -> assertEquals(List.of(USER_EMAIL), this.reportSubscriptionService.recipientsOf(claimed)),
                () -> assertEquals("HUF", claimed.getCurrency()),
                () -> Assertions.assertFalse(claimed.isEnabled()));
    }

    @Test
    public void claimManualSendRejectsASecondSendWithinTheCooldown() {
        this.reportSubscriptionService.claimManualSend(this.user);

        CooldownException thrown = assertThrows(CooldownException.class,
                () -> this.reportSubscriptionService.claimManualSend(this.user));

        Assertions.assertAll("Second claim",
                () -> assertTrue(thrown.getRetryAfter().toHours() >= 23),
                () -> assertTrue(thrown.getMessage().contains("send it again in")));
    }

    @Test
    public void claimManualSendAllowsAnotherSendAfterTheCooldown() {
        this.reportSubscriptionService.claimManualSend(this.user);
        ReportSubscription subscription = this.reportSubscriptionRepository.findByUserId(this.user).orElseThrow();
        subscription.setLastManualSendAt(subscription.getLastManualSendAt().minusHours(25));
        this.reportSubscriptionRepository.save(subscription);

        this.reportSubscriptionService.claimManualSend(this.user);

        assertTrue(this.reportSubscriptionRepository.findByUserId(this.user).orElseThrow()
                .getLastManualSendAt().isAfter(subscription.getLastManualSendAt()));
    }

    @Test
    public void getFallsBackToDefaultsWithoutASubscription() {
        this.reportSubscriptionRepository.deleteAll();

        ReportSubscriptionDTO subscription = this.reportSubscriptionService.get(this.user);

        Assertions.assertAll("Defaults",
                () -> Assertions.assertFalse(subscription.isEnabled()),
                () -> assertEquals("HUF", subscription.getCurrency()),
                () -> assertTrue(subscription.getRecipients().isEmpty()));
    }
}
