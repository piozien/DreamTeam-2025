export interface LoginResponseDTO {
  token: string;
  id: string; // UUID represented as string
  email: string;
  name: string; // Concatenated first and last name
  username: string;
}
