export interface Project {
    id: number;
    name: string;
    description: string;
    startDate: Date;
    endDate: Date;
    status: ProjectStatus;
    teamMembers: TeamMember[];
}

export enum ProjectStatus {
    NOT_STARTED = 'NIE_ROZPOCZĘTY',
    IN_PROGRESS = 'W_TRAKCIE',
    ON_HOLD = 'WSTRZYMANY',
    COMPLETED = 'ZAKOŃCZONY'
}

export interface TeamMember {
    id: number;
    name: string;
    email: string;
    role: string;
}