package eye.on.the.money.service.mail;


import eye.on.the.money.repository.CredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;


@Service
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    private final CredentialRepository credentialRepository;

    @Autowired
    public EmailService(JavaMailSender javaMailSender, CredentialRepository credentialRepository) {
        this.javaMailSender = javaMailSender;
        this.credentialRepository = credentialRepository;
    }

    public void sendMail(String sendTo, String message) {
        log.trace("Enter");
        SimpleMailMessage smp = new SimpleMailMessage();
        smp.setTo(sendTo);
        smp.setSubject("EOTM Alert");
        smp.setText(message);
        smp.setFrom(this.credentialRepository.findById("email_user").orElseThrow(NoSuchElementException::new).getSecret());

        log.trace("Send alert to {} with message: {}", sendTo, message);
        this.javaMailSender.send(smp);
    }
}