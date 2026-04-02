import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserManagementService, CreateUserRequest } from '../../services/user-management.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss'
})
export class AdminUsersComponent {
  private readonly userService = inject(UserManagementService);

  protected users = signal<any[]>([]);
  protected loading = signal(false);
  protected showForm = signal(false);
  protected message = signal('');
  protected messageType = signal('');

  protected formData = {
    matr: '',
    name: '',
    email: '',
    password: '',
    rolle: ''
  };

  constructor() {
    this.loadUsers();
  }

  private loadUsers(): void {
    this.loading.set(true);
    this.userService.getAllUsers().subscribe({
      next: (data) => {
        this.users.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Fehler beim Laden der User.');
        this.messageType.set('error');
        this.loading.set(false);
      }
    });
  }

  protected toggleForm(): void {
    this.showForm.update(val => !val);
    if (!this.showForm()) {
      this.resetForm();
    }
  }

  protected onSubmit(): void {
    const request: CreateUserRequest = { ...this.formData };

    this.userService.createUser(request).subscribe({
      next: () => {
        this.message.set('User erfolgreich erstellt.');
        this.messageType.set('success');
        this.resetForm();
        this.loadUsers();
      },
      error: (err) => {
        this.message.set('Fehler beim Erstellen: ' + (err.error?.message || 'Ungültige Eingabe'));
        this.messageType.set('error');
      }
    });
  }

  private resetForm(): void {
    this.formData = {
      matr: '',
      name: '',
      email: '',
      password: '',
      rolle: ''
    };
    this.showForm.set(false);
  }
}
