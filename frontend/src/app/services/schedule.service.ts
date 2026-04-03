import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RegulaereStundeDto {
  id: number;
  vorlesungId: number;
  vortragsnummer: number;
  vonUhrzeit: string;
  bisUhrzeit: string;
  wochentag: string;
  raumId: number | null;
  vonDatum: string;
  bisDatum: string;
  online: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UnregulaereStundeDto {
  id: number;
  vorlesungId: number;
  status: string;
  vortragsnummer: number;
  alteVortragsnummer: number | null;
  vonUhrzeit: string;
  bisUhrzeit: string;
  datum: string;
  raumId: number | null;
  online: boolean;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class ScheduleService {
  constructor(private readonly http: HttpClient) {}

  getRegularByLecture(vorlesungId: number): Observable<RegulaereStundeDto[]> {
    return this.http.get<RegulaereStundeDto[]>(`/api/stundenplan/vorlesung/${vorlesungId}/regulaer`);
  }

  getIrregularByLecture(vorlesungId: number): Observable<UnregulaereStundeDto[]> {
    return this.http.get<UnregulaereStundeDto[]>(`/api/stundenplan/vorlesung/${vorlesungId}/unregulaer`);
  }
}
