import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <header class="header" *ngIf="shouldShowHeader()">
      <div class="header-inner">
        <a class="brand" [routerLink]="brandLink()">Boodle</a>

        <nav class="nav">
          <a routerLink="/dashboard" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Dashboard</a>
        </nav>

        <div class="user-actions">
          <span class="user-name">{{ authService.getCurrentUser()?.name }}</span>
          <button type="button" (click)="onLogout()">Logout</button>
        </div>
      </div>
    </header>
  `,
  styles: [`
    .header {
      border-bottom: 1px solid #d9dfdc;
      background: #ffffff;
      position: sticky;
      top: 0;
      z-index: 10;
    }

    .header-inner {
      max-width: 1000px;
      margin: 0 auto;
      padding: 12px 16px;
      display: flex;
      align-items: center;
      gap: 24px;
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    }

    .brand {
      text-decoration: none;
      color: #2D332F;
      font-size: 20px;
      font-weight: 700;
      letter-spacing: 0.3px;
      flex-shrink: 0;
    }

    .nav {
      display: flex;
      gap: 12px;
    }

    .nav a {
      text-decoration: none;
      color: #2D332F;
      padding: 6px 10px;
      border-radius: 6px;
      font-weight: 500;
      font-size: 14px;
    }

    .nav a:hover {
      background: #eafdf2;
    }

    .nav a.active {
      background: #35CF78;
      color: #1f2521;
    }

    .user-actions {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-left: auto;
      flex-shrink: 0;
    }

    .user-name {
      color: #2D332F;
      font-size: 14px;
      max-width: 180px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .user-actions button {
      border: 1px solid #2D332F;
      background: #35CF78;
      color: #2D332F;
      padding: 7px 12px;
      border-radius: 6px;
      font-weight: 600;
      font-size: 14px;
      cursor: pointer;
      transition: background-color 0.2s ease;
    }

    .user-actions button:hover {
      background: #5DF991;
    }

    @media (max-width: 640px) {
      .header-inner {
        gap: 12px;
      }

      .nav {
        order: 3;
        width: 100%;
        margin: 0;
      }

      .user-name {
        max-width: 120px;
      }
    }
  `]
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

  protected onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
