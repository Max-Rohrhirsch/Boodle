import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService, UserDto } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard-container">
      <h1>Dashboard</h1>

      <div *ngIf="currentUser; else notLoggedIn" class="user-info">
        <p class="greeting">Hallo {{ currentUser.name }}.</p>

        <div class="info-row">
          <span class="label">Matrikelnummer</span>
          <span class="value">{{ currentUser.matr }}</span>
        </div>

        <div class="info-row">
          <span class="label">E-Mail</span>
          <span class="value">{{ currentUser.email }}</span>
        </div>

        <div class="info-row">
          <span class="label">Rolle</span>
          <span class="value">{{ currentUser.rolle | uppercase }}</span>
        </div>

        <div class="info-row">
          <span class="label">Registriert</span>
          <span class="value">{{ currentUser.createdAt | date: 'short' }}</span>
        </div>
      </div>

      <ng-template #notLoggedIn>
        <p>Nicht eingeloggt. Bitte zuerst anmelden.</p>
      </ng-template>
    </div>
  `,
  styles: [`
    .dashboard-container {
      padding: 24px;
      max-width: 720px;
      margin: 0 auto;
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      color: #1f1f1f;
    }

    h1 {
      font-size: 30px;
      font-weight: 600;
      margin: 0 0 20px;
    }

    .greeting {
      margin: 0 0 16px;
      font-size: 18px;
    }

    .user-info {
      border-top: 1px solid #e5e5e5;
      padding-top: 16px;
    }

    .info-row {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      padding: 10px 0;
      border-bottom: 1px solid #efefef;
    }

    .label {
      color: #666;
      font-size: 14px;
    }

    .value {
      font-size: 14px;
      font-weight: 600;
      color: #222;
    }

    p {
      margin: 0;
      font-size: 14px;
      color: #333;
    }

    @media (max-width: 600px) {
      .info-row {
        flex-direction: column;
      }
    }
  `]
})
export class DashboardComponent {
  private readonly authService = inject(AuthService);

  protected readonly currentUser: UserDto | null = this.authService.getCurrentUser();
}
