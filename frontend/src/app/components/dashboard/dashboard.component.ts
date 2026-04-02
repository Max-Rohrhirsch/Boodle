import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService, UserDto } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  private readonly authService = inject(AuthService);

  protected readonly currentUser: UserDto | null = this.authService.getCurrentUser();
}
