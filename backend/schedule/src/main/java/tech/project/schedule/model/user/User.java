package tech.project.schedule.model.user;

import jakarta.persistence.*;
import lombok.Data;
import tech.project.schedule.model.notification.Notification;
import tech.project.schedule.model.enums.GlobalRole;
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
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "global_role", nullable = false)
    private GlobalRole globalRole;

    @OneToMany(mappedBy = "user")
    private Set<ProjectMember> projectMembers;

    @OneToMany(mappedBy = "user")
    private Set<TaskAssignee> taskAssignments;

    @OneToMany(mappedBy = "user")
    private Set<TaskComment> taskComments;

    @OneToMany(mappedBy = "changedBy")
    private Set<TaskHistory> taskHistories;

    @OneToMany(mappedBy = "uploadedBy")
    private Set<TaskFile> taskFiles;

    @OneToMany(mappedBy = "user")
    private Set<Notification> notifications;
}
