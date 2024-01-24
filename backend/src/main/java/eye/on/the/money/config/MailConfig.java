package eye.on.the.money.config;

import eye.on.the.money.repository.CredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.NoSuchElementException;
import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${spring.mail.host}")
    private String mailServerHost;
    @Value("${spring.mail.port}")
    private Integer mailServerPort;
    @Value("${spring.mail.properties.mail.smtp.auth}")
    private String mailServerAuth;
    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private String mailServerStartTls;

    @Autowired
    private CredentialRepository credentialRepository;

    @Bean
    @DependsOnDatabaseInitialization
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(this.mailServerHost);
        mailSender.setPort(this.mailServerPort);

        mailSender.setUsername(this.credentialRepository.findById("email_user").orElseThrow(NoSuchElementException::new).getSecret());
        mailSender.setPassword(this.credentialRepository.findById("email_password").orElseThrow(NoSuchElementException::new).getSecret());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", this.mailServerAuth);
        props.put("mail.smtp.starttls.enable", this.mailServerStartTls);
        props.put("mail.debug", "true");

        return mailSender;
    }
}
