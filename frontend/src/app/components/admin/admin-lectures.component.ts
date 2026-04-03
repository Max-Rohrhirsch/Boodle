import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LectureService, VorlesungDto, CreateVorlesungRequest, KursLookupDto, UserLookupDto } from '../../services/lecture.service';
import { CourseService } from '../../services/course.service';
import { UserManagementService } from '../../services/user-management.service';

@Component({
  selector: 'app-admin-lectures',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-lectures.component.html',
  styleUrl: './admin-lectures.component.scss'
})
export class AdminLecturesComponent {
  private readonly lectureService = inject(LectureService);
  private readonly courseService = inject(CourseService);
  private readonly userService = inject(UserManagementService);

  protected vorlesungen = signal<VorlesungDto[]>([]);
  protected loading = signal(false);
  protected showForm = signal(false);
  protected editingId = signal<number | null>(null);
  protected managingLectureId = signal<number | null>(null);
  protected message = signal('');
  protected messageType = signal('');
  protected dozentSuggestions = signal<UserLookupDto[]>([]);
  protected courseSuggestions = signal<KursLookupDto[]>([]);
  protected studentSuggestions = signal<UserLookupDto[]>([]);
  protected assignedCourses = signal<KursLookupDto[]>([]);
  protected enrolledStudents = signal<UserLookupDto[]>([]);
  protected courseQuery = '';
  protected studentQuery = '';

  protected formData = {
    code: '',
    name: '',
    beschreibung: '',
    studiengang: '',
    dozentMatr: ''
  };

  constructor() {
    this.loadVorlesungen();
  }

