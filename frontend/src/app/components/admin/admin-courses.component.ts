import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CourseService, KursDto, CreateKursRequest } from '../../services/course.service';

@Component({
  selector: 'app-admin-courses',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-courses.component.html',
  styleUrl: './admin-courses.component.scss'
})
export class AdminCoursesComponent {
  private readonly courseService = inject(CourseService);

  protected kurse = signal<KursDto[]>([]);
  protected loading = signal(false);
  protected showForm = signal(false);
  protected editingId = signal<number | null>(null);
  protected message = signal('');
  protected messageType = signal('');

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
    this.formData = { ...kurs };
    this.showForm.set(true);
    this.message.set('');
  }

  protected onSubmit(): void {
    const request: CreateKursRequest = { ...this.formData };

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
  }
}
