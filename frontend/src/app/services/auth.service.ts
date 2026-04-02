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

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserDto;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly storageKey = 'boodle.jwt';

  constructor(private readonly http: HttpClient) {}

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', { email, password });
  }

  setToken(token: string): void {
    localStorage.setItem(this.storageKey, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.storageKey);
  }

  clearToken(): void {
    localStorage.removeItem(this.storageKey);
  }
}