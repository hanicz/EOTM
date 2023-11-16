package eye.on.the.money.mail.impl;

import eye.on.the.money.EotmApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class EmailServiceImplTest {

    @MockBean
    private JavaMailSender javaMailSender;

    @Autowired
    private EmailServiceImpl emailService;

    @Test
    public void sendEmail() {
        doNothing().when(this.javaMailSender).send(ArgumentMatchers.any(SimpleMailMessage.class));
        ArgumentCaptor<SimpleMailMessage> argument = ArgumentCaptor.forClass(SimpleMailMessage.class);
        this.emailService.sendMail("sendTo", "message");

        verify(this.javaMailSender).send(argument.capture());
        Assertions.assertAll("Check email",
                () -> assertEquals("message", argument.getValue().getText()),
                () -> assertTrue(Arrays.asList(argument.getValue().getTo()).contains("sendTo")),
                () -> assertEquals("EOTM Alert", argument.getValue().getSubject()),
                () -> assertEquals("user", argument.getValue().getFrom()));
    }
}