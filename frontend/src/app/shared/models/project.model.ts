import { User } from './user.model';

export enum GlobalRole {
  CLIENT = 'client',
  ADMIN = 'admin'
}

export enum ProjectUserRole {
  PM = 'pm',
  MEMBER = 'member',
  VIEWER = 'viewer'
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