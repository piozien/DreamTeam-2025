package tech.project.schedule.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.project.Project;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Project entities in the database.
 * Extends JpaRepository to inherit standard data operations including CRUD functionality
 * (create, read, update, delete) and standard query methods.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    /**
     * Finds a project by its unique identifier.
     * Though this method is already provided by JpaRepository, it's explicitly
     * defined here for clarity or potential custom implementation.
     * 
     * @param id The UUID of the project to find
     * @return An Optional containing the found Project or empty if not found
     */
    Optional<Project> findById(UUID id);

    /**
     * Checks if a project with the specified name already exists in the database.
     * Useful for validating uniqueness constraints during project creation or updates.
     * 
     * @param name The project name to check for existence
     * @return true if a project with the given name exists, false otherwise
     */
    boolean existsByName(String name);
}