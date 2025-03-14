package tech.project.schedule.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

// ENUMS
enum GlobalRoleEnum { CLIENT, ADMIN }
enum ProjectUserRoleEnum { PM, MEMBER, VIEWER }
enum TaskPriorityEnum { CRITICAL, IMPORTANT, OPTIONAL }
enum TaskStatusEnum { TO_DO, IN_PROGRESS, FINISHED }

@Entity
@Table(name = "Users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private GlobalRoleEnum globalRole;
}

@Entity
@Table(name = "Projects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
}

@Entity
@Table(name = "Project_Members")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProjectMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private ProjectUserRoleEnum role;
}

@Entity
@Table(name = "Tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private TaskPriorityEnum priority;

    @Enumerated(EnumType.STRING)
    private TaskStatusEnum status;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}

@Entity
@Table(name = "Task_Assignees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TaskAssignee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

@Entity
@Table(name = "Task_Comments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TaskComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String comment;
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

@Entity
@Table(name = "Task_History")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TaskHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime changedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private TaskStatusEnum oldStatus;

    @Enumerated(EnumType.STRING)
    private TaskStatusEnum newStatus;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;
}

@Entity
@Table(name = "Task_Files")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TaskFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String filePath;
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;
}

@Entity
@Table(name = "Task_Dependencies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TaskDependency {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "depends_on_task_id", nullable = false)
    private Task dependsOn;
}

@Entity
@Table(name = "Notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String message;
    private boolean isRead = false;
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
