import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserLookupDto } from './user-management.service';

export interface KursLookupDto {
  id: number;
  name: string;
}

export interface VorlesungLookupDto {
  id: number;
  code: string;
  name: string;
}

export interface KursDto {
  id: number;
  name: string;
  dozentMatr: string;
  kurssprecher1Matr?: string;
  kurssprecher2Matr?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateKursRequest {
  name: string;
  dozentMatr: string;
  kurssprecher1Matr?: string;
  kurssprecher2Matr?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CourseService {
  constructor(private readonly http: HttpClient) {}

  getAllKurse(): Observable<KursDto[]> {
    return this.http.get<KursDto[]>('/api/kurse');
  }

  getKursById(id: number): Observable<KursDto> {
    return this.http.get<KursDto>(`/api/kurse/${id}`);
  }

  searchKurse(query: string, limit = 10): Observable<KursLookupDto[]> {
    const params = new URLSearchParams();
    params.set('q', query);
    params.set('limit', String(limit));
    return this.http.get<KursLookupDto[]>(`/api/kurse/search?${params.toString()}`);
  }

  getLecturesForKurs(kursId: number): Observable<VorlesungLookupDto[]> {
    return this.http.get<VorlesungLookupDto[]>(`/api/kurse/${kursId}/vorlesungen`);
  }

  getStudentsForKurs(kursId: number): Observable<UserLookupDto[]> {
    return this.http.get<UserLookupDto[]>(`/api/kurse/${kursId}/students`);
  }

  assignLectureToKurs(kursId: number, vorlesungId: number): Observable<void> {
    return this.http.post<void>(`/api/kurse/${kursId}/vorlesungen/${vorlesungId}`, {});
  }

  removeLectureFromKurs(kursId: number, vorlesungId: number): Observable<void> {
    return this.http.delete<void>(`/api/kurse/${kursId}/vorlesungen/${vorlesungId}`);
  }

  enrollStudentToKurs(kursId: number, studentMatr: string): Observable<void> {
    return this.http.post<void>(`/api/kurse/${kursId}/students`, { studentMatr });
  }

  unenrollStudentFromKurs(kursId: number, studentMatr: string): Observable<void> {
    return this.http.delete<void>(`/api/kurse/${kursId}/students/${studentMatr}`);
  }

  createKurs(request: CreateKursRequest): Observable<KursDto> {
    return this.http.post<KursDto>('/api/kurse', request);
  }

  updateKurs(id: number, request: CreateKursRequest): Observable<KursDto> {
    return this.http.put<KursDto>(`/api/kurse/${id}`, request);
  }

  deleteKurs(id: number): Observable<void> {
    return this.http.delete<void>(`/api/kurse/${id}`);
  }
}
