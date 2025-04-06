package tech.project.schedule.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.enums.ProjectStatus;
import tech.project.schedule.model.project.Project;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    // Podstawowa metoda findById jest już dostępna z JpaRepository
    // Optional<Project> findById(UUID id);

    // Dodatkowe metody wyszukiwania, które mogą być przydatne:

    // Znajdź projekt po nazwie
    Optional<Project> findByName(String name);

    // Znajdź projekty po statusie
    List<Project> findByProjectStatus(ProjectStatus status);

    // Znajdź projekty, które się rozpoczynają po określonej dacie
    List<Project> findByStartDateAfter(LocalDate date);

    // Znajdź projekty, które kończą się przed określoną datą
    List<Project> findByEndDateBefore(LocalDate date);

    // Znajdź projekty po nazwie (ignorując wielkość liter)
    Optional<Project> findByNameIgnoreCase(String name);

    // Sprawdź czy projekt istnieje po ID
    boolean existsById(UUID id);
}