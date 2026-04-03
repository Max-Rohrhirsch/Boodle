import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserManagementService, CreateUserRequest, UpdateUserRequest, UserDto } from '../../services/user-management.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss'
})
export class AdminUsersComponent {
  private readonly userService = inject(UserManagementService);

  protected users = signal<UserDto[]>([]);
  protected loading = signal(false);
  protected showForm = signal(false);
  protected editingMatr = signal<string | null>(null);
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
    if (this.editingMatr()) {
      const request: UpdateUserRequest = {
        name: this.formData.name,
        email: this.formData.email,
        rolle: this.formData.rolle
      };

      this.userService.updateUser(this.editingMatr()!, request).subscribe({
        next: () => {
          this.message.set('User erfolgreich aktualisiert.');
          this.messageType.set('success');
          this.resetForm();
          this.loadUsers();
        },
        error: (err) => {
          this.message.set('Fehler beim Aktualisieren: ' + (err.error?.message || 'Ungültige Eingabe'));
          this.messageType.set('error');
        }
      });
      return;
    }

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

  protected onEdit(user: UserDto): void {
    this.editingMatr.set(user.matr);
    this.formData = {
      matr: user.matr,
      name: user.name,
      email: user.email,
      password: '',
      rolle: user.rolle
    };
    this.showForm.set(true);
    this.message.set('');
  }

  protected onDelete(matr: string): void {
    if (!confirm(`Sicher, dass du den User ${matr} löschen möchtest?`)) {
      return;
    }

    this.userService.deleteUser(matr).subscribe({
      next: () => {
        this.message.set('User erfolgreich gelöscht.');
        this.messageType.set('success');
        if (this.editingMatr() === matr) {
          this.resetForm();
        }
        this.loadUsers();
      },
      error: (err) => {
        this.message.set('Fehler beim Löschen: ' + (err.error?.message || 'Unbekannter Fehler'));
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
    this.editingMatr.set(null);
    this.showForm.set(false);
  }
}
