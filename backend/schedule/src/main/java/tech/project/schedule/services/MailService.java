package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending emails to users.
 * Provides methods for sending registration and notification emails.
 */
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;

    /**
     * Sends a registration confirmation email with a password reset link.
     *
     * @param to      Recipient's email address
     * @param subject Subject of the email
     * @param text    Body of the email (should contain the reset link)
     */
    public void sendRegistrationConfirmation(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}

