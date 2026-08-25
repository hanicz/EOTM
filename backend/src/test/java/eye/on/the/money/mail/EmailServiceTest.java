package eye.on.the.money.mail;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.AssetClassValueDTO;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.dto.out.MonthlyReportDTO;
import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.service.mail.EmailService;
import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class EmailServiceTest {

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    private EmailService emailService;

    @Test
    public void sendAlertMail() throws Exception {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(this.javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(this.javaMailSender).send(ArgumentMatchers.any(MimeMessage.class));

        this.emailService.sendAlertMail("sendTo", "AAPL", "PRICE_OVER", 100.0, 120.0, 5.0);

        ArgumentCaptor<MimeMessage> argument = ArgumentCaptor.forClass(MimeMessage.class);
        verify(this.javaMailSender).send(argument.capture());

        MimeMessage sentMessage = argument.getValue();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sentMessage.writeTo(out);
        String rawContent = out.toString();

        Assertions.assertAll("Check email",
                () -> assertEquals("sendTo", sentMessage.getAllRecipients()[0].toString()),
                () -> assertEquals("user", sentMessage.getFrom()[0].toString()),
                () -> assertTrue(sentMessage.getSubject().contains("AAPL")),
                () -> assertTrue(rawContent.contains("AAPL")),
                () -> assertTrue(rawContent.contains("100")),
                () -> assertTrue(rawContent.contains("text/html")));
    }

    @Test
    public void sendAlertMailEscapesHtmlInTicker() throws Exception {
        String html = this.htmlPartOf("<img src=x onerror=alert(1)>", "PRICE_OVER");

        Assertions.assertAll("Check escaping",
                () -> assertFalse(html.contains("<img src=x")),
                () -> assertTrue(html.contains("&lt;img src=x onerror=alert(1)&gt;")));
    }

    @Test
    public void sendAlertMailEscapesHtmlInAlertType() throws Exception {
        String html = this.htmlPartOf("AAPL", "<script>alert(1)</script>");

        Assertions.assertAll("Check escaping",
                () -> assertFalse(html.contains("<script>")),
                () -> assertTrue(html.contains("&lt;script&gt;")));
    }

    @Test
    public void sendMonthlyReportMail() throws Exception {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(this.javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(this.javaMailSender).send(ArgumentMatchers.any(MimeMessage.class));

        this.emailService.sendMonthlyReportMail(List.of("owner@test.test", "partner@test.test"),
                this.report("Corsair"));

        ArgumentCaptor<MimeMessage> argument = ArgumentCaptor.forClass(MimeMessage.class);
        verify(this.javaMailSender).send(argument.capture());

        MimeMessage sentMessage = argument.getValue();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sentMessage.writeTo(out);
        String rawContent = out.toString();

        Assertions.assertAll("Check monthly report email",
                () -> assertEquals(2, sentMessage.getAllRecipients().length),
                () -> assertEquals("owner@test.test", sentMessage.getAllRecipients()[0].toString()),
                () -> assertEquals("partner@test.test", sentMessage.getAllRecipients()[1].toString()),
                () -> assertEquals("user", sentMessage.getFrom()[0].toString()),
                () -> assertTrue(sentMessage.getSubject().contains("September 2023")),
                () -> assertTrue(rawContent.contains("Eye OTM")),
                () -> assertTrue(rawContent.contains("text/html")));
    }

    @Test
    public void sendMonthlyReportMailEscapesHtmlInTradeNames() throws Exception {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(this.javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(this.javaMailSender).send(ArgumentMatchers.any(MimeMessage.class));

        this.emailService.sendMonthlyReportMail(List.of("owner@test.test"),
                this.report("<script>alert(1)</script>"));

        ArgumentCaptor<MimeMessage> argument = ArgumentCaptor.forClass(MimeMessage.class);
        verify(this.javaMailSender).send(argument.capture());

        MimeMessage sent = argument.getValue();
        sent.saveChanges();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sent.writeTo(out);
        MimeMessage parsed = new MimeMessage(Session.getInstance(new Properties()),
                new ByteArrayInputStream(out.toByteArray()));
        String html = this.findHtml(parsed.getContent());

        Assertions.assertAll("Check escaping",
                () -> assertFalse(html.contains("<script>")),
                () -> assertTrue(html.contains("&lt;script&gt;")));
    }

    private MonthlyReportDTO report(String tradeName) {
        InvestmentDTO trade = InvestmentDTO.builder()
                .transactionDate(LocalDate.of(2023, 9, 8))
                .buySell("B")
                .shortName(tradeName)
                .amount(200.17)
                .currencyId("USD")
                .build();

        return MonthlyReportDTO.builder()
                .year(2023)
                .month(9)
                .currency("EUR")
                .netWorth(NetWorthDTO.builder()
                        .currency("EUR")
                        .totalSpent(BigDecimal.valueOf(1000))
                        .totalWorth(BigDecimal.valueOf(1200))
                        .totalChangePct(BigDecimal.valueOf(20))
                        .assets(List.of(AssetClassValueDTO.builder()
                                .assetClass("Stock")
                                .spent(BigDecimal.valueOf(1000))
                                .worth(BigDecimal.valueOf(1200))
                                .changePct(BigDecimal.valueOf(20))
                                .build()))
                        .availableCurrencies(List.of("EUR"))
                        .unconvertedCurrencies(List.of())
                        .build())
                .activity(new MonthlyReportDTO.ActivitySection(List.of(trade), List.of(), List.of(), List.of(),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of()))
                .cashFlow(List.of())
                .build();
    }

    private String htmlPartOf(String symbolOrTicker, String type) throws Exception {
        MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
        when(this.javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(this.javaMailSender).send(ArgumentMatchers.any(MimeMessage.class));

        this.emailService.sendAlertMail("sendTo", symbolOrTicker, type, 100.0, 120.0, 5.0);

        ArgumentCaptor<MimeMessage> argument = ArgumentCaptor.forClass(MimeMessage.class);
        verify(this.javaMailSender).send(argument.capture());

        MimeMessage sent = argument.getValue();
        sent.saveChanges();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sent.writeTo(out);
        MimeMessage parsed = new MimeMessage(Session.getInstance(new Properties()),
                new ByteArrayInputStream(out.toByteArray()));

        return this.findHtml(parsed.getContent());
    }

    private String findHtml(Object content) throws Exception {
        if (!(content instanceof MimeMultipart multipart)) {
            return null;
        }
        for (int index = 0; index < multipart.getCount(); index++) {
            BodyPart part = multipart.getBodyPart(index);
            if (part.isMimeType("text/html")) {
                return (String) part.getContent();
            }
            String nested = this.findHtml(part.getContent());
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }
}
