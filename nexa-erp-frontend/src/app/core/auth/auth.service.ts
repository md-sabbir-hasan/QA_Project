import { HttpClient } from '@angular/common/http';
import { Injectable, WritableSignal, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { APP_CONFIG } from '../config/app.config';
import { ApiResponse } from '../models/api-response.model';
import {
  ForgotPasswordRequest,
  LoginRequest,
  LoginResponse,
  RefreshTokenRequest,
  ResetPasswordRequest,
  SetPasswordRequest,
} from '../models/auth.model';
import { CurrentUser } from '../models/current-user.model';
import { StorageService } from '../services/storage.service';
import { TokenService } from '../services/token.service';
import { STORAGE_KEYS } from '../constants/storage.constants';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/auth`;

  private readonly currentUserSignal: WritableSignal<CurrentUser | null>;

  readonly currentUser;
  readonly isLoggedIn;

  constructor(
    private http: HttpClient,
    private tokenService: TokenService,
    private storage: StorageService,
  ) {
    this.currentUserSignal = signal<CurrentUser | null>(
      this.storage.get<CurrentUser>(STORAGE_KEYS.CURRENT_USER),
    );

    this.currentUser = this.currentUserSignal.asReadonly();
    this.isLoggedIn = computed(() => !!this.tokenService.getAccessToken());
  }

  login(request: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>(`${this.baseUrl}/login`, request).pipe(
      tap((response) => {
        const data = response.data;

        this.tokenService.saveTokens(data.accessToken, data.refreshToken);

        const user: CurrentUser = {
          id: data.userId,
          name: data.name,
          email: data.email,
          status: 'ACTIVE',
          roles: [],
          permissions: [],
        };

        this.storage.set(STORAGE_KEYS.CURRENT_USER, user);
        this.currentUserSignal.set(user);
      }),
    );
  }

  refreshToken(): Observable<ApiResponse<LoginResponse>> {
    const request: RefreshTokenRequest = {
      refreshToken: this.tokenService.getRefreshToken() ?? '',
    };

    return this.http.post<ApiResponse<LoginResponse>>(`${this.baseUrl}/refresh`, request).pipe(
      tap((response) => {
        this.tokenService.saveTokens(response.data.accessToken, response.data.refreshToken);
      }),
    );
  }

  logout(): Observable<ApiResponse<null>> {
    return this.http
      .post<ApiResponse<null>>(`${this.baseUrl}/logout`, {})
      .pipe(tap(() => this.clearSession()));
  }

  clearSession(): void {
    this.tokenService.clearTokens();
    this.storage.remove(STORAGE_KEYS.CURRENT_USER);
    this.currentUserSignal.set(null);
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/forgot-password`, request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/reset-password`, request);
  }

  setPassword(request: SetPasswordRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/set-password`, request);
  }

  validateInvite(token: string): Observable<ApiResponse<unknown>> {
    return this.http.get<ApiResponse<unknown>>(`${this.baseUrl}/validate-invite?token=${token}`);
  }
}
