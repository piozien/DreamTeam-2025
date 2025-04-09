package tech.project.schedule.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.user.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u JOIN u.projects pm WHERE pm.project.id = :projectId")
    List<User> getUsersByProjectId(@Param("projectId") UUID projectId);

    Optional<User> findByEmail(String email);

}
