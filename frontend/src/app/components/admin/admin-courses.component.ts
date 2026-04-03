import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CourseService, KursDto, CreateKursRequest, VorlesungLookupDto } from '../../services/course.service';
import { LectureService } from '../../services/lecture.service';
import { UserLookupDto, UserManagementService } from '../../services/user-management.service';

@Component({
  selector: 'app-admin-courses',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-courses.component.html',
  styleUrl: './admin-courses.component.scss'
})
export class AdminCoursesComponent {
  private readonly courseService = inject(CourseService);
  private readonly lectureService = inject(LectureService);
  private readonly userService = inject(UserManagementService);

  protected kurse = signal<KursDto[]>([]);
  protected loading = signal(false);
  protected showForm = signal(false);
  protected editingId = signal<number | null>(null);
  protected managingCourseId = signal<number | null>(null);
  protected message = signal('');
  protected messageType = signal('');
  protected dozentSuggestions = signal<UserLookupDto[]>([]);
  protected kurssprecherSuggestions = signal<UserLookupDto[]>([]);
  protected lectureSuggestions = signal<VorlesungLookupDto[]>([]);
  protected assignedLectures = signal<VorlesungLookupDto[]>([]);
  protected studentSuggestions = signal<UserLookupDto[]>([]);
  protected assignedStudents = signal<UserLookupDto[]>([]);
  protected lectureQuery = '';
  protected studentQuery = '';

  protected formData: any = {
    name: '',
    dozentMatr: '',
    kurssprecher1Matr: '',
    kurssprecher2Matr: ''
  };

  constructor() {
    this.loadKurse();
  }

