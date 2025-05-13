package tech.project.schedule.services;

/**
 * Contains standard email templates used throughout the application.
 * Defines subjects and HTML-formatted message bodies for system notifications
 * like account registration and password reset requests.
 */
public class MailContent {
     /**
     * Email subject for registration confirmation messages.
     */
    public static final String REGISTRATION_SUBJECT = "Account registration - set your password";

    /**
     * HTML template for registration emails with password setup link.
     * Contains a %s placeholder to be replaced with the actual setup URL.
     */
    public static final String REGISTRATION_BODY = "<html>" +
            "<body>" +
            "<p>Dear User,</p>" +
            "<p>Your account has been created successfully. To set your password, please click the link below:</p>" +
            "<p style='text-align: center;'><a href='%s' style='color: #007BFF; text-decoration: none;'>Set Your Password</a></p>" +
            "<p>If you did not request this, please ignore this email.</p>" +
            "<br>" +
            "<p>Best regards,<br>Your Support Team</p>" +
            "</body>" +
            "</html>";

      /**
     * Email subject for password reset requests.
     */
    public static final String PASSWORD_RESET_SUBJECT = "Password reset";
    
    /**
     * HTML template for password reset emails.
     * Contains a %s placeholder to be replaced with the reset URL.
     */
    public static final String PASSWORD_RESET_BODY = "<html>" +
            "<body>" +
            "<p>Dear User,</p>" +
            "<p>We received a request to reset your password. To reset it, please click the link below:</p>" +
            "<p style='text-align: center;'><a href='%s' style='color: #007BFF; text-decoration: none;'>Reset Your Password</a></p>" +
            "<p>If you did not request this, please ignore this email.</p>" +
            "<br>" +
            "<p>Best regards,<br>Your Support Team</p>" +
            "</body>" +
            "</html>";
}
