import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, LoginResponse } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <h1>Boodle</h1>
        <h2>Login</h2>
        
        <form (ngSubmit)="onLogin()">
          <div class="form-group">
            <label for="email">Email:</label>
            <input
              id="email"
              type="email"
              name="email"
              [(ngModel)]="email"
              placeholder="Enter your email"
              required
              [disabled]="loading()"
            />
          </div>

          <div class="form-group">
            <label for="password">Password:</label>
            <input
              id="password"
              type="password"
              name="password"
              [(ngModel)]="password"
              placeholder="Enter your password"
              required
              [disabled]="loading()"
            />
          </div>

          <button type="submit" [disabled]="loading() || !isFormValid()">
            {{ loading() ? 'Logging in...' : 'Login' }}
          </button>
        </form>

        <div *ngIf="errorMessage()" class="error-message">
          {{ errorMessage() }}
        </div>

        <div *ngIf="successMessage()" class="success-message">
          {{ successMessage() }}
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: #ffffff;
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      padding: 24px;
    }

    .login-card {
      background: #f3f3f3;
      padding: 36px;
      border-radius: 12px;
      border: 1px solid #e4e4e4;
      width: 100%;
      max-width: 400px;
    }

    h1 {
      text-align: center;
      margin-bottom: 10px;
      color: #222;
      font-size: 30px;
    }

    h2 {
      text-align: center;
      margin-bottom: 30px;
      color: #333;
      font-size: 19px;
      font-weight: 500;
    }

    .form-group {
      margin-bottom: 20px;
    }

    label {
      display: block;
      margin-bottom: 8px;
      color: #333;
      font-weight: 500;
    }

    input {
      width: 100%;
      padding: 12px;
      border: 1px solid #cfcfcf;
      border-radius: 5px;
      font-size: 14px;
      transition: border-color 0.3s;
      box-sizing: border-box;
      background: #fff;
    }

    input:focus {
      outline: none;
      border-color: #6d6d6d;
      box-shadow: none;
    }

    input:disabled {
      background-color: #f5f5f5;
      cursor: not-allowed;
    }

    button {
      width: 100%;
      padding: 12px;
      background: #35CF78;
      color: #2D332F;
      border: 1px solid #2D332F;
      border-radius: 8px;
      font-size: 16px;
      font-weight: 600;
      cursor: pointer;
      transition: background-color 0.2s ease, transform 0.2s ease;
    }

    button:hover:not(:disabled) {
      background: #5DF991;
      transform: translateY(-2px);
    }

    button:disabled {
      background: #9bb5a5;
      color: #2D332F;
      border-color: #6f7d73;
      opacity: 0.6;
      cursor: not-allowed;
    }

    .error-message {
      margin-top: 15px;
      padding: 12px;
      background-color: #ffecec;
      border: 1px solid #ffc5c5;
      border-radius: 5px;
      color: #c33;
      font-size: 14px;
    }

    .success-message {
      margin-top: 15px;
      padding: 12px;
      background-color: #ebffef;
      border: 1px solid #bfecc7;
      border-radius: 5px;
      color: #157a2f;
      font-size: 14px;
    }
  `]
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected email = '';
  protected password = '';
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');

  protected isFormValid(): boolean {
    return this.email.trim().length > 0 && this.password.length > 0;
  }

  protected onLogin(): void {
    if (!this.isFormValid()) {
      this.errorMessage.set('Please fill in both email and password.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.authService.login(this.email, this.password).subscribe({
      next: (response: LoginResponse) => {
        this.authService.setToken(response.token);
        this.authService.setCurrentUser(response.user);
        this.successMessage.set(`Login successful for ${response.user.name}. Redirecting...`);
        
        // Navigate to dashboard after a short delay for UX
        setTimeout(() => {
          this.router.navigate(['/dashboard']);
        }, 800);
      },
      error: (error) => {
        console.error('Login error:', error);
        this.errorMessage.set('Login failed. Please check your credentials and try again.');
        this.loading.set(false);
      }
    });
  }
}