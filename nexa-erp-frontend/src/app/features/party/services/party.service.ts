import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { Party, PartyType } from '../models/party.model';

@Injectable({
  providedIn: 'root',
})
export class PartyService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/parties`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<Party[]>> {
    return this.http.get<ApiResponse<Party[]>>(this.baseUrl);
  }

  getByType(type: PartyType): Observable<ApiResponse<Party[]>> {
    return this.http.get<ApiResponse<Party[]>>(`${this.baseUrl}/type/${type}`);
  }
}
