package eye.on.the.money.config;

import eye.on.the.money.model.Credential;
import eye.on.the.money.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Optional;
import java.util.Properties;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class MailConfig {

    public static final String EMAIL_USER = "email_user";
    public static final String EMAIL_PASSWORD = "email_password";

    @Value("${spring.mail.host}")
    private String mailServerHost;
    @Value("${spring.mail.port}")
    private Integer mailServerPort;
    @Value("${spring.mail.properties.mail.smtp.auth}")
    private String mailServerAuth;
    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private String mailServerStartTls;

    private final CredentialRepository credentialRepository;


    @Bean
    @DependsOnDatabaseInitialization
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(this.mailServerHost);
        mailSender.setPort(this.mailServerPort);

        Optional<String> username = this.credentialRepository.findById(MailConfig.EMAIL_USER).map(Credential::getSecret);
        Optional<String> password = this.credentialRepository.findById(MailConfig.EMAIL_PASSWORD).map(Credential::getSecret);
        if (username.isEmpty() || password.isEmpty()) {
            log.warn("Credentials {} and {} not found, email sending and alerts are disabled.",
                    MailConfig.EMAIL_USER, MailConfig.EMAIL_PASSWORD);
        } else {
            mailSender.setUsername(username.get());
            mailSender.setPassword(password.get());
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", this.mailServerAuth);
        props.put("mail.smtp.starttls.enable", this.mailServerStartTls);
        props.put("mail.debug", "false");

        return mailSender;
    }
}
