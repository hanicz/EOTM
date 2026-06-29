package eye.on.the.money.service.mail;


import eye.on.the.money.exception.EmailException;
import eye.on.the.money.repository.CredentialRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static java.lang.String.format;


@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    private final CredentialRepository credentialRepository;

    private static final Map<String, String> TYPE_LABELS = new HashMap<>();
    private static final Map<String, String> CONDITION_PHRASES = new HashMap<>();

    static {
        EmailService.TYPE_LABELS.put("PERCENT_OVER", "Percent over");
        EmailService.TYPE_LABELS.put("PERCENT_UNDER", "Percent under");
        EmailService.TYPE_LABELS.put("PRICE_OVER", "Price over");
        EmailService.TYPE_LABELS.put("PRICE_UNDER", "Price under");

        EmailService.CONDITION_PHRASES.put("PERCENT_OVER", "is up over %s%%");
        EmailService.CONDITION_PHRASES.put("PERCENT_UNDER", "is down under %s%%");
        EmailService.CONDITION_PHRASES.put("PRICE_OVER", "is over %s");
        EmailService.CONDITION_PHRASES.put("PRICE_UNDER", "is under %s");
    }

    public void sendAlertMail(String sendTo, String symbolOrTicker, String type, double valuePoint, double actualValue, double actualChange) {
        boolean isPercentType = type.startsWith("PERCENT");
        boolean isOverType = type.endsWith("OVER");
        double currentValue = isPercentType ? actualChange : actualValue;
        String typeLabel = EmailService.TYPE_LABELS.getOrDefault(type, type);
        String conditionPhrase = format(EmailService.CONDITION_PHRASES.getOrDefault(type, "triggered at %s"), formatNumber(valuePoint));
        String accentColor = isOverType ? "#3b6d11" : "#a32d2d";
        String target = isPercentType ? formatNumber(valuePoint) + "%" : formatNumber(valuePoint);
        String current = isPercentType ? formatNumber(currentValue) + "%" : formatNumber(currentValue);
        String subject = format("Eye OTM Alert: %s %s", symbolOrTicker, conditionPhrase);
        String plainText = format("%s %s\nCondition: %s\nTarget: %s\nCurrent: %s", symbolOrTicker, conditionPhrase, typeLabel, target, current);
        String html = buildHtml(symbolOrTicker, conditionPhrase, typeLabel, target, current, accentColor);

        this.send(sendTo, subject, plainText, html);
    }

    private void send(String sendTo, String subject, String plainText, String html) {
        String from = this.credentialRepository.findById("email_user").orElseThrow(NoSuchElementException::new).getSecret();
        try {
            MimeMessage mimeMessage = this.javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            helper.setTo(sendTo);
            helper.setFrom(from);
            helper.setSubject(subject);
            helper.setText(plainText, html);

            log.info("Sending alert email to {} with subject: {}", sendTo, subject);
            this.javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new EmailException("Unable to send alert email to " + sendTo, e);
        }
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return format("%.0f", value);
        }
        return format("%.2f", value);
    }

    private String buildHtml(String symbolOrTicker, String conditionPhrase, String typeLabel, String target, String current, String accentColor) {
        return "<div style=\"background:#f6f4ee;padding:32px 16px;font-family:Arial,Helvetica,sans-serif;\">"
            + "<table role=\"presentation\" align=\"center\" width=\"480\" style=\"max-width:480px;width:100%;background:#ffffff;border-radius:10px;border:1px solid #e3e0d5;border-collapse:collapse;\">"
            + "<tr><td style=\"background:#1b1b1b;padding:20px 28px;border-radius:10px 10px 0 0;\">"
            + "<span style=\"color:#ef9f27;font-size:20px;font-weight:bold;\">Eye OTM</span>"
            + "</td></tr>"
            + "<tr><td style=\"padding:28px;\">"
            + "<p style=\"margin:0 0 6px;font-size:12px;color:#888780;text-transform:uppercase;letter-spacing:0.05em;\">Price alert triggered</p>"
            + "<h1 style=\"margin:0 0 20px;font-size:21px;color:#1b1b1b;\">" + symbolOrTicker + " " + conditionPhrase + "</h1>"
            + "<table role=\"presentation\" style=\"width:100%;border-collapse:collapse;\">"
            + row("Condition", typeLabel, "#1b1b1b")
            + row("Target", target, "#1b1b1b")
            + row("Current", current, accentColor)
            + "</table>"
            + "<p style=\"margin:24px 0 0;font-size:13px;color:#888780;line-height:1.5;\">This alert has been removed from your watchlist. Create a new one anytime from the Alerts page.</p>"
            + "</td></tr>"
            + "</table>"
            + "</div>";
    }

    private String row(String label, String value, String valueColor) {
        return "<tr>"
            + "<td style=\"padding:10px 0;border-top:1px solid #f1efe8;color:#5f5e5a;font-size:13px;\">" + label + "</td>"
            + "<td style=\"padding:10px 0;border-top:1px solid #f1efe8;text-align:right;font-weight:600;color:" + valueColor + ";\">" + value + "</td>"
            + "</tr>";
    }
}
