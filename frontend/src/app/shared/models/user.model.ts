export enum GlobalRole {
    CLIENT = 'client',
    ADMIN = 'admin'
}

export interface User {
    id: string;
    name: string;
    email: string;
    globalRole: GlobalRole;
}
