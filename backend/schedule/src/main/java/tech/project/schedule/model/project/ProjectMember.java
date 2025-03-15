package tech.project.schedule.model.project;

import jakarta.persistence.*;
import lombok.Data;
import tech.project.schedule.model.user.User;
import tech.project.schedule.model.enums.ProjectUserRole;

import java.util.UUID;

@Entity
@Table(name = "Project_Members")
@Data
public class ProjectMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectUserRole role;
}
