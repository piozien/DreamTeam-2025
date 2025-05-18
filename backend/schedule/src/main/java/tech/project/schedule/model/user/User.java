package tech.project.schedule.model.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tech.project.schedule.model.notification.Notification;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.UserStatus;
import tech.project.schedule.model.project.ProjectMember;
import tech.project.schedule.model.task.TaskAssignee;
import tech.project.schedule.model.task.TaskComment;
import tech.project.schedule.model.task.TaskFile;

import java.util.Set;
import java.util.UUID;

/**
 * Entity class representing a user in the scheduling system.
 * Contains personal information and authentication details for users,
 * along with their system-wide role and relationships to various entities
 * across the application.
 */
@Entity
@Table(name = "Users")
@Data
@NoArgsConstructor
public class User {
    /**
     * Unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User's first name.
     */
    private String firstName;

    /**
     * User's last name.
     */
    private String lastName;

     /**
     * User's full name, typically constructed from first and last name.
     */
    @Column(nullable = false)
    private String name;

    /**
     * User's email address, must be unique in the system.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * User's password, typically stored in hashed form.
     */
    private String password;

    /**
     * User's chosen username for identification in the system.
     */
    private String username;

    /**
     * User's system-wide role that determines global permissions.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "global_role", nullable = false)
    private GlobalRole globalRole;

    /**
     * Projects where the user is a member, with their respective roles.
     */
    @OneToMany(mappedBy = "user")
    @EqualsAndHashCode.Exclude
    private Set<ProjectMember> projectMembers;

    /**
     * Tasks assigned to this user.
     */
    @OneToMany(mappedBy = "user")
    @EqualsAndHashCode.Exclude
    private Set<TaskAssignee> taskAssignments;

     /**
     * Comments made by this user on tasks.
     */
    @OneToMany(mappedBy = "user")
    @EqualsAndHashCode.Exclude
    private Set<TaskComment> taskComments;

     /**
     * Files uploaded by this user.
     */
    @OneToMany(mappedBy = "uploadedBy")
    @EqualsAndHashCode.Exclude
    private Set<TaskFile> taskFiles;

    /**
     * Projects associated with this user.
     */
    @OneToMany(mappedBy = "user")
    @EqualsAndHashCode.Exclude
    private Set<ProjectMember> projects;

    /**
     * Notifications directed to this user.
     */
    @OneToMany(mappedBy = "user")
    @EqualsAndHashCode.Exclude
    private Set<Notification> notifications;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    private UserStatus userStatus = UserStatus.UNAUTHORIZED;
    
    /**
     * Refresh token for Google OAuth2 services like Google Calendar.
     * Used to automatically renew access tokens when they expire.
     */
    @Column(name = "google_refresh_token")
    private String googleRefreshToken;

    public User(String firstName, String lastName,
                String email, String password,
                String username) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.username = username;
        this.globalRole = GlobalRole.CLIENT;
        this.name = firstName + " " + lastName;
    }
}
