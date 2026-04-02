import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreateUserRequest {
  matr: string;
  name: string;
  password: string;
  email: string;
  rolle: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {
  constructor(private readonly http: HttpClient) {}

  getAllUsers(): Observable<any[]> {
    return this.http.get<any[]>('/api/users');
  }

  getUserByMatr(matr: string): Observable<any> {
    return this.http.get<any>(`/api/users/${matr}`);
  }

  createUser(request: CreateUserRequest): Observable<any> {
    return this.http.post<any>('/api/users', request);
  }

  updateUser(matr: string, request: any): Observable<any> {
    return this.http.put<any>(`/api/users/${matr}`, request);
  }

  deleteUser(matr: string): Observable<void> {
    return this.http.delete<void>(`/api/users/${matr}`);
  }
}
