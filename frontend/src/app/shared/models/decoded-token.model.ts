import { GlobalRole } from "../enums/global-role.enum";
import { UserStatus } from "../enums/user-status.enum";

// Interface for the decoded JWT payload
export interface DecodedToken {
  userId: string; // User ID (Ensure this matches backend JWT claim type)
  sub: string; // Subject (usually username or email)
  role: GlobalRole; // User's global role
  status: UserStatus; // User's status
  firstName?: string; // Optional: User's first name
  lastName?: string; // Optional: User's last name
  email?: string; // Optional: User's email
  iat: number; // Issued at timestamp
  exp: number; // Expiration timestamp
  // OAuth2 specific fields that might be present
  name?: string; // User's full name (from OAuth)
  preferred_username?: string; // Preferred username (from OAuth)
  // Add any other custom claims if present
}
