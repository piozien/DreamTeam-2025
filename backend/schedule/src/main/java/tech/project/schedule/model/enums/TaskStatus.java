package tech.project.schedule.model.enums;

/**
 * Enumeration representing the possible status states of a task.
 * Used to track the progression of tasks through the workflow lifecycle.
 */
public enum TaskStatus {
    TO_DO, // Task has been created but work hasn't started yet
    IN_PROGRESS, // Task is actively being worked on
    FINISHED; // Task has been completed
}
