import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LectureService, VorlesungDto } from '../../services/lecture.service';
import { ScheduleService, RegulaereStundeDto, UnregulaereStundeDto } from '../../services/schedule.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dozent-schedule',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dozent-schedule.component.html',
  styleUrl: './dozent-schedule.component.scss'
})
export class DozentScheduleComponent {
  private readonly lectureService = inject(LectureService);
  private readonly scheduleService = inject(ScheduleService);
  private readonly authService = inject(AuthService);

  protected loading = signal(false);
  protected message = signal('');
  protected ownLectures = signal<VorlesungDto[]>([]);
  protected selectedLectureId = signal<number | null>(null);
  protected regularSlots = signal<RegulaereStundeDto[]>([]);
  protected irregularSlots = signal<UnregulaereStundeDto[]>([]);

  private readonly ownDozentMatr = this.authService.getCurrentUser()?.matr ?? '';

  constructor() {
    this.loadOwnLectures();
  }

  protected selectLecture(vorlesungId: number): void {
    this.selectedLectureId.set(vorlesungId);
    this.loadSchedule(vorlesungId);
  }

  private loadOwnLectures(): void {
    this.loading.set(true);
    this.lectureService.getAllVorlesungen().subscribe({
      next: (vorlesungen) => {
        const own = vorlesungen.filter((v) => v.dozentMatr === this.ownDozentMatr);
        this.ownLectures.set(own);
        if (own.length > 0) {
          this.selectLecture(own[0].id);
        }
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Vorlesungen konnten nicht geladen werden.');
        this.loading.set(false);
      }
    });
  }

  private loadSchedule(vorlesungId: number): void {
    this.scheduleService.getRegularByLecture(vorlesungId).subscribe({
      next: (data) => this.regularSlots.set(data),
      error: () => this.regularSlots.set([])
    });

    this.scheduleService.getIrregularByLecture(vorlesungId).subscribe({
      next: (data) => this.irregularSlots.set(data),
      error: () => this.irregularSlots.set([])
    });
  }
}
