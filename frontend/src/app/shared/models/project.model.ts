import { User } from './user.model';

export enum GlobalRole {
  CLIENT = 'client',
  ADMIN = 'admin'
}

export enum ProjectUserRole {
  PM = 'PM',
  MEMBER = 'MEMBER',
  CLIENT = 'CLIENT'
}

export enum ProjectStatus {
  COMPLETED = 'completed',
  IN_PROGRESS = 'in_progress',
  PLANNED = 'planned'
}

export interface ProjectMember {
  id: string;
  projectId: string;
  user: User;
  role: ProjectUserRole;
}

export interface Project {
  id: string;
  name: string;
  description: string;
  startDate: Date;
  endDate?: Date;
  members: ProjectMember[];
  status: ProjectStatus;
}

export interface ProjectCreate {
  name: string;
  description: string;
  startDate: Date;
  endDate?: Date;
}

export interface ProjectMemberDTO {
  id: string;
  projectId: string;
  userId: string;
  name: string;  // User name field returned by the backend
  role: ProjectUserRole;
}

export interface AddProjectMemberDTO {
  userId: string;
  role: ProjectUserRole;
}

export interface UpdateProjectMemberRoleDTO {
  role: ProjectUserRole;
}