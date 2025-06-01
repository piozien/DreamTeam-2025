import { GlobalRole } from "../../enums/global-role.enum";

// Note: Password is set later
export interface RegistrationRequest {
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  globalRole: GlobalRole;
}
