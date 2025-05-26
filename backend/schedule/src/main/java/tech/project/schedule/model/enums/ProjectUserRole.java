package tech.project.schedule.model.enums;

/**
 * Enumeration of user roles within the context of a specific project.
 * Defines the different levels of access and responsibility users can have on a project.
 */
public enum ProjectUserRole {
    PM, // Project Manager with administrative authority over the project
    MEMBER, // Team member working on project tasks
    CLIENT // Stakeholder or customer for whom the project is being delivered
}
