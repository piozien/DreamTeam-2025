package tech.project.schedule.model.task;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.project.schedule.model.enums.TaskPriority;
import tech.project.schedule.model.enums.TaskStatus;
import tech.project.schedule.model.project.Project;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "Task")
@Data
@NoArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "startdate", nullable = false)
    private LocalDate startDate;

    @Column(name = "enddate")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    // ToDo: reference to ProejctMembers int the Project
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private Set<TaskAssignee> assignees;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private Set<TaskComment> comments;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private Set<TaskHistory> history;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private Set<TaskFile> files;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private Set<TaskDependency> dependencies;

    @OneToMany(mappedBy = "dependsOnTask", cascade = CascadeType.ALL)
    private Set<TaskDependency> dependentTasks;

    public Task(Project project, String name, String description,
                LocalDate startDate, TaskStatus status, Set<TaskAssignee> assignees,
                Set<TaskComment> comments, Set<TaskHistory> history, Set<TaskDependency> dependencies,
                Set<TaskDependency> dependentTasks, Set<TaskFile> files) {
        this.project = project;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.status = status;
        // ToDo: initialize sets correctly
        this.assignees = new HashSet<>();
        this.comments = new HashSet<>();
        this.history = new HashSet<>();
        this.dependencies = new HashSet<>();
        this.dependentTasks = new HashSet<>();
        this.files = new HashSet<>();
    }
}
