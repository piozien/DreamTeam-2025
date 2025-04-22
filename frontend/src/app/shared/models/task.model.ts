import { Project } from './project.model';

export enum TaskStatus {
  TO_DO = 'TO_DO',
  IN_PROGRESS = 'IN_PROGRESS',
  FINISHED = 'FINISHED'
}

export enum TaskPriority {
  OPTIONAL = 'OPTIONAL',
  IMPORTANT = 'IMPORTANT',
  CRITICAL = 'CRITICAL'
}

export interface Task {
  id: string;
  projectId: string;
  name: string;
  description: string;
  startDate: string;
  endDate?: string;
  priority: TaskPriority;
  status: TaskStatus;
  assigneeIds?: string[];
  comments?: TaskComment[];
  files?: TaskFile[];
  dependencyIds?: string[];
}

export interface TaskComment {
  id: string;
  taskId: string;
  userId: string;
  comment: string;
  createdAt: string;
}

export interface TaskFile {
  id: string;
  taskId: string;
  uploadedById: string;
  filePath: string;
  uploadedAt: string;
}

export interface TaskAssignee {
  id: string;
  taskId: string;
  userId: string;
}

export interface TaskDependency {
  id: string;
  taskId: string;
  dependsOnTaskId: string;
}

export interface TaskRequest {
  projectId: string;
  name: string;
  description?: string;
  startDate: string;
  endDate?: string;
  priority: TaskPriority;
  status: TaskStatus;
  assigneeIds?: string[];
  comments?: TaskComment[];
  files?: TaskFile[];
  dependencyIds?: string[];
}

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