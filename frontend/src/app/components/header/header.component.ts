import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected shouldShowHeader(): boolean {
    return this.router.url !== '/login' && this.authService.isAuthenticated();
  }

  protected brandLink(): string[] {
    return this.authService.isAuthenticated() ? ['/dashboard'] : ['/login'];
  }

  protected isAdmin(): boolean {
    return this.authService.getCurrentUserRole() === 'ADMIN';
  }

  protected onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
