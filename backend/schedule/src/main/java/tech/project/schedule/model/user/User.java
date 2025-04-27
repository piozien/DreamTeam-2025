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
import tech.project.schedule.model.task.TaskHistory;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "Users")
@Data
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String firstName;

    private String lastName;
    
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "global_role", nullable = false)
    private GlobalRole globalRole;

    @OneToMany(mappedBy = "user")
    @EqualsAndHashCode.Exclude
    private Set<ProjectMember> projectMembers;

    @OneToMany(mappedBy = "user")
    @EqualsAndHashCode.Exclude
    private Set<TaskAssignee> taskAssignments;

    @OneToMany(mappedBy = "user")
    @EqualsAndHashCode.Exclude
    private Set<TaskComment> taskComments;

    @OneToMany(mappedBy = "changedBy")
    @EqualsAndHashCode.Exclude
    private Set<TaskHistory> taskHistories;

    @OneToMany(mappedBy = "uploadedBy")
    @EqualsAndHashCode.Exclude
    private Set<TaskFile> taskFiles;

    @OneToMany(mappedBy = "user")
    @EqualsAndHashCode.Exclude
    private Set<ProjectMember> projects;

    @OneToMany(mappedBy = "user")
    @EqualsAndHashCode.Exclude
    private Set<Notification> notifications;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    private UserStatus userStatus = UserStatus.UNAUTHORIZED;

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
