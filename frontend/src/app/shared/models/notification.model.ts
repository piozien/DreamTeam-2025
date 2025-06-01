/**
 * Frontend model that represents a notification in the system.
 * Matches the backend NotificationDTO structure.
 */
export interface Notification {
  id: string;          // UUID from backend
  message: string;     // Notification message content
  createdAt: string;   // ISO timestamp string
  isRead: boolean;     // Whether notification has been read
}
