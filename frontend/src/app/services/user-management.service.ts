import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserDto {
  matr: string;
  name: string;
  email: string;
  rolle: string;
  createdAt: string;
  updatedAt: string;
}

export interface UserLookupDto {
  matr: string;
  name: string;
  email: string;
  rolle: string;
}

export interface CreateUserRequest {
  matr: string;
  name: string;
  password: string;
  email: string;
  rolle: string;
}

export interface UpdateUserRequest {
  name: string;
  email: string;
  rolle: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {
  constructor(private readonly http: HttpClient) {}

  getAllUsers(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>('/api/users');
  }

  getUserByMatr(matr: string): Observable<UserDto> {
    return this.http.get<UserDto>(`/api/users/${matr}`);
  }

  searchUsers(query: string, rolle?: string, limit = 10): Observable<UserLookupDto[]> {
    const params = new URLSearchParams();
    params.set('q', query);
    params.set('limit', String(limit));
    if (rolle) {
      params.set('rolle', rolle);
    }

    return this.http.get<UserLookupDto[]>(`/api/users/search?${params.toString()}`);
  }

  createUser(request: CreateUserRequest): Observable<UserDto> {
    return this.http.post<UserDto>('/api/users', request);
  }

  updateUser(matr: string, request: UpdateUserRequest): Observable<UserDto> {
    return this.http.put<UserDto>(`/api/users/${matr}`, request);
  }

  deleteUser(matr: string): Observable<void> {
    return this.http.delete<void>(`/api/users/${matr}`);
  }
}
