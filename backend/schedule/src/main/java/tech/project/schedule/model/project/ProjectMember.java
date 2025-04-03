package tech.project.schedule.model.project;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.project.schedule.model.user.User;
import tech.project.schedule.model.enums.ProjectUserRole;

import java.util.UUID;

@Entity
@Table(name = "Project_Members")
@Data
@NoArgsConstructor
public class ProjectMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

// one-sided coverage will probably be better
// so that the person who creates the project can manage it right away
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectUserRole role;

    public ProjectMember(User user, ProjectUserRole projectUserRole) {
        this.user = user;
        this.role = projectUserRole;
    }
}
