import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LectureService, VorlesungDto, CreateVorlesungRequest } from '../../services/lecture.service';

@Component({
  selector: 'app-admin-lectures',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-lectures.component.html',
  styleUrl: './admin-lectures.component.scss'
})
export class AdminLecturesComponent {
  private readonly lectureService = inject(LectureService);

  protected vorlesungen = signal<VorlesungDto[]>([]);
  protected loading = signal(false);
  protected showForm = signal(false);
  protected editingId = signal<number | null>(null);
  protected message = signal('');
  protected messageType = signal('');

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
  }
}
