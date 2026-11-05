package com.bupt.ta.service;

import com.bupt.ta.util.AppConfig;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Sends email via SMTP when configured through environment variables; otherwise no-op.
 *
 * <p>Resolution order matches {@link com.bupt.ta.util.AppConfig}: environment variable,
 * JVM property, then defaults. Used for password-reset links and optional application-status
 * notifications. When {@code SMTP_HOST} or {@code SMTP_FROM} is unset, {@link #sendPlainText} returns
 * silently without throwing.
 *
 * @see com.bupt.ta.util.AppConfig
 */
public class NotificationService {

    private final boolean configured;
    private final Session session;
    private final String fromAddress;

    /** Reads SMTP settings from {@link AppConfig}; becomes no-op when host/from are unset. */
    public NotificationService() {
        String host = AppConfig.resolve("SMTP_HOST", new String[]{"smtp.host"}, null, "smtp.host", "");
        String port = AppConfig.resolve("SMTP_PORT", new String[]{"smtp.port"}, null, "smtp.port", "587");
        fromAddress = AppConfig.resolve("SMTP_FROM", new String[]{"smtp.from"}, null, "smtp.from", "");
        String user = AppConfig.resolve("SMTP_USER", new String[]{"smtp.user"}, null, "smtp.user", "");
        String password = AppConfig.resolve("SMTP_PASSWORD", new String[]{"smtp.password"}, null, "smtp.password", "");

        configured = !host.isEmpty() && !fromAddress.isEmpty();
        if (configured) {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", !user.isEmpty());
            props.put("mail.smtp.starttls.enable", "true");
            if (!user.isEmpty()) {
                session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user, password);
                    }
                });
            } else {
                session = Session.getInstance(props);
            }
        } else {
            session = null;
        }
    }

    /** Returns {@code true} when SMTP host and from-address are configured. */
    public boolean isConfigured() {
        return configured;
    }

    /** Sends UTF-8 plain text when configured; silently no-op otherwise or on delivery failure. */
    public void sendPlainText(String to, String subject, String body) {
        if (!configured || to == null || to.trim().isEmpty()) {
            return;
        }
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to.trim(), false));
            message.setSubject(subject != null ? subject : "", "UTF-8");
            message.setText(body != null ? body : "", "UTF-8");
            Transport.send(message);
        } catch (Exception ignored) {
            // graceful no-op on delivery failure
        }
    }

    /**
     * Sends a status-change notification when SMTP is configured; otherwise no-op.
     */
    public void sendStatusChangeEmail(String toEmail, String applicantName, String moduleLabel,
                                      String newStatus, String feedback) {
        if (!configured || toEmail == null || toEmail.trim().isEmpty()) {
            return;
        }
        String name = applicantName != null && !applicantName.trim().isEmpty() ? applicantName.trim() : "Applicant";
        String module = moduleLabel != null && !moduleLabel.trim().isEmpty() ? moduleLabel.trim() : "your application";
        String status = newStatus != null ? newStatus.trim() : "";
        String subject = "TA Recruitment — application " + status.toLowerCase();
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(name).append(",\n\n");
        body.append("Your application for ").append(module).append(" is now: ").append(status).append(".\n");
        if (feedback != null && !feedback.trim().isEmpty()) {
            body.append("\nFeedback from the module organiser:\n").append(feedback.trim()).append("\n");
        }
        body.append("\nLog in to the TA Recruitment System to view details.\n");
        sendPlainText(toEmail, subject, body.toString());
    }
}
