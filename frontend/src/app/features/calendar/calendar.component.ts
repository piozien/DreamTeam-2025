import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { TaskService } from '../../shared/services/task.service';
import { Task, TaskPriority, TaskStatus } from '../../shared/models/task.model';
import { AuthService } from '../../shared/services/auth.service';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-calendar',
  templateUrl: './calendar.component.html',
  styleUrls: ['./calendar.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatTabsModule,
    MatDatepickerModule,
    MatNativeDateModule,
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatTooltipModule
  ]
})
export class CalendarComponent implements OnInit {
  // Current date for calendar
  currentDate: Date = new Date();
  
  // Calendar view type (month, week, day)
  currentView: 'month' | 'week' | 'day' = 'month';
  
  // Calendar data
  calendarDays: Day[] = [];
  weekDays: string[] = ['Pon', 'Wt', 'Śr', 'Czw', 'Pt', 'Sob', 'Niedz'];
  months: string[] = ['Styczeń', 'Luty', 'Marzec', 'Kwiecień', 'Maj', 'Czerwiec', 
                     'Lipiec', 'Sierpień', 'Wrzesień', 'Październik', 'Listopad', 'Grudzień'];
  
  selectedDay: Day | null = null;
  
  // User tasks
  userTasks: Task[] = [];
  loading = false;
  userId: string = '';
  
  // Priority colors for task display
  priorityColors = {
    [TaskPriority.OPTIONAL]: '#10b981',  // Green
    [TaskPriority.IMPORTANT]: '#f59e0b', // Orange
    [TaskPriority.CRITICAL]: '#ef4444'   // Red
  };
  
  // Status styling
  statusIcons = {
    [TaskStatus.TO_DO]: 'calendar_today',
    [TaskStatus.IN_PROGRESS]: 'hourglass_empty',
    [TaskStatus.FINISHED]: 'check_circle'
  };
  
