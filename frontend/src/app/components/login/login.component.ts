import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, LoginResponse } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
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