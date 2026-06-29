package eye.on.the.money.mail;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.service.mail.EmailService;
import jakarta.mail.internet.MimeMessage;
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

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
