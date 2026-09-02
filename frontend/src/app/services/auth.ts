import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
}

interface RegisterResponse {
  id: string;
  email: string;
  role: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  readonly token = signal<string | null>(localStorage.getItem('token'));

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', { email, password })
      .pipe(tap(res => this.setToken(res.token)));
  }

  register(email: string, password: string): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>('/api/auth/register', { email, password });
  }

  logout() {
    this.token.set(null);
    localStorage.removeItem('token');
  }

  isLoggedIn(): boolean {
    return this.token() !== null;
  }

  private setToken(token: string) {
    this.token.set(token);
    localStorage.setItem('token', token);
  }
}