  private loadVorlesungen(): void {
    this.loading.set(true);
    this.lectureService.getAllVorlesungen().subscribe({
      next: (data) => {
        this.vorlesungen.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Fehler beim Laden der Vorlesungen.');
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

  protected onEdit(v: VorlesungDto): void {
    this.editingId.set(v.id);
    this.formData = { ...v };
    this.showForm.set(true);
    this.message.set('');
  }

  protected onDozentInput(value: string): void {
    this.formData.dozentMatr = value;
    if (!value.trim()) {
      this.dozentSuggestions.set([]);
      return;
    }

    this.userService.searchUsers(value, 'DOZENT').subscribe({
      next: (data) => this.dozentSuggestions.set(data),
      error: () => this.dozentSuggestions.set([])
    });
  }

  protected openManagement(lectureId: number): void {
    if (this.managingLectureId() === lectureId) {
      this.managingLectureId.set(null);
      this.assignedCourses.set([]);
      this.enrolledStudents.set([]);
      this.courseSuggestions.set([]);
      this.studentSuggestions.set([]);
      this.courseQuery = '';
      this.studentQuery = '';
      return;
    }

    this.managingLectureId.set(lectureId);
    this.loadLectureManagementData(lectureId);
  }

  protected onCourseSearch(value: string): void {
    this.courseQuery = value;
    if (!value.trim()) {
      this.courseSuggestions.set([]);
      return;
    }

    this.courseService.searchKurse(value).subscribe({
      next: (data) => this.courseSuggestions.set(data),
      error: () => this.courseSuggestions.set([])
    });
  }

  protected onStudentSearch(value: string): void {
    this.studentQuery = value;
    if (!value.trim()) {
      this.studentSuggestions.set([]);
      return;
    }

    this.userService.searchUsers(value, 'STUDENT').subscribe({
      next: (data) => this.studentSuggestions.set(data),
      error: () => this.studentSuggestions.set([])
    });
  }

  protected assignCourseToCurrentLecture(kursId: number): void {
    const lectureId = this.managingLectureId();
    if (!lectureId) {
      return;
    }

    this.courseService.assignLectureToKurs(kursId, lectureId).subscribe({
      next: () => {
        this.message.set('Kurs der Vorlesung zugeordnet.');
        this.messageType.set('success');
        this.loadLectureManagementData(lectureId);
      },
      error: (err) => {
        this.message.set('Zuordnung fehlgeschlagen: ' + (err.error?.message || 'Unbekannter Fehler'));
        this.messageType.set('error');
      }
    });
  }

  protected removeCourseFromCurrentLecture(kursId: number): void {
    const lectureId = this.managingLectureId();
    if (!lectureId) {
      return;
    }

    this.courseService.removeLectureFromKurs(kursId, lectureId).subscribe({
      next: () => {
        this.message.set('Kurs von der Vorlesung entfernt.');
        this.messageType.set('success');
        this.loadLectureManagementData(lectureId);
      },
      error: (err) => {
        this.message.set('Entfernen fehlgeschlagen: ' + (err.error?.message || 'Unbekannter Fehler'));
        this.messageType.set('error');
      }
    });
  }

  protected enrollStudentToCurrentLecture(studentMatr: string): void {
    const lectureId = this.managingLectureId();
    if (!lectureId) {
      return;
    }

    this.lectureService.enrollStudent(lectureId, studentMatr).subscribe({
      next: () => {
        this.message.set('Student eingeschrieben.');
        this.messageType.set('success');
        this.loadLectureManagementData(lectureId);
      },
      error: (err) => {
        this.message.set('Einschreiben fehlgeschlagen: ' + (err.error?.message || 'Unbekannter Fehler'));
        this.messageType.set('error');
      }
    });
  }

  protected unenrollStudentFromCurrentLecture(studentMatr: string): void {
    const lectureId = this.managingLectureId();
    if (!lectureId) {
      return;
    }

    this.lectureService.unenrollStudent(lectureId, studentMatr).subscribe({
      next: () => {
        this.message.set('Student entfernt.');
        this.messageType.set('success');
        this.loadLectureManagementData(lectureId);
      },
      error: (err) => {
        this.message.set('Entfernen fehlgeschlagen: ' + (err.error?.message || 'Unbekannter Fehler'));
        this.messageType.set('error');
      }
    });
  }

  protected displayUserSuggestion(user: UserLookupDto): string {
    return `${user.name} (${user.matr})`;
  }

  protected displayCourseSuggestion(kurs: KursLookupDto): string {
    return `${kurs.name} (#${kurs.id})`;
  }

  protected onSubmit(): void {
    const request: CreateVorlesungRequest = { ...this.formData };

    if (this.editingId()) {
      this.lectureService.updateVorlesung(this.editingId()!, request).subscribe({
        next: () => {
          this.message.set('Vorlesung erfolgreich aktualisiert.');
          this.messageType.set('success');
          this.resetForm();
          this.loadVorlesungen();
        },
        error: (err) => {
          this.message.set('Fehler beim Aktualisieren: ' + (err.error?.message || 'Unbekannter Fehler'));
          this.messageType.set('error');
        }
      });
    } else {
      this.lectureService.createVorlesung(request).subscribe({
        next: () => {
          this.message.set('Vorlesung erfolgreich erstellt.');
          this.messageType.set('success');
          this.resetForm();
          this.loadVorlesungen();
        },
        error: (err) => {
          this.message.set('Fehler beim Erstellen: ' + (err.error?.message || 'Ungültige Eingabe'));
          this.messageType.set('error');
        }
      });
    }
  }

  protected onDelete(id: number): void {
    if (confirm('Sicher, dass du diese Vorlesung löschen möchtest?')) {
      this.lectureService.deleteVorlesung(id).subscribe({
        next: () => {
          this.message.set('Vorlesung erfolgreich gelöscht.');
          this.messageType.set('success');
          this.loadVorlesungen();
        },
        error: () => {
          this.message.set('Fehler beim Löschen der Vorlesung.');
          this.messageType.set('error');
        }
      });
    }
  }

  private resetForm(): void {
    this.formData = {
      code: '',
      name: '',
      beschreibung: '',
      studiengang: '',
      dozentMatr: ''
    };
    this.editingId.set(null);
    this.showForm.set(false);
    this.dozentSuggestions.set([]);
  }

  private loadLectureManagementData(lectureId: number): void {
    this.lectureService.getKurseForVorlesung(lectureId).subscribe({
      next: (data) => this.assignedCourses.set(data),
      error: () => this.assignedCourses.set([])
    });

    this.lectureService.getEnrolledStudents(lectureId).subscribe({
      next: (data) => this.enrolledStudents.set(data),
      error: () => this.enrolledStudents.set([])
    });
  }
}
