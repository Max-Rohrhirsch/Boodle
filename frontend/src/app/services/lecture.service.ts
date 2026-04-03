import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface VorlesungLookupDto {
  id: number;
  code: string;
  name: string;
}

export interface KursLookupDto {
  id: number;
  name: string;
}

export interface UserLookupDto {
  matr: string;
  name: string;
  email: string;
  rolle: string;
}

export interface VorlesungDto {
  id: number;
  code: string;
  name: string;
  beschreibung: string;
  studiengang: string;
  dozentMatr: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateVorlesungRequest {
  code: string;
  name: string;
  beschreibung: string;
  studiengang: string;
  dozentMatr: string;
}

@Injectable({
  providedIn: 'root'
})
export class LectureService {
  constructor(private readonly http: HttpClient) {}

  getAllVorlesungen(): Observable<VorlesungDto[]> {
    return this.http.get<VorlesungDto[]>('/api/vorlesungen');
  }

  getVorlesungById(id: number): Observable<VorlesungDto> {
    return this.http.get<VorlesungDto>(`/api/vorlesungen/${id}`);
  }

  searchVorlesungen(query: string, limit = 10): Observable<VorlesungLookupDto[]> {
    const params = new URLSearchParams();
    params.set('q', query);
    params.set('limit', String(limit));
    return this.http.get<VorlesungLookupDto[]>(`/api/vorlesungen/search?${params.toString()}`);
  }

  getKurseForVorlesung(id: number): Observable<KursLookupDto[]> {
    return this.http.get<KursLookupDto[]>(`/api/vorlesungen/${id}/kurse`);
  }

  getEnrolledStudents(id: number): Observable<UserLookupDto[]> {
    return this.http.get<UserLookupDto[]>(`/api/vorlesungen/${id}/students/details`);
  }

  enrollStudent(id: number, studentMatr: string): Observable<void> {
    return this.http.post<void>(`/api/vorlesungen/${id}/enroll`, { studentMatr });
  }

  unenrollStudent(id: number, studentMatr: string): Observable<void> {
    return this.http.delete<void>(`/api/vorlesungen/${id}/enroll/${studentMatr}`);
  }

  createVorlesung(request: CreateVorlesungRequest): Observable<VorlesungDto> {
    return this.http.post<VorlesungDto>('/api/vorlesungen', request);
  }

  updateVorlesung(id: number, request: CreateVorlesungRequest): Observable<VorlesungDto> {
    return this.http.put<VorlesungDto>(`/api/vorlesungen/${id}`, request);
  }

  deleteVorlesung(id: number): Observable<void> {
    return this.http.delete<void>(`/api/vorlesungen/${id}`);
  }
}
