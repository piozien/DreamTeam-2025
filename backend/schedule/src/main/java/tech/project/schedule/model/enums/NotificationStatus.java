package tech.project.schedule.model.enums;

/**
 * Defines the types of system notifications that can be generated.
 * Used to categorize notifications based on the triggering events
 * within the project management workflow.
 */
public enum NotificationStatus {
    PROJECT_MEMBER_ADDED,
    TASK_ASSIGNEE_ADDED,
    TASK_UPDATED,
    TASK_COMPLETED,
    PROJECT_CREATED
}