  private loadKurse(): void {
    this.loading.set(true);
    this.courseService.getAllKurse().subscribe({
      next: (data) => {
        this.kurse.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.message.set('Fehler beim Laden der Kurse.');
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

  protected onEdit(kurs: KursDto): void {
    this.editingId.set(kurs.id);
    this.formData = {
      name: kurs.name,
      dozentMatr: kurs.dozentMatr,
      kurssprecher1Matr: kurs.kurssprecher1Matr ?? '',
      kurssprecher2Matr: kurs.kurssprecher2Matr ?? ''
    };
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

  protected onKurssprecherInput(value: string): void {
    if (!value.trim()) {
      this.kurssprecherSuggestions.set([]);
      return;
    }

    this.userService.searchUsers(value, 'KURSSPRECHER').subscribe({
      next: (data) => this.kurssprecherSuggestions.set(data),
      error: () => this.kurssprecherSuggestions.set([])
    });
  }

  protected onLectureSearch(value: string): void {
    this.lectureQuery = value;
    if (!value.trim()) {
      this.lectureSuggestions.set([]);
      return;
    }

    this.lectureService.searchVorlesungen(value).subscribe({
      next: (data) => this.lectureSuggestions.set(data),
      error: () => this.lectureSuggestions.set([])
    });
  }

  protected openLectureManagement(kursId: number): void {
    if (this.managingCourseId() === kursId) {
      this.managingCourseId.set(null);
      this.assignedLectures.set([]);
      this.assignedStudents.set([]);
      this.lectureSuggestions.set([]);
      this.studentSuggestions.set([]);
      this.lectureQuery = '';
      this.studentQuery = '';
      return;
    }

    this.managingCourseId.set(kursId);
    this.loadAssignedLectures(kursId);
    this.loadAssignedStudents(kursId);
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

  protected assignLectureToCurrentCourse(vorlesungId: number): void {
    const kursId = this.managingCourseId();
    if (!kursId) {
      return;
    }

    this.courseService.assignLectureToKurs(kursId, vorlesungId).subscribe({
      next: () => {
        this.message.set('Vorlesung dem Kurs zugeordnet.');
        this.messageType.set('success');
        this.loadAssignedLectures(kursId);
      },
      error: (err) => {
        this.message.set('Zuordnung fehlgeschlagen: ' + (err.error?.message || 'Unbekannter Fehler'));
        this.messageType.set('error');
      }
    });
  }

  protected removeLectureFromCurrentCourse(vorlesungId: number): void {
    const kursId = this.managingCourseId();
    if (!kursId) {
      return;
    }

    this.courseService.removeLectureFromKurs(kursId, vorlesungId).subscribe({
      next: () => {
        this.message.set('Vorlesung vom Kurs entfernt.');
        this.messageType.set('success');
        this.loadAssignedLectures(kursId);
      },
      error: (err) => {
        this.message.set('Entfernen fehlgeschlagen: ' + (err.error?.message || 'Unbekannter Fehler'));
        this.messageType.set('error');
      }
    });
  }

  protected enrollStudentToCurrentCourse(studentMatr: string): void {
    const kursId = this.managingCourseId();
    if (!kursId) {
      return;
    }

    this.courseService.enrollStudentToKurs(kursId, studentMatr).subscribe({
      next: () => {
        this.message.set('Student dem Kurs zugeordnet.');
        this.messageType.set('success');
        this.loadAssignedStudents(kursId);
      },
      error: (err) => {
        this.message.set('Zuordnung fehlgeschlagen: ' + (err.error?.message || 'Unbekannter Fehler'));
        this.messageType.set('error');
      }
    });
  }

  protected removeStudentFromCurrentCourse(studentMatr: string): void {
    const kursId = this.managingCourseId();
    if (!kursId) {
      return;
    }

    this.courseService.unenrollStudentFromKurs(kursId, studentMatr).subscribe({
      next: () => {
        this.message.set('Student aus Kurs entfernt.');
        this.messageType.set('success');
        this.loadAssignedStudents(kursId);
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

  protected displayLectureSuggestion(lecture: VorlesungLookupDto): string {
    return `${lecture.code} - ${lecture.name}`;
  }

  protected displayStudentSuggestion(student: UserLookupDto): string {
    return `${student.name} (${student.matr})`;
  }

  protected onSubmit(): void {
    const request: CreateKursRequest = {
      name: this.formData.name.trim(),
      dozentMatr: this.formData.dozentMatr.trim(),
      kurssprecher1Matr: (this.formData.kurssprecher1Matr ?? '').trim() || undefined,
      kurssprecher2Matr: (this.formData.kurssprecher2Matr ?? '').trim() || undefined
    };

    if (this.editingId()) {
      this.courseService.updateKurs(this.editingId()!, request).subscribe({
        next: () => {
          this.message.set('Kurs erfolgreich aktualisiert.');
          this.messageType.set('success');
          this.resetForm();
          this.loadKurse();
        },
        error: (err) => {
          this.message.set('Fehler beim Aktualisieren: ' + (err.error?.message || 'Unbekannter Fehler'));
          this.messageType.set('error');
        }
      });
    } else {
      this.courseService.createKurs(request).subscribe({
        next: () => {
          this.message.set('Kurs erfolgreich erstellt.');
          this.messageType.set('success');
          this.resetForm();
          this.loadKurse();
        },
        error: (err) => {
          this.message.set('Fehler beim Erstellen: ' + (err.error?.message || 'Ungültige Eingabe'));
          this.messageType.set('error');
        }
      });
    }
  }

  protected onDelete(id: number): void {
    if (confirm('Sicher, dass du diesen Kurs löschen möchtest?')) {
      this.courseService.deleteKurs(id).subscribe({
        next: () => {
          this.message.set('Kurs erfolgreich gelöscht.');
          this.messageType.set('success');
          this.loadKurse();
        },
        error: () => {
          this.message.set('Fehler beim Löschen des Kurses.');
          this.messageType.set('error');
        }
      });
    }
  }

  private resetForm(): void {
    this.formData = {
      name: '',
      dozentMatr: '',
      kurssprecher1Matr: '',
      kurssprecher2Matr: ''
    };
    this.editingId.set(null);
    this.showForm.set(false);
    this.dozentSuggestions.set([]);
    this.kurssprecherSuggestions.set([]);
    this.studentSuggestions.set([]);
  }

  private loadAssignedLectures(kursId: number): void {
    this.courseService.getLecturesForKurs(kursId).subscribe({
      next: (data) => this.assignedLectures.set(data),
      error: () => this.assignedLectures.set([])
    });
  }

  private loadAssignedStudents(kursId: number): void {
    this.courseService.getStudentsForKurs(kursId).subscribe({
      next: (data) => this.assignedStudents.set(data),
      error: () => this.assignedStudents.set([])
    });
  }
}
