export enum GlobalRole {
    CLIENT = 'client',
    ADMIN = 'admin'
}

export interface User {
    id: string;
    userName: string;
    firstName: string;
    lastName: string;
    name: string;
    email: string;
    password: string;
    globalRole: GlobalRole;
}

export interface UserCreate {
    userName: string;
    firstName: string;
    lastName: string;
    password: string;
    email: string;
}

export interface UserLogin {
    email: string;
    password: string;
}