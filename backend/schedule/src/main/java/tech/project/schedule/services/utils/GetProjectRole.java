package tech.project.schedule.services.utils;

import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.project.ProjectMember;
import tech.project.schedule.model.user.User;

/**
 * Utility class for determining a user's role within a project.
 * Provides a consistent way to check user permissions across the application,
 * handling special cases like global administrators.
 */
public class GetProjectRole {
    /**
     * Determines a user's role within a specific project.
     * If the user is a global administrator, they are automatically granted
     * Project Manager (PM) privileges. Otherwise, looks up the user's specific
     * role assignment within the project.
     * 
     * @param user The user whose project role should be determined
     * @param project The project context in which to check the user's role
     * @return The user's role in the project, or null if they are not a member
     */
    public static ProjectUserRole getProjectRole(User user, Project project) {
        if(user.getGlobalRole() != null && GlobalRole.ADMIN.equals(user.getGlobalRole())){
            return ProjectUserRole.PM;
        }
        ProjectMember member = project.getMembers().get(user.getId());
        return member != null ? member.getRole() : null;
    }
}
