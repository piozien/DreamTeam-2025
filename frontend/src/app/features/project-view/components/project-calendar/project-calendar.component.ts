import { Component, OnInit, Input, OnChanges, SimpleChanges } from '@angular/core';
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
import { MatTooltipModule } from '@angular/material/tooltip';
import { TaskService } from '../../../../shared/services/task.service';
import { Task, TaskPriority, TaskStatus } from '../../../../shared/models/task.model';
import { AuthService } from '../../../../shared/services/auth.service';
import { Project } from '../../../../shared/models/project.model';

@Component({
  selector: 'app-project-calendar',
  templateUrl: './project-calendar.component.html',
  styleUrls: ['./project-calendar.component.scss'],
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
export class ProjectCalendarComponent implements OnInit, OnChanges {
  @Input() project: Project | null = null;
  @Input() projectTasks: Task[] = [];
  
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
        this.initializeCalendar();
      }
    });
  }
  
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['project'] || changes['projectTasks']) {
      this.initializeCalendar();
    }
  }
  
  initializeCalendar(): void {
    // If a project is available, set the current date to the project start date if it's in the future
    if (this.project) {
      const projectStartDate = new Date(this.project.startDate);
      const today = new Date();
      
      // Set calendar to project start date if it's in the future or to today if the project has already started
      if (projectStartDate > today) {
        this.currentDate = new Date(projectStartDate);
      }
      
      this.generateCalendarDays();
    }
  }
  
  // Generate range of years for the year dropdown (limited to project timeframe)
  getYearRange(): number[] {
    if (!this.project?.startDate) {
      return [];
    }

    const startYear = new Date(this.project.startDate).getFullYear();
    const endYear = this.project.endDate ? new Date(this.project.endDate).getFullYear() : startYear + 1;
    
    const years: number[] = [];
    for (let year = startYear; year <= endYear; year++) {
      years.push(year);
    }
    return years;
  }

  // Get available months for the selected year
  getAvailableMonths(): number[] {
    if (!this.project?.startDate) {
      return [];
    }

    const startDate = new Date(this.project.startDate);
    const endDate = this.project.endDate ? new Date(this.project.endDate) : new Date(startDate.getFullYear() + 1, 11, 31);
    const currentYear = this.currentDate.getFullYear();

    let startMonth = 0;
    let endMonth = 11;

    if (currentYear === startDate.getFullYear()) {
      startMonth = startDate.getMonth();
    }
    if (currentYear === endDate.getFullYear()) {
      endMonth = endDate.getMonth();
    }

    const months: number[] = [];
    for (let month = startMonth; month <= endMonth; month++) {
      months.push(month);
    }
    return months;
  }

  // Check if navigation to previous month/week/day is allowed
  canNavigatePrevious(): boolean {
    if (!this.project?.startDate) {
      return false;
    }

    const startDate = new Date(this.project.startDate);
    startDate.setHours(0, 0, 0, 0);
    
    let currentViewStart = new Date(this.currentDate);
    
    // Handle different view types differently
    switch (this.currentView) {
      case 'month':
        // For month view, check if first day of current month is after project start
        currentViewStart.setDate(1);
        break;
      case 'week':
        // For week view, check if start of current week is after project start
        const day = currentViewStart.getDay() || 7; // Get day of week (0 = Sunday, so convert to 7)
        const diff = currentViewStart.getDate() - day + 1; // Adjust to Monday
        currentViewStart.setDate(diff); // Start of week (Monday)
        break;
      case 'day':
        // For day view, just check the current date
        // currentViewStart is already set to the current date
        break;
    }
    
    currentViewStart.setHours(0, 0, 0, 0);
    
    // Allow navigation if the current view's start date is after project start date
    return currentViewStart > startDate;
  }

  // Check if navigation to next month/week/day is allowed
  canNavigateNext(): boolean {
    if (!this.project?.startDate) {
      return false;
    }

    const endDate = this.project.endDate ? new Date(this.project.endDate) : new Date(new Date(this.project.startDate).getFullYear() + 1, 11, 31);
    endDate.setHours(23, 59, 59, 999);

    let currentViewEnd = new Date(this.currentDate);
    
    // Handle different view types differently
    switch (this.currentView) {
      case 'month':
        // For month view, check if last day of current month is before project end
        currentViewEnd.setMonth(currentViewEnd.getMonth() + 1, 0); // Last day of current month
        break;
      case 'week':
        // For week view, check if end of current week is before project end
        const day = currentViewEnd.getDay() || 7; // Get day of week (0 = Sunday, so convert to 7)
        const diff = currentViewEnd.getDate() - day + 1; // Adjust to Monday
        currentViewEnd.setDate(diff + 6); // End of week (Sunday)
        break;
      case 'day':
        // For day view, just check the current date
        // currentViewEnd is already set to the current date
        break;
    }
    
    currentViewEnd.setHours(23, 59, 59, 999);
    
    // Allow navigation if the current view's end date is before project end date
    return currentViewEnd < endDate;
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
        isWithinProjectDates: this.isDateWithinProjectDates(date),
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
        isWithinProjectDates: this.isDateWithinProjectDates(date),
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
        isWithinProjectDates: this.isDateWithinProjectDates(date),
        events: this.getTasksForDate(date)
      });
    }
  }
  
  // Check if date is within project's start and end dates
  isDateWithinProjectDates(date: Date): boolean {
    if (!this.project) return true;
    
    const projectStart = new Date(this.project.startDate);
    projectStart.setHours(0, 0, 0, 0);
    
    // If project has no end date, then any date after start date is valid
    if (!this.project.endDate) {
      return date >= projectStart;
    }
    
    const projectEnd = new Date(this.project.endDate);
    projectEnd.setHours(23, 59, 59, 999);
    
    return date >= projectStart && date <= projectEnd;
  }
  
  // Get tasks for a specific date
  getTasksForDate(date: Date): CalendarEvent[] {
    if (!this.projectTasks || this.projectTasks.length === 0) {
      return [];
    }
    
    const dateString = this.formatDate(date);
    
    return this.projectTasks
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
          color: this.priorityColors[task.priority] || '#4caf50',
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
    
    // If switching to day view, make sure selectedDay is set
    if (view === 'day') {
      this.updateSelectedDay();
    }
  }
  
  // Update the selectedDay based on currentDate (for day view)
  updateSelectedDay(): void {
    // Create a day object for the current date with events
    const date = new Date(this.currentDate);
    const dayEvents = this.getTasksForDate(date);
    
    this.selectedDay = {
      date: date,
      dayNumber: date.getDate(),
      isCurrentMonth: true,
      isToday: this.isToday(date),
      isWithinProjectDates: this.isDateWithinProjectDates(date),
      events: dayEvents
    };
  }
  
  // Override the existing navigatePrevious method
  navigatePrevious(): void {
    if (!this.canNavigatePrevious()) {
      return;
    }

    switch (this.currentView) {
      case 'month':
        this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() - 1);
        break;
      case 'week':
        this.currentDate = new Date(this.currentDate.getTime() - 7 * 24 * 60 * 60 * 1000);
        break;
      case 'day':
        this.currentDate = new Date(this.currentDate.getTime() - 24 * 60 * 60 * 1000);
        // Update selectedDay when navigating in day view
        this.updateSelectedDay();
        break;
    }
    this.generateCalendarDays();
  }
  
  // Override the existing navigateNext method
  navigateNext(): void {
    if (!this.canNavigateNext()) {
      return;
    }

    switch (this.currentView) {
      case 'month':
        this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() + 1);
        break;
      case 'week':
        this.currentDate = new Date(this.currentDate.getTime() + 7 * 24 * 60 * 60 * 1000);
        break;
      case 'day':
        this.currentDate = new Date(this.currentDate.getTime() + 24 * 60 * 60 * 1000);
        // Update selectedDay when navigating in day view
        this.updateSelectedDay();
        break;
    }
    this.generateCalendarDays();
  }
  
  // Override the existing changeMonthYear method
  changeMonthYear(month: number, year: number): void {
    const newDate = new Date(year, month);
    
    // Check if the new date is within project bounds
    const startDate = new Date(this.project?.startDate || '');
    const endDate = this.project?.endDate ? new Date(this.project.endDate) : new Date(startDate.getFullYear() + 1, 11, 31);
    
    if (newDate >= startDate && newDate <= endDate) {
      this.currentDate = newDate;
      this.generateCalendarDays();
    }
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
        isWithinProjectDates: this.isDateWithinProjectDates(currentDate),
        events: this.getTasksForDate(currentDate)
      });
    }
    
    return weekDays;
  }
  
  // Get the current month and year as a string (e.g., "May 2023")
  getCurrentMonthYear(): string {
    return `${this.months[this.currentDate.getMonth()]} ${this.currentDate.getFullYear()}`;
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
      const eventEndDate = event.endDate ? new Date(event.endDate) : null;
      const selectedDayStr = this.formatDate(this.selectedDay!.date);
      const eventStartStr = this.formatDate(eventDate);
      const eventEndStr = eventEndDate ? this.formatDate(eventEndDate) : eventStartStr;
      
      // If selected day is the start date of the event
      if (selectedDayStr === eventStartStr) {
        // Show at and after the start hour
        return hour >= eventDate.getHours();
      }
      
      // If selected day is the end date of the event
      if (eventEndDate && selectedDayStr === eventEndStr) {
        // Show up to and including the end hour
        return hour <= eventEndDate.getHours();
      }
      
      // If selected day is in the middle of a multi-day event
      if (selectedDayStr > eventStartStr && (!eventEndDate || selectedDayStr < eventEndStr)) {
        // Show all hours for days in the middle
        return true;
      }
      
      return false;
    }).map(event => {
      // Clone the event to add flags for styling
      const newEvent = { ...event };
      const eventDate = new Date(event.startDate);
      const eventEndDate = event.endDate ? new Date(event.endDate) : null;
      const selectedDayStr = this.formatDate(this.selectedDay!.date);
      const eventStartStr = this.formatDate(eventDate);
      const eventEndStr = eventEndDate ? this.formatDate(eventEndDate) : eventStartStr;
      
      // Add isContinuation flag for styling
      newEvent.isContinuation = selectedDayStr > eventStartStr || (selectedDayStr === eventStartStr && hour > eventDate.getHours());
      
      // Flag the first hour of the event
      if (selectedDayStr === eventStartStr && hour === eventDate.getHours()) {
        newEvent.isStartOfDay = true;
      } else if (selectedDayStr > eventStartStr && hour === 0) {
        newEvent.isStartOfDay = true;
      } else {
        newEvent.isStartOfDay = false;
      }
      
      // Flag the last hour of the event
      if (eventEndDate && selectedDayStr === eventEndStr && hour === eventEndDate.getHours()) {
        newEvent.isEndOfDay = true;
      } else {
        newEvent.isEndOfDay = false;
      }
      
      return newEvent;
    });
  }
  
  // Get events for a specific day and hour (for week view)
  getEventsForDayHour(day: Day, hour: number): CalendarEvent[] {
    return day.events.filter(event => {
      if (!event.startDate) return false;
      
      const eventDate = new Date(event.startDate);
      const eventEndDate = event.endDate ? new Date(event.endDate) : null;
      const currentDayStr = this.formatDate(day.date);
      const eventStartStr = this.formatDate(eventDate);
      const eventEndStr = eventEndDate ? this.formatDate(eventEndDate) : eventStartStr;
      
      // For the first day of a task (start date)
      if (currentDayStr === eventStartStr) {
        // On start day, show at and after the start hour
        return hour >= eventDate.getHours();
      }
      
      // For the last day of a task (end date)
      if (eventEndDate && currentDayStr === eventEndStr) {
        // On end day, show up to and including the end hour
        return hour <= eventEndDate.getHours();
      }
      
      // For days in the middle of a multi-day task
      if (currentDayStr > eventStartStr && (!eventEndDate || currentDayStr < eventEndStr)) {
        // For middle days, show all hours (0-23)
        return true;
      }
      
      return false;
    }).map(event => {
      // Clone the event to add continuous flag
      const newEvent = { ...event };
      const eventDate = new Date(event.startDate);
      const eventEndDate = event.endDate ? new Date(event.endDate) : null;
      const currentDayStr = this.formatDate(day.date);
      const eventStartStr = this.formatDate(eventDate);
      const eventEndStr = eventEndDate ? this.formatDate(eventEndDate) : eventStartStr;
      
      // Add isContinuation flag to indicate if this is part of a multi-day task
      newEvent.isContinuation = currentDayStr > eventStartStr || (currentDayStr === eventStartStr && hour > eventDate.getHours());
      
      // Add isStartOfDay flag to identify the first hour of the task for a day
      if (currentDayStr === eventStartStr && hour === eventDate.getHours()) {
        // First hour on the start day
        newEvent.isStartOfDay = true;
      } else if (currentDayStr > eventStartStr && currentDayStr < eventEndStr && hour === 0) {
        // First hour (midnight) on middle days
        newEvent.isStartOfDay = true;
      } else if (eventEndDate && currentDayStr === eventEndStr && hour === 0) {
        // First hour (midnight) on the end day
        newEvent.isStartOfDay = true;
      } else {
        newEvent.isStartOfDay = false;
      }
      
      // Add isEndOfDay flag to identify the last hour of the task for a day
      if (eventEndDate && currentDayStr === eventEndStr && hour === eventEndDate.getHours()) {
        // Last hour on the end day
        newEvent.isEndOfDay = true;
      }
      
      return newEvent;
    });
  }
  
  // Get status icon for a task
  getStatusIcon(status: TaskStatus): string {
    return this.statusIcons[status];
  }
}

// Interface for a calendar day
interface Day {
  date: Date;
  dayNumber: number;
  isCurrentMonth?: boolean;
  isToday?: boolean;
  isWithinProjectDates?: boolean;
  events: CalendarEvent[];
}

// Interface for a calendar event (task)
interface CalendarEvent {
  id: string;
  title: string;
  description: string;
  startDate: string;
  endDate?: string;
  isEndDate?: boolean;
  isContinuation?: boolean; // Flag to indicate if this is a continuation of a multi-day task
  isStartOfDay?: boolean; // Flag to indicate if this is the first hour showing the task for this day
  isEndOfDay?: boolean; // Flag to indicate if this is the last hour showing the task for this day
  color: string;
  task?: Task;
}