package tech.project.schedule.utils;

import org.springframework.http.HttpStatus;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.UserStatus;
import tech.project.schedule.model.user.User;

public class UserUtils {
    /**
     * Throws ApiException if user is blocked.
     * @param user the user to check
     */
    /**
     * Throws ApiException if user is not AUTHORIZED.
     * Only authorized users can perform actions.
     * @param user the user to check
     */
    public static void assertAuthorized(User user) {
        if (user.getUserStatus() != UserStatus.AUTHORIZED) {
            throw new ApiException("User must be authorized to perform this action", HttpStatus.FORBIDDEN);
        }
    }
}
