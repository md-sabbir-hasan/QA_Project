import { Injectable } from '@angular/core';
import { jwtDecode } from 'jwt-decode';
import { APP_CONFIG } from '../config/app.config';
import { JwtPayload } from '../models/jwt-payload.model';
import { StorageService } from './storage.service';

@Injectable({
  providedIn: 'root',
})
export class TokenService {
  constructor(private storage: StorageService) {}

  getAccessToken(): string | null {
    return this.storage.get<string>(APP_CONFIG.storageKeys.accessToken);
  }

  getRefreshToken(): string | null {
    return this.storage.get<string>(APP_CONFIG.storageKeys.refreshToken);
  }

  saveTokens(accessToken: string, refreshToken: string): void {
    this.storage.set(APP_CONFIG.storageKeys.accessToken, accessToken);
    this.storage.set(APP_CONFIG.storageKeys.refreshToken, refreshToken);
  }

  clearTokens(): void {
    this.storage.remove(APP_CONFIG.storageKeys.accessToken);
    this.storage.remove(APP_CONFIG.storageKeys.refreshToken);
  }

  isLoggedIn(): boolean {
    const token = this.getAccessToken();
    return !!token && !this.isTokenExpired(token);
  }

  decodeToken(token: string): JwtPayload | null {
    try {
      return jwtDecode<JwtPayload>(token);
    } catch {
      return null;
    }
  }

  getPermissions(): string[] {
    const token = this.getAccessToken();

    if (!token) {
      return [];
    }

    return this.decodeToken(token)?.permissions ?? [];
  }

  hasPermission(permission: string): boolean {
    return this.getPermissions().includes(permission);
  }

  isTokenExpired(token: string): boolean {
    const payload = this.decodeToken(token);

    if (!payload?.exp) {
      return true;
    }

    const expiryTime = payload.exp * 1000;
    return Date.now() >= expiryTime;
  }
}
