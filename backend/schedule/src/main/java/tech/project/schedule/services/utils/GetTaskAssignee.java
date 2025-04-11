package tech.project.schedule.services.utils;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.user.User;

public class GetTaskAssignee {
    public static User getAssignee(Task task, User user){
         boolean isAssignee = task.getAssignees().stream().anyMatch(a -> a.getUser().getId().equals(user.getId()));
         return isAssignee ? user : null;
    }
}
