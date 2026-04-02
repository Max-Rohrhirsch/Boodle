import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
