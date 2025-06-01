import { TaskPriority } from "../../enums/task-priority.enum";
import { TaskStatus } from "../../enums/task-status.enum";
import { TaskComment, TaskFile } from "../../models/task.model";

export interface TaskUpdate {
  id: string;
  projectId?: string;
  name?: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  priority?: TaskPriority;
  status?: TaskStatus;
  assigneeIds?: string[];
  comments?: TaskComment[];
  files?: TaskFile[];
  dependencyIds?: string[];
}
