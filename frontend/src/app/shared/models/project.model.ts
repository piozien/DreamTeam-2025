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
  endDate: Date;
  members: ProjectMember[];
}