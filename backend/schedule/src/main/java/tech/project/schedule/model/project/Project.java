package tech.project.schedule.model.project;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tech.project.schedule.model.enums.ProjectStatus;
import tech.project.schedule.model.task.Task;

import java.time.LocalDate;
import java.util.*;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Entity class representing a project in the scheduling system.
 * A project serves as a container for tasks and team members, with tracking
 * of timeline information and overall status.
 */
@Entity
@Table(name = "Projects")
@Data
@NoArgsConstructor
public class Project {
     /**
     * Unique identifier for the project.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The name of the project.
     */
    @Column(nullable = false)
    private String name;

     /**
     * Optional detailed description of the project.
     */
    private String description;

    /**
     * The date when the project is scheduled to start.
     */
    @Column(name = "startdate", nullable = false)
    private LocalDate startDate;

    /**
     * Optional date when the project is scheduled to end.
     */
    @Column(name = "enddate")
    private LocalDate endDate;

    /**
     * Map of project members keyed by user ID.
     * Represents the team assigned to this project with their roles.
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @EqualsAndHashCode.Exclude
    private Map<UUID, ProjectMember> members;
    // ToDo: if members is null add PrePersist and PreUpdate

     /**
     * Collection of tasks that belong to this project.
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    private Set<Task> tasks;

    /**
     * Current status of the project (e.g., PLANNED, IN_PROGRESS, COMPLETED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "project_status")
    private ProjectStatus projectStatus;

     /**
     * Adds a new member to the project with the specified user ID.
     * 
     * @param userID The ID of the user to add as a member
     * @param member The ProjectMember entity containing role information
     */
    public void addMember(UUID userID, ProjectMember member) {
        this.members.put(userID, member);
    }

    /**
     * Creates a new project with basic information.
     * 
     * @param name The name of the project
     * @param description The description of the project
     * @param startDate The start date of the project
     * @param endDate The end date of the project (can be null)
     */
    public Project(String name, String description, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = (endDate != null) ? endDate : null;
        this.members = new HashMap<>();
        this.tasks = new HashSet<>();
    }

    /**
     * Ensures that collections are properly initialized before persisting or updating.
     * Prevents null pointer exceptions when interacting with collections.
     */
    @PrePersist
    @PreUpdate
    private void ensureCollectionsAreInitialized() {
        if (members == null) {
            members = new HashMap<>();
        }
        if (tasks == null) {
            tasks = new HashSet<>();
        }
    }
}
