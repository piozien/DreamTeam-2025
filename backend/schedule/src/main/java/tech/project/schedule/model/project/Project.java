package tech.project.schedule.model.project;

import jakarta.persistence.*;
import lombok.Data;
import tech.project.schedule.model.task.Task;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "Projects")
@Data
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "startdate", nullable = false)
    private LocalDate startDate;

    @Column(name = "enddate", nullable = false)
    private LocalDate endDate;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private Set<ProjectMember> members;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private Set<Task> tasks;
}
