package tech.project.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.project.Project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findByName(String name);

    List<Project> findByStartDateBetween(LocalDate start, LocalDate end);

    List<Project> findByEndDateBetween(LocalDate start, LocalDate end);
}
