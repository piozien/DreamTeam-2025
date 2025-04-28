package tech.project.schedule.services;

public class MailContent {
    public static final String REGISTRATION_SUBJECT = "Account registration - set your password";
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

    public static final String PASSWORD_RESET_SUBJECT = "Password reset";
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
