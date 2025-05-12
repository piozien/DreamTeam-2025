package tech.project.schedule.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.user.User;

import java.util.Optional;
import java.util.UUID;
    @Repository
public interface UserRepository extends JpaRepository<User, UUID> {

     /**
     * Finds a user by their email address.
     * Email addresses are unique in the system, making this method useful for
     * authentication processes and user lookups.
     * 
     * @param email The email address to search for
     * @return An Optional containing the User if found, or empty if no user has the specified email
     */
    Optional<User> findByEmail(String email);

}
