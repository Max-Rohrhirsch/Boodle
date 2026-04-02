import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';

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
  private readonly userStorageKey = 'boodle.user';
  
  private currentUserSubject = new BehaviorSubject<UserDto | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private readonly http: HttpClient) {
    // Load user from storage on init
    const stored = localStorage.getItem(this.userStorageKey);
    if (stored) {
      try {
        this.currentUserSubject.next(JSON.parse(stored));
      } catch (e) {
        console.error('Failed to parse stored user', e);
      }
    }
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', { email, password });
  }

  setToken(token: string): void {
    localStorage.setItem(this.storageKey, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.storageKey);
  }

  setCurrentUser(user: UserDto): void {
    localStorage.setItem(this.userStorageKey, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  getCurrentUser(): UserDto | null {
    return this.currentUserSubject.value;
  }

  getCurrentUserRole(): string | null {
    return this.currentUserSubject.value?.rolle ?? null;
  }

  isAuthenticated(): boolean {
    return this.getToken() !== null && this.getCurrentUser() !== null;
  }

  logout(): void {
    localStorage.removeItem(this.storageKey);
    localStorage.removeItem(this.userStorageKey);
    this.currentUserSubject.next(null);
  }

  clearToken(): void {
    localStorage.removeItem(this.storageKey);
  }
}