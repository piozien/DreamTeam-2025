package tech.project.schedule.model.enums;

/**
 * Defines types of notifications that can be triggered by system events.
 * Used to categorize notifications based on the specific action that occurred,
 * allowing for appropriate handling and display throughout the application.
 */
public enum NotificationStatus {
    PROJECT_MEMBER_ADDED,
    TASK_ASSIGNEE_ADDED,
    TASK_UPDATED,
    TASK_COMPLETED,
    PROJECT_CREATED,
    PROJECT_UPDATED,
    PROJECT_DELETED,
    TASK_DELETED,
    TASK_COMMENT_ADDED,
    TASK_COMMENT_DELETED,
    TASK_COMMENT_UPDATED,
    TASK_DEPENDENCY_ADDED,
    TASK_DEPENDENCY_DELETED,
    TASK_DEPENDENCY_UPDATED,
    TASK_FILE_UPLOADED,
    TASK_FILE_DELETED,
    TASK_FILE_UPDATED,
    
    // Error and system notification types
    ERROR,              // For general error notifications
    PERMISSION_DENIED,  // For access control violations
    SYSTEM,             // For system-wide announcements and administrative messages
    
    // Additional notification types for future use
    DEADLINE_APPROACHING,
    TASK_OVERDUE,
    MENTION              // For when a user is mentioned in comments
}
