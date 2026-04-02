import { JsonPipe, NgIf } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

import { AuthService, LoginResponse, UserDto } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, NgIf, JsonPipe],
  template: `
    <main>
      <form (ngSubmit)="onLogin()">
        <input
          type="email"
          name="email"
          [(ngModel)]="email"
          placeholder="Email"
          required
        /><br>
        <input
          type="password"
          name="password"
          [(ngModel)]="password"
          placeholder="Password"
          required
        /><br>
        <button type="submit" [disabled]="loading()">Login</button>
      </form>

      <p *ngIf="message()">{{ message() }}</p>
      <p *ngIf="jwtToken()">JWT: {{ jwtToken() }}</p>
      <pre *ngIf="users()">{{ users() | json }}</pre>
    </main>
  `
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);

  protected email = '';
  protected password = '';
  protected readonly message = signal('');
  protected readonly jwtToken = signal<string | null>(null);
  protected readonly users = signal<UserDto[] | null>(null);
  protected readonly loading = signal(false);

  protected onLogin(): void {
    this.loading.set(true);
    this.message.set('');
    this.jwtToken.set(null);
    this.users.set(null);

    this.authService.login(this.email, this.password).subscribe({
      next: (response: LoginResponse) => {
        this.authService.setToken(response.token);
        this.jwtToken.set(response.token);
        this.message.set(`Login ok for ${response.user.email}. Loading protected data...`);

        this.http.get<UserDto[]>('/api/users').subscribe({
          next: (users) => {
            this.users.set(users);
            this.message.set(`Loaded ${users.length} users.`);
            this.loading.set(false);
          },
          error: () => {
            this.message.set('Token stored, but GET /api/users failed.');
            this.loading.set(false);
          }
        });
      },
      error: () => {
        this.message.set('Login failed.');
        this.loading.set(false);
      }
    });
  }
}