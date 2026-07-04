import { Injectable } from '@angular/core';
import { jwtDecode } from 'jwt-decode';
import { STORAGE_KEYS } from '../constants/storage.constants';
import { JwtPayload } from '../models/jwt-payload.model';
import { StorageService } from './storage.service';

@Injectable({
  providedIn: 'root'
})
export class TokenService {

  constructor(private storage: StorageService) {}

  getAccessToken(): string | null {
    return this.storage.get<string>(STORAGE_KEYS.ACCESS_TOKEN);
  }

  getRefreshToken(): string | null {
    return this.storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN);
  }

  saveTokens(accessToken: string, refreshToken: string): void {
    this.storage.set(STORAGE_KEYS.ACCESS_TOKEN, accessToken);
    this.storage.set(STORAGE_KEYS.REFRESH_TOKEN, refreshToken);
  }

  clearTokens(): void {
    this.storage.remove(STORAGE_KEYS.ACCESS_TOKEN);
    this.storage.remove(STORAGE_KEYS.REFRESH_TOKEN);
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

    return Date.now() >= payload.exp * 1000;
  }
}