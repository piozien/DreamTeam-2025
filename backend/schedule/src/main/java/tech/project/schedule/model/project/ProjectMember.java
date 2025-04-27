package tech.project.schedule.model.project;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tech.project.schedule.model.user.User;
import tech.project.schedule.model.enums.ProjectUserRole;

import java.util.UUID;

/**
 * Entity class representing a member of a project team.
 * Defines the relationship between users and projects, specifying the role
 * that each user holds within a specific project context.
 */
@Entity
@Table(name = "Project_Members")
@Data
@NoArgsConstructor
public class ProjectMember {
    /**
     * Unique identifier for the project membership record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The project to which this membership applies.
     */
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private Project project;

// one-sided coverage will probably be better
// so that the person who creates the project can manage it right away
    /**
     * The user who is a member of the project.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

     /**
     * The role assigned to this user within the project.
     * Defines their permissions and responsibilities.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectUserRole role;

     /**
     * Creates a new project member with the specified user and role.
     * 
     * @param user The user to add as a project member
     * @param projectUserRole The role to assign to the user in the project
     */
    public ProjectMember(User user, ProjectUserRole projectUserRole) {
        this.user = user;
        this.role = projectUserRole;
    }
}
