package tech.project.schedule.model.project;

import jakarta.persistence.*;
import lombok.Data;
import tech.project.schedule.model.enums.ProjectStatus;
import tech.project.schedule.model.task.Task;

import java.time.LocalDate;
import java.util.*;

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

    @Column(name = "enddate")
    private LocalDate endDate;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Map<UUID, ProjectMember> members = new HashMap<>();
    // ToDo: if members is null add PrePersist and PreUpdate

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private Set<Task> tasks;

    @Enumerated(EnumType.STRING)
    private ProjectStatus projectStatus;

    public void addMember(UUID userID, ProjectMember member) {
        this.members.put(userID, member);
    }
}
