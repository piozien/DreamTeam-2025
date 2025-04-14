package tech.project.schedule.services.utils;

import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.project.ProjectMember;
import tech.project.schedule.model.user.User;

public class GetProjectRole {
    public static ProjectUserRole getProjectRole(User user, Project project) {
        if(user.getGlobalRole() != null && GlobalRole.ADMIN.equals(user.getGlobalRole())){
            return ProjectUserRole.PM;
        }
        ProjectMember member = project.getMembers().get(user.getId());
        return member != null ? member.getRole() : null;
    }
}
