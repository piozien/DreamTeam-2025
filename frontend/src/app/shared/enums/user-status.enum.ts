export enum UserStatus {
  UNAUTHORIZED = 'UNAUTHORIZED', // Initial status after registration
  AUTHORIZED = 'AUTHORIZED',   // Status after setting password via email link
  BLOCKED = 'BLOCKED'        // Status set manually by admin
}
