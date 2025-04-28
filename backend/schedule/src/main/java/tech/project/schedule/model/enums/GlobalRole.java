package tech.project.schedule.model.enums;

/**
 * Enumeration of global user roles within the scheduling system.
 * Defines the different types of users and their system-wide permission levels.
 * Used for authorization and access control throughout the application.
 */
public enum GlobalRole {
    CLIENT, // End users who utilize the scheduling system
    ADMIN, // System administrators with full access to all features
    EMPLOYEE // Staff members who work with tasks and projects
}
