import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
