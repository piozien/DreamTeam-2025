import { GlobalRole } from '../enums/global-role.enum';
import { UserStatus } from '../enums/user-status.enum';

export interface User {
    id: string;
    username: string;
    firstName: string;
    lastName: string;
    name: string; // First name + Last name
    email: string;
    globalRole: GlobalRole;
    userStatus: UserStatus;
}

export interface UserLogin {
    email: string;
    password: string;
}

export interface UserCreate {
    username: string;
    firstName: string;
    lastName: string;
    email: string;
    password?: string; // Optional because password may be set later via email link
}