  constructor(
    private taskService: TaskService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      if (user && user.id) {
        this.userId = user.id;
        this.loadUserTasks();
      }
    });
  }
  
  // Load tasks for the current user
  loadUserTasks(): void {
    if (!this.userId) return;
    
    this.loading = true;
    this.taskService.getUserTasks(this.userId).subscribe({
      next: (tasks) => {
        this.userTasks = tasks;
        this.generateCalendarDays();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading user tasks:', error);
        this.loading = false;
      }
    });
  }
  
  // Generate range of years for the year dropdown (current year +/- 2 years)
  getYearRange(): number[] {
    const currentYear = this.currentDate.getFullYear();
    return [
      currentYear - 2, 
      currentYear - 1, 
      currentYear, 
      currentYear + 1, 
      currentYear + 2
    ];
  }
  
  // Generate calendar days for the current month
  generateCalendarDays(): void {
    const year = this.currentDate.getFullYear();
    const month = this.currentDate.getMonth();
    
    // Get first day of month and adjust for Monday start (0=Monday, 6=Sunday)
    const firstDay = new Date(year, month, 1);
    let startDay = firstDay.getDay() - 1;
    if (startDay === -1) startDay = 6; // Adjust Sunday
    
    // Get last day of month
    const lastDay = new Date(year, month + 1, 0).getDate();
    
    // Get last day of previous month
    const prevMonthLastDay = new Date(year, month, 0).getDate();
    
    // Reset calendar days
    this.calendarDays = [];
    
    // Previous month days
    for (let i = startDay; i > 0; i--) {
      const day = prevMonthLastDay - i + 1;
      const date = new Date(year, month - 1, day);
      this.calendarDays.push({
        date,
        dayNumber: day,
        isCurrentMonth: false,
        events: this.getTasksForDate(date)
      });
    }
    
    // Current month days
    for (let i = 1; i <= lastDay; i++) {
      const date = new Date(year, month, i);
      this.calendarDays.push({
        date,
        dayNumber: i,
        isCurrentMonth: true,
        isToday: this.isToday(date),
        events: this.getTasksForDate(date)
      });
    }
    
    // Next month days (to complete the grid - always 6 rows of 7 days)
    const remainingDays = 42 - this.calendarDays.length; // 6 rows * 7 days = 42
    for (let i = 1; i <= remainingDays; i++) {
      const date = new Date(year, month + 1, i);
      this.calendarDays.push({
        date,
        dayNumber: i,
        isCurrentMonth: false,
        events: this.getTasksForDate(date)
      });
    }
  }
  
  // Get tasks for a specific date
  getTasksForDate(date: Date): CalendarEvent[] {
    if (!this.userTasks || this.userTasks.length === 0) {
      return [];
    }
    
    const dateString = this.formatDate(date);
    
    return this.userTasks
      .filter(task => {
        // Check if the task is on this date
        const taskStartDate = task.startDate ? task.startDate.substring(0, 10) : null;
        const taskEndDate = task.endDate ? task.endDate.substring(0, 10) : null;
        
        // If task has start date and end date, check if date is between them
        if (taskStartDate && taskEndDate) {
          return dateString >= taskStartDate && dateString <= taskEndDate;
        }
        
        // If task only has start date (no end date), show it only on the start date
        if (taskStartDate && !taskEndDate) {
          return dateString === taskStartDate;
        }
        
        return false;
      })
      .map(task => {
        // Check if this is the end date of the task
        const isEndDate = task.endDate ? task.endDate.substring(0, 10) === dateString : false;
        
        return {
          id: task.id,
          title: task.name,
          description: task.description || '',
          startDate: task.startDate,
          endDate: task.endDate,
          isEndDate: isEndDate,
          color: this.priorityColors[task.priority] || '#1976d2',
          task: task
        };
      });
  }
  
  // Format date to YYYY-MM-DD
  formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
  
  // Check if date is today
  isToday(date: Date): boolean {
    const today = new Date();
    return date.getDate() === today.getDate() &&
           date.getMonth() === today.getMonth() &&
           date.getFullYear() === today.getFullYear();
  }
  
  // Change view (month, week, day)
  changeView(view: 'month' | 'week' | 'day'): void {
    this.currentView = view;
  }
  
  // Navigate to previous month/week/day
  navigatePrevious(): void {
    switch (this.currentView) {
      case 'month':
        this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() - 1);
        break;
      case 'week':
        this.currentDate = new Date(this.currentDate.getTime() - 7 * 24 * 60 * 60 * 1000);
        break;
      case 'day':
        this.currentDate = new Date(this.currentDate.getTime() - 24 * 60 * 60 * 1000);
        break;
    }
    this.generateCalendarDays();
  }
  
  // Navigate to next month/week/day
  navigateNext(): void {
    switch (this.currentView) {
      case 'month':
        this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() + 1);
        break;
      case 'week':
        this.currentDate = new Date(this.currentDate.getTime() + 7 * 24 * 60 * 60 * 1000);
        break;
      case 'day':
        this.currentDate = new Date(this.currentDate.getTime() + 24 * 60 * 60 * 1000);
        break;
    }
    this.generateCalendarDays();
  }
  
  // Change month and year directly
  changeMonthYear(month: number, year: number): void {
    this.currentDate = new Date(year, month);
    this.generateCalendarDays();
  }
  
  // Select a specific day
  selectDay(day: Day): void {
    this.selectedDay = day;
    this.currentDate = new Date(day.date);
    this.currentView = 'day';
  }
  
  // Get current week days (for week view)
  getCurrentWeekDays(): Day[] {
    const date = new Date(this.currentDate);
    const day = date.getDay() || 7; // Get day of week (0 = Sunday, so convert to 7)
    const diff = date.getDate() - day + 1; // Adjust to Monday
    
    const weekDays: Day[] = [];
    for (let i = 0; i < 7; i++) {
      const currentDate = new Date(date);
      currentDate.setDate(diff + i);
      
      weekDays.push({
        date: currentDate,
        dayNumber: currentDate.getDate(),
        isCurrentMonth: currentDate.getMonth() === this.currentDate.getMonth(),
        isToday: this.isToday(currentDate),
        events: this.getTasksForDate(currentDate)
      });
    }
    
    return weekDays;
  }
  
  // Get the current month and year as a string (e.g., "May 2023")
  getCurrentMonthYear(): string {
    return `${this.months[this.currentDate.getMonth()]} ${this.currentDate.getFullYear()}`;
  }
  
  // Get hours for day view
  getHours(): string[] {
    return Array(24).fill(0).map((_, i) => `${i}:00`);
  }
  
  // Get events for a specific hour on a day (for day view)
  getEventsForHour(hour: number): CalendarEvent[] {
    if (!this.selectedDay) return [];
    
    return this.selectedDay.events.filter(event => {
      if (!event.startDate) return false;
      
      const eventDate = new Date(event.startDate);
      
      // If it's a regular event (not on end date), show it at its start hour
      if (!event.isEndDate && eventDate.getHours() === hour) {
        return true;
      }
      
      // If it's the end date, only show it at 23:00 (last hour of the day)
      if (event.isEndDate && hour === 23) {
        return true;
      }
      
      return false;
    });
  }
  
  // Get events for a specific day and hour (for week view)
  getEventsForDayHour(day: Day, hour: number): CalendarEvent[] {
    return day.events.filter(event => {
      if (!event.startDate) return false;
      
      const eventDate = new Date(event.startDate);
      
      // If it's a regular event (not on end date), show it at its start hour
      if (!event.isEndDate && eventDate.getHours() === hour) {
        return true;
      }
      
      // If it's the end date, only show it at 23:00 (last hour of the day)
      if (event.isEndDate && hour === 23) {
        return true;
      }
      
      return false;
    });
  }
  
  // Get status icon for a task
  getStatusIcon(status: TaskStatus): string {
    return this.statusIcons[status];
  }
  
  // Refresh calendar data
  refreshCalendar(): void {
    this.loadUserTasks();
  }
  
  // Get the current week range as a string (e.g., "12.05 - 18.05")
  getWeekRangeString(): string {
    const weekDays = this.getCurrentWeekDays();
    if (weekDays.length === 0) return '';
    
    const firstDay = weekDays[0].date;
    const lastDay = weekDays[weekDays.length - 1].date;
    
    const formatDay = (date: Date): string => {
      const day = date.getDate().toString().padStart(2, '0');
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      return `${day}.${month}`;
    };
    
    return `${formatDay(firstDay)} - ${formatDay(lastDay)}`;
  }
}

// Interface for a calendar day
interface Day {
  date: Date;
  dayNumber: number;
  isCurrentMonth?: boolean;
  isToday?: boolean;
  events: CalendarEvent[];
}

// Interface for a calendar event (task)
interface CalendarEvent {
  id: string;
  title: string;
  description: string;
  startDate: string;
  endDate?: string;
  isEndDate?: boolean;  // Flag to indicate if the current day is the end date of the task
  color: string;
  task?: Task;
} 