package tech.project.schedule.model.task;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tech.project.schedule.model.enums.TaskPriority;
import tech.project.schedule.model.enums.TaskStatus;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.project.ProjectMember;

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
    @EqualsAndHashCode.Exclude
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
    @EqualsAndHashCode.Exclude
    private Set<TaskAssignee> assignees;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    private Set<TaskComment> comments;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    private Set<TaskHistory> history;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    private Set<TaskFile> files;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    private Set<TaskDependency> dependencies;

    @OneToMany(mappedBy = "dependsOnTask", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    private Set<TaskDependency> dependentTasks;

    public void addAssignee(TaskAssignee assignee) {this.assignees.add(assignee);}

    public Task(Project project, String name, String description,
                LocalDate startDate, TaskStatus status) {
        this.project = project;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.status = status;
        this.assignees = new HashSet<>();
        this.comments = new HashSet<>();
        this.history = new HashSet<>();
        this.dependencies = new HashSet<>();
        this.dependentTasks = new HashSet<>();
        this.files = new HashSet<>();
    }

    @PrePersist
    @PreUpdate
    private void ensureCollectionsAreInitialized(){
        if(assignees == null){
            assignees = new HashSet<>();
        }
        if(comments == null){
            comments = new HashSet<>();
        }
        if(history == null){
            history = new HashSet<>();
        }
        if(dependencies == null){
            dependencies = new HashSet<>();
        }
        if(dependentTasks == null){
            dependentTasks = new HashSet<>();
        }
        if(files == null){
            files = new HashSet<>();
        }
    }
}
